package com.msa.order.global.exception;

import org.springframework.http.HttpStatus;

/**
 * Feign Client 호출 실패 시 발생하는 예외
 */
public class FeignClientException extends OrderServiceException {
    private static final String ERROR_CODE = "FEIGN_CLIENT_ERROR";
    private static final HttpStatus HTTP_STATUS = HttpStatus.SERVICE_UNAVAILABLE;

    private final String serviceName;

    public FeignClientException(String serviceName, String message) {
        super(String.format("[%s 서비스 호출 실패] %s", serviceName, message), ERROR_CODE, HTTP_STATUS);
        this.serviceName = serviceName;
    }

    public FeignClientException(String serviceName, String message, Throwable cause) {
        super(String.format("[%s 서비스 호출 실패] %s", serviceName, message), ERROR_CODE, HTTP_STATUS, cause);
        this.serviceName = serviceName;
    }

    public String getServiceName() {
        return serviceName;
    }
}
