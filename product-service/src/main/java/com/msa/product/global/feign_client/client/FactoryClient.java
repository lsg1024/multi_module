package com.msa.product.global.feign_client.client;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.product.global.exception.RetryableExternalException;
import com.msa.product.global.feign_client.AccountFeignClient;
import com.msa.product.local.product.dto.FactoryDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.msa.product.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.product.global.exception.ExceptionMessage.NO_CONNECT_SERVER;


@Slf4j
@Service
@RequiredArgsConstructor
public class FactoryClient {

    private final JwtUtil jwtUtil;
    private final AccountFeignClient accountFeignClient;

    @Retryable(retryFor = RetryableExternalException.class, backoff = @Backoff(value = 200, multiplier = 2, random = true))
    public FactoryDto.Response getFactoryInfo(String token, Long factoryId) {
        ResponseEntity<ApiResponse<FactoryDto.Response>> response;

        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("X-Forwarded-For", jwtUtil.getForward(token));
        headers.put("X-Tenant-ID", jwtUtil.getTenantId(token));
        headers.put("User-Agent", jwtUtil.getDevice(token));

        try {
            response = accountFeignClient.getFactoryInfo(headers, factoryId);
        } catch (FeignException e) {
            if (e.status() >= 400 && e.status() < 500) {
                throw new IllegalArgumentException(NOT_FOUND);
            }
            throw new RetryableExternalException(NO_CONNECT_SERVER + e.getMessage());
        } catch (Exception e) {
            throw new RetryableExternalException(NO_CONNECT_SERVER + e.getMessage());
        }

        FactoryDto.Response factoryInfo = response.getBody().getData();
        if (factoryInfo == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + factoryId);
        }

        return factoryInfo;
    }

    @Retryable(retryFor = RetryableExternalException.class, backoff = @Backoff(value = 200, multiplier = 2, random = true))
    public List<FactoryDto.ResponseBatch> getFactories(String token) {
        ResponseEntity<ApiResponse<List<FactoryDto.ResponseBatch>>> response;

        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("X-Forwarded-For", jwtUtil.getForward(token));
        headers.put("X-Tenant-ID", jwtUtil.getTenantId(token));
        headers.put("User-Agent", jwtUtil.getDevice(token));

        log.info("getFactories headers = {}", headers.entrySet());

        try {
            response = accountFeignClient.getFactories(headers);
        } catch (FeignException e) {
            if (e.status() >= 400 && e.status() < 500) {
                throw new IllegalArgumentException(NOT_FOUND);
            }
            throw new RetryableExternalException(NO_CONNECT_SERVER + e.getMessage());
        } catch (Exception e) {
            throw new RetryableExternalException(NO_CONNECT_SERVER + e.getMessage());
        }

        return response.getBody().getData();
    }
}