package com.msa.account.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class KafkaMessageException extends IllegalArgumentException {
    public KafkaMessageException(String message) {
        super(message);
    }
}
