package vn.fs.backend.presentation.api.v1.error;

public class ApiUpstreamException extends RuntimeException {

    private final String code;

    public ApiUpstreamException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
