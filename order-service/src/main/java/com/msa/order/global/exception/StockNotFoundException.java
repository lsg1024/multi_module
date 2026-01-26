package com.msa.order.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 재고를 찾을 수 없을 때 발생하는 예외
 */
public class StockNotFoundException extends OrderServiceException {
    private static final String ERROR_CODE = "STOCK_NOT_FOUND";
    private static final HttpStatus HTTP_STATUS = HttpStatus.NOT_FOUND;

    public StockNotFoundException(Long flowCode) {
        super(String.format("재고를 찾을 수 없습니다. FlowCode: %d", flowCode), ERROR_CODE, HTTP_STATUS);
    }

    public StockNotFoundException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
    }
}
