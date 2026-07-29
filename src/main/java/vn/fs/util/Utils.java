package vn.fs.util;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;

public class Utils {

    public static String getBaseURL(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);
        if (serverPort != 80 && serverPort != 443) {
            url.append(":").append(serverPort);
        }
        url.append(contextPath);

        // FIX: chỉ thêm "/" khi CHƯA có
        if (!url.toString().endsWith("/")) {
            url.append("/");
        }
        return url.toString();
    }
    /**
     * Lấy Referer “an toàn”: nếu rỗng hoặc khác origin thì trả về fallbackPath.
     * Trả về CHỈ path + query (không bao gồm origin) để dùng sau "redirect:".
     *
     * Ví dụ dùng:
     *   return "redirect:" + Utils.safeRefererOr(req, "/products");
     */
    public static String safeRefererOr(HttpServletRequest req, String fallbackPath) {
        String ref = req.getHeader("Referer");
        if (ref == null || ref.isBlank()) return fallbackPath;

        try {
            URI u = URI.create(ref);

            // Nếu là URL tương đối => dùng luôn
            if (u.getHost() == null) {
                String path = (u.getRawPath() != null ? u.getRawPath() : "/");
                String q = (u.getRawQuery() != null ? "?" + u.getRawQuery() : "");
                return path + q;
            }

            boolean sameScheme = u.getScheme().equalsIgnoreCase(req.getScheme());
            boolean sameHost   = u.getHost().equalsIgnoreCase(req.getServerName());

            int reqPort = req.getServerPort() == -1
                    ? ("https".equalsIgnoreCase(req.getScheme()) ? 443 : 80)
                    : req.getServerPort();
            int urlPort = u.getPort() == -1
                    ? ("https".equalsIgnoreCase(u.getScheme()) ? 443 : 80)
                    : u.getPort();

            if (sameScheme && sameHost && reqPort == urlPort) {
                String path = (u.getRawPath() != null ? u.getRawPath() : "/");
                String q = (u.getRawQuery() != null ? "?" + u.getRawQuery() : "");
                return path + q;
            }
        } catch (Exception ignore) { }

        return fallbackPath;
    }
}
