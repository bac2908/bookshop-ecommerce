package vn.fs.backend.application.error;

public class DuplicateCategoryNameException extends RuntimeException {

    public DuplicateCategoryNameException(String name) {
        super("Tên thể loại đã tồn tại: " + name);
    }
}
