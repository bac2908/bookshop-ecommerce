package vn.fs.backend.presentation.storefront;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import vn.fs.backend.application.dto.PendingRegistration;
import vn.fs.backend.application.dto.UserRegisterDTO;
import vn.fs.backend.application.service.OtpRateLimitService;
import vn.fs.backend.domain.model.Role;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.RoleRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;
import vn.fs.backend.security.IpUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class RegisterController {

    private static final String SESSION_PENDING_FORM   = "REGISTER_PENDING_FORM";
    private static final String SESSION_OTP            = "REGISTER_OTP";
    private static final String SESSION_OTP_EXPIRE     = "REGISTER_OTP_EXPIRE";
    private static final String SESSION_OTP_ATTEMPT    = "REGISTER_OTP_ATTEMPT";
    private static final String SESSION_OTP_RESEND     = "REGISTER_OTP_RESEND";
    private static final String SESSION_OTP_LAST_SEND  = "REGISTER_OTP_LAST_SEND";
    private static final String OTP_FLOW               = "REGISTER";
    private static final long OTP_TTL_SECONDS          = 5 * 60;
    private static final long RESEND_COOLDOWN_MS       = 60 * 1000;
    private static final int OTP_MAX_TRY               = 5;
    private static final int RESEND_MAX                = 5;
    private static final SecureRandom SECURE_RANDOM    = new SecureRandom();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;
    private final OtpRateLimitService otpRateLimitService;

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
                             HttpSession session,
                             HttpServletRequest request) {

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

        String normalizedEmail = form.getEmail().trim().toLowerCase(Locale.ROOT);
        if (!otpRateLimitService.tryAcquire(
                OTP_FLOW,
                normalizedEmail,
                IpUtils.getClientIp(request))) {
            model.addAttribute(
                    "error",
                    "Bạn đã yêu cầu quá nhiều mã OTP. Vui lòng thử lại sau.");
            return "storefront/pages/register";
        }

        PendingRegistration pending = new PendingRegistration(
                form.getUsername().trim(),
                normalizedEmail,
                form.getName().trim(),
                form.getPhone().trim(),
                passwordEncoder.encode(form.getPassword()));
        String otp = generateOtp();

        try {
            sendRegistrationOtp(pending.email(), otp);
            model.addAttribute("message", "Mã xác nhận đã được gửi tới email của bạn.");
        } catch (Exception ex) {
            clearRegisterSession(session);
            model.addAttribute("error", "Không thể gửi email OTP. Vui lòng kiểm tra cấu hình mail và thử lại!");
            return "storefront/pages/register";
        }

        storePendingRegistration(session, pending, otp, 0);
        model.addAttribute("email", pending.email());
        return "storefront/pages/confirm-otp-register";
    }

    /* ============== GET /confirmOtpRegister ============== */
    @GetMapping("/confirmOtpRegister")
    public String showConfirmOtp(Model model, HttpSession session) {
        PendingRegistration pending = pendingRegistration(session);
        if (pending == null) {
            model.addAttribute("form", new UserRegisterDTO());
            model.addAttribute("error", "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.");
            return "storefront/pages/register";
        }
        model.addAttribute("email", pending.email());
        return "storefront/pages/confirm-otp-register";
    }

    @PostMapping("/resendOtpRegister")
    public String resendOtp(
            Model model,
            HttpSession session,
            HttpServletRequest request) {
        PendingRegistration pending = pendingRegistration(session);
        if (pending == null) {
            clearRegisterSession(session);
            model.addAttribute("form", new UserRegisterDTO());
            model.addAttribute("error", "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại.");
            return "storefront/pages/register";
        }

        int resendCount = sessionInteger(session, SESSION_OTP_RESEND);
        Long lastSend = (Long) session.getAttribute(SESSION_OTP_LAST_SEND);
        long now = Instant.now().toEpochMilli();
        if (lastSend != null && now - lastSend < RESEND_COOLDOWN_MS) {
            model.addAttribute("email", pending.email());
            model.addAttribute("error", "Vui lòng chờ 60 giây trước khi gửi lại OTP.");
            return "storefront/pages/confirm-otp-register";
        }
        if (resendCount >= RESEND_MAX
                || !otpRateLimitService.tryAcquire(
                        OTP_FLOW,
                        pending.email(),
                        IpUtils.getClientIp(request))) {
            model.addAttribute("email", pending.email());
            model.addAttribute("error", "Bạn đã yêu cầu quá nhiều mã OTP. Vui lòng thử lại sau.");
            return "storefront/pages/confirm-otp-register";
        }

        String otp = generateOtp();
        try {
            sendRegistrationOtp(pending.email(), otp);
        } catch (Exception ex) {
            model.addAttribute("email", pending.email());
            model.addAttribute("error", "Không thể gửi email OTP. Vui lòng thử lại sau.");
            return "storefront/pages/confirm-otp-register";
        }
        storePendingRegistration(session, pending, otp, resendCount + 1);
        model.addAttribute("email", pending.email());
        model.addAttribute("message", "Mã OTP mới đã được gửi tới email của bạn.");
        return "storefront/pages/confirm-otp-register";
    }

    /* ============== POST /confirmOtpRegister ============== */
    @PostMapping("/confirmOtpRegister")
    public String confirmRegister(@RequestParam("otp") String otpInput,
                                  Model model,
                                  HttpSession session) {

        PendingRegistration pending = pendingRegistration(session);
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
            int attempt = sessionInteger(session, SESSION_OTP_ATTEMPT) + 1;
            session.setAttribute(SESSION_OTP_ATTEMPT, attempt);
            if (attempt >= OTP_MAX_TRY) {
                clearRegisterSession(session);
                model.addAttribute("form", new UserRegisterDTO());
                model.addAttribute("error", "Bạn nhập sai OTP quá số lần cho phép. Vui lòng đăng ký lại.");
                return "storefront/pages/register";
            }
            model.addAttribute("email", pending.email());
            model.addAttribute("error", "Mã OTP không chính xác!");
            return "storefront/pages/confirm-otp-register";
        }

        if (userRepository.findByEmailIgnoreCase(pending.email()).isPresent()
                || userRepository.findByUsernameIgnoreCase(pending.username()).isPresent()) {
            clearRegisterSession(session);
            model.addAttribute("form", new UserRegisterDTO());
            model.addAttribute("error", "Email hoặc username đã được sử dụng.");
            return "storefront/pages/register";
        }

        User u = new User();
        u.setUsername(pending.username());
        u.setEmail(pending.email());
        u.setName(pending.name());
        u.setPhone(pending.phone());
        u.setPassword(pending.passwordHash());
        u.setStatus(true);
        u.setRegisterDate(new Date());

        if (u.getAvatar() == null || (u.getAvatar() != null && u.getAvatar().isBlank())) {
            u.setAvatar("user.png");
        }

        Role role = resolveOrCreateRole("ROLE_USER");
        if (role == null) {
            clearRegisterSession(session);
            model.addAttribute("form", new UserRegisterDTO());
            model.addAttribute("error", "Không thể hoàn tất đăng ký. Vui lòng thử lại sau.");
            return "storefront/pages/register";
        }
        Set<Role> roles = new HashSet<>();
        roles.add(role);
        u.setRoles(roles);

        try {
            userRepository.save(u);
        } catch (DataIntegrityViolationException ex) {
            clearRegisterSession(session);
            model.addAttribute("form", new UserRegisterDTO());
            model.addAttribute("error", "Email hoặc username đã được sử dụng.");
            return "storefront/pages/register";
        }
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

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private void sendRegistrationOtp(String email, String otp) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(email);
        msg.setSubject("Mã xác nhận đăng ký (OTP) - BookShop");
        msg.setText("Xin chào,\n\nMã OTP của bạn là: " + otp
                + "\nHiệu lực trong 5 phút.\n\nTrân trọng,\nBookShop");
        mailSender.send(msg);
    }

    private void storePendingRegistration(
            HttpSession session,
            PendingRegistration pending,
            String otp,
            int resendCount) {
        session.setAttribute(SESSION_PENDING_FORM, pending);
        session.setAttribute(SESSION_OTP, otp);
        session.setAttribute(
                SESSION_OTP_EXPIRE,
                Instant.now().plusSeconds(OTP_TTL_SECONDS).toEpochMilli());
        session.setAttribute(SESSION_OTP_ATTEMPT, 0);
        session.setAttribute(SESSION_OTP_RESEND, resendCount);
        session.setAttribute(SESSION_OTP_LAST_SEND, Instant.now().toEpochMilli());
    }

    private PendingRegistration pendingRegistration(HttpSession session) {
        Object pending = session.getAttribute(SESSION_PENDING_FORM);
        return pending instanceof PendingRegistration registration
                ? registration
                : null;
    }

    private int sessionInteger(HttpSession session, String attribute) {
        Object value = session.getAttribute(attribute);
        return value instanceof Integer number ? number : 0;
    }

    private void clearRegisterSession(HttpSession session) {
        session.removeAttribute(SESSION_PENDING_FORM);
        session.removeAttribute(SESSION_OTP);
        session.removeAttribute(SESSION_OTP_EXPIRE);
        session.removeAttribute(SESSION_OTP_ATTEMPT);
        session.removeAttribute(SESSION_OTP_RESEND);
        session.removeAttribute(SESSION_OTP_LAST_SEND);
    }
}
