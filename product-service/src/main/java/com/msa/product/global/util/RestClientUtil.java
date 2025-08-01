package com.msa.product.global.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class RestClientUtil {

    private final RestTemplate restTemplate;

    public RestClientUtil(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public <T, R> ResponseEntity<R> post(HttpServletRequest request, String url, T body, Class<R> responseType) {
        HttpEntity<T> entity = new HttpEntity<>(body, buildHeaders(request));
        return restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
    }

    public <R> ResponseEntity<R> get(HttpServletRequest request, String url, Class<R> responseType) {
        HttpEntity<Void> entity = new HttpEntity<>(buildHeaders(request));
        return restTemplate.exchange(url, HttpMethod.GET, entity, responseType);
    }

    public <T, R> ResponseEntity<R> put(HttpServletRequest request, String url, T body, Class<R> responseType) {
        HttpEntity<T> entity = new HttpEntity<>(body, buildHeaders(request));
        return restTemplate.exchange(url, HttpMethod.PUT, entity, responseType);
    }

    public <R> ResponseEntity<R> delete(HttpServletRequest request, String url, Class<R> responseType) {
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
