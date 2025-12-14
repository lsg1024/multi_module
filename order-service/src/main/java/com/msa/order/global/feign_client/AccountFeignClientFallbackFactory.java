package com.msa.order.global.feign_client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.api.ApiResponse;
import com.msa.order.local.order.dto.FactoryDto;
import com.msa.order.local.order.dto.StoreDto;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

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

                return ResponseEntity.ok(ApiResponse.error(errorResult.message));
            }

            @Override
            public ResponseEntity<ApiResponse<StoreDto.Response>> getStoreInfo(Map<String, Object> headers, Long storeId) {
                ErrorResult errorResult = resolveError(cause, "상점 정보를 불러오는 중 오류가 발생했습니다.");

                log.error("[Fallback] getStoreInfo ID: {}, Status: {}, Msg: {}", storeId, errorResult.status, cause.getMessage());

                return ResponseEntity.ok(ApiResponse.error(errorResult.message));
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