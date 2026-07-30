package vn.fs.backend.application.dto;

import vn.fs.backend.domain.model.OrderStatus;

public record CheckoutResult(
        Long orderId,
        OrderStatus status,
        String paymentMethod,
        String approvalUrl,
        String externalPaymentId) {
}
