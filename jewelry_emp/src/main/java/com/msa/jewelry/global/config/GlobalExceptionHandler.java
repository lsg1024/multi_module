package com.msa.jewelry.global.config;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.msa.common.global.api.ApiResponse;
import com.msa.jewelry.global.exception.OrderServiceException;
import com.msa.jewelry.global.exception.DomainException;
import com.msa.jewelry.local.user.exception.UserNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component("jewelryGlobalExceptionHandler")
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== 사용자용 한국어 일반 메시지 (영어/저수준 메시지 치환용) =====
    private static final String MSG_BAD_REQUEST = "입력값이 올바르지 않습니다. 입력 내용을 확인해주세요.";
    private static final String MSG_CONFLICT    = "현재 상태에서는 처리할 수 없는 요청입니다.";
    private static final String MSG_DUPLICATE   = "이미 등록되어 있거나 다른 곳에서 사용 중인 값입니다.";
    private static final String MSG_VALIDATION  = "입력값 검증에 실패했습니다. 입력 내용을 확인해주세요.";
    private static final String MSG_NOT_FOUND   = "요청한 정보를 찾을 수 없습니다.";
    private static final String MSG_INTERNAL    = "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
    private static final String MSG_USER_AUTH   = "사용자 정보를 확인할 수 없습니다. 다시 로그인해주세요.";
    private static final String MSG_ORDER       = "주문 처리 중 오류가 발생했습니다.";
    private static final String MSG_NUMBER      = "숫자 형식이 올바르지 않습니다. 숫자 입력란을 확인해주세요.";

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiResponse<Object>> handleDomainException(DomainException e) {
        return userFacing(e.getHttpStatus(), e, MSG_BAD_REQUEST, "Domain exception");
    }

    @ExceptionHandler(com.msa.jewelry.global.exception.NotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountNotFoundException(
            com.msa.jewelry.global.exception.NotFoundException e) {
        return userFacing(HttpStatus.NOT_FOUND.value(), e, MSG_NOT_FOUND, "NotFoundException");
    }

    @ExceptionHandler(OrderServiceException.class)
    public ResponseEntity<ApiResponse<Object>> handleOrderServiceException(OrderServiceException e) {
        return userFacing(e.getHttpStatus().value(), e, MSG_ORDER,
                "OrderServiceException [" + e.getErrorCode() + "]");
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserNotFoundException(UserNotFoundException e) {
        return userFacing(HttpStatus.UNAUTHORIZED.value(), e, MSG_USER_AUTH, "UserNotFoundException");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        return userFacing(HttpStatus.BAD_REQUEST.value(), e, MSG_BAD_REQUEST, "Illegal argument");
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalState(IllegalStateException e) {
        return userFacing(HttpStatus.CONFLICT.value(), e, MSG_CONFLICT, "Illegal state");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .filter(GlobalExceptionHandler::isKorean)
                .distinct()
                .collect(Collectors.joining("\n"));

        if (message.isEmpty()) {
            String errorId = newErrorId();
            log.warn("Validation failed [{}]: {}", errorId, e.getBindingResult().getAllErrors());
            message = withId(MSG_VALIDATION, errorId);
        } else {
            log.warn("Validation failed: {}", message);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    /**
     * 메서드 파라미터 단위 제약 위반 ("save.arg0.x: must not be null" 형식 차단).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .filter(GlobalExceptionHandler::isKorean)
                .distinct()
                .collect(Collectors.joining("\n"));

        if (message.isEmpty()) {
            String errorId = newErrorId();
            log.warn("Constraint violation [{}]: {}", errorId, e.getMessage());
            message = withId(MSG_VALIDATION, errorId);
        } else {
            log.warn("Constraint violation: {}", message);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    /**
     * 요청 JSON 파싱 실패. 어떤 필드인지는 알려주되, Jackson 영어 원문은 로그로만.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        String errorMessage = "요청 형식이 올바르지 않습니다. 입력 내용을 확인해주세요.";

        if (cause instanceof InvalidFormatException invalidFormatEx) {
            errorMessage = "입력값의 형식이 올바르지 않습니다. (필드: " + buildFieldPath(invalidFormatEx.getPath()) + ")";
        } else if (cause instanceof JsonMappingException jsonEx) {
            errorMessage = "입력값의 형식이 올바르지 않습니다. (필드: " + buildFieldPath(jsonEx.getPath()) + ")";
        }

        log.warn("HttpMessageNotReadable: {} (원본: {})", errorMessage, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errorMessage));
    }

    @ExceptionHandler(NumberFormatException.class)
    public ResponseEntity<ApiResponse<Object>> handleNumberFormat(NumberFormatException ex) {
        String errorId = newErrorId();
        log.warn("NumberFormatException [{}]: {}", errorId, ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(withId(MSG_NUMBER, errorId)));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        String errorId = newErrorId();
        log.warn("DB integrity violation [{}]: {}", errorId, e.getMostSpecificCause().getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(withId(MSG_DUPLICATE, errorId)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleUnexpected(Exception e) {
        String errorId = newErrorId();
        log.error("Unexpected error [{}]", errorId, e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(withId(MSG_INTERNAL, errorId)));
    }

    // 한국어 메시지는 그대로 전달, 영어/저수준 메시지는 오류 ID와 함께 로그에만 남기고 한국어 일반 메시지로 치환
    private ResponseEntity<ApiResponse<Object>> userFacing(int status, Exception e, String fallback, String label) {
        String raw = e.getMessage();
        if (isKorean(raw)) {
            log.warn("{}: {} (status={})", label, raw, status);
            return ResponseEntity.status(status).body(ApiResponse.error(raw));
        }
        String errorId = newErrorId();
        log.warn("{} [{}] (status={}): {}", label, errorId, status, raw, e);
        return ResponseEntity.status(status).body(ApiResponse.error(withId(fallback, errorId)));
    }

    // 한글 포함 = 사용자에게 보여주려고 작성된 메시지로 간주
    private static boolean isKorean(String s) {
        return s != null && s.codePoints().anyMatch(cp -> cp >= 0xAC00 && cp <= 0xD7A3);
    }

    // 로그와 응답을 잇는 짧은 추적 ID
    private static String newErrorId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private static String withId(String message, String errorId) {
        return message + " (오류 ID: " + errorId + ")";
    }

    // JsonMappingException 의 path 를 읽기 좋은 경로 문자열로 변환
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
