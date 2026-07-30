package vn.fs.backend.application.error;

public class PublisherNotFoundException extends RuntimeException {

    private final Long publisherId;

    public PublisherNotFoundException(Long publisherId) {
        super("Không tìm thấy nhà xuất bản có id " + publisherId);
        this.publisherId = publisherId;
    }

    public Long getPublisherId() {
        return publisherId;
    }
}
