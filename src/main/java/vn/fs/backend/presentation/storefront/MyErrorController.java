package vn.fs.backend.presentation.storefront;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class MyErrorController implements ErrorController{

	@RequestMapping("/error")
	public String handleError() {
		return "storefront/pages/not-found";
	}

	public String getErrorPath() {
		return null;
	}
}
