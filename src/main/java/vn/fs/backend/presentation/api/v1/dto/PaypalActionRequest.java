package vn.fs.backend.presentation.api.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PaypalActionRequest")
public record PaypalActionRequest(
        @NotBlank(message = "externalOrderId không được để trống")
        @Size(max = 255, message = "externalOrderId quá dài")
        String externalOrderId) {
}
