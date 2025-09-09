package com.msa.product.local.stone.type.controller;

import com.msa.product.local.stone.type.dto.StoneTypeDto;
import com.msa.product.local.stone.type.service.StoneTypeService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StoneTypeController {

    private final StoneTypeService stoneTypeService;

    public StoneTypeController(StoneTypeService stoneTypeService) {
        this.stoneTypeService = stoneTypeService;
    }

    @PostMapping("/stone/types")
    public ResponseEntity<ApiResponse<String>> createStoneType(
            @Valid @RequestBody StoneTypeDto stoneTypeDto) {

        stoneTypeService.saveStoneType(stoneTypeDto);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @PostMapping("/stonetypess")
    public ResponseEntity<ApiResponse<String>> createStoneTypes(
            @Valid @RequestBody List<StoneTypeDto> stoneTypeDto) {

        stoneTypeDto.forEach(stoneTypeService::saveStoneType);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @GetMapping("/stone/types/{id}")
    public ResponseEntity<ApiResponse<StoneTypeDto.ResponseSingle>> getStoneType(
            @PathVariable(name = "id") Long stoneTypeId) {

        StoneTypeDto.ResponseSingle response = stoneTypeService.getStoneType(stoneTypeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stone/types")
    public ResponseEntity<ApiResponse<List<StoneTypeDto.ResponseSingle>>> getStoneTypes(
            @RequestParam(name = "name", required = false) String stoneTypeName) {

        List<StoneTypeDto.ResponseSingle> responses =
                stoneTypeService.getStoneTypes(stoneTypeName);

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PatchMapping("/stone/types/{id}")
    public ResponseEntity<ApiResponse<String>> updateStoneType(
            @PathVariable(name = "id") Long stoneTypeId,
            @Valid @RequestBody StoneTypeDto stoneTypeDto) {

        stoneTypeService.updateStoneType(stoneTypeId, stoneTypeDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @DeleteMapping("/stone/types/{id}")
    public ResponseEntity<ApiResponse<String>> deleteStoneType(
            @AccessToken String accessToken,
            @PathVariable(name = "id") Long stoneTypeId) {

        stoneTypeService.deleteStoneType(accessToken, stoneTypeId);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }
}
