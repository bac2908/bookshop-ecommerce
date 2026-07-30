package vn.fs.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.fs.backend.domain.model.OrderPayment;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {
    Optional<OrderPayment> findByProviderAndExternalOrderId(String provider, String externalOrderId);
    Optional<OrderPayment> findByProviderAndExternalCaptureId(String provider, String externalCaptureId);
    Optional<OrderPayment> findByOrder_OrderId(Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from OrderPayment p
            join fetch p.order o
            join fetch o.user u
            where p.provider = :provider and p.externalOrderId = :externalOrderId
            """)
    Optional<OrderPayment> findByExternalOrderIdForUpdate(
            @Param("provider") String provider,
            @Param("externalOrderId") String externalOrderId);
}
