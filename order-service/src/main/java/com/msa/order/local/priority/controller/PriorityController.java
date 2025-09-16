package com.msa.order.local.priority.controller;


import com.msa.common.global.api.ApiResponse;
import com.msa.order.local.priority.dto.PriorityDto;
import com.msa.order.local.priority.service.PriorityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
