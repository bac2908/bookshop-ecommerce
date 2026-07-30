package vn.fs.backend.presentation.storefront;

import java.security.Principal;
import java.util.Collections;
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
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import vn.fs.backend.domain.model.Order;
import vn.fs.backend.domain.model.OrderDetail;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.OrderDetailRepository;
import vn.fs.backend.infrastructure.persistence.OrderRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;
import vn.fs.backend.application.service.UserDetailService;

@Controller
public class ProfileController {

    @Autowired UserRepository userRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderDetailRepository orderDetailRepository;

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

        Page<Order> orderPage = findPaginated(PageRequest.of(currentPage - 1, pageSize), current.getUserId());
        int totalPages = orderPage.getTotalPages();
        if (totalPages > 0) {
            List<Integer> pageNumbers = IntStream.rangeClosed(1, totalPages).boxed().collect(Collectors.toList());
            model.addAttribute("pageNumbers", pageNumbers);
        }

        model.addAttribute("orderByUser", orderPage);
        return "storefront/pages/profile";
    }

    public Page<Order> findPaginated(Pageable pageable, Long userId) {
        List<Order> orderList = (userId == null)
                ? Collections.emptyList()
                : orderRepository.findOrderByUserId(userId);

        int pageSize = pageable.getPageSize();
        int currentPage = pageable.getPageNumber();
        int startItem = currentPage * pageSize;

        List<Order> list;
        if (orderList.size() < startItem) {
            list = Collections.emptyList();
        } else {
            int toIndex = Math.min(startItem + pageSize, orderList.size());
            list = orderList.subList(startItem, toIndex);
        }

        return new PageImpl<>(list, PageRequest.of(currentPage, pageSize), orderList.size());
    }

    @GetMapping("/order/detail/{order_id}")
    public ModelAndView detail(Model model,
                               Principal principal,
                               @PathVariable("order_id") Long id) {
        User current = resolveCurrentUser(principal);
        if (current == null) {
            return new ModelAndView("redirect:/login");
        }

        Optional<Order> order = orderRepository.findById(id);
        if (order.isEmpty()
                || order.get().getUser() == null
                || !current.getUserId().equals(order.get().getUser().getUserId())) {
            return new ModelAndView("redirect:/profile");
        }

        model.addAttribute("user", current);
        List<OrderDetail> listO = orderDetailRepository.findByOrderId(id);
        model.addAttribute("orderDetail", listO);
        return new ModelAndView("storefront/pages/order-detail");
    }

    @PostMapping("/order/cancel/{order_id}")
    public ModelAndView cancel(ModelMap model,
                               Principal principal,
                               @PathVariable("order_id") Long id,
                               RedirectAttributes ra) {
        User current = resolveCurrentUser(principal);
        if (current == null) {
            return new ModelAndView("redirect:/login", model);
        }

        Optional<Order> o = orderRepository.findById(id);
        if (o.isEmpty()
                || o.get().getUser() == null
                || !current.getUserId().equals(o.get().getUser().getUserId())) {
            ra.addFlashAttribute("err", "Không tìm thấy đơn hàng.");
            return new ModelAndView("redirect:/profile", model);
        }

        Order oReal = o.get();
        if (oReal.getStatus() != 0) {
            ra.addFlashAttribute("err", "Chỉ có thể hủy đơn đang chờ xác nhận.");
            return new ModelAndView("redirect:/profile", model);
        }
        oReal.setStatus(3);
        orderRepository.save(oReal);
        ra.addFlashAttribute("ok", "Đã hủy đơn hàng.");
        return new ModelAndView("redirect:/profile", model);
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
