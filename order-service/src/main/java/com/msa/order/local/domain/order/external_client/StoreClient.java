package com.msa.order.local.domain.order.external_client;

import com.msa.common.global.api.ApiResponse;
import com.msa.order.global.exception.RetryableExternalException;
import com.msa.order.global.util.RestClientUtil;
import com.msa.order.local.domain.order.dto.StoreDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NO_CONNECT_SERVER;

@Service
public class StoreClient {
    @Value("${ACCOUNT_SERVER_URL}")
    private String baseUrl;

    private final RestClientUtil restClientUtil;

    public StoreClient(RestClientUtil restClientUtil) {
        this.restClientUtil = restClientUtil;
    }

    @Retryable(retryFor = RetryableExternalException.class, backoff = @Backoff(value = 200, multiplier = 2, random = true))
    public StoreDto.Response getStoreInfo(String tenantId, Long storeId) {
        ResponseEntity<ApiResponse<StoreDto.Response>> response = null;

        try {
            String url = "http://" + tenantId + baseUrl + "/store/" + storeId;
            response = restClientUtil.get(url,
                    new ParameterizedTypeReference<>() {
                    }
            );
        } catch (Exception e) {
            throw new RetryableExternalException(NO_CONNECT_SERVER + e.getMessage());

        }
        if (response.getStatusCode().is4xxClientError()) {
            throw new IllegalArgumentException(NOT_FOUND);
        }

        StoreDto.Response data = response.getBody().getData();
        if (data == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + storeId);
        }

        return data;
    }

}
