package com.msa.product.global.util;

import com.msa.common.global.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestClientUtil {

    private final RestTemplate restTemplate;

    public RestClientUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // POST
    public <T, R> ResponseEntity<ApiResponse<R>> post(HttpServletRequest request, String url, T body, ParameterizedTypeReference<ApiResponse<R>> responseType) {
        HttpEntity<T> entity = new HttpEntity<>(body, buildHeaders(request));
        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
    }

    // GET
    public <R> ResponseEntity<ApiResponse<R>> get(HttpServletRequest request, String url, ParameterizedTypeReference<ApiResponse<R>> responseType) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(request));
        return restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
    }

    // PUT
    public <T, R> ResponseEntity<ApiResponse<R>> put(HttpServletRequest request, String url, T body, ParameterizedTypeReference<ApiResponse<R>> responseType) {
        HttpEntity<T> entity = new HttpEntity<>(body, buildHeaders(request));
        return restTemplate.exchange(url, HttpMethod.PUT, entity, responseType);
    }

    // DELETE
    public <R> ResponseEntity<ApiResponse<R>> delete(HttpServletRequest request, String url, ParameterizedTypeReference<ApiResponse<R>> responseType) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(request));
        return restTemplate.exchange(url, HttpMethod.DELETE, entity, responseType);
    }

    private HttpHeaders buildHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String[] headerKeys = {
                "X-Tenant-ID", "Authorization", "User-Agent",
                "X-Forwarded-For", "X-Device", "X-Request-ID"
        };

        for (String key : headerKeys) {
            String value = request.getHeader(key);
            if (value != null) headers.add(key, value);
        }

        return headers;
    }
}
