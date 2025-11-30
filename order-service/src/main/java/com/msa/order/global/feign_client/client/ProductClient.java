package com.msa.order.global.feign_client.client;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.order.global.exception.RetryableExternalException;
import com.msa.order.global.feign_client.ProductFeignClient;
import com.msa.order.global.feign_client.dto.ProductDetailDto;
import com.msa.order.global.feign_client.dto.ProductImageDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NO_CONNECT_SERVER;

@Service
@RequiredArgsConstructor
public class ProductClient {

    private final JwtUtil jwtUtil;
    private final ProductFeignClient productFeignClient;

    public ProductDetailDto getProductInfo(String token, Long productId, String grade) {
        ResponseEntity<ApiResponse<ProductDetailDto>> response;

        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("X-Forwarded-For", jwtUtil.getForward(token));
        headers.put("X-Tenant-ID", jwtUtil.getTenantId(token));
        headers.put("User-Agent", jwtUtil.getDevice(token));

        try {
            response = productFeignClient.getProductInfo(headers, productId, grade);
        } catch (FeignException e) {
            if (e.status() >= 400 && e.status() < 500) {
                throw new IllegalArgumentException(NOT_FOUND);
            }
            throw new IllegalArgumentException(NO_CONNECT_SERVER + e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException(NO_CONNECT_SERVER + e.getMessage());
        }

        ProductDetailDto data = response.getBody().getData();
        if (data == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + productId);
        }

        return data;
    }

    public Map<Long, ProductImageDto> getProductImages(String token, List<Long> productIds) {

        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("X-Forwarded-For", jwtUtil.getForward(token));
        headers.put("X-Tenant-ID", jwtUtil.getTenantId(token));
        headers.put("User-Agent", jwtUtil.getDevice(token));

        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            ResponseEntity<ApiResponse<Map<Long, ProductImageDto>>> response = productFeignClient.getProductImages(headers, productIds);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalArgumentException("Product 이미지 조회 응답 실패 - Status: " + response.getStatusCode());
            }
            return response.getBody().getData();

        } catch (Exception e) {
            throw new IllegalArgumentException(NO_CONNECT_SERVER + e.getMessage());
        }
    }
}