package com.msa.auth.user;

import com.msacommon.global.api.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ms1")
public interface UserServerClient {

    @PostMapping("/user/login-check")
    ResponseEntity<ApiResponse<UserDto.UserInfo>> getLoginCheck(@RequestBody UserDto.Login login);
}
