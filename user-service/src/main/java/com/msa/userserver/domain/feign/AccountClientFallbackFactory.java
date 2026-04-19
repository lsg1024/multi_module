package com.msa.userserver.domain.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.domain.dto.MessageDto;
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
public class AccountClientFallbackFactory implements FallbackFactory<AccountClient> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AccountClient create(Throwable cause) {
        return new AccountClient() {
            @Override
            public ResponseEntity<ApiResponse<List<MessageDto.StorePhoneInfo>>> getStorePhones(
                    Map<String, Object> headers, List<Long> storeIds) {

                String message = "거래처 전화번호를 불러오는 중 오류가 발생했습니다.";

                if (cause instanceof FeignException feignException) {
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

                log.error("[Fallback] getStorePhones IDs: {}, Msg: {}", storeIds, cause.getMessage());
                return ResponseEntity.ok(ApiResponse.error(message));
            }
        };
    }
}
