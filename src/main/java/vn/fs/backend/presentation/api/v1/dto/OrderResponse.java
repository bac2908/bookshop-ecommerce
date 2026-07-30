package vn.fs.backend.presentation.api.v1.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrderResponse")
public record OrderResponse(
        Long id,
        LocalDate orderDate,
        BigDecimal amount,
        String address,
        String phone,
        String note,
        String status,
        int statusCode,
        String paymentMethod,
        String paymentStatus,
        boolean cancellable,
        List<OrderItemResponse> items) {
}
