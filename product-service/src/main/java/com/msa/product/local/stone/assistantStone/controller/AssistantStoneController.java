package com.msa.product.local.stone.assistantStone.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.product.local.stone.assistantStone.dto.AssistantStoneDto;
import com.msa.product.local.stone.assistantStone.service.AssistantStoneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AssistantStoneController {

    private final AssistantStoneService assistantStoneService;

    public AssistantStoneController(AssistantStoneService assistantStoneService) {
        this.assistantStoneService = assistantStoneService;
    }

    @GetMapping("/assistants")
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

}
