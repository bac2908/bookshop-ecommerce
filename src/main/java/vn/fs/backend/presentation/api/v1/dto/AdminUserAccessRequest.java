package vn.fs.backend.presentation.api.v1.dto;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "AdminUserAccessRequest")
public record AdminUserAccessRequest(
        @NotNull(message = "enabled không được để trống")
        Boolean enabled,
        @NotEmpty(message = "Người dùng phải có ít nhất một vai trò")
        List<Long> roleIds) {
}
