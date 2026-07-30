package vn.fs.backend.application.error;

public class OrderNotFoundException extends RuntimeException {

    private final Long orderId;

    public OrderNotFoundException(Long orderId) {
        super("Không tìm thấy đơn hàng");
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
