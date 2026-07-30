package vn.fs.backend.application.error;

public class CartItemNotFoundException extends RuntimeException {

    private final Long productId;

    public CartItemNotFoundException(Long productId) {
        super("Sản phẩm chưa có trong giỏ hàng");
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
