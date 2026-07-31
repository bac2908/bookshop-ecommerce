package vn.fs.backend.presentation.storefront;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.fs.backend.domain.model.Order;
import vn.fs.backend.domain.model.OrderDetail;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.application.error.OrderNotFoundException;
import vn.fs.backend.application.service.OrderService;
import vn.fs.backend.infrastructure.persistence.UserRepository;
import vn.fs.backend.application.service.UserDetailService;

@Controller
public class ProfileController {

    @Autowired UserRepository userRepository;
    @Autowired OrderService orderService;

    @Autowired UserDetailService userDetailService; // để load lại UserDetails sau khi đổi username

    // Lấy User theo principal: chấp nhận login id là username HOẶC email
    private User resolveCurrentUser(Principal principal) {
        if (principal == null) return null;
        String login = principal.getName();
        User u = Optional.ofNullable(userRepository.findByUsername(login))
                .orElseGet(() -> userRepository.findByEmail(login));
        return u;
    }

    // Sau khi đổi username/email, refresh lại Authentication trong SecurityContext để không bị đăng xuất
    private void refreshAuthentication(String newLoginId, HttpServletRequest request) {
        // newLoginId có thể là username mới hoặc email (ở đây ta dùng username mới)
        var userDetails = userDetailService.loadUserByUsername(newLoginId);
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                userDetails,
                null, // không cần credentials
                userDetails.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        // đảm bảo session đang giữ context mới
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        }
    }

    @GetMapping("/profile")
    public String profile(Model model,
                          Principal principal,
                          Pageable pageable,
                          @RequestParam("page") Optional<Integer> page,
                          @RequestParam("size") Optional<Integer> size) {

        User current = resolveCurrentUser(principal);
        if (current == null) return "redirect:/login";

        model.addAttribute("user", current);

        int currentPage = page.orElse(1);
        int pageSize = size.orElse(6);

        Page<Order> orderPage = orderService.listByUser(
                current.getUserId(),
                PageRequest.of(currentPage - 1, pageSize));
        int totalPages = orderPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }

        model.addAttribute("orderByUser", orderPage);
        return "storefront/pages/profile";
    }

    @GetMapping("/order/detail/{order_id}")
    public ModelAndView detail(Model model,
                               Principal principal,
                               @PathVariable("order_id") Long id) {
        User current = resolveCurrentUser(principal);
        if (current == null) {
            return new ModelAndView("redirect:/login");
        }

        List<OrderDetail> details;
        try {
            details = orderService.detailsOfOwned(id, current.getUserId());
        } catch (OrderNotFoundException exception) {
            return new ModelAndView("redirect:/profile");
        }

        model.addAttribute("user", current);
        model.addAttribute("orderDetail", details);
        return new ModelAndView("storefront/pages/order-detail");
    }

    @PostMapping("/profile/update")
    public String updateProfile(Principal principal,
                                HttpServletRequest request,
                                RedirectAttributes ra,
                                @RequestParam String name,
                                @RequestParam String email,
                                @RequestParam String phone,
                                @RequestParam String username) {

        User current = resolveCurrentUser(principal);
        if (current == null) return "redirect:/login";

        String trimmedName = name == null ? "" : name.trim();
        String trimmedEmail = email == null ? "" : email.trim();
        String trimmedPhone = phone == null ? "" : phone.trim();
        String trimmedUsername = username == null ? "" : username.trim();

        if (trimmedName.length() < 2 || trimmedName.length() > 50) {
            ra.addFlashAttribute("err", "Họ tên phải từ 2-50 ký tự.");
            return "redirect:/profile";
        }
        if (!trimmedEmail.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            ra.addFlashAttribute("err", "Email không hợp lệ.");
            return "redirect:/profile";
        }
        if (!trimmedPhone.matches("^[0-9]{9,11}$")) {
            ra.addFlashAttribute("err", "Số điện thoại phải 9-11 chữ số.");
            return "redirect:/profile";
        }
        if (!trimmedUsername.matches("^[A-Za-z0-9._-]{4,32}$")) {
            ra.addFlashAttribute("err", "Username 4-32 ký tự, chỉ gồm chữ/số/dấu . _ -");
            return "redirect:/profile";
        }

        // Check trùng email/username (loại trừ chính mình)
        if (userRepository.existsByEmailIgnoreCaseAndUserIdNot(trimmedEmail, current.getUserId())) {
            ra.addFlashAttribute("err", "Email đã được sử dụng.");
            return "redirect:/profile";
        }
        if (userRepository.existsByUsernameIgnoreCaseAndUserIdNot(trimmedUsername, current.getUserId())) {
            ra.addFlashAttribute("err", "Username đã có người dùng.");
            return "redirect:/profile";
        }

        // Lưu
        String oldUsername = current.getUsername();
        current.setName(trimmedName);
        current.setEmail(trimmedEmail);
        current.setPhone(trimmedPhone);
        current.setUsername(trimmedUsername);
        userRepository.save(current);

        // Nếu username thay đổi → refresh Authentication để KHÔNG bắt đăng nhập lại
        if (oldUsername != null && !oldUsername.equalsIgnoreCase(trimmedUsername)) {
            refreshAuthentication(trimmedUsername, request);
        }

        ra.addFlashAttribute("ok", "Cập nhật thông tin thành công!");
        return "redirect:/profile";
    }
}
