package com.msa.product.local.product.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.product.global.util.RestClientUtil;
import com.msa.product.local.product.dto.FactoryDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.msa.product.global.exception.ExceptionMessage.NOT_FOUND;

@Service
public class AccountClient {


    @Value("${BASE_URL}")
    private String BASE_URL;

    @Value("${ACCOUNT_SERVER_URL}")
    private String ACCOUNT_URL;
    private final RestClientUtil restClientUtil;

    public AccountClient(RestClientUtil restClientUtil) {
        this.restClientUtil = restClientUtil;
    }

    public FactoryDto.Response getFactoryInfo(String token, Long factoryId) {

        ResponseEntity<ApiResponse<FactoryDto.Response>> response;
        try {
            String url = "https://" + BASE_URL + ACCOUNT_URL + "/factory/" + factoryId;
            response = restClientUtil.get(url, token, new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("서버 연결 실패");
        }

        if (response.getStatusCode().is4xxClientError()) {
            throw new IllegalArgumentException(NOT_FOUND);
        }
        return response.getBody().getData();
    }

}
