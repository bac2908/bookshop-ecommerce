package vn.fs.backend.presentation.storefront;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping(value = "/login")
    public String login() {
        return "storefront/pages/login";
    }
}
