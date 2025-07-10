package com.msa.account.global.exception;

import jakarta.ws.rs.ForbiddenException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class NotAuthorityException extends ForbiddenException {
    public NotAuthorityException(String msg) {
        super(msg);
    }
}
