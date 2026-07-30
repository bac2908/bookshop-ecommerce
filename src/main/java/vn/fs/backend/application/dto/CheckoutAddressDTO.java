package vn.fs.backend.application.dto;

import lombok.Data;

import jakarta.validation.constraints.*;

@Data
public class CheckoutAddressDTO {

    // Email hiển thị readonly, không bắt buộc; nếu bạn muốn bắt buộc thì thêm @NotBlank
    @Email(message = "Email không hợp lệ")
    @Size(max = 254, message = "Email quá dài")
    private String email;

    // Họ tên hiển thị readonly (lấy từ user), để trống -> không validate
    private String fullName;

    @NotBlank(message = "Vui lòng nhập địa chỉ nhận hàng.")
    @Size(min = 5, max = 200, message = "Địa chỉ từ 5-200 ký tự.")
    private String address;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(
            regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$",
            message = "Số điện thoại VN không hợp lệ. Ví dụ: 090xxxxxxx hoặc +843xxxxxxx"
    )
    private String phone;

    @Size(max = 255, message = "Ghi chú tối đa 255 ký tự.")
    private String note;

    @NotBlank(message = "Vui lòng chọn phương thức thanh toán.")
    private String paymentMethod = "cod";
}
