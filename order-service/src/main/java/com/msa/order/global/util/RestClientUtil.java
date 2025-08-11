package com.msa.order.global.util;

import com.msa.common.global.api.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestClientUtil {

    private final RestTemplate restTemplate;

    public RestClientUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // POST
    public <T, R> ResponseEntity<ApiResponse<R>> post(String url, T body,
                                                      ParameterizedTypeReference<ApiResponse<R>> typeRef) {
        return restTemplate.exchange(url, HttpMethod.POST,
                null, typeRef);
    }

    public <R> ResponseEntity<ApiResponse<R>> get(String url,
                                                  ParameterizedTypeReference<ApiResponse<R>> typeRef) {
        return restTemplate.exchange(url, HttpMethod.GET,
                null, typeRef);
    }

    public <T, R> ResponseEntity<ApiResponse<R>> put(String url, T body,
                                                     ParameterizedTypeReference<ApiResponse<R>> typeRef) {
        return restTemplate.exchange(url, HttpMethod.PUT,
                null, typeRef);
    }

    public <R> ResponseEntity<ApiResponse<R>> delete(String url,
                                                     ParameterizedTypeReference<ApiResponse<R>> typeRef) {
        return restTemplate.exchange(url, HttpMethod.DELETE,
                null, typeRef);
    }
}