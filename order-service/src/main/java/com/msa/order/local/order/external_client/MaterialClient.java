package com.msa.order.local.order.external_client;

import com.msa.common.global.aop.Retry;
import com.msa.common.global.api.ApiResponse;
import com.msa.order.global.exception.RetryableExternalException;
import com.msa.order.global.util.RestClientUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NO_CONNECT_SERVER;

@Service
public class MaterialClient {

    @Value("${PRODUCT_SERVER_URL}")
    private String baseUrl;

    private final RestClientUtil restClientUtil;

    public MaterialClient(RestClientUtil restClientUtil) {
        this.restClientUtil = restClientUtil;
    }

    @Retry(value = 3)
    public String getMaterialInfo(String tenantId, Long materialId) {
        ResponseEntity<ApiResponse<String>> response;

        try {
            String url = "http://" + tenantId + baseUrl + "/material/" + materialId;
            response = restClientUtil.get(url,
                    new ParameterizedTypeReference<>() {}
            );
        } catch (Exception e) {
            throw new RetryableExternalException(NO_CONNECT_SERVER + e.getMessage());
        }

        if (response.getStatusCode().is4xxClientError()) {
            throw new IllegalArgumentException(NOT_FOUND);
        }

        String data = response.getBody().getData();
        if (data == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + materialId);
        }

        return data;
    }
}
