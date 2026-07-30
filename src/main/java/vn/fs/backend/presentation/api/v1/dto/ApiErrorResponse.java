package vn.fs.backend.presentation.api.v1.dto;

import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiErrorResponse")
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<ApiFieldError> fieldErrors) {

    public static ApiErrorResponse of(
            int status,
            String error,
            String code,
            String message,
            String path,
            List<ApiFieldError> fieldErrors) {
        return new ApiErrorResponse(
                Instant.now(), status, error, code, message, path,
                fieldErrors == null ? List.of() : List.copyOf(fieldErrors));
    }
}
