package vn.fs.backend.presentation.api.v1.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CsrfTokenResponse")
public record CsrfTokenResponse(
        String headerName,
        String parameterName,
        String token) {
}
