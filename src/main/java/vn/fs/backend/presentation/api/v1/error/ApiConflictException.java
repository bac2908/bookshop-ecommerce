package vn.fs.backend.presentation.api.v1.error;

public class ApiConflictException extends RuntimeException {

    private final String code;

    public ApiConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
