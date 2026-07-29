package vn.fs.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.fs.dto.SessionUser;
import vn.fs.entities.User;

import jakarta.servlet.http.HttpSession;

public final class SessionUtils {
    private SessionUtils() {}

    /**
     * Ưu tiên lấy userId từ session attribute "customer" (có thể là SessionUser hoặc User).
     * Nếu không có, trả null (Controller sẽ tự đọc từ SecurityContext và truy DB).
     */
    public static Long getUserId(HttpSession session) {
        if (session != null) {
            Object obj = session.getAttribute("customer");
            if (obj instanceof SessionUser) {
                return ((SessionUser) obj).getUserId();
            }
            if (obj instanceof User) {
                return ((User) obj).getUserId();
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            // Không truy DB tại đây để tránh phụ thuộc; Controller sẽ xử lý tiếp.
            return null;
        }
        return null;
    }
}
