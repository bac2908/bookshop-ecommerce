package vn.fs.backend.presentation.api.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiLoginRequest", description = "Thông tin đăng nhập bằng username hoặc email")
public record ApiLoginRequest(
        @NotBlank(message = "Username hoặc email không được để trống")
        @Size(max = 254, message = "Username hoặc email tối đa 254 ký tự")
        @Schema(example = "customer@example.com")
        String login,

        @NotBlank(message = "Mật khẩu không được để trống")
        @Size(max = 200, message = "Mật khẩu tối đa 200 ký tự")
        @Schema(example = "your-password", format = "password")
        String password) {

    public ApiLoginRequest {
        if (login != null) {
            login = login.trim();
        }
    }
}
