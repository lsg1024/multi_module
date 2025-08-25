package com.msa.order.local.order.external_client;

import com.msa.common.global.aop.Retry;
import com.msa.common.global.api.ApiResponse;
import com.msa.order.global.exception.RetryableExternalException;
import com.msa.order.global.util.RestClientUtil;
import com.msa.order.local.order.dto.FactoryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NO_CONNECT_SERVER;

@Service
public class FactoryClient {

    @Value("${ACCOUNT_SERVER_URL}")
    private String baseUrl;

    private final RestClientUtil restClientUtil;

    public FactoryClient(RestClientUtil restClientUtil) {
        this.restClientUtil = restClientUtil;
    }

    @Retry(value = 3)
    public FactoryDto.Response getFactoryInfo(String tenantId, Long factoryId) {
        ResponseEntity<ApiResponse<FactoryDto.Response>> response;

        try {
            String url = "http://" + tenantId + baseUrl + "/factory/" + factoryId;
            response = restClientUtil.get(url,
                    new ParameterizedTypeReference<>() {}
            );
        } catch (Exception e) {
            throw new RetryableExternalException(NO_CONNECT_SERVER + e.getMessage());
        }

        if (response.getStatusCode().is4xxClientError()) {
            throw new IllegalArgumentException(NOT_FOUND);
        }

        FactoryDto.Response factoryInfo = response.getBody().getData();
        if (factoryInfo == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + factoryId);
        }

        return factoryInfo;
    }

}
