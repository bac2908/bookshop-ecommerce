package vn.fs.backend.application.service;

import java.util.Date;
import java.util.Set;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.fs.backend.domain.model.Role;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.RoleRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;

@Service
@RequiredArgsConstructor
public class AdminBootstrapService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Transactional
    public boolean provisionFirstAdministrator(
            String username,
            String email,
            String password,
            String name,
            String phone) {
        if (userRepository.countActiveAdministrators() > 0) {
            return false;
        }

        String normalizedUsername = required(username, "ADMIN_BOOTSTRAP_USERNAME");
        String normalizedEmail = required(email, "ADMIN_BOOTSTRAP_EMAIL")
                .toLowerCase();
        String normalizedPassword = required(password, "ADMIN_BOOTSTRAP_PASSWORD");
        String normalizedName = required(name, "ADMIN_BOOTSTRAP_NAME");
        String normalizedPhone = required(phone, "ADMIN_BOOTSTRAP_PHONE");

        if (!normalizedUsername.matches("^[A-Za-z0-9._-]{4,32}$")) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_USERNAME phải có 4-32 ký tự hợp lệ");
        }
        if (!normalizedEmail.matches(
                "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalStateException("ADMIN_BOOTSTRAP_EMAIL không hợp lệ");
        }
        if (normalizedPassword.length() < 12
                || normalizedPassword.equalsIgnoreCase(normalizedUsername)) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_PASSWORD phải có ít nhất 12 ký tự và khác username");
        }
        if (userRepository.findByUsernameIgnoreCase(normalizedUsername).isPresent()
                || userRepository.findByEmailIgnoreCase(normalizedEmail).isPresent()) {
            throw new IllegalStateException(
                    "Username hoặc email bootstrap đã thuộc một tài khoản hiện có");
        }

        Role adminRole = roleRepository.findByName("ROLE_ADMIN");
        if (adminRole == null) {
            throw new IllegalStateException("Migration ROLE_ADMIN chưa được áp dụng");
        }

        User administrator = new User();
        administrator.setUsername(normalizedUsername);
        administrator.setEmail(normalizedEmail);
        administrator.setPassword(passwordEncoder.encode(normalizedPassword));
        administrator.setName(normalizedName);
        administrator.setPhone(normalizedPhone);
        administrator.setRegisterDate(new Date());
        administrator.setStatus(true);
        administrator.setFailedAttempt(0);
        administrator.setRoles(Set.of(adminRole));
        userRepository.save(administrator);
        return true;
    }

    private String required(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    variableName + " bắt buộc khi bật bootstrap admin");
        }
        return value.trim();
    }
}
