package vn.fs.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.fs.backend.domain.model.LoginIpLock;

import java.util.Optional;

public interface LoginIpLockRepository extends JpaRepository<LoginIpLock, Long> {
    Optional<LoginIpLock> findByIp(String ip);
}
