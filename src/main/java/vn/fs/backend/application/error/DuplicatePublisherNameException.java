package vn.fs.backend.application.error;

public class DuplicatePublisherNameException extends RuntimeException {

    public DuplicatePublisherNameException(String name) {
        super("Tên nhà xuất bản đã tồn tại: " + name);
    }
}
