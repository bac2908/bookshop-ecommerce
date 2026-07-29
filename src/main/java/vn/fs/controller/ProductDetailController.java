package vn.fs.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import vn.fs.commom.CommomDataService;
import vn.fs.entities.Product;
import vn.fs.entities.User;
import vn.fs.repository.FavoriteRepository;
import vn.fs.repository.ProductRepository;
import vn.fs.repository.UserRepository;
import vn.fs.util.SessionUtils;

import jakarta.servlet.http.HttpSession;


@Controller
public class ProductDetailController extends CommomController {

	@Autowired
	ProductRepository productRepository;

	@Autowired
	CommomDataService commomDataService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private FavoriteRepository favoriteRepository;

	// Lấy user hiện tại từ session hoặc SecurityContext
	private User resolveCurrentUser(HttpSession session) {
		Long uid = SessionUtils.getUserId(session);
		if (uid != null) return userRepository.findById(uid).orElse(null);

		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
			String login = auth.getName();
			User byUsername = userRepository.findByUsername(login);
			return (byUsername != null) ? byUsername : userRepository.findByEmail(login);
		}
		return null;
	}

	// Đánh dấu 1 sản phẩm đã favorite hay chưa
	private void markFavorite(User user, Product product) {
		if (user == null || product == null) return;
		boolean isFav = favoriteRepository.existsByUserIdAndProductId(user.getUserId(), product.getProductId());
		product.setFavorite(isFav);
	}

	@GetMapping(value = "productDetail")
	public String productDetail(@RequestParam("id") Long id, Model model, HttpSession session) {

		User currentUser = resolveCurrentUser(session);

		Product product = productRepository.findById(id).orElse(null);

		// đánh dấu favorite
		markFavorite(currentUser, product);

		model.addAttribute("product", product);
		commomDataService.commonData(model, currentUser);

		if (product != null && product.getCategory() != null) {
			listProductByCategory10(model, product.getCategory().getCategoryId());
		}

		return "web/productDetail";
	}

	// Gợi ý top 10 sản phẩm cùng loại
	public void listProductByCategory10(Model model, Long categoryId) {
		List<Product> products = productRepository.listProductByCategory10(categoryId);
		model.addAttribute("productByCategory", products);
	}
}

