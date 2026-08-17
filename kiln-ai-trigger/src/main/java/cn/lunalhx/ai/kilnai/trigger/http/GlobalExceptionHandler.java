package cn.lunalhx.ai.kilnai.trigger.http;

import cn.lunalhx.ai.kilnai.api.response.ApiErrorResponse;
import cn.lunalhx.ai.kilnai.types.error.ActiveWorkConflictException;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import cn.lunalhx.ai.kilnai.types.error.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    ResponseEntity<ApiErrorResponse> handleApplicationException(ApplicationException exception) {
        HttpStatus status = switch (exception.errorCode()) {
            case FLOW_NOT_FOUND, REVIEW_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case UNPROCESSABLE -> HttpStatus.UNPROCESSABLE_ENTITY;
            case SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case MODEL_CONTRACT_INVALID -> HttpStatus.INTERNAL_SERVER_ERROR;
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status)
                .body(new ApiErrorResponse(exception.errorCode().name(), exception.getMessage(), Instant.now()));
    }

    /**
     * The learner-safe Start conflict of ADR-0070: the body carries the
     * existing Flow id needed for recovery and nothing else.
     */
    @ExceptionHandler(ActiveWorkConflictException.class)
    ResponseEntity<ApiErrorResponse> handleActiveWorkConflict(ActiveWorkConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(ErrorCode.CONFLICT.name(), exception.getMessage(), Instant.now(),
                        exception.existingFlowId()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    ApiErrorResponse handleInvalidArgument(IllegalArgumentException exception) {
        return new ApiErrorResponse(ErrorCode.UNPROCESSABLE.name(), exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiErrorResponse handleMissingHeader(MissingRequestHeaderException exception) {
        return new ApiErrorResponse(ErrorCode.INVALID_ARGUMENT.name(), exception.getMessage(), Instant.now());
    }
}
