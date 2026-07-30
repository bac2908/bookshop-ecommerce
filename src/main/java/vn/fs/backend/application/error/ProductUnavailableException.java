package vn.fs.backend.application.error;

public class ProductUnavailableException extends RuntimeException {

    private final Long productId;

    public ProductUnavailableException(Long productId) {
        super("Sản phẩm không tồn tại hoặc không còn được bán");
        this.productId = productId;
    }

    public Long getProductId() {
        return productId;
    }
}
