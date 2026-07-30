// src/main/java/vn/fs/service/impl/UserAdminServiceImpl.java
package vn.fs.backend.application.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.backend.application.error.AdminUserNotFoundException;
import vn.fs.backend.application.error.InvalidAccessChangeException;
import vn.fs.backend.domain.model.Role;
import vn.fs.backend.domain.model.User;
import vn.fs.backend.infrastructure.persistence.RoleRepository;
import vn.fs.backend.infrastructure.persistence.UserRepository;
import vn.fs.backend.application.service.UserAdminService;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
    public Page<User> search(
            String keyword,
            Boolean enabled,
            String role,
            Pageable pageable) {
        String normalizedKeyword = keyword == null || keyword.isBlank()
                ? null
                : keyword.trim();
        String normalizedRole = role == null || role.isBlank()
                ? null
                : normalizeRole(role);
        Page<Long> idPage = userRepository.searchIdsForAdministration(
                normalizedKeyword,
                enabled,
                normalizedRole,
                pageable);
        if (idPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }

        java.util.Map<Long, User> usersById = userRepository
                .findAllWithRolesByIdIn(idPage.getContent())
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        User::getUserId,
                        user -> user));
        List<User> orderedUsers = idPage.getContent().stream()
                .map(usersById::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        return new PageImpl<>(
                orderedUsers,
                pageable,
                idPage.getTotalElements());
    }

    @Override
    public User getById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public List<Role> listAllRoles() {
        return roleRepository.findAllByOrderByNameAsc();
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
    public User updateRolesAndStatus(
            Long userId,
            List<Long> roleIds,
            boolean enabled,
            String actorLogin) {
        User target = userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new AdminUserNotFoundException(userId));
        User actor = userRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(actorLogin, actorLogin)
                .orElseThrow(() -> new InvalidAccessChangeException(
                        "ADMIN_ACTOR_NOT_FOUND",
                        "Không xác định được quản trị viên đang thao tác"));

        if (roleIds == null || roleIds.isEmpty()) {
            throw new InvalidAccessChangeException(
                    "USER_ROLE_REQUIRED",
                    "Người dùng phải có ít nhất một vai trò");
        }

        Set<Long> requestedIds = new HashSet<>(roleIds);
        List<Role> resolvedRoles = roleRepository.findAllById(requestedIds);
        if (resolvedRoles.size() != requestedIds.size()) {
            throw new InvalidAccessChangeException(
                    "INVALID_ROLE",
                    "Danh sách vai trò chứa giá trị không tồn tại");
        }

        boolean retainsAdmin = resolvedRoles.stream()
                .anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        boolean targetIsActiveAdmin = Boolean.TRUE.equals(target.getStatus())
                && hasRole(target, "ROLE_ADMIN");
        boolean removesActiveAdmin = targetIsActiveAdmin
                && (!enabled || !retainsAdmin);

        if (target.getUserId().equals(actor.getUserId())) {
            if (!enabled) {
                throw new InvalidAccessChangeException(
                        "SELF_DISABLE_FORBIDDEN",
                        "Quản trị viên không thể tự vô hiệu hóa tài khoản");
            }
            if (hasRole(actor, "ROLE_ADMIN") && !retainsAdmin) {
                throw new InvalidAccessChangeException(
                        "SELF_DEMOTION_FORBIDDEN",
                        "Quản trị viên không thể tự gỡ vai trò ADMIN");
            }
        }

        if (removesActiveAdmin
                && userRepository.countActiveAdministrators() <= 1) {
            throw new InvalidAccessChangeException(
                    "LAST_ADMIN_PROTECTED",
                    "Không thể vô hiệu hóa hoặc hạ quyền quản trị viên cuối cùng");
        }

        target.setStatus(enabled);
        target.setRoles(new HashSet<>(resolvedRoles));
        return userRepository.save(target);
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> roleName.equals(role.getName()));
    }

    private String normalizeRole(String role) {
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_")
                ? normalized
                : "ROLE_" + normalized;
    }
}
