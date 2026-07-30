package vn.fs.backend.application.service;

import java.util.Collection;
import java.util.Locale;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import vn.fs.backend.domain.model.CartItem;
import vn.fs.backend.domain.model.Order;

/**
 * Gửi email xác nhận đơn hàng.
 *
 * <p>Service này thuộc application layer để luồng checkout không phụ thuộc
 * vào presentation/view-support.</p>
 */
@Service
@RequiredArgsConstructor
public class OrderConfirmationMailService {

    private static final String TEMPLATE = "email/order-confirmation";

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void send(String email, String subject, String content,
                     Collection<CartItem> cartItems, double totalPrice, Order order)
            throws MessagingException {
        Locale locale = LocaleContextHolder.getLocale();
        Context context = new Context(locale);
        context.setVariable("cartItems", cartItems);
        context.setVariable("totalPrice", totalPrice);
        context.setVariable("orderFinal", order);
        context.setVariable("contentEmail", content);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
        helper.setSubject(subject);
        helper.setTo(email);
        helper.setText(templateEngine.process(TEMPLATE, context), true);
        mailSender.send(message);
    }
}
