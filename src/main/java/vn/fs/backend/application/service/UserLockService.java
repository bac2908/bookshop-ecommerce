package vn.fs.backend.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.backend.infrastructure.persistence.UserRepository;

import java.time.LocalDateTime;

@Service
@Transactional
public class UserLockService {

    @Value("${auth.user.max-fail:5}")
    private int maxFail;

    @Value("${auth.user.lock-minutes:15}")
    private long lockMinutes;

    private final UserRepository users;

    public UserLockService(UserRepository users) {
        this.users = users;
    }

    public boolean isLocked(String loginId) {
        return users.findByUsernameIgnoreCase(loginId)
                .or(() -> users.findByEmailIgnoreCase(loginId))
                .map(u -> u.getLockedUntil() != null && u.getLockedUntil().isAfter(LocalDateTime.now()))
                .orElse(false);
    }

    public void onFailure(String loginId) {
        LocalDateTime now = LocalDateTime.now();
        int touched = users.bumpFailureCounter(loginId, now);
        if (touched > 0) {
            users.applyLockIfExceeded(
                    loginId,
                    maxFail,
                    now.plusMinutes(lockMinutes),
                    now
            );
        }
    }

    public void onSuccess(String loginId) {
        users.resetLoginState(loginId, LocalDateTime.now());
    }
}
