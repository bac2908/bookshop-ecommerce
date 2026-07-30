package vn.fs.backend.presentation.api.v1.dto;

import java.math.BigDecimal;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CartResponse")
public record CartResponse(
        List<CartItemResponse> items,
        int distinctItems,
        int totalQuantity,
        BigDecimal totalAmount) {
}
