package vn.fs.backend.presentation.storefront;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import vn.fs.backend.application.dto.UserRegisterDTO;
import vn.fs.backend.domain.model.Role;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.RoleRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class RegisterController {

    private static final String SESSION_PENDING_FORM   = "REGISTER_PENDING_FORM";
    private static final String SESSION_OTP            = "REGISTER_OTP";
    private static final String SESSION_OTP_EXPIRE     = "REGISTER_OTP_EXPIRE";
    private static final long   OTP_TTL_SECONDS        = 5 * 60; // 5 phút

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private final JavaMailSender mailSender;

    @GetMapping("/register")
    public String showRegister(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new UserRegisterDTO());
        }
        return "storefront/pages/register";
    }

    @PostMapping("/register")
    public String doRegister(@Valid @ModelAttribute("form") UserRegisterDTO form,
                             BindingResult br,
                             Model model,
                             HttpSession session) {

        // Validate DTO
        if (br.hasErrors()) return "storefront/pages/register";

        // Unique checks
        if (userRepository.findByEmailIgnoreCase(form.getEmail()).isPresent()) {
            br.rejectValue("email", "email.taken", "Email đã tồn tại");
        }
        if (userRepository.findByUsernameIgnoreCase(form.getUsername()).isPresent()) {
            br.rejectValue("username", "username.taken", "Username đã tồn tại");
        }
        if (br.hasErrors()) return "storefront/pages/register";

        // Phát OTP
        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        long expireAt = Instant.now().plusSeconds(OTP_TTL_SECONDS).toEpochMilli();

        // LƯU FORM VÀO SESSION (tạm thời, trước khi xác thực OTP)
        session.setAttribute(SESSION_PENDING_FORM, form);
        session.setAttribute(SESSION_OTP, otp);
        session.setAttribute(SESSION_OTP_EXPIRE, expireAt);

        // GỬI EMAIL OTP (nếu cấu hình mail đầy đủ)
        try {
            if (mailSender == null) throw new IllegalStateException("MailSender bean not available");
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(form.getEmail());
            msg.setSubject("Mã xác nhận đăng ký (OTP) - BookShop");
            msg.setText("Xin chào,\n\nMã OTP của bạn là: " + otp +
                    "\nHiệu lực trong 5 phút.\n\nTrân trọng,\nBookShop");
            mailSender.send(msg);
            model.addAttribute("message", "Mã xác nhận đã được gửi tới email của bạn.");
        } catch (Exception ex) {
            clearRegisterSession(session);
            model.addAttribute("error", "Không thể gửi email OTP. Vui lòng kiểm tra cấu hình mail và thử lại!");
            return "storefront/pages/register";
        }

        model.addAttribute("email", form.getEmail());
        return "storefront/pages/confirm-otp-register";
    }

    /* ============== GET /confirmOtpRegister ============== */
    @GetMapping("/confirmOtpRegister")
    public String showConfirmOtp(Model model, HttpSession session) {
        UserRegisterDTO pending = (UserRegisterDTO) session.getAttribute(SESSION_PENDING_FORM);
        if (pending == null) {
            model.addAttribute("form", new UserRegisterDTO());
            model.addAttribute("error", "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.");
            return "storefront/pages/register";
        }
        model.addAttribute("email", pending.getEmail());
        return "storefront/pages/confirm-otp-register";
    }

    /* ============== POST /confirmOtpRegister ============== */
    @PostMapping("/confirmOtpRegister")
    public String confirmRegister(@RequestParam("otp") String otpInput,
                                  Model model,
                                  HttpSession session) {

        UserRegisterDTO pending = (UserRegisterDTO) session.getAttribute(SESSION_PENDING_FORM);
        String otp = (String) session.getAttribute(SESSION_OTP);
        Long expireAt = (Long) session.getAttribute(SESSION_OTP_EXPIRE);

        if (pending == null || otp == null || expireAt == null) {
            clearRegisterSession(session);
            model.addAttribute("form", new UserRegisterDTO());
            model.addAttribute("error", "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.");
            return "storefront/pages/register";
        }

        if (Instant.now().toEpochMilli() > expireAt) {
            clearRegisterSession(session);
            model.addAttribute("form", new UserRegisterDTO());
            model.addAttribute("error", "Mã OTP đã hết hạn. Vui lòng đăng ký lại.");
            return "storefront/pages/register";
        }

        if (!otp.equals(otpInput == null ? "" : otpInput.trim())) {
            model.addAttribute("email", pending.getEmail());
            model.addAttribute("error", "Mã OTP không chính xác!");
            return "storefront/pages/confirm-otp-register";
        }

        User u = new User();
        u.setUsername(pending.getUsername().trim());
        u.setEmail(pending.getEmail().trim().toLowerCase());
        u.setName(pending.getName().trim());
        u.setPhone(pending.getPhone().trim());
        u.setPassword(passwordEncoder.encode(pending.getPassword()));
        u.setStatus(true);
        u.setRegisterDate(new Date());

        if (u.getAvatar() == null || (u.getAvatar() != null && u.getAvatar().isBlank())) {
            u.setAvatar("user.png");
        }

        Role role = resolveOrCreateRole("CUSTOMER");
        Set<Role> roles = new HashSet<>();
        if (role != null) roles.add(role);
        u.setRoles(roles);

        userRepository.save(u);
        clearRegisterSession(session);

        return "redirect:/login?registered=1";
    }

    private Role resolveOrCreateRole(String name) {
        Role role = roleRepository.findByName(name);
        if (role != null) return role;
        Role created = new Role(name);
        try {
            return roleRepository.save(created);
        } catch (Exception ex) {
            return null;
        }
    }

    private void clearRegisterSession(HttpSession session) {
        session.removeAttribute(SESSION_PENDING_FORM);
        session.removeAttribute(SESSION_OTP);
        session.removeAttribute(SESSION_OTP_EXPIRE);
    }
}
