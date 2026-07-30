package vn.fs.backend.application.service;

import vn.fs.backend.domain.model.Role;
import vn.fs.backend.domain.model.User;

import java.util.List;
import java.util.Set;

public interface UserAdminService {
    List<User> listAll();
    User getById(Long id);
    List<Role> listAllRoles();
    Set<Long> roleIdsOf(User user);
    void updateRolesAndStatus(Long userId, List<Long> roleIds, boolean enabled);
}
