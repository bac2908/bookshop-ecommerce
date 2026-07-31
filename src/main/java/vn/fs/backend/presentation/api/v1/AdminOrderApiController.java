package vn.fs.backend.presentation.api.v1;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import vn.fs.backend.application.service.OrderAdminService;
import vn.fs.backend.application.service.AdminRefundService;
import vn.fs.backend.application.dto.RefundResult;
import vn.fs.backend.domain.model.OrderStatus;
import vn.fs.backend.presentation.api.v1.dto.AdminOrderStatusRequest;
import vn.fs.backend.presentation.api.v1.dto.AdminOrderStatusResponse;
import vn.fs.backend.presentation.api.v1.dto.AdminRefundResponse;
import vn.fs.backend.presentation.api.v1.error.ApiInvalidRequestException;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Admin orders", description = "Điều phối trạng thái đơn hàng")
@SecurityRequirement(name = "sessionCookie")
public class AdminOrderApiController {

    private final OrderAdminService orderService;
    private final AdminRefundService refundService;

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Chuyển trạng thái đơn hàng")
    @SecurityRequirement(name = "csrfToken")
    public AdminOrderStatusResponse changeStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody AdminOrderStatusRequest request) {
        OrderStatus target = request.status();
        switch (target) {
            case CONFIRMED -> orderService.confirm(orderId);
            case COMPLETED -> orderService.complete(orderId);
            case CANCELLED -> orderService.cancel(orderId);
            default -> throw new ApiInvalidRequestException(
                    "UNSUPPORTED_ORDER_STATUS",
                    "Quản trị viên chỉ có thể chuyển đơn sang CONFIRMED, COMPLETED hoặc CANCELLED");
        }
        return new AdminOrderStatusResponse(orderId, target.name(), target.getCode());
    }

    @PostMapping("/{orderId}/refund")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Hoàn toàn bộ giao dịch PayPal của đơn")
    @SecurityRequirement(name = "csrfToken")
    public AdminRefundResponse refund(@PathVariable Long orderId) {
        RefundResult result = refundService.refundFull(orderId);
        return new AdminRefundResponse(
                orderId,
                result.getRefundId(),
                result.getStatus());
    }
}
