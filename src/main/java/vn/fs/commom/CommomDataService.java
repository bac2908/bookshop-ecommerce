package vn.fs.commom;

import java.util.Collection;
import java.util.Locale;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import vn.fs.entities.CartItem;
import vn.fs.entities.Order;
import vn.fs.entities.User;
import vn.fs.repository.CategoryRepository;
import vn.fs.repository.FavoriteRepository;
import vn.fs.repository.NxbRepository;
import vn.fs.repository.ProductRepository;
import vn.fs.service.ShoppingCartService;

@Service
public class CommomDataService {

    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private ShoppingCartService shoppingCartService;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private NxbRepository nxbRepository;

    @Autowired public JavaMailSender emailSender;
    @Autowired private TemplateEngine templateEngine;

     // Đổ dữ liệu dùng chung cho header/navbar/footer.
    public void commonData(Model model, User user) {
        // Yêu thích
        Integer totalSave = 0;
        try {
            if (user != null) {
                totalSave = favoriteRepository.selectCountSave(user.getUserId());
            }
        } catch (Exception ignored) { }
        model.addAttribute("totalSave", totalSave);

        // Giỏ hàng
        int distinct = 0;
        int qtySum = 0;
        double totalPrice = 0.0;
        Collection<CartItem> cartItems = null;
        try {
            cartItems = shoppingCartService.getCartItems();
            totalPrice = shoppingCartService.getAmount();
            if (cartItems != null) {
                distinct = cartItems.size();
                for (CartItem it : cartItems) {
                    if (it != null) qtySum += it.getQuantity();
                }
            }
        } catch (Exception ignored) { }
        // giữ tên cũ để không vỡ view
        model.addAttribute("totalCartItems", distinct);
        model.addAttribute("totalCartDistinct", distinct);
        model.addAttribute("totalCartQtySum", qtySum);
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("totalPrice", totalPrice);

        // Navbar: dùng phiên bản đã lọc ACTIVE + có hàng
        model.addAttribute("categoryList", categoryRepository.findActiveForMenu());
        model.addAttribute("nxbList", nxbRepository.findActiveForMenu());

        // User hiện tại
        model.addAttribute("user", user);

        // Gợi ý danh mục + số lượng (chỉ một lần)
        model.addAttribute("countProductByCategory",
                productRepository.listCategoryByProductNameActive());
    }

    /// Gửi email xác nhận đặt hàng
    public void sendSimpleEmail(String email, String subject, String contentEmail,
                                Collection<CartItem> cartItems, double totalPrice, Order orderFinal)
            throws MessagingException {
        Locale locale = LocaleContextHolder.getLocale();

        Context ctx = new Context(locale);
        ctx.setVariable("cartItems", cartItems);
        ctx.setVariable("totalPrice", totalPrice);
        ctx.setVariable("orderFinal", orderFinal);
        ctx.setVariable("contentEmail", contentEmail);

        MimeMessage mimeMessage = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");
        helper.setSubject(subject);
        helper.setTo(email);

        String htmlContent = templateEngine.process("mail/email_en.html", ctx);
        helper.setText(htmlContent, true);

        emailSender.send(mimeMessage);
    }
}
