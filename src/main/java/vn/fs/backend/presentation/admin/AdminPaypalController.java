package vn.fs.backend.presentation.admin;

import org.springframework.stereotype.Controller;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.backend.domain.model.Order;
import vn.fs.backend.infrastructure.persistence.OrderRepository;

import java.util.Optional;

// Admin controller cho refund 1 lan toan phan
@Controller
@RequestMapping("/admin/paypal")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaypalController {

    // 0 Cho xac nhan, 1 Dang giao, 2 Da thanh toan, 3 Da huy, 4 Da hoan tien
    public static final int ORDER_STATUS_REFUNDED = 4;

    private final OrderRepository orderRepository;

    public AdminPaypalController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
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

}
