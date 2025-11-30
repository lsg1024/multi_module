package com.msa.auth.user;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.domain.dto.UserDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserClient> {

    @Override
    public UserClient create(Throwable cause) {
        return (headers, loginDto) -> {
            log.error("UserClient Login Fallback triggered. Email: {}, Error: {}", loginDto.getUserId(), cause.getMessage());

            ApiResponse<UserDto.UserInfo> response = ApiResponse.error("현재 사용자 서비스 이용이 불가능합니다.");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
        };
    }
}