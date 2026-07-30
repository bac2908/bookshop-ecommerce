package vn.fs.backend.application.error;

import vn.fs.backend.domain.model.OrderStatus;

public class InvalidOrderStateException extends RuntimeException {

    private final OrderStatus currentStatus;

    public InvalidOrderStateException(String message, OrderStatus currentStatus) {
        super(message);
        this.currentStatus = currentStatus;
    }

    public OrderStatus getCurrentStatus() {
        return currentStatus;
    }
}
