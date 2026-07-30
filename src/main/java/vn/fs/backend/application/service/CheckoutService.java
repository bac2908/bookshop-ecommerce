package vn.fs.backend.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.backend.application.dto.CheckoutAddressDTO;
import vn.fs.backend.domain.model.CartItem;
import vn.fs.backend.domain.model.Order;
import vn.fs.backend.domain.model.OrderDetail;
import vn.fs.backend.domain.model.OrderPayment;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.OrderDetailRepository;
import vn.fs.backend.infrastructure.persistence.OrderPaymentRepository;
import vn.fs.backend.infrastructure.persistence.OrderRepository;
import vn.fs.backend.infrastructure.persistence.ProductRepository;
import vn.fs.backend.application.service.impl.ExchangeRateService;
import vn.fs.backend.application.service.paypal.PaypalOrdersV2Service;
import vn.fs.backend.shared.util.Utils;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final ShoppingCartService cart;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final OrderPaymentRepository paymentRepo;
    private final ExchangeRateService exchangeRateService;
    private final PaypalOrdersV2Service paypalV2;
    private final OrderConfirmationMailService orderConfirmationMailService;

    public static final String DRAFT_KEY = "orderDraft";

    /** v2: Tạo PayPal Order → trả approve URL */
    public String beginPaypalCheckout(User user, CheckoutAddressDTO dto, HttpServletRequest req, HttpSession session)
            throws Exception {

        // Snapshot giỏ hàng + tỷ giá
        double totalVnd = cart.getAmount();
        double rate = exchangeRateService.getVNDExchangeRate();
        BigDecimal usdAmount = BigDecimal.valueOf(totalVnd / rate).setScale(2, RoundingMode.HALF_UP);

        // Lưu draft (địa chỉ, phone) + snapshot vào session
        Order draft = draftFrom(dto);
        draft.setAmount(totalVnd);
        session.setAttribute(DRAFT_KEY, draft);
        session.setAttribute("exchangeRate", rate);
        session.setAttribute("usdAmount", usdAmount.doubleValue());

        String base = Utils.getBaseURL(req);
        if (!base.endsWith("/")) base = base + "/";
        String successUrl = base + "pay/success"; // PayPal v2 sẽ trả về token=orderId
        String cancelUrl  = base + "pay/cancel";

        var created = paypalV2.createOrder(usdAmount, "USD", successUrl, cancelUrl);
        session.setAttribute("orderId_pp_hint", created.orderId); // optional debug

        return created.approveUrl;
    }

    /** v2: Sau khi approve → capture + ghi Order/OrderDetails/order_payments */
    @Transactional
    public Long finalizePaypalV2(User user, String orderId, HttpSession session) throws Exception {
        // Idempotent: nếu đã có payment cho orderId này → trả về luôn
        var existedPay = paymentRepo.findByProviderAndExternalOrderId("PAYPAL", orderId);
        if (existedPay.isPresent()) {
            return existedPay.get().getOrder().getOrderId();
        }

        // Capture (thu tiền thật)
        var cap = paypalV2.captureOrder(orderId); // COMPLETED, có captureId + payerEmail

        // Lấy snapshot đã lưu
        Order draft = (Order) session.getAttribute(DRAFT_KEY);
        if (draft == null) draft = new Order();

        double totalVnd = cart.getAmount();
        Double rate = (Double) session.getAttribute("exchangeRate");
        Double usdAmount = (Double) session.getAttribute("usdAmount");
        if (rate == null || usdAmount == null) {
            rate = exchangeRateService.getVNDExchangeRate();
            usdAmount = BigDecimal.valueOf(totalVnd / rate).setScale(2, RoundingMode.HALF_UP).doubleValue();
        }

        // ====== LƯU captureId vào đơn (trực tiếp qua trường public của cap) ======
        draft.setPaypalCaptureId(cap.captureId);

        // Ghi Order (PAID)
        draft.setUser(user);
        draft.setOrderDate(new Date());
        draft.setStatus(2); // 2 = ĐÃ THANH TOÁN
        draft.setAmount(totalVnd);
        orderRepository.save(draft);

        // Ghi chi tiết + trừ kho an toàn
        for (CartItem ci : cart.getCartItems()) {
            OrderDetail od = new OrderDetail();
            od.setOrder(draft);
            od.setProduct(ci.getProduct());
            od.setQuantity(ci.getQuantity());
            od.setPrice(ci.getProduct().getPrice());
            orderDetailRepository.save(od);

            int updated = productRepository.decreaseStock(ci.getProduct().getProductId(), ci.getQuantity());
            if (updated == 0) {
                throw new IllegalStateException("Không đủ tồn kho cho productId=" + ci.getProduct().getProductId());
            }
        }

        // Ghi bản ghi thanh toán (order_payments)
        OrderPayment pay = new OrderPayment();
        pay.setOrder(draft);
        pay.setProvider("PAYPAL");
        pay.setMethod("PAYPAL_BALANCE"); // optional
        pay.setExternalOrderId(orderId);
        pay.setExternalCaptureId(cap.captureId);
        pay.setCurrency("USD");
        pay.setAmount(BigDecimal.valueOf(usdAmount));
        pay.setExchangeRate(BigDecimal.valueOf(rate));
        pay.setPayerEmail(cap.payerEmail);
        pay.setStatus("COMPLETED");
        pay.setPaymentTime(new Date());
        paymentRepo.save(pay);

        // Email (không rollback đơn nếu lỗi)
        try {
            orderConfirmationMailService.send(
                    user.getEmail(),
                    "Book-Shop Xác Nhận Đơn hàng",
                    "CONFIRM",
                    cart.getCartItems(),
                    totalVnd,
                    draft
            );
        } catch (MessagingException ignore) {}

        // Clear session/cart
        cart.clear();
        session.removeAttribute("cartItems");
        session.removeAttribute(DRAFT_KEY);
        session.removeAttribute("exchangeRate");
        session.removeAttribute("usdAmount");
        session.removeAttribute("orderId_pp_hint");

        return draft.getOrderId();
    }

    /* ==================== COD giữ nguyên ==================== */

    @Transactional
    public Long placeOrderCOD(User user, CheckoutAddressDTO dto, HttpSession session) throws MessagingException {
        Order order = createOrderSkeleton(user, dto, 0, cart.getAmount());
        persistOrderDetailsAndDecreaseStockOrThrow(order);
        afterPersistSuccess(user, order, session);
        return order.getOrderId();
    }

    private void afterPersistSuccess(User user, Order order, HttpSession session) throws MessagingException {
        Collection<CartItem> items = cart.getCartItems();
        double total = cart.getAmount();

        orderConfirmationMailService.send(
                user.getEmail(),
                "Book-Shop Xác Nhận Đơn hàng",
                "CONFIRM",
                items,
                total,
                order
        );

        cart.clear();
        session.removeAttribute("cartItems");
        session.removeAttribute(DRAFT_KEY);
    }

    private Order createOrderSkeleton(User user, CheckoutAddressDTO dto, int status, double total) {
        Order order = new Order();
        order.setOrderDate(new Date());
        order.setStatus(status);
        order.setAmount(total);
        order.setUser(user);
        order.setAddress(dto.getAddress());
        order.setPhone(dto.getPhone());
        return orderRepository.save(order);
    }

    private void persistOrderDetailsAndDecreaseStockOrThrow(Order order) {
        for (CartItem ci : cart.getCartItems()) {
            OrderDetail od = new OrderDetail();
            od.setQuantity(ci.getQuantity());
            od.setOrder(order);
            od.setProduct(ci.getProduct());
            od.setPrice(ci.getProduct().getPrice());
            orderDetailRepository.save(od);

            int updated = productRepository.decreaseStock(
                    ci.getProduct().getProductId(), ci.getQuantity());
            if (updated == 0) {
                throw new IllegalStateException(
                        "Không đủ tồn kho cho productId=" + ci.getProduct().getProductId());
            }
        }
    }

    private Order draftFrom(CheckoutAddressDTO dto) {
        Order o = new Order();
        o.setAddress(dto.getAddress());
        o.setPhone(dto.getPhone());
        return o;
    }
}
