package vn.fs.backend.presentation.storefront;

import java.util.Collection;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import vn.fs.backend.application.dto.CheckoutAddressDTO;
import vn.fs.backend.domain.model.CartItem;
import vn.fs.backend.domain.model.Product;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.ProductRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;
import vn.fs.backend.application.service.CheckoutService;
import vn.fs.backend.application.service.ShoppingCartService;

import jakarta.validation.Valid;

@Controller
public class CartController {

    @Autowired private HttpSession session;

    @Autowired private ShoppingCartService shoppingCartService;
    @Autowired private CheckoutService checkoutService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;

    private final Logger log = LoggerFactory.getLogger(getClass());

    public static final String URL_PAYPAL_SUCCESS = "pay/success";
    public static final String URL_PAYPAL_CANCEL  = "pay/cancel";

    /* ============================ Helpers ============================ */

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        String login = auth.getName();
        return userRepository.findByUsernameIgnoreCase(login)
                .or(() -> userRepository.findByEmailIgnoreCase(login))
                .or(() -> userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(login, login))
                .orElse(null);
    }

    private void bindCartToModel(Model model) {
        Collection<CartItem> cartItems = shoppingCartService.getCartItems();
        model.addAttribute("cartItems", cartItems);
        model.addAttribute("total", shoppingCartService.getAmount());
        model.addAttribute("totalPrice", shoppingCartService.getAmount());
        model.addAttribute("totalCartItems", shoppingCartService.getDistinctCount());
        model.addAttribute("totalCartQtySum", shoppingCartService.getQuantitySum());
    }

    /* ============================ Pages ============================ */

    @GetMapping("/shoppingCart_checkout")
    public String shoppingCart(Model model) {
        bindCartToModel(model);
        User me = currentUser();

        if (!model.containsAttribute("checkout")) {
            CheckoutAddressDTO dto = new CheckoutAddressDTO();
            if (me != null) {
                dto.setEmail(me.getEmail());
                dto.setFullName(me.getName());
                dto.setPhone(me.getPhone());
            }
            model.addAttribute("checkout", dto);
        }
        return "storefront/pages/checkout";
    }

    @GetMapping("/checkout")
    public String checkOut(Model model) {
        return shoppingCart(model);
    }

    /* ============================ Cart ops ============================ */

    @PostMapping("/addToCart")
    public String add(@RequestParam("productId") Long productId,
                      @RequestParam(value = "qty", defaultValue = "1") int qty) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product != null) {
            int desired = Math.max(1, qty);
            shoppingCartService.addOrIncrease(productId, desired);
        }
        return "redirect:/products";
    }

    @PostMapping("/remove/{id}")
    public String remove(@PathVariable("id") Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product != null) {
            CartItem item = new CartItem();
            BeanUtils.copyProperties(product, item);
            item.setProduct(product);
            item.setId(id);
            shoppingCartService.remove(item);
        }
        return "redirect:/checkout";
    }

    /* ============================ Checkout ============================ */

    @PostMapping("/checkout")
    @Transactional
    public String checkedOut(@Valid @ModelAttribute("checkout") CheckoutAddressDTO checkout,
                             BindingResult br,
                             Model model,
                             HttpServletRequest request) {

        Collection<CartItem> cartItems = shoppingCartService.getCartItems();
        if (cartItems == null || cartItems.isEmpty()) {
            return "redirect:/products";
        }

        bindCartToModel(model);

        if (br.hasErrors()) {
            return "storefront/pages/checkout";
        }

        User cur = currentUser();
        if (cur == null) return "redirect:/login";

        String method = (checkout.getPaymentMethod() == null)
                ? "cod" : checkout.getPaymentMethod().trim().toLowerCase();

        try {
            if (StringUtils.equals(method, "paypal")) {
                String approvalUrl = checkoutService.beginPaypalCheckout(cur, checkout, request, session);
                if (approvalUrl != null) return "redirect:" + approvalUrl;
                log.warn("No approval_url returned by PayPal.");
                return "redirect:/checkout";
            } else {
                Long orderId = checkoutService.placeOrderCOD(cur, checkout, session);
                model.addAttribute("orderId", orderId);
                return "redirect:/checkout_success";
            }
        } catch (Exception e) {
            log.error("Checkout error", e);
            return "redirect:/checkout";
        }
    }

    /** v2: PayPal redirect về với token=orderId */
    @GetMapping(URL_PAYPAL_SUCCESS)
    @Transactional
    public String successPay(@RequestParam("token") String orderIdToken, Model model) {
        User cur = currentUser();
        if (cur == null) return "redirect:/login";

        try {
            Long orderId = checkoutService.finalizePaypalV2(cur, orderIdToken, session);
            model.addAttribute("orderId", orderId);
            return "redirect:/checkout_paypal_success";
        } catch (Exception e) {
            log.error("PayPal v2 finalize error", e);
            return "redirect:/";
        }
    }

    @GetMapping("/checkout_success")
    public String checkoutSuccess(Model model) {
        return "storefront/pages/checkout-success";
    }

    @GetMapping("/checkout_paypal_success")
    public String paypalSuccess(Model model) {
        return "storefront/pages/checkout-paypal-success";
    }

    @GetMapping("/pay/cancel")
    public String cancelPayment(@RequestParam(name = "token", required = false) String token, Model model) {
        model.addAttribute("message", "Bạn đã hủy thanh toán qua PayPal.");
        return "storefront/pages/payment-cancel";
    }
    // Tăng số lượng 1 đơn vị (hoặc step) rồi redirect về nơi gọi
    @PostMapping("/cart/inc")
    public String cartInc(@RequestParam("productId") Long productId,
                          @RequestParam(value = "step", defaultValue = "1") int step,
                          @RequestParam(value = "redirect", required = false) String redirect,
                          HttpServletRequest req) {
        try {
            shoppingCartService.increase(productId, Math.max(1, step));
        } catch (IllegalStateException ex) {
            // chưa đăng nhập hoặc giỏ chưa sẵn sàng
            return "redirect:/login";
        }
        return redirectBack(redirect, req);
    }

    // Giảm số lượng 1 đơn vị (không thấp hơn 1) rồi redirect về nơi gọi
    @PostMapping("/cart/dec")
    public String cartDec(@RequestParam("productId") Long productId,
                          @RequestParam(value = "step", defaultValue = "1") int step,
                          @RequestParam(value = "redirect", required = false) String redirect,
                          HttpServletRequest req) {
        try {
            shoppingCartService.decrease(productId, Math.max(1, step));
        } catch (IllegalStateException ex) {
            return "redirect:/login";
        }
        return redirectBack(redirect, req);
    }

    // Helper: điều hướng về trang phù hợp
    private String redirectBack(String redirect, HttpServletRequest req) {
        if ("checkout".equalsIgnoreCase(redirect)) return "redirect:/checkout";
        if ("mini".equalsIgnoreCase(redirect)) {
            String ref = req.getHeader("Referer");
            return "redirect:" + (ref != null ? ref : "/");
        }
        return "redirect:/products";
    }

}
