package com.msa.product.global.feign_client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.api.ApiResponse;
import com.msa.product.local.product.dto.FactoryDto;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AccountFeignClientFallbackFactory implements FallbackFactory<AccountFeignClient> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AccountFeignClient create(Throwable cause) {
        return new AccountFeignClient() {
            @Override
            public ResponseEntity<ApiResponse<FactoryDto.Response>> getFactoryInfo(Map<String, Object> headers, Long factoryId) {
                ErrorResult errorResult = resolveError(cause, "공장 상세 정보를 불러오는 중 오류가 발생했습니다.");

                log.error("[Fallback] getFactoryInfo ID: {}, Status: {}, Msg: {}", factoryId, errorResult.status, cause.getMessage());

                return ResponseEntity.status(errorResult.status)
                        .body(ApiResponse.error(errorResult.message));
            }

            @Override
            public ResponseEntity<ApiResponse<List<FactoryDto.ResponseBatch>>> getFactories(Map<String, Object> headers) {
                ErrorResult errorResult = resolveError(cause, "공장 목록을 불러오는 중 오류가 발생했습니다.");

                log.error("[Fallback] getFactories Status: {}, Msg: {}", errorResult.status, cause.getMessage());

                return ResponseEntity.status(errorResult.status)
                        .body(ApiResponse.error(errorResult.message));
            }

        };
    }

    private ErrorResult resolveError(Throwable cause, String defaultMessage) {
        HttpStatus status = HttpStatus.SERVICE_UNAVAILABLE;
        String message = defaultMessage;

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