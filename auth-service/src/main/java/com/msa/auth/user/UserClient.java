package com.msa.auth.user;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.domain.dto.UserDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

@FeignClient(name = "users")
public interface UserClient {
    @CircuitBreaker(name = "userService", fallbackMethod = "loginFallback")
    @PostMapping("/login")
    ResponseEntity<ApiResponse<UserDto.UserInfo>> login(
            @RequestHeader Map<String, Object> headers,
            @RequestBody UserDto.Login loginDto
    );

    default ResponseEntity<ApiResponse<UserDto.UserInfo>> loginFallback(
            Map<String, Object> headers,
            UserDto.Login loginDto,
            Throwable t) {

        throw new AuthenticationServiceException("인증 서비스가 일시적으로 사용할 수 없습니다.");
    }
}