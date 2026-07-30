package vn.fs.backend.application.error;

public class CartChangedDuringCheckoutException extends RuntimeException {

    public CartChangedDuringCheckoutException() {
        super("Giỏ hàng đã thay đổi trong lúc khởi tạo thanh toán");
    }
}
