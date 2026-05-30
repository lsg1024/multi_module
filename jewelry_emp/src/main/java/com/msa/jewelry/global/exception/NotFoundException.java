package com.msa.jewelry.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 리소스 미존재 예외 (HTTP 404 매핑).
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message, 404);
    }
}
