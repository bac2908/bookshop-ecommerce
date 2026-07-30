package vn.fs.backend.presentation.api.v1;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.springframework.web.util.UriComponentsBuilder;

import vn.fs.backend.domain.model.Order;
import vn.fs.backend.domain.model.OrderDetail;
import vn.fs.backend.domain.model.OrderPayment;
import vn.fs.backend.presentation.api.v1.dto.CheckoutResponse;
import vn.fs.backend.presentation.api.v1.dto.OrderItemResponse;
import vn.fs.backend.presentation.api.v1.dto.OrderResponse;
import vn.fs.backend.application.dto.CheckoutResult;

final class OrderApiMapper {

    private OrderApiMapper() {
    }

    static CheckoutResponse toResponse(CheckoutResult result) {
        return new CheckoutResponse(
                result.orderId(),
                result.status().name(),
                result.paymentMethod(),
                result.approvalUrl(),
                result.externalPaymentId());
    }

    static OrderResponse toResponse(
            Order order,
            List<OrderDetail> details,
            OrderPayment payment) {
        List<OrderItemResponse> items = details == null
                ? List.of()
                : details.stream()
                        .map(OrderApiMapper::toResponse)
                        .toList();
        String paymentMethod = payment == null ? "COD" : payment.getProvider();
        String paymentStatus = payment == null ? null : payment.getStatus();
        return new OrderResponse(
                order.getOrderId(),
                toLocalDate(order),
                money(order.getAmount()),
                order.getAddress(),
                order.getPhone(),
                order.getNote(),
                order.getOrderStatus().name(),
                order.getStatus(),
                paymentMethod,
                paymentStatus,
                order.getOrderStatus().canBeCancelled(),
                items);
    }

    private static OrderItemResponse toResponse(OrderDetail detail) {
        String imageName = detail.getProduct() == null
                ? null
                : detail.getProduct().getProductImage();
        BigDecimal unitPrice = money(detail.getPrice());
        return new OrderItemResponse(
                detail.getProduct() == null
                        ? null
                        : detail.getProduct().getProductId(),
                detail.getProduct() == null
                        ? null
                        : detail.getProduct().getProductName(),
                imageUrl(imageName),
                detail.getQuantity(),
                unitPrice,
                unitPrice.multiply(BigDecimal.valueOf(detail.getQuantity()))
                        .setScale(2, RoundingMode.HALF_UP));
    }

    private static BigDecimal money(Double value) {
        return BigDecimal.valueOf(value == null ? 0.0 : value)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private static LocalDate toLocalDate(Order order) {
        return order.getOrderDate() == null
                ? null
                : new Date(order.getOrderDate().getTime()).toLocalDate();
    }

    private static String imageUrl(String imageName) {
        if (imageName == null || imageName.isBlank()) {
            return null;
        }
        return UriComponentsBuilder.fromPath("/loadImage")
                .queryParam("imageName", imageName)
                .build()
                .encode()
                .toUriString();
    }
}
