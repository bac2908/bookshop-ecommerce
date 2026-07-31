package vn.fs.backend.presentation.api.v1.dto;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

import vn.fs.backend.domain.model.OrderStatus;

@Schema(name = "AdminOrderStatusRequest")
public record AdminOrderStatusRequest(
        @NotNull(message = "status không được để trống")
        OrderStatus status) {
}
