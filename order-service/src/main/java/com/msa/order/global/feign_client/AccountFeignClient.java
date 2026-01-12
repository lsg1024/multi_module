package com.msa.order.global.feign_client;

import com.msa.common.global.api.ApiResponse;
import com.msa.order.local.order.dto.FactoryDto;
import com.msa.order.local.order.dto.StoreDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "account", fallbackFactory = AccountFeignClientFallbackFactory.class)
public interface AccountFeignClient {

    @GetMapping("/api/factory/{factoryId}")
    ResponseEntity<ApiResponse<FactoryDto.Response>> getFactoryInfo(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("factoryId") Long factoryId
    );

    @GetMapping("/api/store/{storeId}")
    ResponseEntity<ApiResponse<StoreDto.Response>> getStoreInfo(
            @RequestHeader Map<String, Object> headers,
            @PathVariable("storeId") Long storeId
    );

    @GetMapping("/stores/receivable/sale-log/{id}")
    ResponseEntity<ApiResponse<StoreDto.accountResponse>> getStoreReceivableDetailLog(
            @RequestHeader Map<String, Object> header,
            @PathVariable(name = "id") String storeId,
            @RequestParam(name = "saleCode") String saleCode);

}