package vn.fs.backend.application.dto;

import lombok.*;
import jakarta.validation.constraints.*;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDTO {

    private Long categoryId;

    @NotBlank(message = "Tên thể loại không được để trống.")
    @Size(min = 2, max = 255, message = "Tên thể loại phải từ 2 đến 255 ký tự.")
    private String categoryName;

    private String categoryImage;

    @NotNull(message = "Vui lòng chọn trạng thái.")
    private Boolean status;

    // Chuẩn hoá chuỗi để tránh lỗi trùng do khoảng trắng
    public void normalize() {
        if (categoryName != null) {
            categoryName = categoryName.replaceAll("\\s+", " ").trim();
        }
    }
}
