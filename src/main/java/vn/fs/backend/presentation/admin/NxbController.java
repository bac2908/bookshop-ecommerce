package vn.fs.backend.presentation.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.backend.application.dto.NxbDTO;
import vn.fs.backend.domain.model.NXB;
import vn.fs.backend.application.service.NxbService;
import vn.fs.backend.application.service.ProductService;

import jakarta.validation.Valid;
import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class NxbController {

    private final NxbService nxbService;
    private final ProductService productService;

    // Trang danh sách NXB
    @GetMapping("/nxbs")
    public String list(Model model) {
        model.addAttribute("nxbs", nxbService.findAll());
        if (!model.containsAttribute("nxb")) {
            model.addAttribute("nxb", nxbService.newDefaultDTO());
        }
        return "admin/pages/publishers";
    }

    // Thêm mới NXB
    @PostMapping("/addNxb")
    public String add(@Valid @ModelAttribute("nxb") NxbDTO dto,
                      BindingResult br,
                      Model model,
                      RedirectAttributes ra) {

        dto.normalize(); // cắt khoảng trắng, set default status

        // Check trùng tên
        if (nxbService.nameExists(dto.getName())) {
            br.rejectValue("name", null, "Tên NXB đã tồn tại.");
        }

        // Lỗi thì mở lại modal
        if (br.hasErrors()) {
            model.addAttribute("nxbs", nxbService.findAll());
            model.addAttribute("openAddModal", true);
            return "admin/pages/publishers";
        }

        nxbService.create(dto);
        ra.addFlashAttribute("message", "Thêm NXB thành công!");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/nxbs";
    }

    // Mở trang sửa
    @GetMapping("/editNxb/{id}")
    public String editPage(@PathVariable("id") Long id, Model model, RedirectAttributes ra) {
        Optional<NXB> opt = nxbService.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy NXB!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/nxbs";
        }
        model.addAttribute("nxb", nxbService.toDTO(opt.get()));
        return "admin/pages/publisher-form";
    }

    // Cập nhật NXB
    @PostMapping("/editNxb/{id}")
    public String update(@PathVariable("id") Long id,
                         @Valid @ModelAttribute("nxb") NxbDTO dto,
                         BindingResult br,
                         RedirectAttributes ra) {

        dto.normalize();

        // Check trùng tên (trừ chính nó)
        if (nxbService.nameExistsOther(dto.getName(), id)) {
            br.rejectValue("name", null, "Tên NXB đã tồn tại.");
        }
        if (br.hasErrors()) return "admin/pages/publisher-form";

        nxbService.update(id, dto);
        ra.addFlashAttribute("message", "Cập nhật NXB thành công!");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/nxbs";
    }

    // Ẩn NXB + Ẩn tất cả sản phẩm con; nếu không còn sp tham chiếu thì xóa cứng
    @PostMapping("/deleteNxb/{id}")
    public String delete(@PathVariable("id") Long id, RedirectAttributes ra) {
        Optional<NXB> opt = nxbService.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("message", "Không tìm thấy NXB!");
            ra.addFlashAttribute("alertType", "danger");
            return "redirect:/admin/nxbs";
        }

        boolean hardDeleted = nxbService.hideAndCascade(id);
        if (hardDeleted) {
            ra.addFlashAttribute("message", "Đã ẨN sản phẩm & XÓA vĩnh viễn NXB (không còn sản phẩm tham chiếu).");
            ra.addFlashAttribute("alertType", "success");
        } else {
            ra.addFlashAttribute("message", "Đã NGỪNG bán NXB và ẨN toàn bộ sản phẩm thuộc NXB này.");
            ra.addFlashAttribute("alertType", "success");
        }
        return "redirect:/admin/nxbs";
    }

    // Khôi phục NXB (không tự mở sp con)
    @PostMapping("/restoreNxb/{id}")
    public String restoreNxb(@PathVariable("id") Long id, RedirectAttributes ra) {
        nxbService.restore(id);
        productService.restoreByNxb(id);
        ra.addFlashAttribute("message", "Đã HIỂN THỊ lại NXB (sản phẩm đã mở bán lại).");
        ra.addFlashAttribute("alertType", "success");
        return "redirect:/admin/nxbs";
    }
}
