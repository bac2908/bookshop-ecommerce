package vn.fs.backend.presentation.api.v1.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ProductUpsertRequest", description = "Dữ liệu tạo hoặc cập nhật sản phẩm")
public record ProductUpsertRequest(
        @NotBlank(message = "Tên sản phẩm không được để trống")
        @Size(min = 2, max = 255, message = "Tên sản phẩm phải từ 2 đến 255 ký tự")
        @Schema(example = "Nhà Giả Kim")
        String name,

        @NotNull(message = "Đơn giá không được để trống")
        @DecimalMin(value = "0.0", message = "Đơn giá không được âm")
        @Digits(integer = 14, fraction = 2, message = "Đơn giá tối đa 14 số nguyên và 2 số lẻ")
        @Schema(example = "89000")
        BigDecimal price,

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 0, message = "Số lượng không được âm")
        @Schema(example = "100")
        Integer quantity,

        @NotNull(message = "Giảm giá không được để trống")
        @Min(value = 0, message = "Giảm giá không được âm")
        @Max(value = 90, message = "Giảm giá tối đa 90%")
        @Schema(example = "10")
        Integer discount,

        @Size(max = 255, message = "Mô tả tối đa 255 ký tự")
        @Schema(example = "Hành trình theo đuổi ước mơ và lắng nghe tiếng gọi của trái tim.")
        String description,

        @NotNull(message = "Ngày nhập không được để trống")
        @PastOrPresent(message = "Ngày nhập không được vượt quá hôm nay")
        @Schema(example = "2026-07-30")
        LocalDate enteredDate,

        @NotNull(message = "Trạng thái không được để trống")
        @Schema(example = "true")
        Boolean active,

        @NotNull(message = "Thể loại không được để trống")
        @Schema(example = "1")
        Long categoryId,

        @Schema(example = "1")
        Long publisherId,

        @Pattern(
                regexp = "^[A-Za-z0-9._-]{1,255}$",
                message = "Tên ảnh chỉ được chứa chữ không dấu, số, dấu chấm, gạch ngang và gạch dưới")
        @Schema(example = "nha-gia-kim.jpg")
        String imageName) {

    public ProductUpsertRequest {
        if (name != null) {
            name = name.replaceAll("\\s+", " ").trim();
        }
        if (description != null) {
            description = description.trim();
        }
        if (imageName != null) {
            imageName = imageName.trim();
        }
    }

    public String normalizedName() {
        return name;
    }

    public String normalizedDescription() {
        return description;
    }
}
