// src/main/java/vn/fs/service/OrderService.java
package vn.fs.backend.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.fs.backend.domain.model.Order;
import vn.fs.backend.domain.model.OrderDetail;
import vn.fs.backend.domain.model.Product;
import vn.fs.backend.infrastructure.persistence.OrderDetailRepository;
import vn.fs.backend.infrastructure.persistence.OrderRepository;
import vn.fs.backend.infrastructure.persistence.ProductRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService implements OrderAdminService {

    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;

    @Override
    public List<Order> listAll() {
        return orderRepository.findAll();
    }

    @Override
    public List<Order> listAllFiltered(Integer status, LocalDate from, LocalDate to, String q, String payment) {
        // Chuẩn hóa input rỗng -> null để query gọn gàng
        String safeQ = (q != null && !q.trim().isEmpty()) ? q.trim() : null;
        String safePayment = (payment != null && !payment.trim().isEmpty()) ? payment.trim().toUpperCase() : null;
        return orderRepository.findAllFiltered(status, from, to, safeQ, safePayment);
    }

    @Override
    public Optional<Double> amountOf(Long orderId) {
        return orderRepository.findById(orderId).map(Order::getAmount);
    }

    @Override
    public List<OrderDetail> detailsOf(Long orderId) {
        return orderDetailRepository.findByOrderId(orderId);
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    @Override
    @Transactional
    public void cancel(Long orderId) {
        orderRepository.findById(orderId).ifPresent(o -> {
            o.setStatus((short) 3); // Huỷ
            orderRepository.save(o);
        });
    }

    @Override
    @Transactional
    public void confirm(Long orderId) {
        orderRepository.findById(orderId).ifPresent(o -> {
            o.setStatus((short) 1); // Xác nhận
            orderRepository.save(o);
        });
    }

    @Override
    @Transactional
    public void deliveredAndDecreaseStock(Long orderId) {
        orderRepository.findById(orderId).ifPresent(o -> {
            o.setStatus((short) 2); // Đã giao / đã thanh toán
            orderRepository.save(o);

            List<OrderDetail> items = orderDetailRepository.findByOrderId(orderId);
            for (OrderDetail od : items) {
                Product p = od.getProduct();
                if (p == null) continue;
                p.setQuantity(p.getQuantity() - od.getQuantity());
                productRepository.save(p);
            }
        });
    }
}
