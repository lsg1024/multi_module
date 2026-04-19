package com.msa.userserver.domain.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.domain.dto.MessageDto;
import com.msa.common.global.jwt.AccessToken;
import com.msa.userserver.domain.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sens")
@RequiredArgsConstructor
public class SensConfigController {

    private final MessageService messageService;

    @PostMapping("/config")
    public ResponseEntity<ApiResponse<MessageDto.SensConfigResponse>> saveSensConfig(
            @AccessToken String accessToken,
            @Valid @RequestBody MessageDto.SensConfigRequest request) {

        MessageDto.SensConfigResponse response = messageService.saveSensConfig(accessToken, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<MessageDto.SensConfigResponse>> getSensConfig(
            @AccessToken String accessToken) {

        MessageDto.SensConfigResponse response = messageService.getSensConfig(accessToken);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/config")
    public ResponseEntity<ApiResponse<String>> deleteSensConfig(
            @AccessToken String accessToken) {

        messageService.deleteSensConfig(accessToken);
        return ResponseEntity.ok(ApiResponse.success("SENS 설정이 삭제되었습니다."));
    }
}
