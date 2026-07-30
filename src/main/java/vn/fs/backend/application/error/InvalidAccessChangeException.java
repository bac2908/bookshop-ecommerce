package vn.fs.backend.application.error;

public class InvalidAccessChangeException extends RuntimeException {

    private final String code;

    public InvalidAccessChangeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
