package com.msa.jewelry.global.exception;

public abstract class DomainException extends RuntimeException {

    private final int httpStatus;

    protected DomainException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
