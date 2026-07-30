package vn.fs.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.fs.backend.application.service.LoginAttemptService;
import vn.fs.backend.application.service.LoginAttemptService.BlockReason;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class PreLoginLockFilter extends OncePerRequestFilter {

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if ("/doLogin".equals(req.getRequestURI()) && "POST".equalsIgnoreCase(req.getMethod())) {
            String ip = IpUtils.getClientIp(req);
            String loginId = req.getParameter("username");
            BlockReason reason = loginAttemptService.blockReason(loginId, ip);
            if (reason == BlockReason.IP_LOCKED) {
                res.sendRedirect("/login?lockedIp");
                return;
            }
            if (reason == BlockReason.USER_LOCKED) {
                res.sendRedirect("/login?lockedUser");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
