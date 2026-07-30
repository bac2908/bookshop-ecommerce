package vn.fs.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import vn.fs.backend.domain.model.NXB;

import java.util.List;

@Repository
public interface NxbRepository extends JpaRepository<NXB, Long> {

    // Dùng cho navbar (chỉ NXB đang hiển thị và có ít nhất 1 sp đang bán)
    @Query(value =
            "SELECT n.* FROM nxb n " +
                    "WHERE n.status = 1 " +
                    "  AND EXISTS (SELECT 1 FROM products p WHERE p.nxb_id = n.id AND p.status = 1) " +
                    "ORDER BY n.name",
            nativeQuery = true)
    List<NXB> findActiveForMenu();

    // Lấy thô theo id (nếu cần)
    @Query(value = "SELECT * FROM nxb WHERE id = ?1", nativeQuery = true)
    NXB findRaw(Long id);

    // Đổ dropdown (hiện trước NXB đang hiển thị)
    @Query("select n from NXB n order by n.status desc, n.name asc")
    List<NXB> findAllForDropdown();

    // Dùng cho form thêm sản phẩm (chỉ NXB đang mở)
    List<NXB> findByStatusTrueOrderByNameAsc();

    // Check trùng tên
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long excludeId);
}
