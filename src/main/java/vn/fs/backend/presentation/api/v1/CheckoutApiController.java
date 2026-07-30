package vn.fs.backend.presentation.api.v1;

import java.net.URI;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.dto.CheckoutAddressDTO;
import vn.fs.backend.application.dto.CheckoutResult;
import vn.fs.backend.application.service.CheckoutService;
import vn.fs.backend.application.service.CurrentUserService;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.presentation.api.v1.dto.ApiErrorResponse;
import vn.fs.backend.presentation.api.v1.dto.CheckoutRequest;
import vn.fs.backend.presentation.api.v1.dto.CheckoutResponse;
import vn.fs.backend.presentation.api.v1.dto.PaypalActionRequest;
import vn.fs.backend.presentation.api.v1.error.ApiUnauthorizedException;
import vn.fs.backend.presentation.api.v1.error.ApiUpstreamException;

@RestController
@RequestMapping("/api/v1/checkout")
@RequiredArgsConstructor
@Tag(name = "Checkout", description = "Đặt hàng COD và điều phối thanh toán PayPal")
@SecurityRequirement(name = "sessionCookie")
@SecurityRequirement(name = "csrfToken")
@ApiResponses({
        @ApiResponse(
                responseCode = "400",
                description = "Dữ liệu checkout không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "CSRF token không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Giỏ hàng, tồn kho hoặc trạng thái thanh toán xung đột",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "502",
                description = "Nhà cung cấp thanh toán không phản hồi hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class CheckoutApiController {

    private final CheckoutService checkoutService;
    private final CurrentUserService currentUserService;

    @PostMapping
    @Operation(summary = "Đặt hàng COD hoặc khởi tạo PayPal")
    public ResponseEntity<CheckoutResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest servletRequest,
            HttpSession session) {
        User user = requireCurrentUser();
        CheckoutAddressDTO address = toAddress(request);
        String paymentMethod = request.paymentMethod()
                .toUpperCase(Locale.ROOT);

        try {
            CheckoutResult result;
            if ("PAYPAL".equals(paymentMethod)) {
                result = checkoutService.beginPaypalCheckout(
                        user,
                        address,
                        servletRequest,
                        session);
                return ResponseEntity.status(HttpStatus.ACCEPTED)
                        .body(OrderApiMapper.toResponse(result));
            }

            result = checkoutService.placeOrderCOD(user, address, session);
            URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/api/v1/orders/{id}")
                    .buildAndExpand(result.orderId())
                    .toUri();
            return ResponseEntity.created(location)
                    .body(OrderApiMapper.toResponse(result));
        } catch (vn.fs.backend.application.error.EmptyCartException
                | vn.fs.backend.application.error.InsufficientStockException
                | vn.fs.backend.application.error.ProductUnavailableException
                | vn.fs.backend.application.error.CartChangedDuringCheckoutException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiUpstreamException(
                    "PAYMENT_PROVIDER_ERROR",
                    "Không thể kết nối nhà cung cấp thanh toán",
                    exception);
        }
    }

    @PostMapping("/paypal/capture")
    @Operation(summary = "Capture PayPal idempotent sau khi người dùng approve")
    public CheckoutResponse capturePaypal(
            @Valid @RequestBody PaypalActionRequest request,
            HttpSession session) {
        try {
            return OrderApiMapper.toResponse(
                    checkoutService.finalizePaypalV2(
                            requireCurrentUser(),
                            request.externalOrderId(),
                            session));
        } catch (vn.fs.backend.application.error.OrderNotFoundException
                | vn.fs.backend.application.error.InvalidOrderStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ApiUpstreamException(
                    "PAYMENT_CAPTURE_FAILED",
                    "Không thể hoàn tất thanh toán PayPal",
                    exception);
        }
    }

    @PostMapping("/paypal/cancel")
    @Operation(summary = "Hủy checkout PayPal và hoàn lại tồn kho đã giữ")
    public CheckoutResponse cancelPaypal(
            @Valid @RequestBody PaypalActionRequest request,
            HttpSession session) {
        return OrderApiMapper.toResponse(
                checkoutService.cancelPaypalCheckout(
                        requireCurrentUser(),
                        request.externalOrderId(),
                        session));
    }

    private CheckoutAddressDTO toAddress(CheckoutRequest request) {
        CheckoutAddressDTO address = new CheckoutAddressDTO();
        address.setAddress(request.address());
        address.setPhone(request.phone());
        address.setNote(request.note());
        address.setPaymentMethod(request.paymentMethod());
        return address;
    }

    private User requireCurrentUser() {
        User user = currentUserService.get();
        if (user == null) {
            throw new ApiUnauthorizedException(
                    "SESSION_USER_NOT_FOUND",
                    "Phiên đăng nhập không còn hợp lệ");
        }
        return user;
    }
}
