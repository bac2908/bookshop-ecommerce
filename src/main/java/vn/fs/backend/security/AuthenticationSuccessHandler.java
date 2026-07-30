package vn.fs.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import vn.fs.backend.application.dto.SessionUser;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.UserRepository;
import vn.fs.backend.security.IpUtils;
import vn.fs.backend.application.service.LoginAttemptService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class AuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    @Autowired private LoginAttemptService loginAttemptService;

    // Lấy user theo username/email để đẩy vào session
    @Autowired private UserRepository  userRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest req,
                                        HttpServletResponse res,
                                        Authentication auth)
            throws IOException, ServletException {

        // Reset counters khi login thành công
        loginAttemptService.onSuccess(auth.getName(), IpUtils.getClientIp(req));

        // Đặt "customer" vào session dưới dạng DTO an toàn
        String loginId = auth.getName();
        User me = userRepository.findByUsername(loginId);
        if (me == null) {
            me = userRepository.findByEmail(loginId);
        }
        if (me != null) {
            SessionUser su = SessionUser.from(me);
            req.getSession().setAttribute("customer", su);
        }

        // Điều hướng theo role
        boolean isAdmin = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch("ROLE_ADMIN"::equals);
        boolean isStaff = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch("ROLE_STAFF"::equals);

        if (isAdmin || isStaff) {
            getRedirectStrategy().sendRedirect(req, res, "/admin/home");
        } else {
            // Về trang đã request trước đó hoặc "/"
            super.onAuthenticationSuccess(req, res, auth);
        }
    }
}
