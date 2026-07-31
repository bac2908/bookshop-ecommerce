package vn.fs.backend.application.error;

public class OrderNotRefundableException extends RuntimeException {

    public OrderNotRefundableException(String message) {
        super(message);
    }
}
