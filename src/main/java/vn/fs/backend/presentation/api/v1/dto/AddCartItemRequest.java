package vn.fs.backend.presentation.api.v1.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AddCartItemRequest")
public record AddCartItemRequest(
        @NotNull(message = "productId không được để trống")
        @Schema(example = "1")
        Long productId,

        @Min(value = 1, message = "quantity phải lớn hơn hoặc bằng 1")
        @Max(value = 9999, message = "quantity tối đa là 9999")
        @Schema(example = "1")
        int quantity) {
}
