package vn.fs.backend.presentation.support;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import vn.fs.backend.domain.model.CartItem;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.CategoryRepository;
import vn.fs.backend.infrastructure.persistence.FavoriteRepository;
import vn.fs.backend.infrastructure.persistence.NxbRepository;
import vn.fs.backend.infrastructure.persistence.ProductRepository;
import vn.fs.backend.application.service.ShoppingCartService;

@Service
public class CommonViewDataService {

    @Autowired private FavoriteRepository favoriteRepository;
    @Autowired private ShoppingCartService shoppingCartService;
    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private NxbRepository nxbRepository;

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

}
