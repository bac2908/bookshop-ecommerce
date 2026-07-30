package vn.fs.backend.presentation.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CategoryResponse")
public record CategoryResponse(
        Long id,
        String name,
        String imageName,
        String imageUrl,
        Boolean active) {
}
