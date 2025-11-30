package com.msa.order.global.feign_client.client;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.order.global.feign_client.AccountFeignClient;
import com.msa.order.local.order.dto.FactoryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NO_CONNECT_SERVER;

@Service
@RequiredArgsConstructor
public class FactoryClient {

    private final JwtUtil jwtUtil;
    private final AccountFeignClient accountFeignClient;

    public FactoryDto.Response getFactoryInfo(String token, Long factoryId) {
        ResponseEntity<ApiResponse<FactoryDto.Response>> response;

        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("X-Forwarded-For", jwtUtil.getForward(token));
        headers.put("X-Tenant-ID", jwtUtil.getTenantId(token));
        headers.put("User-Agent", jwtUtil.getDevice(token));

        try {
            response = accountFeignClient.getFactoryInfo(headers, factoryId);
        } catch (Exception e) {
            throw new IllegalArgumentException(NO_CONNECT_SERVER + e.getMessage());
        }

        if (response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
            throw new IllegalArgumentException("Service Unavailable: " + response.getBody().getMessage());
        }

        if (response.getBody().getData() == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + factoryId);
        }

        return response.getBody().getData();
    }
}