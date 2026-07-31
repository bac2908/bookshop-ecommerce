package vn.fs.backend.presentation.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.backend.domain.model.Category;
import vn.fs.backend.domain.model.NXB;
import vn.fs.backend.domain.model.Product;
import vn.fs.backend.infrastructure.persistence.CategoryRepository;
import vn.fs.backend.infrastructure.persistence.NxbRepository;
import vn.fs.backend.application.service.ProductService;

import java.text.SimpleDateFormat;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final NxbRepository nxbRepository;
    @ModelAttribute("products")
    public List<Product> products(Model model) {
        List<Product> list = productService.findAll();
        model.addAttribute("products", list);
        return list;
    }
    @ModelAttribute("categoryList")
    public List<Category> categoryActiveList() {
        return categoryRepository.findByStatusTrueOrderByCategoryNameAsc();
    }
    @ModelAttribute("nxbList")
    public List<NXB> nxbActiveList() {
        return nxbRepository.findByStatusTrueOrderByNameAsc();
    }
    @ModelAttribute("categoryAllList")
    public List<Category> categoryAllList() { return categoryRepository.findAllForDropdown(); }
    @ModelAttribute("nxbAllList")
    public List<NXB> nxbAllList() { return nxbRepository.findAllForDropdown(); }

    // ép kiểu ngày cho form (yyyy-MM-dd)
    @InitBinder
    public void initBinder(WebDataBinder binder) {
        var sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(true);
        binder.registerCustomEditor(java.util.Date.class, new CustomDateEditor(sdf, true));
    }

    // trang list
    @GetMapping("/products")
    public String page(Model model) {
        if (!model.containsAttribute("product")) {
            model.addAttribute("product", productService.newDefaultDTO());
        }
        return "admin/pages/products";
    }

    // trang edit theo id
    @GetMapping("/editProduct/{id}")
    public String editPage(@PathVariable Long id, ModelMap model, RedirectAttributes ra) {
        return productService.findById(id)
                .map(p -> { model.addAttribute("product", productService.toDTO(p)); return "admin/pages/product-form"; })
                .orElseGet(() -> {
                    ra.addFlashAttribute("message", "Không tìm thấy sản phẩm!");
                    ra.addFlashAttribute("alertType", "danger");
                    return "redirect:/admin/products";
                });
    }

}
