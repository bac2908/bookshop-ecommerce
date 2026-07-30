package vn.fs.backend.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.fs.backend.domain.model.OrderDetail;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {


    @Query(value = "SELECT * FROM order_details WHERE order_id = ?1", nativeQuery = true)
    List<OrderDetail> findByOrderId(Long orderId);

    @Query("select count(od) > 0 from OrderDetail od where od.product.productId = :pid")
    boolean existsRefByProductId(@Param("pid") Long productId);

    @Query(value =
            "SELECT p.product_name, " +
                    "       SUM(o.quantity)           AS quantity, " +
                    "       SUM(o.quantity * o.price) AS sum, " +
                    "       AVG(o.price)              AS avg, " +
                    "       MIN(o.price)              AS min, " +
                    "       MAX(o.price)              AS max " +
            "FROM order_details o " +
                    "JOIN products p ON o.product_id = p.product_id " +
                    "JOIN orders ord ON o.order_id = ord.order_id " +
                    "WHERE ord.status = 2 " +
                    "GROUP BY p.product_name",
            nativeQuery = true)
    List<Object[]> repo();

    @Query(value =
            "SELECT c.category_name, " +
                    "       SUM(o.quantity)           AS quantity, " +
                    "       SUM(o.quantity * o.price) AS sum, " +
                    "       AVG(o.price)              AS avg, " +
                    "       MIN(o.price)              AS min, " +
                    "       MAX(o.price)              AS max " +
                    "FROM order_details o " +
                    "JOIN products p   ON o.product_id = p.product_id " +
                    "JOIN categories c ON p.category_id = c.category_id " +
                    "JOIN orders ord ON o.order_id = ord.order_id " +
                    "WHERE ord.status = 2 " +
                    "GROUP BY c.category_name",
            nativeQuery = true)
    List<Object[]> repoWhereCategory();

    @Query(value =
            "SELECT YEAR(od.order_date)       AS y, " +
                    "       SUM(o.quantity)            AS quantity, " +
                    "       SUM(o.quantity * o.price)  AS sum, " +
                    "       AVG(o.price)               AS avg, " +
                    "       MIN(o.price)               AS min, " +
                    "       MAX(o.price)               AS max " +
                    "FROM order_details o " +
                    "JOIN orders od ON o.order_id = od.order_id " +
                    "WHERE od.status = 2 " +
                    "GROUP BY YEAR(od.order_date)",
            nativeQuery = true)
    List<Object[]> repoWhereYear();

    @Query(value =
            "SELECT MONTH(od.order_date)      AS m, " +
                    "       SUM(o.quantity)            AS quantity, " +
                    "       SUM(o.quantity * o.price)  AS sum, " +
                    "       AVG(o.price)               AS avg, " +
                    "       MIN(o.price)               AS min, " +
                    "       MAX(o.price)               AS max " +
                    "FROM order_details o " +
                    "JOIN orders od ON o.order_id = od.order_id " +
                    "WHERE od.status = 2 " +
                    "GROUP BY MONTH(od.order_date)",
            nativeQuery = true)
    List<Object[]> repoWhereMonth();

    @Query(value =
            "SELECT QUARTER(od.order_date)    AS q, " +
                    "       SUM(o.quantity)            AS quantity, " +
                    "       SUM(o.quantity * o.price)  AS sum, " +
                    "       AVG(o.price)               AS avg, " +
                    "       MIN(o.price)               AS min, " +
                    "       MAX(o.price)               AS max " +
                    "FROM order_details o " +
                    "JOIN orders od ON o.order_id = od.order_id " +
                    "WHERE od.status = 2 " +
                    "GROUP BY QUARTER(od.order_date)",
            nativeQuery = true)
    List<Object[]> repoWhereQUARTER();

    @Query(value =
            "SELECT COALESCE(u.name, u.email)      AS grp, " +
                    "       SUM(odt.quantity)              AS quantity, " +
                    "       SUM(odt.quantity * odt.price)  AS sum, " +
                    "       AVG(odt.price)                 AS avg, " +
                    "       MIN(odt.price)                 AS min, " +
                    "       MAX(odt.price)                 AS max " +
                    "FROM order_details odt " +
                    "JOIN orders o  ON odt.order_id = o.order_id " +
                    "JOIN user   u  ON o.user_id    = u.user_id " +
                    "WHERE o.status = 2 " +
            "GROUP BY COALESCE(u.name, u.email)",
    nativeQuery = true)
    List<Object[]> reportCustomer();

    @Query(value =
            "SELECT COALESCE(SUM(od.quantity), 0) " +
                    "FROM order_details od " +
                    "JOIN orders o ON o.order_id = od.order_id " +
                    "WHERE o.order_date >= :from " +
                    "  AND o.status = 2",
            nativeQuery = true)
    long totalSoldQtyFrom(@Param("from") LocalDate from);

    @Query(value =
            "SELECT DATE(o.order_date) AS d, COALESCE(SUM(od.quantity), 0) AS qty " +
                    "FROM order_details od " +
                    "JOIN orders o ON o.order_id = od.order_id " +
                    "WHERE o.order_date >= :from " +
                    "  AND o.status = 2 " +
                    "GROUP BY DATE(o.order_date) " +
                    "ORDER BY d",
            nativeQuery = true)
    List<Object[]> soldQtyByDateFrom(@Param("from") LocalDate from);
}
