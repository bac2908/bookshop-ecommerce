package vn.fs.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.commom.CommomDataService;
import vn.fs.entities.Favorite;
import vn.fs.entities.User;
import vn.fs.service.FavoriteService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class FavoriteController extends CommomController {

    private final FavoriteService favoriteService;
    private final CommomDataService commomDataService;

    // trang yêu thích
    @GetMapping("/favorite")
    public String favorite(Model model, User user) {
        if (user == null) return "redirect:/login";
        List<Favorite> favorites = favoriteService.listByUser(user.getUserId());
        commomDataService.commonData(model, user);
        model.addAttribute("favorites", favorites);
        return "web/favorite";
    }

    // thêm
    @PostMapping("/doFavorite")
    public String doFavorite(User user,
                             @RequestParam("id") Long productId,
                             @RequestParam(value = "redirect", defaultValue = "/products") String redirect,
                             RedirectAttributes ra) {
        if (user == null) return "redirect:/login";
        boolean ok = favoriteService.add(user.getUserId(), productId);
        if (!ok) {
            ra.addFlashAttribute("message", "Không thể thêm yêu thích.");
            ra.addFlashAttribute("alertType", "danger");
        }
        return "redirect:" + safeRedirect(redirect);
    }

    // bỏ
    @PostMapping("/doUnFavorite")
    public String doUnFavorite(User user,
                               @RequestParam("id") Long productId,
                               @RequestParam(value = "redirect", defaultValue = "/products") String redirect) {
        if (user == null) return "redirect:/login";
        favoriteService.remove(user.getUserId(), productId);
        return "redirect:" + safeRedirect(redirect);
    }

    // tránh open redirect
    private String safeRedirect(String r) {
        if (r == null) return "/products";
        r = r.trim();
        if (r.startsWith("http://") || r.startsWith("https://") || r.startsWith("//")) return "/products";
        if (!r.startsWith("/")) return "/products";
        return r;
    }
}
