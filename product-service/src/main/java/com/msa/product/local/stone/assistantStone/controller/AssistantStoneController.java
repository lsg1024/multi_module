package com.msa.product.local.stone.assistantStone.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.product.local.stone.assistantStone.dto.AssistantStoneDto;
import com.msa.product.local.stone.assistantStone.service.AssistantStoneService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AssistantStoneController {

    private final AssistantStoneService assistantStoneService;

    public AssistantStoneController(AssistantStoneService assistantStoneService) {
        this.assistantStoneService = assistantStoneService;
    }

    @GetMapping("/assistant_stones")
    public ResponseEntity<ApiResponse<List<AssistantStoneDto.Response>>> getAssistants() {
        List<AssistantStoneDto.Response> assistantStoneInfo = assistantStoneService.getAssistantStoneAll();
        return ResponseEntity.ok(ApiResponse.success(assistantStoneInfo));
    }

    @GetMapping("/api/assistant_stone/{id}")
    public ResponseEntity<ApiResponse<AssistantStoneDto.Response>> getAssistantInfo(
            @PathVariable Long id) {
        AssistantStoneDto.Response assistantStone = assistantStoneService.getAssistantStone(id);
        return ResponseEntity.ok(ApiResponse.success(assistantStone));
    }

    @PostMapping("/assistant_stones")
    public ResponseEntity<ApiResponse<String>> createAssistantStone(
            @AccessToken String accessToken,
            @Valid @RequestBody AssistantStoneDto.Request assistantDto) {
        assistantStoneService.createAssistantStone(accessToken, assistantDto);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @PatchMapping("/assistant_stones/{id}")
    public ResponseEntity<ApiResponse<String>> updateAssistantStone(
            @AccessToken String accessToken,
            @PathVariable(name = "id") String assistantId,
            @Valid @RequestBody AssistantStoneDto.Request assistantDto) {
        assistantStoneService.updateAssistantStone(accessToken, assistantId, assistantDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @DeleteMapping("/assistant_stones/{id}")
    public ResponseEntity<ApiResponse<String>> deleteAssistantStone(
            @AccessToken String accessToken,
            @PathVariable(name = "id") String assistantId) {
        assistantStoneService.deletedAssistantStone(accessToken, assistantId);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }

}
