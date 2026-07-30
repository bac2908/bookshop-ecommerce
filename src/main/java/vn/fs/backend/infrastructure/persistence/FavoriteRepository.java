package vn.fs.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.fs.backend.domain.model.Favorite;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    // native cũ
    @Query(value = "SELECT * FROM favorites WHERE product_id = ?1 AND user_id = ?2", nativeQuery = true)
    Favorite selectSaves(Long productId, Long userId);

    @Query(value = "SELECT * FROM favorites WHERE user_id = ?1", nativeQuery = true)
    List<Favorite> selectAllSaves(Long userId);

    @Query(value = "SELECT COUNT(favorite_id) FROM favorites WHERE user_id = ?1", nativeQuery = true)
    Integer selectCountSave(Long userId);

    // JPQL hỗ trợ chỗ khác đang dùng
    @Query("SELECT f.product.productId FROM Favorite f WHERE f.user.userId = :uid AND f.product.productId IN :pids")
    List<Long> findProductIdsByUserIdAndProductIds(@Param("uid") Long uid, @Param("pids") List<Long> productIds);

    // JPQL đúng tên hàm mà ProductDetailController đang gọi
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Favorite f WHERE f.user.userId = :userId AND f.product.productId = :productId")
    boolean existsByUserIdAndProductId(@Param("userId") Long userId, @Param("productId") Long productId);

    // method-name tiện dùng trong service
    List<Favorite> findByUser_UserId(Long userId);
    boolean existsByUser_UserIdAndProduct_ProductId(Long userId, Long productId);
    Optional<Favorite> findByUser_UserIdAndProduct_ProductId(Long userId, Long productId);
    void deleteByUser_UserIdAndProduct_ProductId(Long userId, Long productId);
}
