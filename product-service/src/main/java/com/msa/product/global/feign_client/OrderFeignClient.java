package com.msa.product.global.feign_client;

import com.msa.common.global.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;
import java.util.Map;

@FeignClient(name = "order", path = "/api")
public interface OrderFeignClient {

    @PostMapping("/stocks/count-by-names")
    ResponseEntity<ApiResponse<Map<String, Integer>>> getStockCountByProductNames(
            @RequestHeader Map<String, Object> headers,
            @RequestBody List<String> productNames
    );
}
