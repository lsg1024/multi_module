package com.msa.jewelry.shared.exception;

/**
 * 리소스 미존재 예외 (HTTP 404 매핑).
 */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message, 404);
    }
}
