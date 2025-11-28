package com.msa.order.local.priority.controller;


import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.order.local.priority.dto.PriorityDto;
import com.msa.order.local.priority.service.PriorityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class PriorityController  {

    private final PriorityService priorityService;

    public PriorityController(PriorityService priorityService) {
        this.priorityService = priorityService;
    }

    @GetMapping("/priorities")
    public ResponseEntity<ApiResponse<List<PriorityDto>>> findAllPriority() {
        List<PriorityDto> allPriority = priorityService.findAllPriority();
        return ResponseEntity.ok(ApiResponse.success(allPriority));
    }

    @PostMapping("/priority")
    public ResponseEntity<ApiResponse<String>> createPriority(
            @AccessToken String accessToken,
            @RequestBody PriorityDto.Request priorityDto) {
        priorityService.createPriority(accessToken, priorityDto);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @PatchMapping("/priorities/{id}")
    public ResponseEntity<ApiResponse<String>> updatePriority(
            @AccessToken String accessToken,
            @PathVariable(name = "id") String priorityId,
            @RequestBody PriorityDto.Update priorityDto) {
        priorityService.updatePriority(accessToken, priorityId, priorityDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @DeleteMapping("/priorities/{id}")
    public ResponseEntity<ApiResponse<String>> deletedPriority(
            @AccessToken String accessToken,
            @PathVariable(name = "id") String priorityId) {
        priorityService.delete(accessToken, priorityId);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }

}
