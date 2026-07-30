package vn.fs.backend.domain.model;

import java.util.Arrays;

public enum OrderStatus {
    PENDING_CONFIRMATION(0),
    CONFIRMED(1),
    COMPLETED(2),
    CANCELLED(3),
    REFUNDED(4),
    AWAITING_PAYMENT(5);

    private final int code;

    OrderStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static OrderStatus fromCode(int code) {
        return Arrays.stream(values())
                .filter(status -> status.code == code)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Trạng thái đơn hàng không hợp lệ: " + code));
    }

    public boolean canBeCancelled() {
        return this == PENDING_CONFIRMATION
                || this == CONFIRMED
                || this == AWAITING_PAYMENT;
    }
}
