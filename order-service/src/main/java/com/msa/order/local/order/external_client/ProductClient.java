package com.msa.order.local.order.external_client;

import com.msa.common.global.api.ApiResponse;
import com.msa.order.global.exception.RetryableExternalException;
import com.msa.order.global.util.RestClientUtil;
import com.msa.order.local.order.external_client.dto.ProductDetailDto;
import com.msa.order.local.order.external_client.dto.ProductImageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;
import static com.msa.order.global.exception.ExceptionMessage.NO_CONNECT_SERVER;

@Slf4j
@Service
public class ProductClient {

    @Value("${PRODUCT_SERVER_URL}")
    private String baseUrl;

    private final RestClientUtil restClientUtil;

    public ProductClient(RestClientUtil restClientUtil) {
        this.restClientUtil = restClientUtil;
    }

    @Retryable(retryFor = RetryableExternalException.class, backoff = @Backoff(value = 200, multiplier = 2, random = true))
    public ProductDetailDto getProductInfo(String tenantId, Long productId, String grade) {

        ResponseEntity<ApiResponse<ProductDetailDto>> response;

        try {
            String url = "http://" + tenantId + baseUrl + "/product/" + productId + "/" + grade;
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
        ProductDetailDto data = response.getBody().getData();
        if (data == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + productId);
        }

        return data;
    }

    @Retryable(recover= "recoverGetProductImages" ,retryFor = RetryableExternalException.class, backoff = @Backoff(value = 200, multiplier = 2, random = true))
    public Map<Long, ProductImageDto> getProductImages(String tenantId, List<Long> productIds) {

        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String url = UriComponentsBuilder.fromUriString("http://" + tenantId + baseUrl + "/products/images")
                .queryParam("ids", productIds)
                .build(true)
                .toUriString();

        try {
            ParameterizedTypeReference<ApiResponse<Map<Long, ProductImageDto>>> responseType =
                    new ParameterizedTypeReference<>() {};

            ResponseEntity<ApiResponse<Map<Long, ProductImageDto>>> response = restClientUtil.get(url, responseType);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Product 이미지 조회 응답 실패 - Status: {}", response.getStatusCode());
                throw new RetryableExternalException("Product 이미지 조회 응답 실패 - Status: " + response.getStatusCode());
            }
            return response.getBody() != null ? response.getBody().getData() : Collections.emptyMap();

        } catch (RestClientException e) {
            throw new RetryableExternalException(NO_CONNECT_SERVER + e.getMessage());
        }
    }

    @Recover
    public Map<Long, ProductImageDto> recoverGetProductImages(RetryableExternalException e, String tenantId, List<Long> productIds) {
        log.error("ProductClient: getProductImages 모든 재시도 실패. Fallback 실행. tenantId={}, productIds={}, error={}",
                tenantId, productIds, e.getMessage());

        return Collections.emptyMap();
    }

}
