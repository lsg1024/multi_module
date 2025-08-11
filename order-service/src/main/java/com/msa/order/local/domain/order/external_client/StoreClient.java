package com.msa.order.local.domain.order.external_client;

import com.msa.common.global.api.ApiResponse;
import com.msa.order.global.util.RestClientUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;

@Service
public class StoreClient {
    @Value("${ACCOUNT_SERVER_URL}")
    private String baseUrl;

    private final RestClientUtil restClientUtil;

    public StoreClient(RestClientUtil restClientUtil) {
        this.restClientUtil = restClientUtil;
    }

    public StoreInfo getStoreInfo(String tenantId, Long storeId) {
        ResponseEntity<ApiResponse<StoreInfo>> response;
        try {
            String url = "http://" + tenantId + baseUrl + "/store/" + storeId;
            response = restClientUtil.get(url,
                    new ParameterizedTypeReference<>() {
                    }
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("서버 연결 실패");
        }

        if (response.getStatusCode().is4xxClientError()) {
            throw new IllegalArgumentException(NOT_FOUND);
        }

        StoreInfo data = response.getBody().getData();
        if (data == null) {
            throw new IllegalArgumentException(NOT_FOUND + " " + storeId);
        }

        return data;
    }
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StoreInfo {
        private String storeName;
        private String grade;
    }

}
