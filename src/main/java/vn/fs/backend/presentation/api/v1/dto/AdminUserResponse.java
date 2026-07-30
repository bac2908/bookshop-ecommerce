package vn.fs.backend.presentation.api.v1.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminUserResponse")
public record AdminUserResponse(
        Long id,
        String username,
        String email,
        String name,
        String phone,
        LocalDate registeredOn,
        boolean enabled,
        int failedAttempts,
        LocalDateTime lockedUntil,
        LocalDateTime lastLoginAt,
        List<String> roles,
        boolean currentUser) {
}
