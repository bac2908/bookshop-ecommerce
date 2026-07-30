package vn.fs.backend.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.fs.backend.domain.model.OrderPayment;

import java.util.Optional;

public interface OrderPaymentRepository extends JpaRepository<OrderPayment, Long> {
    Optional<OrderPayment> findByProviderAndExternalOrderId(String provider, String externalOrderId);
    Optional<OrderPayment> findByProviderAndExternalCaptureId(String provider, String externalCaptureId);
}
