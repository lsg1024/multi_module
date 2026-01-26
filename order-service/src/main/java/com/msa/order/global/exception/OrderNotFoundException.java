package com.msa.order.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 주문을 찾을 수 없을 때 발생하는 예외
 */
public class OrderNotFoundException extends OrderServiceException {
    private static final String ERROR_CODE = "ORDER_NOT_FOUND";
    private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

    public OrderNotFoundException(Long flowCode) {
        super(String.format("주문을 찾을 수 없습니다. FlowCode: %d", flowCode), ERROR_CODE, HTTP_STATUS);
    }

    public OrderNotFoundException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
    }
}
