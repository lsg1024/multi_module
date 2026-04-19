package com.msa.userserver.domain.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.domain.dto.MessageDto;
import com.msa.common.global.jwt.AccessToken;
import com.msa.userserver.domain.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<List<MessageDto.SendResult>>> sendMessage(
            @AccessToken String accessToken,
            @Valid @RequestBody MessageDto.SendRequest request) {

        List<MessageDto.SendResult> results = messageService.sendMessage(accessToken, request);
        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Page<MessageDto.HistoryResponse>>> getHistory(
            @AccessToken String accessToken,
            @PageableDefault(size = 20) Pageable pageable) {

        Page<MessageDto.HistoryResponse> history = messageService.getHistory(accessToken, pageable);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
