package com.msa.product.global.feign_client.client;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.product.global.feign_client.OrderFeignClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockClient {

    private final JwtUtil jwtUtil;
    private final OrderFeignClient orderFeignClient;

    /**
     * 상품명 목록에 대한 활성 재고 수량을 조회한다.
     */
    public Map<String, Integer> getStockCountByProductNames(String token, List<String> productNames) {
        if (productNames == null || productNames.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("X-Forwarded-For", jwtUtil.getForward(token));
        headers.put("X-Tenant-ID", jwtUtil.getTenantId(token));
        headers.put("User-Agent", jwtUtil.getDevice(token));

        try {
            ResponseEntity<ApiResponse<Map<String, Integer>>> response =
                    orderFeignClient.getStockCountByProductNames(headers, productNames);
            Map<String, Integer> data = response.getBody().getData();
            return data != null ? data : Collections.emptyMap();
        } catch (FeignException e) {
            log.warn("재고 수량 조회 실패 (order-service 통신 오류): {}", e.getMessage());
            return Collections.emptyMap();
        } catch (Exception e) {
            log.warn("재고 수량 조회 중 오류: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
