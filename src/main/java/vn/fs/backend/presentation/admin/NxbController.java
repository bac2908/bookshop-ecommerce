package vn.fs.backend.presentation.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.fs.backend.domain.model.NXB;
import vn.fs.backend.application.service.NxbService;

import java.util.Optional;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class NxbController {

    private final NxbService nxbService;

    // Trang danh sách NXB
    @GetMapping("/nxbs")
    public String list(Model model) {
        model.addAttribute("nxbs", nxbService.findAll());
        if (!model.containsAttribute("nxb")) {
            model.addAttribute("nxb", nxbService.newDefaultDTO());
        }
        return "admin/pages/publishers";
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

}
