package com.msa.product.global.feign_client;


import com.msa.common.global.api.ApiResponse;
import com.msa.product.local.product.dto.FactoryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "account", path = "/api", fallbackFactory = AccountFeignClientFallbackFactory.class)
public interface AccountFeignClient {

    @GetMapping("/factory/{factoryId}")
    ResponseEntity<ApiResponse<FactoryDto.Response>> getFactoryInfo(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("factoryId") Long factoryId
    );

}