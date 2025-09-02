package com.msa.product.local.product.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.product.global.util.RestClientUtil;
import com.msa.product.local.product.dto.FactoryDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.msa.product.global.exception.ExceptionMessage.NOT_FOUND;

@Service
public class AccountClient {

    @Value("${ACCOUNT_SERVER_URL}")
    private String baseUrl;
    private final RestClientUtil restClientUtil;

    public AccountClient(RestClientUtil restClientUtil) {
        this.restClientUtil = restClientUtil;
    }

    public FactoryDto.Response getFactoryInfo(HttpServletRequest request, Long factoryId) {

        String tenantId = request.getHeader("X-Tenant-ID");

        ResponseEntity<ApiResponse<FactoryDto.Response>> response;
        try {
            String url = "http://" + tenantId + baseUrl + "/factory/" + factoryId;
            response = restClientUtil.get(request, url, new ParameterizedTypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("서버 연결 실패");
        }

        if (response.getStatusCode().is4xxClientError()) {
            throw new IllegalArgumentException(NOT_FOUND);
        }
        return response.getBody().getData();
    }

}
