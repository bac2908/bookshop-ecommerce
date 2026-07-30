package vn.fs.backend.application.error;

public class CategoryNotFoundException extends RuntimeException {

    private final Long categoryId;

    public CategoryNotFoundException(Long categoryId) {
        super("Không tìm thấy thể loại có id " + categoryId);
        this.categoryId = categoryId;
    }

    public Long getCategoryId() {
        return categoryId;
    }
}
