package vn.fs.backend.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import vn.fs.backend.application.dto.CartSnapshot;
import vn.fs.backend.application.dto.CheckoutAddressDTO;
import vn.fs.backend.application.dto.CheckoutResult;
import vn.fs.backend.application.error.CartChangedDuringCheckoutException;
import vn.fs.backend.application.error.EmptyCartException;
import vn.fs.backend.application.error.InsufficientStockException;
import vn.fs.backend.application.error.InvalidOrderStateException;
import vn.fs.backend.application.error.OrderNotFoundException;
import vn.fs.backend.application.error.ProductUnavailableException;
import vn.fs.backend.application.service.impl.ExchangeRateService;
import vn.fs.backend.application.service.paypal.PaypalOrdersV2Service;
import vn.fs.backend.domain.model.CartItem;
import vn.fs.backend.domain.model.Order;
import vn.fs.backend.domain.model.OrderDetail;
import vn.fs.backend.domain.model.OrderPayment;
import vn.fs.backend.domain.model.OrderStatus;
import vn.fs.backend.domain.model.Product;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.OrderDetailRepository;
import vn.fs.backend.infrastructure.persistence.OrderPaymentRepository;
import vn.fs.backend.infrastructure.persistence.OrderRepository;
import vn.fs.backend.infrastructure.persistence.ProductRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;
import vn.fs.backend.shared.util.Utils;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private static final String PAYPAL = "PAYPAL";

    private final ShoppingCartService cart;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderPaymentRepository paymentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final ExchangeRateService exchangeRateService;
    private final PaypalOrdersV2Service paypalV2;
    private final OrderConfirmationMailService orderConfirmationMailService;
    private final PlatformTransactionManager transactionManager;

    public CheckoutResult placeOrderCOD(
            User user,
            CheckoutAddressDTO address,
            HttpSession session) {
        CheckoutResult result = inTransaction(() -> {
            User lockedUser = lockUser(user);
            CartSnapshot snapshot = requireCart();
            Order order = reserveInventoryAndCreateOrder(
                    lockedUser,
                    address,
                    OrderStatus.PENDING_CONFIRMATION,
                    snapshot);
            cart.clear();
            scheduleConfirmationEmail(lockedUser, order, snapshot);
            return new CheckoutResult(
                    order.getOrderId(),
                    order.getOrderStatus(),
                    "COD",
                    null,
                    null);
        });
        cleanupSession(session);
        return result;
    }

    public CheckoutResult beginPaypalCheckout(
            User user,
            CheckoutAddressDTO address,
            HttpServletRequest request,
            HttpSession session) throws Exception {
        CartSnapshot initialSnapshot = requireCart();
        String fingerprint = fingerprint(initialSnapshot);

        double exchangeRate = exchangeRateService.getVNDExchangeRate();
        if (exchangeRate <= 0) {
            throw new IllegalStateException("Tỷ giá thanh toán không hợp lệ");
        }
        BigDecimal usdAmount = BigDecimal
                .valueOf(initialSnapshot.totalAmount() / exchangeRate)
                .setScale(2, RoundingMode.HALF_UP);

        String baseUrl = Utils.getBaseURL(request);
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        var external = paypalV2.createOrder(
                usdAmount,
                "USD",
                baseUrl + "pay/success",
                baseUrl + "pay/cancel");

        CheckoutResult result = inTransaction(() -> {
            User lockedUser = lockUser(user);
            CartSnapshot currentSnapshot = requireCart();
            if (!fingerprint.equals(fingerprint(currentSnapshot))) {
                throw new CartChangedDuringCheckoutException();
            }

            Order order = reserveInventoryAndCreateOrder(
                    lockedUser,
                    address,
                    OrderStatus.AWAITING_PAYMENT,
                    currentSnapshot);

            OrderPayment payment = new OrderPayment();
            payment.setOrder(order);
            payment.setProvider(PAYPAL);
            payment.setMethod("PAYPAL");
            payment.setExternalOrderId(external.orderId);
            payment.setCurrency("USD");
            payment.setAmount(usdAmount);
            payment.setExchangeRate(BigDecimal.valueOf(exchangeRate));
            payment.setStatus("PENDING");
            payment.setCreatedAt(new Date());
            paymentRepository.save(payment);

            cart.clear();
            return new CheckoutResult(
                    order.getOrderId(),
                    order.getOrderStatus(),
                    PAYPAL,
                    external.approveUrl,
                    external.orderId);
        });
        cleanupSession(session);
        return result;
    }

    public CheckoutResult finalizePaypalV2(
            User user,
            String externalOrderId,
            HttpSession session) throws Exception {
        PaymentSnapshot beforeCapture = paymentSnapshotForUser(
                user,
                externalOrderId);
        if ("COMPLETED".equalsIgnoreCase(beforeCapture.paymentStatus())) {
            Order order = orderService.findOwnedById(
                    beforeCapture.orderId(),
                    user.getUserId());
            return paypalResult(order, externalOrderId);
        }
        if (!"PENDING".equalsIgnoreCase(beforeCapture.paymentStatus())) {
            throw new InvalidOrderStateException(
                    "Thanh toán PayPal không còn ở trạng thái chờ capture",
                    beforeCapture.orderStatus());
        }

        var capture = paypalV2.captureOrder(externalOrderId);
        CheckoutResult result = inTransaction(() -> {
            Order order = orderRepository.findOwnedByIdForUpdate(
                            beforeCapture.orderId(),
                            user.getUserId())
                    .orElseThrow(() -> new OrderNotFoundException(
                            beforeCapture.orderId()));
            OrderPayment payment = paymentRepository.findByExternalOrderIdForUpdate(
                            PAYPAL,
                            externalOrderId)
                    .orElseThrow(() -> new OrderNotFoundException(
                            beforeCapture.orderId()));

            if ("COMPLETED".equalsIgnoreCase(payment.getStatus())) {
                return paypalResult(order, externalOrderId);
            }
            if (order.getOrderStatus() != OrderStatus.AWAITING_PAYMENT) {
                throw new InvalidOrderStateException(
                        "Đơn hàng không còn chờ thanh toán PayPal",
                        order.getOrderStatus());
            }

            order.setOrderStatus(OrderStatus.COMPLETED);
            order.setPaypalCaptureId(capture.captureId);
            orderRepository.save(order);

            payment.setExternalCaptureId(capture.captureId);
            payment.setPayerEmail(capture.payerEmail);
            payment.setStatus("COMPLETED");
            payment.setPaymentTime(new Date());
            payment.setUpdatedAt(new Date());
            paymentRepository.save(payment);

            CartSnapshot purchasedItems = snapshotFromOrder(order);
            scheduleConfirmationEmail(order.getUser(), order, purchasedItems);
            return paypalResult(order, externalOrderId);
        });
        cleanupSession(session);
        return result;
    }

    public CheckoutResult cancelPaypalCheckout(
            User user,
            String externalOrderId,
            HttpSession session) {
        PaymentSnapshot payment = paymentSnapshotForUser(user, externalOrderId);
        Order cancelled = orderService.cancelOwned(
                payment.orderId(),
                user.getUserId());
        cleanupSession(session);
        return new CheckoutResult(
                cancelled.getOrderId(),
                cancelled.getOrderStatus(),
                PAYPAL,
                null,
                externalOrderId);
    }

    private Order reserveInventoryAndCreateOrder(
            User user,
            CheckoutAddressDTO address,
            OrderStatus status,
            CartSnapshot snapshot) {
        Order order = new Order();
        order.setOrderDate(new Date());
        order.setOrderStatus(status);
        order.setAmount(snapshot.totalAmount());
        order.setUser(user);
        order.setAddress(address.getAddress().trim());
        order.setPhone(address.getPhone().trim());
        order.setNote(address.getNote() == null ? null : address.getNote().trim());
        orderRepository.save(order);

        for (CartItem item : snapshot.items()) {
            Product product = item.getProduct();
            int updated = productRepository.decreaseStock(
                    product.getProductId(),
                    item.getQuantity());
            if (updated == 0) {
                Product current = productRepository.findById(product.getProductId())
                        .orElseThrow(() -> new ProductUnavailableException(
                                product.getProductId()));
                if (!Boolean.TRUE.equals(current.getStatus())) {
                    throw new ProductUnavailableException(product.getProductId());
                }
                throw new InsufficientStockException(
                        product.getProductId(),
                        item.getQuantity(),
                        Math.max(0, current.getQuantity()));
            }

            OrderDetail detail = new OrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(item.getQuantity());
            detail.setPrice(item.getUnitPrice());
            orderDetailRepository.save(detail);
        }
        return order;
    }

    private PaymentSnapshot paymentSnapshotForUser(
            User user,
            String externalOrderId) {
        if (externalOrderId == null || externalOrderId.isBlank()) {
            throw new OrderNotFoundException(null);
        }
        return inTransaction(() -> {
            OrderPayment payment = paymentRepository.findByProviderAndExternalOrderId(
                            PAYPAL,
                            externalOrderId)
                    .orElseThrow(() -> new OrderNotFoundException(null));
            Order order = payment.getOrder();
            if (order.getUser() == null
                    || !Objects.equals(
                            order.getUser().getUserId(),
                            user.getUserId())) {
                throw new OrderNotFoundException(order.getOrderId());
            }
            return new PaymentSnapshot(
                    order.getOrderId(),
                    order.getOrderStatus(),
                    payment.getStatus());
        });
    }

    private CartSnapshot snapshotFromOrder(Order order) {
        List<CartItem> items = orderDetailRepository.findByOrderId(
                        order.getOrderId())
                .stream()
                .map(detail -> {
                    CartItem item = new CartItem();
                    item.setId(detail.getProduct().getProductId());
                    item.setName(detail.getProduct().getProductName());
                    item.setProduct(detail.getProduct());
                    item.setQuantity(detail.getQuantity());
                    item.setUnitPrice(detail.getPrice());
                    return item;
                })
                .toList();
        int totalQuantity = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
        return new CartSnapshot(
                items,
                items.size(),
                totalQuantity,
                order.getAmount());
    }

    private User lockUser(User user) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalStateException("Người dùng không tồn tại");
        }
        return userRepository.findByIdForUpdate(user.getUserId())
                .orElseThrow(() -> new IllegalStateException(
                        "Người dùng không tồn tại"));
    }

    private CartSnapshot requireCart() {
        CartSnapshot snapshot = cart.getSnapshot();
        if (snapshot.items().isEmpty() || snapshot.totalQuantity() == 0) {
            throw new EmptyCartException();
        }
        return snapshot;
    }

    private String fingerprint(CartSnapshot snapshot) {
        return snapshot.items().stream()
                .sorted(Comparator.comparing(CartItem::getId))
                .map(item -> item.getId()
                        + ":" + item.getQuantity()
                        + ":" + BigDecimal.valueOf(item.getUnitPrice())
                                .setScale(2, RoundingMode.HALF_UP))
                .reduce((left, right) -> left + "|" + right)
                .orElse("");
    }

    private CheckoutResult paypalResult(Order order, String externalOrderId) {
        return new CheckoutResult(
                order.getOrderId(),
                order.getOrderStatus(),
                PAYPAL,
                null,
                externalOrderId);
    }

    private void scheduleConfirmationEmail(
            User user,
            Order order,
            CartSnapshot snapshot) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        try {
                            orderConfirmationMailService.send(
                                    user.getEmail(),
                                    "Book-Shop Xác Nhận Đơn hàng",
                                    "CONFIRM",
                                    snapshot.items(),
                                    snapshot.totalAmount(),
                                    order);
                        } catch (MessagingException ignored) {
                            // Email failure must never roll back a committed order.
                        }
                    }
                });
    }

    private void cleanupSession(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute("cartItems");
        session.removeAttribute("orderDraft");
        session.removeAttribute("exchangeRate");
        session.removeAttribute("usdAmount");
        session.removeAttribute("orderId_pp_hint");
    }

    private <T> T inTransaction(TransactionWork<T> work) {
        T result = new TransactionTemplate(transactionManager)
                .execute(status -> work.execute());
        return Objects.requireNonNull(result);
    }

    @FunctionalInterface
    private interface TransactionWork<T> {
        T execute();
    }

    private record PaymentSnapshot(
            Long orderId,
            OrderStatus orderStatus,
            String paymentStatus) {
    }
}
