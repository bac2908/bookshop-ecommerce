package vn.fs.backend.application.error;

public class PaymentRefundFailedException extends RuntimeException {

    public PaymentRefundFailedException(Throwable cause) {
        super("Không thể hoàn tiền qua cổng thanh toán", cause);
    }
}
