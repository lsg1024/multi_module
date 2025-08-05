package com.msa.order.local.domain.order.external_client;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.tenant.TenantContext;
import com.msa.order.global.util.RestClientUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;

@Service
public class ProductClient {

    @Value("${PRODUCT_SERVER_URL}")
    private String baseUrl;

    private final RestClientUtil restClientUtil;

    public ProductClient(RestClientUtil restClientUtil) {
        this.restClientUtil = restClientUtil;
    }

    public String getProductInfo(HttpServletRequest request, Long productId) {
        String tenant = TenantContext.getTenant();
        ResponseEntity<ApiResponse<String>> response;
        try {
            String url = "http://" + tenant + baseUrl + "/product/" + productId;
            response = restClientUtil.get(request, url,
                    new ParameterizedTypeReference<>() {}
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("서버 연결 실패");
        }

        if (response.getStatusCode().is4xxClientError()) {
            throw new IllegalArgumentException(NOT_FOUND);
        }
        String data = response.getBody().getData();
        if (data == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + productId);
        }

        return data;
    }

}
