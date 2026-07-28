package vn.fs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.fs.commom.CommomDataService;
import vn.fs.entities.Product;
import vn.fs.entities.User;
import vn.fs.repository.FavoriteRepository;
import vn.fs.repository.ProductRepository;
import vn.fs.repository.UserRepository;
import vn.fs.util.SessionUtils;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    @Autowired private CommomDataService commomDataService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private FavoriteRepository favoriteRepository;

    @GetMapping({"/", "/home"})
    public String home(Model model, HttpSession session) {
        User current = resolveCurrentUser(session);
        commomDataService.commonData(model, current);
        List<Product> newArrivals = safe(productRepository.listProductNew20());
        List<Product> featured = safe(productRepository.topDiscount20());
        List<Object[]> rows = safe(productRepository.bestSaleProduct20());
        List<Long> ids = rows.stream()
                .map(r -> r[0] == null ? null : ((Number) r[0]).longValue())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        Map<Long, Product> map = ids.isEmpty()
                ? Collections.emptyMap()
                : productRepository.findByInventoryIds(ids).stream()
                        .collect(Collectors.toMap(Product::getProductId, p -> p));
        List<Product> bestSellers = ids.stream()
                .map(map::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        markFavorites(current, newArrivals);
        markFavorites(current, featured);
        markFavorites(current, bestSellers);

        model.addAttribute("productList", newArrivals);          // Mặt hàng mới về
        model.addAttribute("featuredProducts", featured);        // Sách nổi bật
        model.addAttribute("bestSaleProduct20", bestSellers);    // Sách bán chạy

        // fallback cho các th:each nếu list rỗng
        if (!model.containsAttribute("countProductByCategory")) {
            model.addAttribute("countProductByCategory", Collections.emptyList());
        }

        return "web/home";
    }

    private void markFavorites(User user, List<Product> products) {
        if (user == null || products == null || products.isEmpty()) return;

        List<Long> ids = products.stream()
                .map(Product::getProductId)
                .collect(Collectors.toList());

        List<Long> favIds = favoriteRepository.findProductIdsByUserIdAndProductIds(user.getUserId(), ids);

        for (Product p : products) {
            if (favIds.contains(p.getProductId())) {
                p.setFavorite(true);
            }
        }
    }

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

    private static <T> List<T> safe(List<T> list) {
        return (list == null) ? Collections.emptyList() : list;
    }
}
