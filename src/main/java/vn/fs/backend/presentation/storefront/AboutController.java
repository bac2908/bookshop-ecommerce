package vn.fs.backend.presentation.storefront;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AboutController {

	@GetMapping(value = "/aboutUs")
	public String about(Model model) {
		return "storefront/pages/about";
	}
}
