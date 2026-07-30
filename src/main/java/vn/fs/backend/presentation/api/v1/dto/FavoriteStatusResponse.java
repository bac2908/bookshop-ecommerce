package vn.fs.backend.presentation.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FavoriteStatusResponse")
public record FavoriteStatusResponse(
        Long productId,
        boolean favorite,
        int count) {
}
