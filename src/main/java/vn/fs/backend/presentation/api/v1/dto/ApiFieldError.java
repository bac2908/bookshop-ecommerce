package vn.fs.backend.presentation.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiFieldError")
public record ApiFieldError(String field, String message) {
}
