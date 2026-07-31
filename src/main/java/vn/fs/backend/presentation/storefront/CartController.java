package vn.fs.backend.presentation.storefront;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import vn.fs.backend.application.dto.CheckoutAddressDTO;
import vn.fs.backend.application.dto.CartSnapshot;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.UserRepository;
import vn.fs.backend.application.service.ShoppingCartService;

@Controller
public class CartController {

    @Autowired private ShoppingCartService shoppingCartService;
    @Autowired private UserRepository userRepository;

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

    @GetMapping(URL_PAYPAL_SUCCESS)
    public String successPay(
            @RequestParam("token") String orderIdToken,
            Model model) {
        model.addAttribute("paypalAction", "capture");
        model.addAttribute("paypalToken", orderIdToken);
        return "storefront/pages/paypal-return";
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

    @GetMapping("/payment-cancel")
    public String paymentCancelled(Model model) {
        model.addAttribute("message", "Bạn đã hủy thanh toán qua PayPal.");
        return "storefront/pages/payment-cancel";
    }
}
