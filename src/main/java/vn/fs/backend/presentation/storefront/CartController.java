package vn.fs.backend.presentation.storefront;

import java.util.Collection;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import vn.fs.backend.application.dto.CheckoutAddressDTO;
import vn.fs.backend.application.dto.CartSnapshot;
import vn.fs.backend.application.dto.CheckoutResult;
import vn.fs.backend.domain.model.CartItem;
import vn.fs.backend.domain.model.User;
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
        CartSnapshot cart = shoppingCartService.getSnapshot();
        model.addAttribute("cartItems", cart.items());
        model.addAttribute("total", cart.totalAmount());
        model.addAttribute("totalPrice", cart.totalAmount());
        model.addAttribute("totalCartItems", cart.distinctItems());
        model.addAttribute("totalCartQtySum", cart.totalQuantity());
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
        try {
            int desired = Math.max(1, qty);
            shoppingCartService.addOrIncrease(productId, desired);
        } catch (RuntimeException ignored) { }
        return "redirect:/products";
    }

    @PostMapping("/remove/{id}")
    public String remove(@PathVariable("id") Long id) {
        shoppingCartService.removeByProductId(id);
        return "redirect:/checkout";
    }

    /* ============================ Checkout ============================ */

    @PostMapping("/checkout")
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
                CheckoutResult result = checkoutService.beginPaypalCheckout(
                        cur, checkout, request, session);
                if (result.approvalUrl() != null) {
                    return "redirect:" + result.approvalUrl();
                }
                log.warn("No approval_url returned by PayPal.");
                return "redirect:/checkout";
            } else {
                CheckoutResult result = checkoutService.placeOrderCOD(
                        cur, checkout, session);
                model.addAttribute("orderId", result.orderId());
                return "redirect:/checkout_success";
            }
        } catch (Exception e) {
            log.error("Checkout error", e);
            return "redirect:/checkout";
        }
    }

    @GetMapping(URL_PAYPAL_SUCCESS)
    public String successPay(
            @RequestParam("token") String orderIdToken,
            Model model) {
        model.addAttribute("paypalAction", "capture");
        model.addAttribute("paypalToken", orderIdToken);
        return "storefront/pages/paypal-return";
    }

    @PostMapping(URL_PAYPAL_SUCCESS)
    public String completePaypal(
            @RequestParam("token") String orderIdToken,
            Model model) {
        User cur = currentUser();
        if (cur == null) return "redirect:/login";

        try {
            CheckoutResult result = checkoutService.finalizePaypalV2(
                    cur, orderIdToken, session);
            model.addAttribute("orderId", result.orderId());
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
    public String cancelPayment(
            @RequestParam(name = "token") String token,
            Model model) {
        model.addAttribute("paypalAction", "cancel");
        model.addAttribute("paypalToken", token);
        return "storefront/pages/paypal-return";
    }

    @PostMapping("/pay/cancel")
    public String cancelPaypalPayment(@RequestParam("token") String token) {
        User cur = currentUser();
        if (cur == null) return "redirect:/login";
        checkoutService.cancelPaypalCheckout(cur, token, session);
        return "redirect:/payment-cancel";
    }

    @GetMapping("/payment-cancel")
    public String paymentCancelled(Model model) {
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
