package com.msa.auth.user;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.domain.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class UserFeignClient {

    private final UserClient userClient;

    public UserFeignClient(UserClient userClient) {
        this.userClient = userClient;
    }

    public ResponseEntity<ApiResponse<UserDto.UserInfo>> getLogin(HttpServletRequest request, UserDto.Login loginDto) {

        // 1. 진입 로그
        log.info("[UserFeignClient] 로그인 요청 시작 - Email: {}", loginDto.getUserId());

        Map<String, Object> headers = new HashMap<>();
        headers.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
        headers.put("X-Tenant-ID", request.getHeader("X-Tenant-ID"));
        headers.put("User-Agent", request.getHeader("User-Agent"));

        try {
            // 2. Feign 호출 직전 로그
            log.info("[UserFeignClient] User Service(Feign) 호출 시도...");

            // Feign Client (서킷 브레이커 적용) 호출
            ResponseEntity<ApiResponse<UserDto.UserInfo>> response = userClient.login(headers, loginDto);

            // 3. Feign 호출 성공 로그
            log.info("[UserFeignClient] User Service 호출 성공 - Status: {}", response.getStatusCode());

            return response;

        } catch (feign.FeignException.FeignClientException.BadRequest e) {
            // User Server에서 400 Bad Request 응답 (로그인 정보 불일치 등)
            log.warn("[UserFeignClient] 로그인 실패 (Bad Credentials): {}", e.getMessage());
            throw new BadCredentialsException("아이디/비밀번호가 일치하지 않습니다.");

        } catch (AuthenticationServiceException e) {
            // 4. 서킷 브레이커 Fallback 발생 로그
            // UserClient.loginFallback() 에서 던진 예외가 이쪽으로 옵니다.
            log.error("[UserFeignClient] 서킷 브레이커/Fallback 감지: {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            // 5. 알 수 없는 예외 로그
            log.error("[UserFeignClient] 알 수 없는 예외 발생! Type: {}, Message: {}", e.getClass().getName(), e.getMessage());
            e.printStackTrace(); // 스택 트레이스 출력하여 상세 원인 파악
            throw new AuthenticationServiceException("알 수 없는 인증 오류가 발생했습니다.");
        }
    }
}