package cn.lunalhx.ai.kilnai.types.error;

import java.util.Objects;

public class ApplicationException extends RuntimeException {

    private final ErrorCode errorCode;

    public ApplicationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
