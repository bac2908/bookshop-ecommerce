package vn.fs.backend.application.dto;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NxbDTO {
    private Long id;

    @NotBlank(message = "Tên NXB không được để trống")
    @Size(max = 150, message = "Tên NXB tối đa 150 ký tự")
    private String name;

    private Boolean status;

    // Chuẩn hoá input
    public void normalize() {
        if (name != null) name = name.trim();
        if (status == null) status = true;
    }
}
