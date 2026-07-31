package vn.fs.backend.presentation.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminOrderStatusResponse")
public record AdminOrderStatusResponse(
        Long id,
        String status,
        int statusCode) {
}
