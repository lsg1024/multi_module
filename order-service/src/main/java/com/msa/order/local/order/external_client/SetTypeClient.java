package com.msa.order.local.order.external_client;

import com.msa.common.global.aop.Retry;
import com.msa.common.global.api.ApiResponse;
import com.msa.order.global.exception.RetryableExternalException;
import com.msa.order.global.util.RestClientUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NO_CONNECT_SERVER;

@Service
public class SetTypeClient {

    @Value("${PRODUCT_SERVER_URL}")
    private String baseUrl;

    private final RestClientUtil restClientUtil;

    public SetTypeClient(RestClientUtil restClientUtil) {
        this.restClientUtil = restClientUtil;
    }

    @Retry(value = 3)
    public String getSetTypeName(String tenantId, Long setTypeId) {
        ResponseEntity<ApiResponse<String>> response;

        String url = "http://" + tenantId + baseUrl + "/set-type/" + setTypeId;
        response = restClientUtil.get(url, new ParameterizedTypeReference<>() {});

        HttpStatusCode status = response.getStatusCode();
        if (status.is5xxServerError()
                || status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)
                || status.isSameCodeAs(HttpStatus.REQUEST_TIMEOUT)) {
            throw new RetryableExternalException(NO_CONNECT_SERVER + status.value());
        }

        if (response.getStatusCode().is4xxClientError()) {
            throw new IllegalArgumentException(NOT_FOUND);
        }

        String data = response.getBody().getData();
        if (data == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + setTypeId);
        }

        return data;
    }

}
