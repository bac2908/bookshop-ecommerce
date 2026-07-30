package vn.fs.backend.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.InvalidCsrfTokenException;
import org.springframework.security.web.csrf.MissingCsrfTokenException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.fs.backend.presentation.api.v1.dto.ApiErrorResponse;

@Component
@RequiredArgsConstructor
public class ApiSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException {
        write(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED",
                "Bạn cần đăng nhập để sử dụng API này");
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            org.springframework.security.access.AccessDeniedException exception)
            throws IOException, ServletException {
        boolean csrfFailure = exception instanceof MissingCsrfTokenException
                || exception instanceof InvalidCsrfTokenException;
        write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                csrfFailure ? "CSRF_TOKEN_INVALID" : "ACCESS_DENIED",
                csrfFailure
                        ? "CSRF token bị thiếu hoặc không hợp lệ"
                        : "Bạn không có quyền thực hiện thao tác này");
    }

    private void write(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                List.of()));
    }
}
