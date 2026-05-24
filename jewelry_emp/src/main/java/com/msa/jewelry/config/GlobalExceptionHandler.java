package com.msa.jewelry.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.msa.common.global.api.ApiResponse;
import com.msa.jewelry.account.internal.global.exception.KafkaMessageException;
import com.msa.jewelry.order.internal.global.exception.OrderServiceException;
import com.msa.jewelry.shared.exception.DomainException;
import com.msa.jewelry.user.internal.exception.UserNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * jewelry_emp 통합 예외 처리기.
 *
 * <p>2026-05 모놀로식 통합 시점에 모듈별로 분산되어 있던 5개 핸들러
 * (config / account / order / product / user) 를 본 클래스 하나로 통합.
 * 같은 ExceptionHandler 가 여러 곳에 동시 등록되어 우선순위가 불명확하던
 * 문제를 제거한다.
 *
 * <p>처리 정책 요약:
 * <ul>
 *   <li>{@link DomainException} 계열 (shared.NotFoundException 포함) → {@code e.getHttpStatus()}</li>
 *   <li>account.NotFoundException → 404 (레거시, 추후 DomainException 으로 통합 권장)</li>
 *   <li>{@link OrderServiceException} → {@code e.getHttpStatus()}</li>
 *   <li>{@link UserNotFoundException} → 401</li>
 *   <li>{@link KafkaMessageException} → 404 (Kafka 제거 후 함께 삭제 예정)</li>
 *   <li>{@link IllegalArgumentException} → 400</li>
 *   <li>{@link IllegalStateException} → 409</li>
 *   <li>{@link MethodArgumentNotValidException} / {@link ConstraintViolationException} → 400 (필드 정보 포함)</li>
 *   <li>{@link HttpMessageNotReadableException} → 400 (필드 경로 + 원인 메시지)</li>
 *   <li>{@link NumberFormatException} → 400</li>
 *   <li>{@link DataIntegrityViolationException} → 409</li>
 *   <li>나머지 {@link Exception} → 500</li>
 * </ul>
 */
@Slf4j
@Component("jewelryGlobalExceptionHandler")
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ============================================================
     * 도메인 / 모듈 비즈니스 예외
     * ============================================================ */

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Object>> handleDomainException(DomainException e) {
        log.warn("Domain exception: {} (status={})", e.getMessage(), e.getHttpStatus());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(com.msa.jewelry.account.internal.global.exception.NotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleAccountNotFoundException(
            com.msa.jewelry.account.internal.global.exception.NotFoundException e) {
        log.warn("Account NotFoundException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(OrderServiceException.class)
    public ResponseEntity<ApiResponse<String>> handleOrderServiceException(OrderServiceException e) {
        log.warn("OrderServiceException [{}]: {}", e.getErrorCode(), e.getMessage());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleUserNotFoundException(UserNotFoundException e) {
        log.warn("UserNotFoundException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getMessage()));
    }

    /**
     * Kafka 메시지 처리 실패 — Kafka 제거 후 본 핸들러도 함께 삭제 예정 (P4).
     */
    @ExceptionHandler(KafkaMessageException.class)
    public ResponseEntity<ApiResponse<String>> handleKafkaMessageException(KafkaMessageException e) {
        log.warn("KafkaMessageException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage()));
    }

    /* ============================================================
     * 자바 표준 예외 / Validation
     * ============================================================ */

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("Illegal argument: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(IllegalStateException e) {
        log.warn("Illegal state: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("\n"));
        if (message.isEmpty()) {
            message = e.getBindingResult().getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
                    .collect(Collectors.joining("\n"));
        }
        if (message.isEmpty()) {
            message = "Validation failed";
        }
        log.warn("Validation failed: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("Constraint violation: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<String>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
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

        log.warn("HttpMessageNotReadable: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage));
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ApiResponse<String>> handleNumberFormat(NumberFormatException ex) {
        String errorMessage = "숫자 형식 오류: " + ex.getMessage() + " (비어있거나 잘못된 숫자 필드가 있는지 확인하세요)";
        log.warn("NumberFormatException: {}", errorMessage);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(errorMessage));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("DB integrity violation: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Duplicate or invalid data."));
    }

    /* ============================================================
     * 최후 catch-all
     * ============================================================ */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error."));
    }

    /* ============================================================
     * 헬퍼
     * ============================================================ */

    /**
     * JsonMappingException 의 path 를 사람이 읽기 좋은 경로 문자열로 변환.
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
}
