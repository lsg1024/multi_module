package com.msa.order.global.exception;

import com.msa.common.global.api.ApiResponse;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * OrderServiceException 계층 예외 처리
     * 모든 커스텀 비즈니스 예외를 처리합니다.
     */
    @ExceptionHandler(OrderServiceException.class)
    public ResponseEntity<ApiResponse<String>> handleOrderServiceException(OrderServiceException e) {
        ApiResponse<String> body = ApiResponse.error(e.getMessage());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(body);
    }

    /**
     * FeignClientException 처리
     * 외부 서비스 호출 실패 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(FeignClientException.class)
    public ResponseEntity<ApiResponse<String>> handleFeignClientException(FeignClientException e) {
        ApiResponse<String> body = ApiResponse.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(body);
    }

    /**
     * IllegalArgumentException 처리 (레거시 지원)
     * 기존 코드와의 호환성을 위해 유지합니다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<String>> handleIllegalArgumentException(IllegalArgumentException e) {
        ApiResponse<String> body = ApiResponse.error(e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    /**
     * Validation 예외 처리
     * DTO 검증 실패 시 발생하는 예외를 처리합니다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining("\n"));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage));
    }

    /**
     * 예상치 못한 예외 처리
     * 모든 처리되지 않은 예외를 포착합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleUnexpectedException(Exception e) {
        ApiResponse<String> body = ApiResponse.error("서버 내부 오류가 발생했습니다.");
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

}
