package vn.fs.backend.application.service;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final UserLockService userLockService;
    private final IpLockService ipLockService;

    public BlockReason blockReason(String login, String ip) {
        if (ipLockService.isLocked(StringUtils.defaultString(ip))) {
            return BlockReason.IP_LOCKED;
        }
        if (StringUtils.isNotBlank(login) && userLockService.isLocked(login.trim())) {
            return BlockReason.USER_LOCKED;
        }
        return BlockReason.NONE;
    }

    public void onFailure(String login, String ip) {
        if (StringUtils.isNotBlank(login)) {
            userLockService.onFailure(login.trim());
        }
        ipLockService.onFailure(StringUtils.defaultString(ip));
    }

    public void onSuccess(String login, String ip) {
        if (StringUtils.isNotBlank(login)) {
            userLockService.onSuccess(login.trim());
        }
        ipLockService.onSuccess(StringUtils.defaultString(ip));
    }

    public enum BlockReason {
        NONE,
        USER_LOCKED,
        IP_LOCKED
    }
}
