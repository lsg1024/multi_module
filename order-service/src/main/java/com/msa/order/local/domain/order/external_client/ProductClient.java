package com.msa.order.local.domain.order.external_client;

import com.msa.common.global.api.ApiResponse;
import com.msa.order.global.util.RestClientUtil;
import com.msa.order.local.domain.order.external_client.dto.ProductDetailDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;

@Slf4j
@Service
public class ProductClient {

    @Value("${PRODUCT_SERVER_URL}")
    private String baseUrl;

    private final RestClientUtil restClientUtil;

    public ProductClient(RestClientUtil restClientUtil) {
        this.restClientUtil = restClientUtil;
    }

    public ProductDetailDto getProductInfo(String tenantId, Long productId, String grade) {

        ResponseEntity<ApiResponse<ProductDetailDto>> response;
        try {
            String url = "http://" + tenantId + baseUrl + "/product/" + productId + "/" + grade;
            response = restClientUtil.get(url,
                    new ParameterizedTypeReference<>() {}
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("서버 연결 실패");
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

}
