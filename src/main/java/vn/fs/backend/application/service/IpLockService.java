package vn.fs.backend.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.backend.domain.model.LoginIpLock;
import vn.fs.backend.infrastructure.persistence.LoginIpLockRepository;

import java.time.LocalDateTime;

@Service
@Transactional
public class IpLockService {

    @Value("${auth.ip.max-fail:20}")
    private int maxFail;

    @Value("${auth.ip.window-minutes:10}")
    private long windowMinutes;

    @Value("${auth.ip.lock-minutes:30}")
    private long lockMinutes;

    private final LoginIpLockRepository repo;

    public IpLockService(LoginIpLockRepository repo) { this.repo = repo; }

    public boolean isLocked(String ip) {
        return repo.findByIp(ip)
                .map(r -> r.getLockedUntil()!=null && r.getLockedUntil().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    public void onFailure(String ip) {
        LoginIpLock r = repo.findByIp(ip).orElseGet(() -> {
            LoginIpLock x = new LoginIpLock(); x.setIp(ip); return x;
        });
        // Reset window nếu quá hạn
        if (r.getLastFailedAt()!=null &&
                r.getLastFailedAt().isBefore(LocalDateTime.now().minusMinutes(windowMinutes))) {
            r.setFailedAttempt(0);
        }
        r.setFailedAttempt(r.getFailedAttempt() + 1);
        r.setLastFailedAt(LocalDateTime.now());
        if (r.getFailedAttempt() >= maxFail) {
            r.setLockedUntil(LocalDateTime.now().plusMinutes(lockMinutes));
        }
        repo.save(r);
    }

    public void onSuccess(String ip) {
    }
}
