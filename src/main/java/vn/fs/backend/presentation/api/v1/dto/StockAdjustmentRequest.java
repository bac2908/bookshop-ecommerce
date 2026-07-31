package vn.fs.backend.presentation.api.v1.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "StockAdjustmentRequest")
public record StockAdjustmentRequest(
        @NotNull(message = "quantity không được để trống")
        @Min(value = 1, message = "quantity phải lớn hơn hoặc bằng 1")
        @Max(value = 1_000_000, message = "quantity tối đa là 1000000")
        Integer quantity) {
}
