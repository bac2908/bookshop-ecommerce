package vn.fs.backend.presentation.api.v1.dto;

import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CatalogVisibilityRequest")
public record CatalogVisibilityRequest(
        @NotNull(message = "active không được để trống")
        Boolean active) {
}
