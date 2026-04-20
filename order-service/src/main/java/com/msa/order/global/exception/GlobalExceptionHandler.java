package com.msa.order.global.exception;

import com.msa.common.global.api.ApiResponse;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
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
     * HttpMessageNotReadableException 처리
     * JSON 파싱 오류 시 필드 정보를 포함한 상세 메시지를 반환합니다.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        String errorMessage = "요청 JSON 파싱 오류";

        if (cause instanceof InvalidFormatException invalidFormatEx) {
            String fieldPath = buildFieldPath(invalidFormatEx.getPath());
            String rootMsg = invalidFormatEx.getOriginalMessage();
            if (rootMsg != null && rootMsg.contains("from String")) {
                rootMsg = "타입 불일치";
            }
            errorMessage = "요청 JSON 파싱 오류: 필드 '" + fieldPath + "' — " + rootMsg;
        } else if (cause instanceof JsonMappingException jsonEx) {
            String fieldPath = buildFieldPath(jsonEx.getPath());
            errorMessage = "요청 JSON 파싱 오류: 필드 '" + fieldPath + "' — " + jsonEx.getOriginalMessage();
        }

        ApiResponse<String> body = ApiResponse.error(errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    /**
     * NumberFormatException 처리
     * 숫자 형식 오류 발생 시 상세 메시지를 반환합니다.
     */
    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ApiResponse<String>> handleNumberFormatException(NumberFormatException ex) {
        String errorMessage = "숫자 형식 오류: " + ex.getMessage() + " (비어있거나 잘못된 숫자 필드가 있는지 확인하세요)";
        ApiResponse<String> body = ApiResponse.error(errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(body);
    }

    /**
     * JsonMappingException 경로 생성 헬퍼
     */
    private String buildFieldPath(List<JsonMappingException.Reference> path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        return path.stream()
                .map(ref -> {
                    if (ref.getIndex() >= 0) {
                        return ref.getFieldName() + "[" + ref.getIndex() + "]";
                    }
                    return ref.getFieldName();
                })
                .collect(Collectors.joining("."));
    }

    /**
     * 예상치 못한 예외 처리
     * 모든 처리되지 않은 예외를 포착합니다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleUnexpectedException(Exception e) {
        log.error("[UnexpectedException] {}", e.getMessage(), e);
        ApiResponse<String> body = ApiResponse.error("서버 내부 오류가 발생했습니다.");
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(body);
    }

}
