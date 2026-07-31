package vn.fs.backend.presentation.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.backend.application.service.CategoryService;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    /* bind list category cho bảng */
    @ModelAttribute("categories")
    public java.util.List<vn.fs.backend.domain.model.Category> categories(Model model) {
        var list = categoryService.findAll();
        model.addAttribute("categories", list);
        return list;
    }

    /* trang danh sách */
    @GetMapping("/categories")
    public String page(Model model) {
        if (!model.containsAttribute("category")) {
            model.addAttribute("category", categoryService.newDefaultDTO());
        }
        return "admin/pages/categories";
    }

    /* mở trang sửa */
    @GetMapping("/editCategory/{id}")
    public String editPage(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        return categoryService.findById(id)
                .map(c -> { model.addAttribute("category", categoryService.toDTO(c)); return "admin/pages/category-form"; })
                .orElseGet(() -> {
                    ra.addFlashAttribute("message", "Không tìm thấy thể loại!");
                    ra.addFlashAttribute("alertType", "danger");
                    return "redirect:/admin/categories";
                });
    }

}
