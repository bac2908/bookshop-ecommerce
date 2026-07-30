package vn.fs.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.fs.backend.domain.model.Category;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query(value =
            "SELECT c.* FROM categories c " +
                    "WHERE c.status = 1 " +
                    "  AND EXISTS (SELECT 1 FROM products p WHERE p.category_id = c.category_id AND p.status = 1) " +
                    "ORDER BY c.category_name",
            nativeQuery = true)
    List<Category> findActiveForMenu();
    @Query(value = "SELECT * FROM categories WHERE category_id = ?1", nativeQuery = true)
    Category findRaw(Long id);

    @Query("select c from Category c order by c.status desc, c.categoryName asc")
    List<Category> findAllForDropdown();

    List<Category> findByStatusTrueOrderByCategoryNameAsc();
    boolean existsByCategoryNameIgnoreCase(String categoryName);
    boolean existsByCategoryNameIgnoreCaseAndCategoryIdNot(String categoryName, Long excludeId);
}
