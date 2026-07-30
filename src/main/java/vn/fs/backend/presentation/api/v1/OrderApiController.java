package vn.fs.backend.presentation.api.v1;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.service.CurrentUserService;
import vn.fs.backend.application.service.OrderService;
import vn.fs.backend.domain.model.Order;
import vn.fs.backend.domain.model.OrderPayment;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.OrderPaymentRepository;
import vn.fs.backend.presentation.api.v1.dto.ApiErrorResponse;
import vn.fs.backend.presentation.api.v1.dto.OrderResponse;
import vn.fs.backend.presentation.api.v1.dto.PageResponse;
import vn.fs.backend.presentation.api.v1.error.ApiUnauthorizedException;

@Validated
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Đơn hàng thuộc người dùng hiện tại")
@SecurityRequirement(name = "sessionCookie")
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Chưa đăng nhập",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "403",
                description = "CSRF token không hợp lệ",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "Không tìm thấy đơn thuộc người dùng hiện tại",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(
                responseCode = "409",
                description = "Không thể chuyển trạng thái đơn",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class OrderApiController {

    private final OrderService orderService;
    private final CurrentUserService currentUserService;
    private final OrderPaymentRepository paymentRepository;

    @GetMapping
    @Operation(summary = "Danh sách đơn hàng của người dùng hiện tại")
    public PageResponse<OrderResponse> getOrders(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page phải lớn hơn hoặc bằng 0")
            int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "size phải lớn hơn hoặc bằng 1")
            @Max(value = 50, message = "size tối đa là 50")
            int size) {
        User user = requireCurrentUser();
        Page<Order> orders = orderService.listByUser(
                user.getUserId(),
                PageRequest.of(
                        page,
                        size,
                        Sort.by(Sort.Direction.DESC, "orderId")));
        return PageResponse.from(
                orders,
                order -> OrderApiMapper.toResponse(
                        order,
                        null,
                        payment(order.getOrderId())));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Chi tiết một đơn thuộc người dùng hiện tại")
    public OrderResponse getOrder(@PathVariable Long orderId) {
        User user = requireCurrentUser();
        Order order = orderService.findOwnedById(orderId, user.getUserId());
        return OrderApiMapper.toResponse(
                order,
                orderService.detailsOfOwned(orderId, user.getUserId()),
                payment(orderId));
    }

    @PutMapping("/{orderId}/cancellation")
    @Operation(summary = "Hủy đơn idempotent và hoàn lại tồn kho đúng một lần")
    @SecurityRequirement(name = "csrfToken")
    public OrderResponse cancelOrder(@PathVariable Long orderId) {
        User user = requireCurrentUser();
        Order order = orderService.cancelOwned(orderId, user.getUserId());
        return OrderApiMapper.toResponse(
                order,
                orderService.detailsOfOwned(orderId, user.getUserId()),
                payment(orderId));
    }

    private OrderPayment payment(Long orderId) {
        return paymentRepository.findByOrder_OrderId(orderId).orElse(null);
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
