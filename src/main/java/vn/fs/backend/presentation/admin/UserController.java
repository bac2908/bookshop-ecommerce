// src/main/java/vn/fs/controller/admin/UserController.java
package vn.fs.backend.presentation.admin;

import java.security.Principal;
import java.util.List;
import java.util.Set;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import vn.fs.backend.domain.model.Role;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.application.service.UserAdminService;

@Controller
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
public class UserController {

    private final UserAdminService userAdminService;

    // Danh sách người dùng
    @GetMapping
    public String list(Model model, Principal principal,
                       @RequestParam(value = "updated", required = false) String updated,
                       @RequestParam(value = "notfound", required = false) String notfound) {
        if (principal == null) return "redirect:/login";

        List<User> users = userAdminService.listAll();
        model.addAttribute("users", users);

        if (StringUtils.hasText(updated))  model.addAttribute("flash", "Cập nhật thành công!");
        if (StringUtils.hasText(notfound)) model.addAttribute("flashErr", "Không tìm thấy người dùng!");

        return "admin/pages/users";
    }

    // Form chỉnh sửa quyền (ADMIN only)
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) return "redirect:/login";

        User target = userAdminService.getById(id);
        if (target == null) return "redirect:/admin/customers?notfound=1";

        List<Role> allRoles = userAdminService.listAllRoles();
        Set<Long> selectedRoleIds = userAdminService.roleIdsOf(target);

        model.addAttribute("target", target);
        model.addAttribute("allRoles", allRoles);
        model.addAttribute("selectedRoleIds", selectedRoleIds);

        return "admin/pages/user-form";
    }

    // Lưu quyền + trạng thái (ADMIN only)
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/edit/{id}")
    public String update(@PathVariable("id") Long id,
                         @RequestParam(value = "roleIds", required = false) List<Long> roleIds,
                         @RequestParam(value = "enabled", required = false) String enabled // "on" nếu tick
    ) {
        User target = userAdminService.getById(id);
        if (target == null) {
            return "redirect:/admin/customers?notfound=1";
        }

        boolean isEnabled = StringUtils.hasText(enabled);
        userAdminService.updateRolesAndStatus(id, roleIds, isEnabled);

        return "redirect:/admin/customers?updated=1";
    }
}
