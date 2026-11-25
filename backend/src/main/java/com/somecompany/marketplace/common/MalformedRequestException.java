package com.somecompany.marketplace.common;

/** Thrown when a request body is not valid JSON or has the wrong shape; mapped to 400. */
public class MalformedRequestException extends RuntimeException {

    public MalformedRequestException(String message) {
        super(message);
    }
}
