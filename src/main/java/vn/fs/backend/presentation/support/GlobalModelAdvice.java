package vn.fs.backend.presentation.support;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;
import vn.fs.backend.application.dto.SessionUser;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.UserRepository;

import jakarta.servlet.http.HttpSession;
 // Inject các dữ liệu dùng chung cho layout (header/footer/nav) theo mô hình MVC.
 // QUAN TRỌNG:
 //Chỉ thực hiện READ-ONLY.
 // Việc bind số lượng giỏ hàng, danh sách item, tổng tiền... phải được CommonViewDataService triển khai ở chế độ readOnly.

@ControllerAdvice(annotations = Controller.class)
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final CommonViewDataService commonViewDataService;
    private final UserRepository userRepository;

    @ModelAttribute
    public void injectCommonData(Model model, HttpSession session) {
        //Lấy user hiện tại (ưu tiên session, sau đó SecurityContext)
        User current = resolveCurrentUser(session);

        //Bơm dữ liệu dùng chung PHẢI là read-only ở tầng service)
        commonViewDataService.commonData(model, current);

        //Đảm bảo luôn có 'user' để view không lỗi khi truy cập
        model.addAttribute("user", (current != null) ? current : new User());

        //Thêm displayName & isLoggedIn cho tiện dùng ở header
        model.addAttribute("displayName", buildDisplayName(session, current));
        model.addAttribute("isLoggedIn", current != null);
    }


    private User resolveCurrentUser(HttpSession session) {
        // Ưu tiên session attribute "customer"
        if (session != null) {
            Object customer = session.getAttribute("customer");
            if (customer instanceof User) {
                return (User) customer;
            }
            if (customer instanceof SessionUser) {
                User u = resolveFromSessionUser((SessionUser) customer);
                if (u != null) return u;
            }
        }

        // Fallback: SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        // có thể là username hoặc email
        String loginId = auth.getName();
        User byUsername = userRepository.findByUsernameIgnoreCase(loginId).orElse(null);
        if (byUsername != null) return byUsername;
        return userRepository.findByEmailIgnoreCase(loginId).orElse(null);
    }

    private User resolveFromSessionUser(SessionUser su) {
        if (su == null) return null;
        if (su.getUserId() != null) {
            User byId = userRepository.findById(su.getUserId()).orElse(null);
            if (byId != null) return byId;
        }
        if (su.getUsername() != null && !su.getUsername().isBlank()) {
            User byUsername = userRepository.findByUsernameIgnoreCase(su.getUsername()).orElse(null);
            if (byUsername != null) return byUsername;
        }
        if (su.getEmail() != null && !su.getEmail().isBlank()) {
            return userRepository.findByEmailIgnoreCase(su.getEmail()).orElse(null);
        }
        return null;
    }

    private String buildDisplayName(HttpSession session, User current) {
        if (current != null) {
            if (notBlank(current.getName()))     return current.getName();
            if (notBlank(current.getUsername())) return current.getUsername();
            if (notBlank(current.getEmail()))    return current.getEmail();
        }
        // nếu chưa đăng nhập, thử lấy thông tin hiển thị từ SessionUser (nếu có)
        if (session != null) {
            Object customer = session.getAttribute("customer");
            if (customer instanceof SessionUser) {
                SessionUser su = (SessionUser) customer;
                if (notBlank(su.getName()))     return su.getName();
                if (notBlank(su.getUsername())) return su.getUsername();
                if (notBlank(su.getEmail()))    return su.getEmail();
            }
        }
        return "Khách";
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
