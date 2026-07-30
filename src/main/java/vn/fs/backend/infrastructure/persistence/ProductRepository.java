package vn.fs.backend.infrastructure.persistence;

import java.util.List;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.fs.backend.domain.model.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = true AND p.category.categoryId = :categoryId")
    long countActiveByCategory(@Param("categoryId") Long categoryId);

    @Query(value = "SELECT COUNT(*) FROM products WHERE status = 1 AND nxb_id = :nxbId", nativeQuery = true)
    long countActiveByNxb(@Param("nxbId") Long nxbId);

    long countByCategory_CategoryId(Long categoryId);
    long countByNxb_Id(Long nxbId);


    @Query(value = "SELECT * FROM products WHERE status = 1", nativeQuery = true)
    List<Product> findAllActive();

    @Query(value = "SELECT * FROM products WHERE status = 1 AND category_id = ?1", nativeQuery = true)
    List<Product> listProductByCategoryActive(Long categoryId);

    @Query(value = "SELECT * FROM products WHERE status = 1 AND category_id = ?1 LIMIT 10", nativeQuery = true)
    List<Product> listProductByCategory10(Long categoryId);

    @Query(value = "SELECT * FROM products WHERE status = 1 ORDER BY entered_date DESC LIMIT 20", nativeQuery = true)
    List<Product> listProductNew20();

    @Query(value = "SELECT * FROM products WHERE status = 1 AND product_name LIKE %?1%", nativeQuery = true)
    List<Product> searchProductActive(String keyword);

    @Query(value =
            "SELECT c.category_id, c.category_name, c.category_image, " +
                    "       COUNT(p.product_id) AS so_luong " +
                    "FROM categories c " +
                    "LEFT JOIN products p ON p.category_id = c.category_id AND p.status = 1 " +
                    "WHERE c.status = 1 " +
                    "GROUP BY c.category_id, c.category_name, c.category_image " +
                    "HAVING COUNT(p.product_id) > 0 " +
                    "ORDER BY c.category_name",
            nativeQuery = true)
    List<Object[]> listCategoryByProductNameActive();

    @Query(value =
            "SELECT od.product_id, COUNT(*) AS SoLuong " +
                    "FROM order_details od " +
                    "JOIN products p ON od.product_id = p.product_id " +
                    "WHERE p.status = 1 " +
                    "GROUP BY od.product_id " +
                    "ORDER BY SoLuong DESC " +
                    "LIMIT 20",
            nativeQuery = true)
    List<Object[]> bestSaleProduct20();

    @Query(value = "SELECT * FROM products WHERE product_id IN :ids", nativeQuery = true)
    List<Product> findByInventoryIds(@Param("ids") List<Long> ids);

    @Query(value = "SELECT * FROM products WHERE status = 1 AND nxb_id = :nxbId", nativeQuery = true)
    List<Product> listProductByNxbActive(@Param("nxbId") Long nxbId);

    @Query(value = "SELECT * FROM products WHERE status = 1 AND nxb_id = :nxbId LIMIT 10", nativeQuery = true)
    List<Product> listProductByNxb10Active(@Param("nxbId") Long nxbId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Product p SET p.quantity = p.quantity - :qty " +
            "WHERE p.productId = :productId AND p.quantity >= :qty")
    int decreaseStock(@Param("productId") Long productId, @Param("qty") int qty);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Product p SET p.quantity = p.quantity + :qty " +
            "WHERE p.productId = :productId")
    int increaseStock(@Param("productId") Long productId, @Param("qty") int qty);

    @Query(value = "SELECT * FROM products WHERE status = 1 ORDER BY discount DESC, entered_date DESC LIMIT 20", nativeQuery = true)
    List<Product> topDiscount20();

    boolean existsByProductNameIgnoreCase(String productName);
    boolean existsByProductNameIgnoreCaseAndProductIdNot(String productName, Long excludeProductId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Product p SET p.status = false WHERE p.category.categoryId = :categoryId")
    int hideByCategory(@Param("categoryId") Long categoryId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Product p SET p.status = false WHERE p.nxb.id = :nxbId")
    int hideByNxb(@Param("nxbId") Long nxbId);
    // Hiển thị sản phẩm theo Category
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Product p SET p.status = true WHERE p.category.categoryId = :categoryId")
    int visibleByCategory(@Param("categoryId") Long categoryId);

    // Hiển thị sản phẩm theo NXB
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE Product p SET p.status = true WHERE p.nxb.id = :nxbId")
    int visibleByNxb(@Param("nxbId") Long nxbId);
}
