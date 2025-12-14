package com.msa.auth.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.api.ApiResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public UserClient create(Throwable cause) {

        return (headers, loginDto) -> {
            ErrorResult errorResult = resolveError(cause);

            log.error("[Fallback] loginDto: {}, Status: {}, Msg: {}", loginDto, errorResult.status, cause.getMessage());

            return ResponseEntity.ok(ApiResponse.error(errorResult.message()));
        };
    }

    private ErrorResult resolveError(Throwable cause) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String message = "로그인 정보를 불러오는 중 오류가 발생했습니다.";

        if (cause instanceof FeignException feignException) {

            int feignStatus = feignException.status();
            if (feignStatus > 0) {
                status = HttpStatus.valueOf(feignStatus);
            }

            String content = feignException.contentUTF8();
            if (content != null && !content.isEmpty()) {
                try {
                    ApiResponse<?> errorResponse = objectMapper.readValue(content, ApiResponse.class);
                    if (errorResponse.getMessage() != null) {
                        message = errorResponse.getMessage();
                    }
                } catch (Exception e) {
                    log.warn("Fallback JSON Parsing Failed. Raw content: {}", content);
                }
            }
        }

        return new ErrorResult(status, message);
    }

    private record ErrorResult(HttpStatus status, String message) {}
}