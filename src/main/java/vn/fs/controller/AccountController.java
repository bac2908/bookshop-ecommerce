package vn.fs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import vn.fs.dto.ChangePassword;
import vn.fs.entities.User;
import vn.fs.repository.UserRepository;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Controller
public class AccountController {

    private static final String FP_EMAIL    = "FP_EMAIL";
    private static final String FP_OTP      = "FP_OTP";
    private static final String FP_EXPIRE   = "FP_EXPIRE";
    private static final String FP_ATTEMPT  = "FP_ATTEMPT";
    private static final String FP_RESEND   = "FP_RESEND";
    private static final String FP_LASTSEND = "FP_LASTSEND";

    private static final long OTP_TTL_MS        = 5 * 60 * 1000;
    private static final int  OTP_MAX_TRY       = 5;
    private static final int  RESEND_MAX        = 5;
    private static final long RESEND_COOLDOWNMS = 60 * 1000;

    @Autowired BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired HttpSession session;
    @Autowired UserRepository userRepository;
    @Autowired vn.fs.service.SendMailService sendMailService;

    @GetMapping("/forgotPassword")
    public String forgotPassword() {
        clearFlowSession();
        return "web/forgotPassword";
    }

    @PostMapping("/forgotPassword")
    public ModelAndView forgotPassowrd(ModelMap model, @RequestParam("email") String email) {
        String mail = email == null ? "" : email.trim();
        if (mail.isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập email đã đăng ký.");
            return new ModelAndView("web/forgotPassword", model);
        }
        Optional<User> opt = userRepository.findByEmailIgnoreCase(mail);
        if (opt.isEmpty()) {
            model.addAttribute("error", "Email này chưa đăng ký!");
            return new ModelAndView("web/forgotPassword", model);
        }
        issueOtpAndSend(opt.get().getEmail());
        model.addAttribute("email", opt.get().getEmail());
        model.addAttribute("message", "Mã OTP đã được gửi tới email của bạn. Vui lòng kiểm tra hộp thư!");
        return new ModelAndView("web/confirmOtpForgotPassword", model);
    }

    @PostMapping("/resendOtpForgotPassword")
    public ModelAndView resendOtp(ModelMap model, @RequestParam("email") String email) {
        String savedEmail = (String) session.getAttribute(FP_EMAIL);
        if (savedEmail == null || !savedEmail.equalsIgnoreCase(email)) {
            model.addAttribute("error", "Phiên xác thực đã hết hạn. Vui lòng thực hiện lại.");
            return new ModelAndView("web/forgotPassword", model);
        }
        Integer resendCount = (Integer) session.getAttribute(FP_RESEND);
        Long lastSend = (Long) session.getAttribute(FP_LASTSEND);
        int count = resendCount == null ? 0 : resendCount;
        long now = Instant.now().toEpochMilli();

        if (lastSend != null && (now - lastSend) < RESEND_COOLDOWNMS) {
            long waitMore = (RESEND_COOLDOWNMS - (now - lastSend)) / 1000;
            if (waitMore <= 0) waitMore = 1;
            model.addAttribute("email", savedEmail);
            model.addAttribute("error", "Bạn vừa yêu cầu gửi lại OTP. Vui lòng thử sau " + waitMore + " giây.");
            return new ModelAndView("web/confirmOtpForgotPassword", model);
        }
        if (count >= RESEND_MAX) {
            clearFlowSession();
            model.addAttribute("error", "Bạn đã yêu cầu OTP quá số lần cho phép. Vui lòng thực hiện lại từ đầu.");
            return new ModelAndView("web/forgotPassword", model);
        }

        issueOtpAndSend(savedEmail);
        session.setAttribute(FP_RESEND, count + 1);
        session.setAttribute(FP_LASTSEND, now);

        model.addAttribute("email", savedEmail);
        model.addAttribute("message", "Đã gửi lại OTP. Vui lòng kiểm tra email!");
        return new ModelAndView("web/confirmOtpForgotPassword", model);
    }

    @PostMapping("/confirmOtpForgotPassword")
    public ModelAndView confirm(ModelMap model,
                                @RequestParam("otp") String otpInput,
                                @RequestParam("email") String email) {

        String savedEmail = (String) session.getAttribute(FP_EMAIL);
        String savedOtp   = (String) session.getAttribute(FP_OTP);
        Long   expireAt   = (Long)   session.getAttribute(FP_EXPIRE);
        Integer tries     = (Integer) session.getAttribute(FP_ATTEMPT);

        if (savedEmail == null || savedOtp == null || expireAt == null) {
            model.addAttribute("error", "Phiên xác thực đã hết hạn. Vui lòng thực hiện lại.");
            return new ModelAndView("web/forgotPassword", model);
        }
        if (!savedEmail.equalsIgnoreCase(email)) {
            model.addAttribute("error", "Email không khớp với phiên xác thực.");
            model.addAttribute("email", savedEmail);
            return new ModelAndView("web/confirmOtpForgotPassword", model);
        }
        if (Instant.now().toEpochMilli() > expireAt) {
            clearFlowSession();
            model.addAttribute("error", "Mã OTP đã hết hạn. Vui lòng yêu cầu lại.");
            return new ModelAndView("web/forgotPassword", model);
        }

        int currentTry = (tries == null) ? 0 : tries;
        String input = otpInput == null ? "" : otpInput.trim();
        if (!input.equals(savedOtp)) {
            currentTry++;
            session.setAttribute(FP_ATTEMPT, currentTry);
            model.addAttribute("email", savedEmail);
            if (currentTry >= OTP_MAX_TRY) {
                clearFlowSession();
                model.addAttribute("error", "Bạn nhập sai OTP quá số lần cho phép. Vui lòng thực hiện lại.");
                return new ModelAndView("web/forgotPassword", model);
            }
            model.addAttribute("error", "Mã OTP không đúng, thử lại!");
            return new ModelAndView("web/confirmOtpForgotPassword", model);
        }

        model.addAttribute("email", savedEmail);
        model.addAttribute("changePassword", new vn.fs.dto.ChangePassword());
        return new ModelAndView("web/changePassword", model);
    }

