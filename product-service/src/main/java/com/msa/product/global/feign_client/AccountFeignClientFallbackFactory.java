package com.msa.product.global.feign_client;

import com.msa.common.global.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AccountFeignClientFallbackFactory implements FallbackFactory<AccountFeignClient> {

    @Override
    public AccountFeignClient create(Throwable cause) {
        return (headers, factoryId) -> {
            log.error("FactoryInfo Fallback ID: {}. Error: {}", factoryId, cause.getMessage());

            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(ApiResponse.error("현재 공장 정보 서비스를 이용할 수 없습니다."));
        };
    }
}