package com.msa.order.global.exception;

public class KafkaProcessingException extends RuntimeException {
    public KafkaProcessingException(String message) {
        super(message);
    }
}

