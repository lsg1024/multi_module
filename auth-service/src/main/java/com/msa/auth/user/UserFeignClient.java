package com.msa.auth.user;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.domain.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

        Map<String, Object> headers = new HashMap<>();
        headers.put("X-Forwarded-For", request.getHeader("X-Forwarded-For"));
        headers.put("X-Tenant-ID", request.getHeader("X-Tenant-ID"));
        headers.put("User-Agent", request.getHeader("User-Agent"));

        ResponseEntity<ApiResponse<UserDto.UserInfo>> response = userClient.login(headers, loginDto);

        ApiResponse<UserDto.UserInfo> body = response.getBody();

        log.info("UserFeignClient = {} , {}", body.getMessage(), body.getData());
        if (!body.isSuccess()) {
            String msg = body.getMessage() != null ? body.getMessage() : "아이디/비밀번호가 일치하지 않습니다.";
            throw new BadCredentialsException(msg);
        }

        return response;
    }
}