package vn.fs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import vn.fs.dto.ChangePasswordInProfile;
import vn.fs.entities.User;
import vn.fs.repository.UserRepository;

import jakarta.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfilePasswordController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @GetMapping("/change-password")
    public String showChangePassword(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ChangePasswordInProfile());
        }
        return "web/profile-change-password";
    }

    @PostMapping("/change-password")
    @Transactional
    public String doChangePassword(@Valid @ModelAttribute("form") ChangePasswordInProfile form,
                                   BindingResult br,
                                   Model model) {

        if (br.hasErrors()) {
            return "web/profile-change-password";
        }

        // Lấy user đang đăng nhập (có thể là username hoặc email)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String loginId = (auth == null) ? "" : auth.getName();

        Optional<User> opt = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(loginId, loginId);
        if (opt.isEmpty()) {
            model.addAttribute("error", "Không tìm thấy tài khoản hiện tại.");
            return "web/profile-change-password";
        }

        User me = opt.get();

        // Kiểm tra mật khẩu hiện tại
        if (!passwordEncoder.matches(form.getCurrentPassword(), me.getPassword())) {
            model.addAttribute("error", "Mật khẩu hiện tại không đúng.");
            return "web/profile-change-password";
        }

        // Xác nhận trùng khớp
        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("error", "Xác nhận mật khẩu không khớp.");
            return "web/profile-change-password";
        }

        // Không cho đặt y hệt mật khẩu cũ (khuyến nghị)
        if (passwordEncoder.matches(form.getNewPassword(), me.getPassword())) {
            model.addAttribute("error", "Mật khẩu mới phải khác mật khẩu hiện tại.");
            return "web/profile-change-password";
        }

        // Cập nhật field-level để tránh validate toàn entity
        String hashed = passwordEncoder.encode(form.getNewPassword());
        int updated = userRepository.updatePasswordByEmail(me.getEmail(), hashed);
        if (updated <= 0) {
            model.addAttribute("error", "Cập nhật mật khẩu thất bại. Vui lòng thử lại.");
            return "web/profile-change-password";
        }

        // ✅ Thành công: vẫn giữ nguyên session & đăng nhập
        model.addAttribute("message", "Đổi mật khẩu thành công!");
        model.addAttribute("showHomeLink", true);       // để view hiện nút về trang chủ
        model.addAttribute("form", new ChangePasswordInProfile());
        return "web/profile-change-password";
    }
}
