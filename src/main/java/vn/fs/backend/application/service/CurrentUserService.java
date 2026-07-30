package vn.fs.backend.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.UserRepository;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    // lấy user hiện tại
    public User get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) return null;
        String login = auth.getName();
        return userRepository.findByUsernameIgnoreCase(login)
                .or(() -> userRepository.findByEmailIgnoreCase(login))
                .or(() -> userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(login, login))
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public User findByLogin(String login) {
        if (login == null || login.isBlank()) {
            return null;
        }
        String normalized = login.trim();
        return userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(normalized, normalized)
                .orElse(null);
    }
}
