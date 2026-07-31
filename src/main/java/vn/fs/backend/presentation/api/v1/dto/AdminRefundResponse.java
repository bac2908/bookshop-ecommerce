package vn.fs.backend.presentation.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminRefundResponse")
public record AdminRefundResponse(
        Long orderId,
        String refundId,
        String status) {
}
