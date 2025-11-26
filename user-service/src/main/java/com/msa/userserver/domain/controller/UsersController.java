package com.msa.userserver.domain.controller;

import com.msa.userserver.domain.service.UsersService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.domain.dto.UserDto;
import com.msa.common.global.jwt.AccessToken;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
public class UsersController {

    private final UsersService usersService;

    public UsersController(UsersService usersService) {
        this.usersService = usersService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDto.UserInfo>> loginCheck(@Valid @RequestBody UserDto.Login userDto) {

        // 1. [진입점 로그] 요청이 여기까지 오는지 확인 필수
        log.info("[UsersController] 로그인 요청 받음. ID: {}", userDto.getUserId());

        UserDto.UserInfo userInfo = usersService.login(userDto);

        log.info("[UsersController] 로그인 로직 처리 완료. 응답 반환.");

        return ResponseEntity.ok(ApiResponse.success(userInfo));

    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> createUser(@Valid @RequestBody UserDto.Create userDto) {
        log.info("[UsersController] 회원가입 요청 받음. ID: {}", userDto.getUserId());
        usersService.createUser(userDto);
        return ResponseEntity.ok(ApiResponse.success("가입 완료"));
    }

    @GetMapping("/info")
    public ResponseEntity<ApiResponse<UserDto.UserInfo>> getUserInfo(
            @AccessToken String accessToken) {
        log.info("[UsersController] 유저 정보 조회 요청");
        UserDto.UserInfo userInfo = usersService.getUserInfo(accessToken);
        return ResponseEntity.ok(ApiResponse.success(userInfo));
    }

    //유저 수정
    @PatchMapping("/info")
    public ResponseEntity<ApiResponse<String>> updateUserInfo(
            @AccessToken String accessToken,
            @Valid @RequestBody UserDto.Update updateDto) {
        usersService.updateUserInfo(accessToken, updateDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    //유저 삭제
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deletedUser(
            @AccessToken String accessToken) {
        usersService.deletedUser(accessToken);
        return ResponseEntity.ok(ApiResponse.success("삭제완료"));
    }

    //유저 목록
    @GetMapping("/list")
    public List<UserDto.UserInfo> getUserList(
            @AccessToken String accessToken) {
        return usersService.getAllUsers(accessToken);
    }

}