package com.msa.auth.user;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.domain.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/**
 * auth-service → user 도메인 Feign client.
 *
 * 모놀리스 통합으로 user-service 가 사라지고 jewelry_emp 의 user 모듈로 흡수됨에 따라
 * 호출 대상 service-id 를 "users" → "jewelry" 로 변경.
 *
 * jewelry_emp 의 UsersController.@PostMapping("/login") 이 동일 시그니처로 처리하므로
 * path / 요청 body / 응답 구조 변경 없음.
 *
 * 환경 변수로 토글 가능하게 두려면:
 *   @FeignClient(name = "${auth.user-service-id:jewelry}", ...)
 * 형태도 가능. 우선 직접 jewelry 로 변경.
 */
@FeignClient(name = "jewelry", fallbackFactory = UserClientFallbackFactory.class)
public interface UserClient {
    @PostMapping("/login")
    ResponseEntity<ApiResponse<UserDto.UserInfo>> login(
            @RequestHeader Map<String, Object> headers,
            @RequestBody UserDto.Login loginDto
    );
}