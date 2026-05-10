package com.msa.jewelry.product.internal.global.exception;

public class RetryableExternalException extends RuntimeException {
    public RetryableExternalException(String message) { super(message); }
    public RetryableExternalException(String message, Throwable cause) { super(message, cause); }
}

