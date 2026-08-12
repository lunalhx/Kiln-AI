package cn.lunalhx.ai.kilnai.trigger.http;

import cn.lunalhx.ai.kilnai.api.response.ApiErrorResponse;
import cn.lunalhx.ai.kilnai.types.error.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiErrorResponse handleApplicationException(ApplicationException exception) {
        return new ApiErrorResponse(exception.errorCode().name(), exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiErrorResponse handleInvalidArgument(IllegalArgumentException exception) {
        return new ApiErrorResponse("INVALID_ARGUMENT", exception.getMessage(), Instant.now());
    }
}
