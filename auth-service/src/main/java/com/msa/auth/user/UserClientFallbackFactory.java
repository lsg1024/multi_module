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
            String errorMessage = "로그인 서비스 연결 실패";
            HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;

            if (cause instanceof FeignException feignException) {
                log.error("UserClient Feign Error: Status={}, Msg={}", feignException.status(), feignException.getMessage());

                String responseBody = feignException.contentUTF8();

                if (responseBody != null && !responseBody.isEmpty()) {
                    try {
                        ApiResponse<?> errorResponse = objectMapper.readValue(responseBody, ApiResponse.class);
                        if (errorResponse.getMessage() != null) {
                            errorMessage = errorResponse.getMessage();
                        }

                        if (feignException.status() == 400) {
                            status = HttpStatus.BAD_REQUEST;
                        }
                    } catch (Exception e) {
                        log.error("Fallback JSON Parsing Error", e);
                    }
                }
            } else {
                log.error("UserClient Connection Error", cause);
            }

            return ResponseEntity.status(status)
                    .body(ApiResponse.error(errorMessage));
        };
    }
}