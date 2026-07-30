package vn.fs.backend.security;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtils {
    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        // Never trust forwarding headers supplied directly by a client. A trusted
        // reverse proxy/container must sanitize them and rewrite remoteAddr.
        return request.getRemoteAddr();
    }
}
