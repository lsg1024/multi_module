package com.msa.product.global.util;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestClientUtil {

    private final RestTemplate restTemplate;
    private final JwtUtil jwtUtil;

    public RestClientUtil(@Qualifier("clientRestTemplate") RestTemplate restTemplate, JwtUtil jwtUtil) {
        this.restTemplate = restTemplate;
        this.jwtUtil = jwtUtil;
    }

    // POST
    public <T, R> ResponseEntity<ApiResponse<R>> post(String url, T body,
                                                      ParameterizedTypeReference<ApiResponse<R>> typeRef) {
        return restTemplate.exchange(url, HttpMethod.POST,
                null, typeRef);
    }

    public <R> ResponseEntity<ApiResponse<R>> get(String url, String token,
                                                  ParameterizedTypeReference<ApiResponse<R>> typeRef) {
        HttpEntity<Object> requestEntity = new HttpEntity<>(getHeader(token));
        return restTemplate.exchange(url, HttpMethod.GET,
                requestEntity, typeRef);
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

    private HttpHeaders getHeader(String token) {
        HttpHeaders headers = new HttpHeaders();
        String tenantId = jwtUtil.getTenantId(token);
        String forward = jwtUtil.getForward(token);
        String device = jwtUtil.getDevice(token);

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Forwarded-For", forward);
        headers.add("X-Tenant-ID", tenantId);
        headers.add("User-Agent", device);

        return headers;
    }
}
