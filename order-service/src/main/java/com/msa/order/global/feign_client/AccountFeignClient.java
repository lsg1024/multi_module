package com.msa.order.global.feign_client;

import com.msa.common.global.api.ApiResponse;
import com.msa.order.local.order.dto.FactoryDto;
import com.msa.order.local.order.dto.StoreDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "account", path = "/account/api")
public interface AccountFeignClient {

    @CircuitBreaker(name = "factoryService", fallbackMethod = "loginFallback")
    @GetMapping("/factory/{factoryId}")
    ResponseEntity<ApiResponse<FactoryDto.Response>> getFactoryInfo(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("factoryId") Long factoryId
    );

    @CircuitBreaker(name = "storeService", fallbackMethod = "loginFallback")
    @GetMapping("/store/{storeId}")
    ResponseEntity<ApiResponse<StoreDto.Response>> getStoreInfo(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("storeId") Long storeId
    );
}