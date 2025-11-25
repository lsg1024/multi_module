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
public class StoneClient {

    @Value("${BASE_URL}")
    private String BASE_URL;
    @Value("${PRODUCT_SERVER_URL}")
    private String PRODUCT_URL;

    private final RestClientUtil restClientUtil;

    public StoneClient(RestClientUtil restClientUtil) {
        this.restClientUtil = restClientUtil;
    }

    @Retry(value = 3)
    public Boolean getExistStoneId(String token, Long stoneId) {
        ResponseEntity<ApiResponse<Boolean>> response;

        String url = "https://" + BASE_URL + PRODUCT_URL + "/stone/" + stoneId;
        response = restClientUtil.get(url, token,
                new ParameterizedTypeReference<>() {}
        );

        HttpStatusCode status = response.getStatusCode();
        if (status.is5xxServerError()
                || status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)
                || status.isSameCodeAs(HttpStatus.REQUEST_TIMEOUT)) {
            throw new RetryableExternalException(NO_CONNECT_SERVER + status.value());
        }

        if (response.getStatusCode().is4xxClientError()) {
            throw new IllegalArgumentException(NOT_FOUND);
        }

        Boolean data = response.getBody().getData();
        if (data == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + stoneId);
        }

        return data;
    }
}
