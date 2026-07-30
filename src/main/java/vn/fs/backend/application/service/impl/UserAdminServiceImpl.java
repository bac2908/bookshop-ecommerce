// src/main/java/vn/fs/service/impl/UserAdminServiceImpl.java
package vn.fs.backend.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.backend.domain.model.Role;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.RoleRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;
import vn.fs.backend.application.service.UserAdminService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    @Override
    public List<User> listAll() {
        return userRepository.findAll();
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public List<Role> listAllRoles() {
        return roleRepository.findAll();
    }

    @Override
    public Set<Long> roleIdsOf(User user) {
        if (user == null || user.getRoles() == null) return Set.of();
        Set<Long> ids = new HashSet<>();
        user.getRoles().forEach(r -> ids.add(r.getId()));
        return ids;
    }

    @Override
    @Transactional
    public void updateRolesAndStatus(Long userId, List<Long> roleIds, boolean enabled) {
        User u = userRepository.findById(userId).orElse(null);
        if (u == null) return;

        u.setStatus(enabled);

        Set<Role> newRoles = new HashSet<>();
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long rid : roleIds) {
                roleRepository.findById(rid).ifPresent(newRoles::add);
            }
        }
        u.setRoles(newRoles);

        userRepository.save(u);
    }
}
