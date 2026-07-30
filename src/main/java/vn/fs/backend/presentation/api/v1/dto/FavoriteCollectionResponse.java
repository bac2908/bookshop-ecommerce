package vn.fs.backend.presentation.api.v1.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "FavoriteCollectionResponse")
public record FavoriteCollectionResponse(
        List<ProductResponse> items,
        int count) {
}
