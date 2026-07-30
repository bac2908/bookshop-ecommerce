package vn.fs.backend.presentation.api.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PublisherAdminUpsertRequest")
public record PublisherAdminUpsertRequest(
        @NotBlank(message = "Tên nhà xuất bản không được để trống")
        @Size(min = 2, max = 150, message = "Tên nhà xuất bản phải từ 2 đến 150 ký tự")
        String name,
        @NotNull(message = "active không được để trống")
        Boolean active) {

    public String normalizedName() {
        return name == null ? null : name.replaceAll("\\s+", " ").trim();
    }
}
