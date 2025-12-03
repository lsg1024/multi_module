package com.msa.auth.user;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.domain.dto.UserDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

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

        try {
            return userClient.login(headers, loginDto);

        } catch (feign.FeignException.FeignClientException.BadRequest e) {
            throw new BadCredentialsException("아이디/비밀번호가 일치하지 않습니다.");

        } catch (AuthenticationServiceException e) {
            throw e;

        } catch (Exception e) {
            throw new IllegalStateException("알수 없는 서버 연결 에러 발생");
        }
    }
}