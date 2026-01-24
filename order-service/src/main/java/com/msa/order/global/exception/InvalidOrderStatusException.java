package com.msa.order.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 주문/재고 상태가 유효하지 않을 때 발생하는 예외
 */
public class InvalidOrderStatusException extends OrderServiceException {
    private static final String ERROR_CODE = "INVALID_ORDER_STATUS";
    private static final HttpStatus HTTP_STATUS = HttpStatus.BAD_REQUEST;

    public InvalidOrderStatusException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
    }

    public InvalidOrderStatusException(String currentStatus, String expectedStatus) {
        super(String.format("상태 변경이 불가능합니다. 현재: %s, 요구: %s", currentStatus, expectedStatus),
              ERROR_CODE, HTTP_STATUS);
    }
}
