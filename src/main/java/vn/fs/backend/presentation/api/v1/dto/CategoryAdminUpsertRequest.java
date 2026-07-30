package vn.fs.backend.presentation.api.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CategoryAdminUpsertRequest")
public record CategoryAdminUpsertRequest(
        @NotBlank(message = "Tên thể loại không được để trống")
        @Size(min = 2, max = 255, message = "Tên thể loại phải từ 2 đến 255 ký tự")
        String name,
        @NotNull(message = "active không được để trống")
        Boolean active) {

    public String normalizedName() {
        return name == null ? null : name.replaceAll("\\s+", " ").trim();
    }
}
