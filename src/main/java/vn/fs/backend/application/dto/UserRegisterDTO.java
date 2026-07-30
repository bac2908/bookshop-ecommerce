package vn.fs.backend.application.dto;

import lombok.Data;

import jakarta.validation.constraints.*;

@Data
public class UserRegisterDTO {

    @NotBlank(message = "Vui lòng nhập username.")
    @Size(min = 4, max = 50, message = "Username 4–50 ký tự.")
    @Pattern(regexp = "^[A-Za-z0-9]+$", message = "Username chỉ gồm chữ và số, không khoảng trắng/ký tự đặc biệt.")
    private String username;

    @NotBlank @Email @Size(max = 254)
    private String email;

    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(min = 2, max = 50, message = "Tên từ 2-50 ký tự.")
    @Pattern(regexp = "^[A-Za-zÀ-ỹĐđ ]{2,50}$", message = "Tên chỉ chứa chữ cái (có dấu) và khoảng trắng.")
    private String name;

    @NotBlank(message = "Vui lòng nhập số điện thoại.")
    @Pattern(
            regexp = "^(0|\\+84)(3|5|7|8|9)\\d{8}$",
            message = "Số điện thoại VN không hợp lệ. Ví dụ: 090xxxxxxx hoặc +843xxxxxxx"
    )
    private String phone;

    @NotBlank
    @Size(min = 12, max = 128, message = "Mật khẩu tối thiểu 12 ký tự.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[^A-Za-z0-9])\\S{12,128}$",
            message = "Mật khẩu phải có chữ cái, số và ký tự đặc biệt, không khoảng trắng."
    )
    private String password;

    @NotBlank
    private String confirmPassword;

    @AssertTrue(message = "Mật khẩu và xác nhận mật khẩu không khớp.")
    public boolean isConfirmed() {
        if (password == null || confirmPassword == null) return true;
        return password.equals(confirmPassword);
    }
}
