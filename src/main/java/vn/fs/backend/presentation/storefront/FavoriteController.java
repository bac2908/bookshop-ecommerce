package vn.fs.backend.presentation.storefront;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import vn.fs.backend.domain.model.Favorite;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.application.service.FavoriteService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // trang yêu thích
    @GetMapping("/favorite")
    public String favorite(Model model, User user) {
        if (user == null) return "redirect:/login";
        List<Favorite> favorites = favoriteService.listByUser(user.getUserId());
        model.addAttribute("favorites", favorites);
        return "storefront/pages/favorite";
    }

}
