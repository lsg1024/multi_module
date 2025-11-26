package com.msa.order.global.feign_client.client;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.order.global.exception.RetryableExternalException;
import com.msa.order.global.feign_client.ProductFeignClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NO_CONNECT_SERVER;

@Service
@RequiredArgsConstructor
public class ClassificationClient {

    private final JwtUtil jwtUtil;
    private final ProductFeignClient productFeignClient;

    @Retryable(retryFor = RetryableExternalException.class, backoff = @Backoff(value = 200, multiplier = 2, random = true))
    public String getClassificationInfo(String token, Long classificationId) {
        ResponseEntity<ApiResponse<String>> response;

        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("X-Forwarded-For", jwtUtil.getForward(token));
        headers.put("X-Tenant-ID", jwtUtil.getTenantId(token));
        headers.put("User-Agent", jwtUtil.getDevice(token));

        try {
            response = productFeignClient.getClassificationInfo(headers, classificationId);
        } catch (FeignException e) {
            if (e.status() >= 400 && e.status() < 500) {
                throw new IllegalArgumentException(NOT_FOUND);
            }
            throw new RetryableExternalException(NO_CONNECT_SERVER + e.getMessage());
        } catch (Exception e) {
            throw new RetryableExternalException(NO_CONNECT_SERVER + e.getMessage());
        }

        String data = response.getBody().getData();
        if (data == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + classificationId);
        }

        return data;
    }
}