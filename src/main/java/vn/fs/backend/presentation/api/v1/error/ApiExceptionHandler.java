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
import vn.fs.backend.application.error.CartItemNotFoundException;
import vn.fs.backend.application.error.InsufficientStockException;
import vn.fs.backend.application.error.ProductUnavailableException;
import vn.fs.backend.application.error.CartChangedDuringCheckoutException;
import vn.fs.backend.application.error.EmptyCartException;
import vn.fs.backend.application.error.InvalidOrderStateException;
import vn.fs.backend.application.error.OrderNotFoundException;
import vn.fs.backend.application.error.AdminUserNotFoundException;
import vn.fs.backend.application.error.CategoryNotFoundException;
import vn.fs.backend.application.error.DuplicateCategoryNameException;
import vn.fs.backend.application.error.DuplicatePublisherNameException;
import vn.fs.backend.application.error.InvalidAccessChangeException;
import vn.fs.backend.application.error.PublisherNotFoundException;

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

    @ExceptionHandler(ApiUnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(
            ApiUnauthorizedException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.UNAUTHORIZED,
                exception.getCode(),
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(ApiLockedException.class)
    public ResponseEntity<ApiErrorResponse> handleLocked(
            ApiLockedException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.LOCKED,
                exception.getCode(),
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(ProductUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleProductUnavailable(
            ProductUnavailableException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.NOT_FOUND,
                "PRODUCT_UNAVAILABLE",
                "Sản phẩm không tồn tại hoặc không còn được bán",
                request,
                List.of());
    }

    @ExceptionHandler(CartItemNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCartItemNotFound(
            CartItemNotFoundException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.NOT_FOUND,
                "CART_ITEM_NOT_FOUND",
                "Sản phẩm chưa có trong giỏ hàng",
                request,
                List.of());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiErrorResponse> handleInsufficientStock(
            InsufficientStockException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "INSUFFICIENT_STOCK",
                "Chỉ còn " + exception.getAvailableQuantity()
                        + " sản phẩm trong kho; số lượng yêu cầu là "
                        + exception.getRequestedQuantity(),
                request,
                List.of());
    }

    @ExceptionHandler(EmptyCartException.class)
    public ResponseEntity<ApiErrorResponse> handleEmptyCart(
            EmptyCartException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "CART_EMPTY",
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(CartChangedDuringCheckoutException.class)
    public ResponseEntity<ApiErrorResponse> handleCartChanged(
            CartChangedDuringCheckoutException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "CART_CHANGED",
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleOrderNotFound(
            OrderNotFoundException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.NOT_FOUND,
                "ORDER_NOT_FOUND",
                "Không tìm thấy đơn hàng",
                request,
                List.of());
    }

    @ExceptionHandler(AdminUserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAdminUserNotFound(
            AdminUserNotFoundException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND",
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleCategoryNotFound(
            CategoryNotFoundException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.NOT_FOUND,
                "CATEGORY_NOT_FOUND",
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(PublisherNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePublisherNotFound(
            PublisherNotFoundException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.NOT_FOUND,
                "PUBLISHER_NOT_FOUND",
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(DuplicateCategoryNameException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateCategory(
            DuplicateCategoryNameException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "CATEGORY_NAME_EXISTS",
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(DuplicatePublisherNameException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicatePublisher(
            DuplicatePublisherNameException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "PUBLISHER_NAME_EXISTS",
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(InvalidAccessChangeException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidAccessChange(
            InvalidAccessChangeException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                exception.getCode(),
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidOrderState(
            InvalidOrderStateException exception,
            HttpServletRequest request) {
        return error(
                HttpStatus.CONFLICT,
                "INVALID_ORDER_STATE",
                exception.getMessage(),
                request,
                List.of());
    }

    @ExceptionHandler(ApiUpstreamException.class)
    public ResponseEntity<ApiErrorResponse> handleUpstream(
            ApiUpstreamException exception,
            HttpServletRequest request) {
        log.warn("Upstream API failure at {}: {}",
                request.getRequestURI(),
                exception.getMessage());
        return error(
                HttpStatus.BAD_GATEWAY,
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
