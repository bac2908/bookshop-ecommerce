package vn.fs.backend.presentation.admin;

import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.backend.application.dto.RefundResult;
import vn.fs.backend.domain.model.Order;
import vn.fs.backend.infrastructure.persistence.OrderPaymentRepository;
import vn.fs.backend.infrastructure.persistence.OrderRepository;
import vn.fs.backend.application.service.paypal.PaypalRefundService;

import java.util.Date;
import java.util.Optional;

// Admin controller cho refund 1 lan toan phan
@Controller
@RequestMapping("/admin/paypal")
public class AdminPaypalController {

    // 0 Cho xac nhan, 1 Dang giao, 2 Da thanh toan, 3 Da huy, 4 Da hoan tien
    public static final int ORDER_STATUS_REFUNDED = 4;

    private final OrderRepository orderRepository;
    private final OrderPaymentRepository paymentRepo;
    private final PaypalRefundService paypalRefundService;

    public AdminPaypalController(OrderRepository orderRepository,
                                 OrderPaymentRepository paymentRepo,
                                 PaypalRefundService paypalRefundService) {
        this.orderRepository = orderRepository;
        this.paymentRepo = paymentRepo;
        this.paypalRefundService = paypalRefundService;
    }

    @GetMapping("/refund/{orderId}")
    public String showRefundPage(@PathVariable("orderId") Long orderId,
                                 Model model,
                                 RedirectAttributes ra) {
        Optional<Order> opt = orderRepository.findById(orderId);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Khong tim thay don hang: " + orderId);
            return "redirect:/admin/orders";
        }
        Order order = opt.get();

        // status la int nen khong so sanh null
        if (order.getStatus() == ORDER_STATUS_REFUNDED) {
            ra.addFlashAttribute("error", "Don da hoan 1 lan truoc do.");
            return "redirect:/admin/orders";
        }
        if (order.getPaypalCaptureId() == null || order.getPaypalCaptureId().isBlank()) {
            ra.addFlashAttribute("error", "Don nay khong co captureId PayPal => khong the hoan online.");
            return "redirect:/admin/orders";
        }

        model.addAttribute("order", order);
        return "admin/pages/paypal-refund";
    }

    // Hoan toan phan 1 lan
    @PostMapping("/refund")
    @Transactional
    public String doRefund(@RequestParam("orderId") Long orderId, RedirectAttributes ra) {
        Optional<Order> opt = orderRepository.findById(orderId);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Khong tim thay don hang: " + orderId);
            return "redirect:/admin/orders";
        }
        Order order = opt.get();

        // status la int nen khong so sanh null
        if (order.getStatus() == ORDER_STATUS_REFUNDED) {
            ra.addFlashAttribute("error", "Don da hoan 1 lan roi.");
            return "redirect:/admin/orders";
        }
        String captureId = order.getPaypalCaptureId();
        if (captureId == null || captureId.isBlank()) {
            ra.addFlashAttribute("error", "Don nay khong co captureId PayPal.");
            return "redirect:/admin/orders";
        }

        try {
            // amount=null => PayPal hoan toan phan
            RefundResult result = paypalRefundService.refundCapture(
                    captureId, null, null, "Admin full refund (one shot)");

            // Cap nhat payment neu co
            paymentRepo.findByProviderAndExternalCaptureId("PAYPAL", captureId).ifPresent(pay -> {
                pay.setStatus("REFUNDED");
                pay.setRefundId(result.getRefundId());
                pay.setRefundAmount(null); // USD
                pay.setRefundCurrency("USD");
                pay.setRefundedAt(new Date());
                pay.setUpdatedAt(new Date());
                pay.setMessage("Refund OK - RefundID=" + result.getRefundId());
                paymentRepo.save(pay);
            });

            // Cap nhat don ve 4
            order.setStatus(ORDER_STATUS_REFUNDED);
            orderRepository.save(order);

            ra.addFlashAttribute("message",
                    "Hoan tien thanh cong. RefundID: " + result.getRefundId() + " - Trang thai: " + result.getStatus());
        } catch (Exception ex) {
            String msg = ex.getMessage() == null ? "" : ex.getMessage();
            if (msg.contains("CAPTURE_FULLY_REFUNDED")) {
                // Dong bo neu PayPal bao hoan roi
                order.setStatus(ORDER_STATUS_REFUNDED);
                orderRepository.save(order);
                ra.addFlashAttribute("message", "Capture da hoan truoc do. Da cap nhat don ve 'Da hoan tien'.");
            } else {
                ra.addFlashAttribute("error", "Hoan tien that bai: " + msg);
            }
        }
        return "redirect:/admin/orders";
    }
}
