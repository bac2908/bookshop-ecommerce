package vn.fs.backend.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import vn.fs.backend.application.service.IpLockService;
import vn.fs.backend.application.service.UserLockService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class PreLoginLockFilter extends OncePerRequestFilter {

    @Autowired
    private UserLockService userLockService;

    @Autowired
    private IpLockService ipLockService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        if ("/doLogin".equals(req.getRequestURI()) && "POST".equalsIgnoreCase(req.getMethod())) {
            String ip = IpUtils.getClientIp(req);
            if (ipLockService.isLocked(ip)) {
                res.sendRedirect("/login?lockedIp");
                return;
            }
            String loginId = req.getParameter("username");
            if (loginId != null && userLockService.isLocked(loginId.trim())) {
                res.sendRedirect("/login?lockedUser");
                return;
            }
        }
        chain.doFilter(req, res);
    }
}
