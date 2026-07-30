package vn.fs.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.backend.domain.model.Cart;
import vn.fs.backend.domain.model.CartStatus;

import java.util.Date;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser_UserIdAndStatus(Long userId, CartStatus status);

    @Modifying
    @Transactional
    @Query("update Cart c set c.status = vn.fs.backend.domain.model.CartStatus.EXPIRED " +
            "where c.expiresAt < :now and c.status = vn.fs.backend.domain.model.CartStatus.ACTIVE")
    int markExpired(@Param("now") Date now);
    @Modifying
    @Transactional
    @Query("delete from Cart c where c.expiresAt < :now and c.items is empty")
    int deleteExpired(@Param("now") Date now);
}
