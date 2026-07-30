package vn.fs.backend.application.error;

public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Giỏ hàng đang trống");
    }
}
