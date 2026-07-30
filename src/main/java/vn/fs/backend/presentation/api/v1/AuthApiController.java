package vn.fs.backend.presentation.api.v1;

import java.util.Comparator;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.dto.SessionUser;
import vn.fs.backend.application.service.CurrentUserService;
import vn.fs.backend.application.service.LoginAttemptService;
import vn.fs.backend.application.service.LoginAttemptService.BlockReason;
import vn.fs.backend.domain.model.Role;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.presentation.api.v1.dto.ApiLoginRequest;
import vn.fs.backend.presentation.api.v1.dto.AuthenticatedUserResponse;
import vn.fs.backend.presentation.api.v1.dto.CsrfTokenResponse;
import vn.fs.backend.presentation.api.v1.error.ApiLockedException;
import vn.fs.backend.presentation.api.v1.error.ApiUnauthorizedException;
import vn.fs.backend.security.IpUtils;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Session, CSRF và người dùng hiện tại")
public class AuthApiController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final ChangeSessionIdAuthenticationStrategy sessionFixationStrategy;
    private final CsrfAuthenticationStrategy csrfAuthenticationStrategy;
    private final LoginAttemptService loginAttemptService;
    private final CurrentUserService currentUserService;

    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    @GetMapping("/csrf")
    @Operation(
            summary = "Lấy CSRF token",
            description = "Gọi khi ứng dụng khởi tạo và gọi lại sau login/logout.")
    public CsrfTokenResponse csrf(CsrfToken csrfToken) {
        return new CsrfTokenResponse(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken());
    }

    @PostMapping("/login")
    @Operation(
            summary = "Đăng nhập bằng JSON",
            description = "Yêu cầu CSRF token; tạo session dùng chung với website Thymeleaf.")
    @SecurityRequirement(name = "csrfToken")
    public AuthenticatedUserResponse login(
            @Valid @RequestBody ApiLoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response) {

        String clientIp = IpUtils.getClientIp(request);
        assertNotLocked(loginRequest.login(), clientIp);

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            loginRequest.login(),
                            loginRequest.password()));
        } catch (AuthenticationException exception) {
            loginAttemptService.onFailure(loginRequest.login(), clientIp);
            throw new ApiUnauthorizedException(
                    "INVALID_CREDENTIALS",
                    "Username, email hoặc mật khẩu không chính xác");
        }

        User user = currentUserService.findByLogin(authentication.getName());
        if (user == null) {
            throw new ApiUnauthorizedException(
                    "SESSION_USER_NOT_FOUND",
                    "Không thể khởi tạo phiên đăng nhập");
        }

        sessionFixationStrategy.onAuthentication(authentication, request, response);
        csrfAuthenticationStrategy.onAuthentication(authentication, request, response);

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        request.getSession().setAttribute("customer", SessionUser.from(user));
        loginAttemptService.onSuccess(authentication.getName(), clientIp);
        return toResponse(user);
    }

    @GetMapping("/me")
    @Operation(summary = "Lấy người dùng đang đăng nhập")
    @SecurityRequirement(name = "sessionCookie")
    public AuthenticatedUserResponse me() {
        User user = currentUserService.get();
        if (user == null) {
            throw new ApiUnauthorizedException(
                    "SESSION_USER_NOT_FOUND",
                    "Phiên đăng nhập không còn hợp lệ");
        }
        return toResponse(user);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Đăng xuất",
            description = "Spring Security xử lý trước controller, hủy session và CSRF token.")
    @SecurityRequirement(name = "sessionCookie")
    @SecurityRequirement(name = "csrfToken")
    @ApiResponse(responseCode = "204", description = "Đăng xuất thành công")
    public ResponseEntity<Void> logoutDocumentationEndpoint() {
        return ResponseEntity.noContent().build();
    }

    private void assertNotLocked(String login, String ip) {
        BlockReason reason = loginAttemptService.blockReason(login, ip);
        if (reason == BlockReason.IP_LOCKED) {
            throw new ApiLockedException(
                    "IP_TEMPORARILY_LOCKED",
                    "Địa chỉ IP đang bị khóa đăng nhập tạm thời");
        }
        if (reason == BlockReason.USER_LOCKED) {
            throw new ApiLockedException(
                    "ACCOUNT_TEMPORARILY_LOCKED",
                    "Tài khoản đang bị khóa đăng nhập tạm thời");
        }
    }

    private AuthenticatedUserResponse toResponse(User user) {
        List<String> roles = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .map(Role::getName)
                        .filter(role -> role != null && !role.isBlank())
                        .sorted(Comparator.naturalOrder())
                        .toList();
        return new AuthenticatedUserResponse(
                user.getUserId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.getAvatar(),
                roles);
    }
}
