package vn.fs.backend.presentation.storefront;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ContactController {

	@GetMapping(value = "/contact")
	public String contact(Model model) {
		return "storefront/pages/contact";
	}
}
