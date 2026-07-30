package vn.fs.backend.presentation.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CheckoutResponse")
public record CheckoutResponse(
        Long orderId,
        String status,
        String paymentMethod,
        String approvalUrl,
        String externalPaymentId) {
}
