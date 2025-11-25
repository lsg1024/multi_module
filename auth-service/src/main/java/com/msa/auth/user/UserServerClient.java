package com.msa.auth.user;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.domain.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class UserServerClient {

    @Value("${BASE_URL}")
    private String BASE_URL;
    @Value("${USER_SERVER_URL}")
    private String USER_CLIENT_URL;
    private final RestTemplate restTemplate;

    public UserServerClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<ApiResponse<UserDto.UserInfo>> getLogin(HttpServletRequest request, UserDto.Login loginDto) {

        String tenantId = request.getHeader("X-Tenant-ID");

        String url = "https://" + BASE_URL + USER_CLIENT_URL;

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
        headers.add("X-Tenant-ID", tenantId);
        headers.add("User-Agent", request.getHeader("User-Agent"));

        HttpEntity<UserDto.Login> entity = new HttpEntity<>(loginDto, headers);
        try {
            return restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<UserDto.UserInfo>>() {}
            );
        } catch (HttpClientErrorException e) {
            log.error("로그인 실패: {}", e.getMessage());
            throw new BadCredentialsException("아이디/비밀번호가 일치하지 않습니다.");
        } catch (RestClientException e) {
            log.error("user-service 연결 실패: {}", e.getMessage());
            throw new AuthenticationServiceException("인증 서버 장애가 발생했습니다.");
        }
    }

}
