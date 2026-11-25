package com.somecompany.marketplace.common;

import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ParameterValidationException.class)
    public ResponseEntity<Map<String, List<FieldError>>> handleParams(ParameterValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("search", ex.getErrors()));
    }

    @ExceptionHandler(MalformedRequestException.class)
    public ResponseEntity<Map<String, String>> handleMalformed(MalformedRequestException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", "Request body is missing or not readable"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleOther(Exception ex) {
        if (ex instanceof ErrorResponse errorResponse) {
            // Framework errors such as 404 / 405 / 415 keep their status, without a stack trace.
            String detail = errorResponse.getBody().getDetail();
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .body(Map.of("error", detail == null ? "Request failed" : detail));
        }
        log.error("Unexpected error", ex);
        return ResponseEntity.internalServerError().body(Map.of("error", "Internal server error"));
    }
}
