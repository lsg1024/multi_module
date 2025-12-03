package com.msa.order.global.feign_client;

import com.msa.common.global.api.ApiResponse;
import com.msa.order.local.order.dto.FactoryDto;
import com.msa.order.local.order.dto.StoreDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class AccountFeignClientFallbackFactory implements FallbackFactory<AccountFeignClient> {

    @Override
    public AccountFeignClient create(Throwable cause) {
        return new AccountFeignClient() {
            @Override
            public ResponseEntity<ApiResponse<FactoryDto.Response>> getFactoryInfo(Map<String, Object> headers, Long factoryId) {
                log.error("FactoryInfo Fallback ID: {}. Error: {}", factoryId, cause.getMessage());

                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ApiResponse.error("현재 공장 정보 서비스를 이용할 수 없습니다."));
            }

            @Override
            public ResponseEntity<ApiResponse<StoreDto.Response>> getStoreInfo(Map<String, Object> headers, Long storeId) {
                log.error("StoreInfo Fallback ID: {}. Error: {}", storeId, cause.getMessage());

                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(ApiResponse.error("현재 상점 정보 서비스를 이용할 수 없습니다."));
            }
        };
    }
}