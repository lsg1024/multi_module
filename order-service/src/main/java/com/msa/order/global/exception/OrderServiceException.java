package com.msa.order.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Order Service의 기본 예외 클래스
 * 모든 비즈니스 예외는 이 클래스를 상속받습니다.
 */
@Getter
public abstract class OrderServiceException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;

    protected OrderServiceException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    protected OrderServiceException(String message, String errorCode, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
