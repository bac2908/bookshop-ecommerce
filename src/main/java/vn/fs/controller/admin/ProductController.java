package vn.fs.controller.admin;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.dto.ProductDTO;
import vn.fs.entities.Category;
import vn.fs.entities.NXB;
import vn.fs.entities.Product;
import vn.fs.entities.User;
import vn.fs.repository.CategoryRepository;
import vn.fs.repository.NxbRepository;
import vn.fs.repository.UserRepository;
import vn.fs.service.ImageStorageService;
import vn.fs.service.ProductService;

import jakarta.validation.Valid;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final CategoryRepository categoryRepository;
    private final NxbRepository nxbRepository;
    private final UserRepository userRepository;
    private final ImageStorageService image;

    // bind mấy dữ liệu dùng chung cho layout
    @ModelAttribute("user")
    public User bindUser(Model model, Principal principal) {
        if (principal == null) return null;
        User u = userRepository.findByEmail(principal.getName());
        model.addAttribute("user", u);
        return u;
    }
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
        return "admin/products";
    }

    // trang edit theo id
    @GetMapping("/editProduct/{id}")
    public String editPage(@PathVariable Long id, ModelMap model, RedirectAttributes ra) {
        return productService.findById(id)
                .map(p -> { model.addAttribute("product", productService.toDTO(p)); return "admin/editProduct"; })
                .orElseGet(() -> {
                    ra.addFlashAttribute("message", "Không tìm thấy sản phẩm!");
                    ra.addFlashAttribute("alertType", "danger");
                    return "redirect:/admin/products";
                });
    }

    // thêm mới (ảnh bắt buộc: có file hoặc đã có tên ảnh tạm trong DTO)
    @PostMapping("/addProduct")
    public String add(@Valid @ModelAttribute("product") ProductDTO dto,
                      BindingResult br,
                      @RequestParam(value = "file", required = false) MultipartFile file,
                      Model model) {

        dto.normalize();

        // check cơ bản ở controller cho nhanh
        if (StringUtils.isBlank(dto.getProductImage()) && (file == null || file.isEmpty())) {
            br.rejectValue("productId", null, "Vui lòng chọn ảnh sản phẩm.");
        }
        if (productService.nameExists(dto.getProductName())) {
            br.rejectValue("productName", null, "Tên sản phẩm đã tồn tại.");
        }
        if (file != null && !file.isEmpty() && image.saveTemp(file) == null) {
            br.rejectValue("productId", null, "Ảnh không hợp lệ (jpg, jpeg, png, webp).");
        }

        if (br.hasErrors()) {
            model.addAttribute("openAddModal", true);
            return "admin/products";
        }

        productService.create(dto, file);
        model.addAttribute("message", "Thêm sản phẩm thành công.");
        model.addAttribute("alertType", "success");
        model.addAttribute("product", productService.newDefaultDTO());
//        return "admin/products";
        return "redirect:/admin/products";
    }

    // cập nhật
    @PostMapping("/editProduct/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("product") ProductDTO dto,
                         BindingResult br,
                         @RequestParam(value = "file", required = false) MultipartFile file,
                         @RequestParam(value = "existingImage", required = false) String existingImage,
                         RedirectAttributes ra) {

        dto.normalize();

        // trùng tên (trừ chính nó)
        if (productService.nameExistsOther(dto.getProductName(), id)) {
            br.rejectValue("productName", null, "Tên sản phẩm đã tồn tại.");
        }
        // nếu có file mới mà save tạm fail → báo lỗi
        if (file != null && !file.isEmpty() && image.saveTemp(file) == null) {
            br.rejectValue("productId", null, "Ảnh tải lên không hợp lệ (jpg, jpeg, png, webp).");
        }
        if (br.hasErrors()) return "admin/editProduct";

        productService.update(id, dto, file, existingImage);
        ra.addFlashAttribute("message", "Cập nhật sản phẩm thành công.");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/products";
    }

    // nhập thêm kho (cho phép /increaseStock/{id} hoặc /increaseStock/{id}/{qty})
    @PostMapping({"/increaseStock/{id}", "/increaseStock/{id}/{qty}"})
    public String increaseStock(@PathVariable Long id,
                                @PathVariable(value = "qty", required = false) Integer qtyInPath,
                                @RequestParam(value = "qty", required = false) Integer qtyParam,
                                RedirectAttributes ra) {
        int qty = qtyInPath != null ? qtyInPath : (qtyParam != null ? qtyParam : 0);
        if (qty <= 0 || qty > 1_000_000) {
            ra.addFlashAttribute("message", "Số lượng nhập không hợp lệ.");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/products";
        }
        var p = productService.findById(id);
        if (p.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy sản phẩm!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/products";
        }
        boolean ok = productService.increaseStock(id, qty);
        if (ok) {
            ra.addFlashAttribute("message", "Đã nhập thêm " + qty + " cho \"" + p.get().getProductName() + "\".");
            ra.addFlashAttribute("alertType", "success");
        } else {
            ra.addFlashAttribute("message", "Không thể cập nhật kho.");
            ra.addFlashAttribute("alertType", "danger");
        }
        return "redirect:/admin/products";
    }

    // xoá: nếu có đơn → ẩn, còn lại xoá cứng
    @PostMapping("/deleteProduct/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        var opt = productService.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy sản phẩm!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/products";
        }
        boolean hard = productService.softOrHardDelete(id);
        if (hard) {
            ra.addFlashAttribute("message", "Xóa vĩnh viễn sản phẩm thành công.");
            ra.addFlashAttribute("alertType", "success");
        } else {
            ra.addFlashAttribute("message", "Sản phẩm đã phát sinh đơn hàng → đã chuyển sang trạng thái ẨN.");
            ra.addFlashAttribute("alertType", "warning");
        }
        return "redirect:/admin/products";
    }

    // mở bán lại
    @PostMapping("/restoreProduct/{id}")
    public String restore(@PathVariable Long id, RedirectAttributes ra) {
        productService.restore(id);
        ra.addFlashAttribute("message", "Đã mở bán lại sản phẩm.");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/products";

    }
}
