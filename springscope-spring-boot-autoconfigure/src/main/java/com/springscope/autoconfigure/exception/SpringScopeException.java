package com.springscope.autoconfigure.exception;

public class SpringScopeException extends RuntimeException {

    private final int statusCode;

    public SpringScopeException(String message) {
        this(500, message);
    }

    public SpringScopeException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public SpringScopeException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public static SpringScopeException beanNotFound(String beanName) {
        return new SpringScopeException(404, "Bean not found: " + beanName);
    }
}
