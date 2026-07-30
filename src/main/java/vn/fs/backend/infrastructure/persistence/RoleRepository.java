package vn.fs.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.fs.backend.domain.model.Role;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
    List<Role> findAllByOrderByNameAsc();
}
