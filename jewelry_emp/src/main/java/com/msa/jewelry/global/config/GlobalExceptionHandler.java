package com.msa.jewelry.global.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.msa.common.global.api.ApiResponse;
import com.msa.jewelry.global.exception.OrderServiceException;
import com.msa.jewelry.global.exception.DomainException;
import com.msa.jewelry.local.user.exception.UserNotFoundException;
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

@Slf4j
@Component("jewelryGlobalExceptionHandler")
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Object>> handleDomainException(DomainException e) {
        log.warn("Domain exception: {} (status={})", e.getMessage(), e.getHttpStatus());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(com.msa.jewelry.global.exception.NotFoundException.class)
    public ResponseEntity<ApiResponse<String>> handleAccountNotFoundException(
            com.msa.jewelry.global.exception.NotFoundException e) {
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error."));
    }

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
