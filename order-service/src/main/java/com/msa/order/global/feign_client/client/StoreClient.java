package com.msa.order.global.feign_client.client;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.order.global.feign_client.AccountFeignClient;
import com.msa.order.local.order.dto.StoreDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NO_CONNECT_SERVER;

@Service
@RequiredArgsConstructor
public class StoreClient {

    private final JwtUtil jwtUtil;
    private final AccountFeignClient accountFeignClient;

    public StoreDto.Response getStoreInfo(String token, Long storeId) {
        ResponseEntity<ApiResponse<StoreDto.Response>> response;

        Map<String, Object> headers = getStringObjectMap(token);

        try {
            response = accountFeignClient.getStoreInfo(headers, storeId);
        } catch (FeignException e) {
            if (e.status() >= 400 && e.status() < 500) {
                throw new IllegalArgumentException(NOT_FOUND);
            }
            throw new IllegalArgumentException(NO_CONNECT_SERVER + e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException(NO_CONNECT_SERVER + e.getMessage());
        }

        StoreDto.Response data = response.getBody().getData();
        if (data == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + storeId);
        }

        return data;
    }

    public StoreDto.accountResponse getStoreReceivableDetailLog(String token, String storeId, String saleCode) {
        ResponseEntity<ApiResponse<StoreDto.accountResponse>>  response;

        Map<String, Object> headers = getStringObjectMap(token);

        try {
            response = accountFeignClient.getStoreReceivableDetailLog(headers, storeId, saleCode);
        } catch (FeignException e) {
            if (e.status() >= 400 && e.status() < 500) {
                throw new IllegalArgumentException(NOT_FOUND);
            }
            throw new IllegalArgumentException(NO_CONNECT_SERVER + e.getMessage());
        } catch (Exception e) {
            throw new IllegalArgumentException(NO_CONNECT_SERVER + e.getMessage());
        }

        StoreDto.accountResponse data = response.getBody().getData();
        if (data == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + storeId);
        }

        return data;
    }

    @NotNull
    private Map<String, Object> getStringObjectMap(String token) {
        Map<String, Object> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + token);
        headers.put("X-Forwarded-For", jwtUtil.getForward(token));
        headers.put("X-Tenant-ID", jwtUtil.getTenantId(token));
        headers.put("User-Agent", jwtUtil.getDevice(token));
        return headers;
    }
}