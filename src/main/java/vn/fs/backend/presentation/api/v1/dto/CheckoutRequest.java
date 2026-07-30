package vn.fs.backend.presentation.api.v1.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CheckoutRequest")
public record CheckoutRequest(
        @NotBlank(message = "Vui lòng nhập địa chỉ nhận hàng")
        @Size(min = 5, max = 200, message = "Địa chỉ phải từ 5-200 ký tự")
        String address,

        @NotBlank(message = "Vui lòng nhập số điện thoại")
        @Pattern(
                regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$",
                message = "Số điện thoại Việt Nam không hợp lệ")
        String phone,

        @Size(max = 255, message = "Ghi chú tối đa 255 ký tự")
        String note,

        @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
        @Pattern(
                regexp = "(?i)COD|PAYPAL",
                message = "paymentMethod chỉ nhận COD hoặc PAYPAL")
        @Schema(example = "COD")
        String paymentMethod) {
}
