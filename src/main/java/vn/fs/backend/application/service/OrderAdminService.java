package vn.fs.backend.application.service;

import vn.fs.backend.domain.model.Order;
import vn.fs.backend.domain.model.OrderDetail;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderAdminService {
    // list mặc định (mới -> cũ)
    List<Order> listAll();

    // list có bộ lọc hiện đại
    List<Order> listAllFiltered(Integer status, LocalDate from, LocalDate to, String q, String payment);

    // amount
    Optional<Double> amountOf(Long orderId);

    // chi tiết
    List<OrderDetail> detailsOf(Long orderId);

    // đổi trạng thái
    void cancel(Long orderId);    // 3
    void confirm(Long orderId);   // 1
    void deliveredAndDecreaseStock(Long orderId); // 2 + trừ kho
}
