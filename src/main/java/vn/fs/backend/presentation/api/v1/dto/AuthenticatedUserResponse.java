package vn.fs.backend.presentation.api.v1.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AuthenticatedUserResponse")
public record AuthenticatedUserResponse(
        Long id,
        String username,
        String name,
        String email,
        String avatar,
        List<String> roles) {
}
