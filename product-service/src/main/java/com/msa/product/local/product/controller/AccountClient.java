package com.msa.product.local.product.controller;

import com.msa.product.global.util.RestClientUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.msa.product.global.exception.ExceptionMessage.NOT_FOUND;

@Slf4j
@Service
public class AccountClient {

    @Value("${server_url}")
    private String baseUrl;
    private final RestClientUtil restClientUtil;

    public AccountClient(RestClientUtil restClientUtil) {
        this.restClientUtil = restClientUtil;
    }

    public String validateFactoryId(HttpServletRequest request, Long factoryId) {

        String tenantId = request.getHeader("X-Tenant-ID");
        String authorization = request.getHeader("Authorization");

        ResponseEntity<String> response;
        try {
            String url = "http://" + tenantId + baseUrl + "/factory/" + factoryId + "/exists";
            log.info("validateFactoryId {} {} {}", tenantId, url, authorization);
            response = restClientUtil.get(request, url, String.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("서버 연결 실패");
        }

        if (response.getStatusCode().is4xxClientError()) {
            throw new IllegalArgumentException(NOT_FOUND);
        }
        return response.getBody();
    }

}
