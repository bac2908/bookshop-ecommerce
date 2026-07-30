package vn.fs.backend.presentation.api.v1.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "OrderItemResponse")
public record OrderItemResponse(
        Long productId,
        String name,
        String imageUrl,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {
}
