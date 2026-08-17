package vn.fs.backend.application.service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Small in-process limiter for OTP issuance. It protects both the email target
 * and the client IP, while session counters separately protect verification.
 */
@Service
public class OtpRateLimitService {

    private final Map<String, Deque<Long>> attempts = new HashMap<>();

    @Value("${auth.otp.max-requests:5}")
    private int maxRequests;

    @Value("${auth.otp.window-minutes:15}")
    private long windowMinutes;

    public synchronized boolean tryAcquire(
            String flow,
            String email,
            String clientIp) {
        long now = System.currentTimeMillis();
        long cutoff = now - windowMillis();
        evictExpired(cutoff);

        List<String> keys = List.of(
                key(flow, "email", email),
                key(flow, "ip", clientIp));
        int safeMaximum = Math.max(1, maxRequests);
        for (String key : keys) {
            Deque<Long> window = attempts.get(key);
            if (window != null && window.size() >= safeMaximum) {
                return false;
            }
        }
        for (String key : keys) {
            attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>())
                    .addLast(now);
        }
        return true;
    }

    @Scheduled(fixedDelayString = "${auth.otp.cleanup-ms:900000}")
    public synchronized void cleanup() {
        evictExpired(System.currentTimeMillis() - windowMillis());
    }

    private long windowMillis() {
        return Duration.ofMinutes(Math.max(1, windowMinutes)).toMillis();
    }

    private void evictExpired(long cutoff) {
        attempts.entrySet().removeIf(entry -> {
            Deque<Long> window = entry.getValue();
            while (!window.isEmpty() && window.peekFirst() < cutoff) {
                window.removeFirst();
            }
            return window.isEmpty();
        });
    }

    private String key(String flow, String dimension, String value) {
        return normalize(flow) + ':' + dimension + ':' + normalize(value);
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? "unknown"
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
