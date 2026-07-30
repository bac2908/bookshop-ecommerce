package vn.fs.backend.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.fs.backend.application.error.InvalidOrderStateException;
import vn.fs.backend.application.error.OrderNotFoundException;
import vn.fs.backend.domain.model.Order;
import vn.fs.backend.domain.model.OrderDetail;
import vn.fs.backend.domain.model.OrderPayment;
import vn.fs.backend.domain.model.OrderStatus;
import vn.fs.backend.infrastructure.persistence.OrderDetailRepository;
import vn.fs.backend.infrastructure.persistence.OrderPaymentRepository;
import vn.fs.backend.infrastructure.persistence.OrderRepository;
import vn.fs.backend.infrastructure.persistence.ProductRepository;

@Service
@RequiredArgsConstructor
public class OrderService implements OrderAdminService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Order> listAll() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> listAllFiltered(
            Integer status,
            LocalDate from,
            LocalDate to,
            String query,
            String payment) {
        String safeQuery = query != null && !query.trim().isEmpty()
                ? query.trim()
                : null;
        String safePayment = payment != null && !payment.trim().isEmpty()
                ? payment.trim().toUpperCase()
                : null;
        return orderRepository.findAllFiltered(
                status,
                from,
                to,
                safeQuery,
                safePayment);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Double> amountOf(Long orderId) {
        return orderRepository.findById(orderId).map(Order::getAmount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDetail> detailsOf(Long orderId) {
        return orderDetailRepository.findByOrderId(orderId);
    }

    @Transactional(readOnly = true)
    public Page<Order> listByUser(Long userId, Pageable pageable) {
        return orderRepository.findByUser_UserIdOrderByOrderIdDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Order findOwnedById(Long orderId, Long userId) {
        return orderRepository.findByOrderIdAndUser_UserId(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderDetail> detailsOfOwned(Long orderId, Long userId) {
        findOwnedById(orderId, userId);
        return orderDetailRepository.findByOrderId(orderId);
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Transactional
    public Order cancelOwned(Long orderId, Long userId) {
        Order order = orderRepository.findOwnedByIdForUpdate(orderId, userId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return cancelLockedOrder(order);
    }

    @Override
    @Transactional
    public void cancel(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        cancelLockedOrder(order);
    }

    @Override
    @Transactional
    public void confirm(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        OrderStatus current = order.getOrderStatus();
        if (current == OrderStatus.CONFIRMED) {
            return;
        }
        if (current != OrderStatus.PENDING_CONFIRMATION) {
            throw new InvalidOrderStateException(
                    "Chỉ có thể xác nhận đơn đang chờ xác nhận",
                    current);
        }
        order.setOrderStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void complete(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        OrderStatus current = order.getOrderStatus();
        if (current == OrderStatus.COMPLETED) {
            return;
        }
        if (current != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException(
                    "Chỉ có thể hoàn tất đơn đã được xác nhận",
                    current);
        }

        // Inventory was reserved exactly once when checkout created the order.
        order.setOrderStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);
    }

    private Order cancelLockedOrder(Order order) {
        OrderStatus current = order.getOrderStatus();
        if (current == OrderStatus.CANCELLED) {
            return order;
        }
        if (!current.canBeCancelled()) {
            throw new InvalidOrderStateException(
                    "Không thể hủy đơn ở trạng thái " + current.name(),
                    current);
        }

        List<OrderDetail> items = orderDetailRepository.findByOrderId(order.getOrderId());
        for (OrderDetail item : items) {
            if (item.getProduct() != null && item.getQuantity() > 0) {
                productRepository.increaseStock(
                        item.getProduct().getProductId(),
                        item.getQuantity());
            }
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        orderPaymentRepository.findByOrder_OrderId(order.getOrderId())
                .filter(payment -> !"COMPLETED".equalsIgnoreCase(payment.getStatus()))
                .ifPresent(this::markPaymentCancelled);
        return order;
    }

    private void markPaymentCancelled(OrderPayment payment) {
        payment.setStatus("CANCELLED");
        payment.setUpdatedAt(new java.util.Date());
        orderPaymentRepository.save(payment);
    }
}
