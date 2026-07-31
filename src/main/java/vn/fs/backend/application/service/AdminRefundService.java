package vn.fs.backend.application.service;

import java.util.Date;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.fs.backend.application.dto.RefundResult;
import vn.fs.backend.application.error.OrderNotFoundException;
import vn.fs.backend.application.error.OrderNotRefundableException;
import vn.fs.backend.application.error.PaymentRefundFailedException;
import vn.fs.backend.application.service.paypal.PaypalRefundService;
import vn.fs.backend.domain.model.Order;
import vn.fs.backend.domain.model.OrderPayment;
import vn.fs.backend.domain.model.OrderStatus;
import vn.fs.backend.infrastructure.persistence.OrderPaymentRepository;
import vn.fs.backend.infrastructure.persistence.OrderRepository;

@Service
@RequiredArgsConstructor
public class AdminRefundService {

    private final OrderRepository orderRepository;
    private final OrderPaymentRepository paymentRepository;
    private final PaypalRefundService paypalRefundService;

    @Transactional
    public RefundResult refundFull(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.getOrderStatus() == OrderStatus.REFUNDED) {
            return existingRefund(order);
        }
        if (order.getOrderStatus() != OrderStatus.COMPLETED) {
            throw new OrderNotRefundableException(
                    "Chỉ có thể hoàn tiền đơn đã hoàn tất");
        }

        String captureId = order.getPaypalCaptureId();
        if (captureId == null || captureId.isBlank()) {
            throw new OrderNotRefundableException(
                    "Đơn hàng không có giao dịch PayPal để hoàn tiền");
        }

        RefundResult result;
        try {
            result = paypalRefundService.refundCapture(
                    captureId,
                    null,
                    null,
                    "Admin full refund");
        } catch (Exception exception) {
            if (isAlreadyRefunded(exception)) {
                markRefunded(order, null, "COMPLETED");
                return existingRefund(order);
            }
            throw new PaymentRefundFailedException(exception);
        }

        markRefunded(order, result.getRefundId(), result.getStatus());
        return result;
    }

    private RefundResult existingRefund(Order order) {
        return paymentRepository.findByProviderAndExternalCaptureId(
                        "PAYPAL",
                        order.getPaypalCaptureId())
                .map(payment -> new RefundResult(
                        payment.getRefundId(),
                        payment.getStatus()))
                .orElseGet(() -> new RefundResult(null, "REFUNDED"));
    }

    private void markRefunded(Order order, String refundId, String providerStatus) {
        Date now = new Date();
        String captureId = order.getPaypalCaptureId();
        paymentRepository.findByProviderAndExternalCaptureId("PAYPAL", captureId)
                .ifPresent(payment -> updatePayment(
                        payment,
                        refundId,
                        providerStatus,
                        now));
        order.setOrderStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);
    }

    private void updatePayment(
            OrderPayment payment,
            String refundId,
            String providerStatus,
            Date now) {
        payment.setStatus("REFUNDED");
        if (refundId != null && !refundId.isBlank()) {
            payment.setRefundId(refundId);
        }
        payment.setRefundAmount(null);
        payment.setRefundCurrency("USD");
        payment.setRefundedAt(now);
        payment.setUpdatedAt(now);
        payment.setMessage("Refund " + (providerStatus == null ? "accepted" : providerStatus));
        paymentRepository.save(payment);
    }

    private boolean isAlreadyRefunded(Exception exception) {
        String message = exception.getMessage();
        return message != null && message.contains("CAPTURE_FULLY_REFUNDED");
    }
}
