package vn.fs.backend.presentation.api.v1.error;

import java.util.Comparator;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import vn.fs.backend.presentation.api.v1.CatalogApiController;
import vn.fs.backend.presentation.api.v1.ProductApiController;
import vn.fs.backend.presentation.api.v1.dto.ApiErrorResponse;
import vn.fs.backend.presentation.api.v1.dto.ApiFieldError;

@Slf4j
@RestControllerAdvice(basePackageClasses = {
        ProductApiController.class,
        CatalogApiController.class
})
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        List<ApiFieldError> fields = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiFieldError(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(ApiFieldError::field))
                .toList();
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Dữ liệu gửi lên không hợp lệ",
                request,
                fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleParameterValidation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        List<ApiFieldError> fields = exception.getConstraintViolations().stream()
                .map(violation -> new ApiFieldError(
                        lastPathSegment(violation.getPropertyPath().toString()),
                        violation.getMessage()))
                .sorted(Comparator.comparing(ApiFieldError::field))
                .toList();
        return error(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Tham số truy vấn không hợp lệ",
                request,
                fields);
    }

    @ExceptionHandler(ApiResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(
            ApiResourceNotFoundException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.NOT_FOUND,
                exception.getCode(),
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(ApiConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(
            ApiConflictException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                exception.getCode(),
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDatabaseConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request) {
        log.warn("API data conflict at {}", request.getRequestURI());
        return error(
                HttpStatus.CONFLICT,
                "DATA_CONFLICT",
                "Dữ liệu xung đột với bản ghi hiện có",
                request,
                List.of());
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
            Exception exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Cấu trúc hoặc kiểu dữ liệu của yêu cầu không hợp lệ",
                request,
                List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(
            Exception exception,
            HttpServletRequest request) {
        log.error("Unhandled API error at {}", request.getRequestURI(), exception);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "Hệ thống không thể xử lý yêu cầu",
                request,
                List.of());
    }

    private ResponseEntity<ApiErrorResponse> error(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<ApiFieldError> fieldErrors) {
        return ResponseEntity.status(status).body(ApiErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                request.getRequestURI(),
                fieldErrors));
    }

    private String lastPathSegment(String propertyPath) {
        int separator = propertyPath.lastIndexOf('.');
        return separator < 0 ? propertyPath : propertyPath.substring(separator + 1);
    }
}
