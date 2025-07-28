package com.msa.product.local.stone.stone.controller;

import com.msa.product.local.stone.stone.dto.StoneDto;
import com.msa.product.local.stone.stone.service.StoneService;
import com.msacommon.global.api.ApiResponse;
import com.msacommon.global.jwt.AccessToken;
import com.msacommon.global.util.CustomPage;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StoneController {
    private final StoneService stoneService;

    public StoneController(StoneService stoneService) {
        this.stoneService = stoneService;
    }

    // 생성
    @PostMapping("/stones")
    public ResponseEntity<ApiResponse<String>> createStone(
            @Valid @RequestBody StoneDto stoneDto) {
        stoneService.saveStone(stoneDto);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @PostMapping("/stoness")
    public ResponseEntity<ApiResponse<String>> createStones(
            @Valid @RequestBody List<StoneDto> stoneDto) {
        stoneDto.forEach(stoneService::saveStone);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    // 단건 조회
    @GetMapping("/stones/{id}")
    public ResponseEntity<ApiResponse<StoneDto.ResponseSingle>> getStone(
            @PathVariable(name = "id") Long stoneId) {
        StoneDto.ResponseSingle stone = stoneService.getStone(stoneId);
        return ResponseEntity.ok(ApiResponse.success(stone));
    }

    // 복수 조회 + 검색 + 페이징
    @GetMapping("/stones")
    public ResponseEntity<ApiResponse<CustomPage<StoneDto.PageDto>>> getStones(
            @RequestParam(name = "name", required = false) String stoneName,
            @PageableDefault(size = 20) Pageable pageable) {
        CustomPage<StoneDto.PageDto> result = stoneService.getStones(stoneName, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 수정
    @PatchMapping("/stones/{id}")
    public ResponseEntity<ApiResponse<String>> updateStone(
            @PathVariable(name = "id") Long stoneId,
            @Valid @RequestBody StoneDto stoneDto) {
        stoneService.updateStone(stoneId, stoneDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    // 삭제
    @DeleteMapping("/stones/{id}")
    public ResponseEntity<ApiResponse<String>> deleteStone(
            @AccessToken String accessToken,
            @PathVariable(name = "id") Long stoneId) {
        stoneService.deletedStone(accessToken, stoneId);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }
}
