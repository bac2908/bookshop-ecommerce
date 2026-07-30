package vn.fs.backend.application.error;

public class AdminUserNotFoundException extends RuntimeException {

    private final Long userId;

    public AdminUserNotFoundException(Long userId) {
        super("Không tìm thấy người dùng có id " + userId);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