    @GetMapping("/changePassword")
    public ModelAndView showChangePassword(ModelMap model) {
        String savedEmail = (String) session.getAttribute(FP_EMAIL);
        if (savedEmail == null) {
            model.addAttribute("error", "Phiên đặt lại mật khẩu đã hết hạn. Vui lòng thực hiện lại.");
            return new ModelAndView("web/forgotPassword", model);
        }
        if (!model.containsAttribute("changePassword")) {
            model.addAttribute("changePassword", new vn.fs.dto.ChangePassword());
        }
        model.addAttribute("email", savedEmail);
        return new ModelAndView("web/changePassword", model);
    }

    /* ===== STEP 3: ĐỔI MẬT KHẨU – cập nhật field-level để tránh Bean Validation ===== */
    @PostMapping("/changePassword")
    @Transactional
    public ModelAndView changeForm(ModelMap model,
                                   @Valid @ModelAttribute("changePassword") ChangePassword changePassword,
                                   BindingResult result,
                                   @RequestParam("email") String email,
                                   @RequestParam("newPassword") String newPassword,
                                   @RequestParam("confirmPassword") String confirmPassword) {

        String savedEmail = (String) session.getAttribute(FP_EMAIL);
        if (savedEmail == null || !savedEmail.equalsIgnoreCase(email)) {
            model.addAttribute("error", "Phiên đặt lại mật khẩu đã hết hạn. Vui lòng thực hiện lại.");
            return new ModelAndView("web/forgotPassword", model);
        }

        if (result.hasErrors()) {
            model.addAttribute("email", email);
            model.addAttribute("newPassword", newPassword);
            model.addAttribute("confirmPassword", confirmPassword);
            return new ModelAndView("web/changePassword", model);
        }

        if (!changePassword.getNewPassword().equals(changePassword.getConfirmPassword())) {
            model.addAttribute("email", email);
            model.addAttribute("newPassword", newPassword);
            model.addAttribute("confirmPassword", confirmPassword);
            model.addAttribute("error", "Xác nhận mật khẩu không chính xác!");
            return new ModelAndView("web/changePassword", model);
        }

        // Check tồn tại email
        Optional<User> opt = userRepository.findByEmailIgnoreCase(email);
        if (opt.isEmpty()) {
            clearFlowSession();
            model.addAttribute("error", "Không tìm thấy tài khoản tương ứng.");
            return new ModelAndView("web/forgotPassword", model);
        }

        // >>> Field-level UPDATE (tránh validate toàn entity)
        String hashed = bCryptPasswordEncoder.encode(newPassword);
        int updated = userRepository.updatePasswordByEmail(email, hashed);
        if (updated <= 0) {
            model.addAttribute("email", email);
            model.addAttribute("error", "Không thể cập nhật mật khẩu. Vui lòng thử lại.");
            return new ModelAndView("web/changePassword", model);
        }

        clearFlowSession();
        model.addAttribute("message", "Đổi mật khẩu thành công! Bạn có thể đăng nhập bằng mật khẩu mới.");
        model.addAttribute("email", "");
        model.addAttribute("showLoginLink", true);
        return new ModelAndView("web/changePassword", model);
    }

    private void issueOtpAndSend(String email) {
        String otp = String.format("%06d", new Random().nextInt(1_000_000));
        long expireAt = Instant.now().toEpochMilli() + OTP_TTL_MS;

        session.setAttribute(FP_EMAIL, email);
        session.setAttribute(FP_OTP, otp);
        session.setAttribute(FP_EXPIRE, expireAt);
        session.setAttribute(FP_ATTEMPT, 0);

        String body = "<div><h3>Mã xác thực OTP của bạn là: " +
                "<span style=\"color:#119744; font-weight:bold;\">" + otp + "</span></h3>" +
                "<p>Hiệu lực trong 5 phút.</p></div>";

        sendMailService.queue(email, "Quên mật khẩu? - OTP xác nhận", body);

        session.setAttribute(FP_LASTSEND, Instant.now().toEpochMilli());
        if (session.getAttribute(FP_RESEND) == null) session.setAttribute(FP_RESEND, 0);
    }

    private void clearFlowSession() {
        session.removeAttribute(FP_EMAIL);
        session.removeAttribute(FP_OTP);
        session.removeAttribute(FP_EXPIRE);
        session.removeAttribute(FP_ATTEMPT);
        session.removeAttribute(FP_RESEND);
        session.removeAttribute(FP_LASTSEND);
    }
}
