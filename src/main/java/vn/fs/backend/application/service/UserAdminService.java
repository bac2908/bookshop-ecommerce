package vn.fs.backend.application.service;

import vn.fs.backend.domain.model.Role;
import vn.fs.backend.domain.model.User;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface UserAdminService {
    List<User> listAll();
    Page<User> search(String keyword, Boolean enabled, String role, Pageable pageable);
    User getById(Long id);
    List<Role> listAllRoles();
    Set<Long> roleIdsOf(User user);
    User updateRolesAndStatus(
            Long userId,
            List<Long> roleIds,
            boolean enabled,
            String actorLogin);
}
