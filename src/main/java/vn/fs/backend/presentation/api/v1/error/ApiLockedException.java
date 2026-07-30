package vn.fs.backend.presentation.api.v1.error;

public class ApiLockedException extends RuntimeException {

    private final String code;

    public ApiLockedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
