package vn.fs.backend.presentation.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PublisherResponse")
public record PublisherResponse(Long id, String name, Boolean active) {
}
