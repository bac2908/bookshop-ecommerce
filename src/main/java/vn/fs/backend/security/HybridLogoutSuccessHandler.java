package vn.fs.backend.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class HybridLogoutSuccessHandler implements LogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {
        String applicationPath = request.getRequestURI().substring(request.getContextPath().length());
        if (applicationPath.startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            return;
        }
        response.sendRedirect(request.getContextPath() + "/?logout_success");
    }
}
