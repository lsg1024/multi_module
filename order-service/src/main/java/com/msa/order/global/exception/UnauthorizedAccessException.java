package com.msa.order.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 권한이 없는 접근 시 발생하는 예외
 */
public class UnauthorizedAccessException extends OrderServiceException {
    private static final String ERROR_CODE = "UNAUTHORIZED_ACCESS";
    private static final HttpStatus HTTP_STATUS = HttpStatus.FORBIDDEN;

    public UnauthorizedAccessException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
    }

    public UnauthorizedAccessException() {
        super("접근 권한이 없습니다.", ERROR_CODE, HTTP_STATUS);
    }
}
