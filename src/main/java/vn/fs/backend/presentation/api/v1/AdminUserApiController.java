package vn.fs.backend.presentation.api.v1;

import java.sql.Date;
import java.util.Comparator;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.error.AdminUserNotFoundException;
import vn.fs.backend.application.service.UserAdminService;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.presentation.api.v1.dto.AdminUserAccessRequest;
import vn.fs.backend.presentation.api.v1.dto.AdminUserResponse;
import vn.fs.backend.presentation.api.v1.dto.PageResponse;
import vn.fs.backend.presentation.api.v1.dto.RoleResponse;

@Validated
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin users", description = "Quản trị tài khoản và vai trò")
@SecurityRequirement(name = "sessionCookie")
public class AdminUserApiController {

    private final UserAdminService userAdminService;

    @GetMapping("/users")
    @Operation(summary = "Tìm kiếm và phân trang người dùng")
    public PageResponse<AdminUserResponse> users(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page phải lớn hơn hoặc bằng 0")
            int page,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size phải lớn hơn hoặc bằng 1")
            @Max(value = 100, message = "size tối đa là 100")
            int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(required = false) String role,
            Authentication authentication) {
        Page<User> users = userAdminService.search(
                keyword,
                enabled,
                role,
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.ASC, "username")));
        return PageResponse.from(
                users,
                user -> toResponse(user, authentication.getName()));
    }

    @GetMapping("/users/{userId}")
    @Operation(summary = "Chi tiết quyền truy cập của người dùng")
    public AdminUserResponse user(
            @PathVariable Long userId,
            Authentication authentication) {
        User user = userAdminService.getById(userId);
        if (user == null) {
            throw new AdminUserNotFoundException(userId);
        }
        return toResponse(user, authentication.getName());
    }

    @PutMapping("/users/{userId}/access")
    @Operation(summary = "Cập nhật trạng thái và vai trò của người dùng")
    @SecurityRequirement(name = "csrfToken")
    public AdminUserResponse updateAccess(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserAccessRequest request,
            Authentication authentication) {
        User updated = userAdminService.updateRolesAndStatus(
                userId,
                request.roleIds(),
                request.enabled(),
                authentication.getName());
        return toResponse(updated, authentication.getName());
    }

    @GetMapping("/roles")
    @Operation(summary = "Danh sách vai trò có thể gán")
    public java.util.List<RoleResponse> roles() {
        return userAdminService.listAllRoles().stream()
                .map(role -> new RoleResponse(role.getId(), role.getName()))
                .toList();
    }

    private AdminUserResponse toResponse(User user, String actorLogin) {
        java.time.LocalDate registeredOn = user.getRegisterDate() == null
                ? null
                : new Date(user.getRegisterDate().getTime()).toLocalDate();
        java.util.List<String> roles = user.getRoles() == null
                ? java.util.List.of()
                : user.getRoles().stream()
                        .map(role -> role.getName())
                        .sorted(Comparator.naturalOrder())
                        .toList();
        boolean current = actorLogin != null
                && (actorLogin.equalsIgnoreCase(user.getUsername())
                    || actorLogin.equalsIgnoreCase(user.getEmail()));
        return new AdminUserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                registeredOn,
                Boolean.TRUE.equals(user.getStatus()),
                user.getFailedAttempt(),
                user.getLockedUntil(),
                user.getLastLoginAt(),
                roles,
                current);
    }
}
