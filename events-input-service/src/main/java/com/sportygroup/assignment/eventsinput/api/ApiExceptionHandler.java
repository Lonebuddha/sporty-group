package com.sportygroup.assignment.eventsinput.api;

import com.sportygroup.assignment.eventsinput.messaging.EventOutcomePublishException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EventOutcomePublishException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiErrorResponse handleEventOutcomePublishException(EventOutcomePublishException exception) {
        return new ApiErrorResponse(
            "KAFKA_UNAVAILABLE",
            exception.getMessage()
        );
    }
}
