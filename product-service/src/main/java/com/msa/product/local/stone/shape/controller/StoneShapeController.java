package com.msa.product.local.stone.shape.controller;

import com.msa.product.local.stone.shape.dto.StoneShapeDto;
import com.msa.product.local.stone.shape.service.StoneShapeService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StoneShapeController {

    private final StoneShapeService stoneShapeService;

    public StoneShapeController(StoneShapeService stoneShapeService) {
        this.stoneShapeService = stoneShapeService;
    }

    @PostMapping("/stoneshapes")
    public ResponseEntity<ApiResponse<String>> createStoneShape(
            @Valid @RequestBody StoneShapeDto stoneShapeDto) {

        stoneShapeService.saveStoneShape(stoneShapeDto);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @PostMapping("/stoneshapess")
    public ResponseEntity<ApiResponse<String>> createStoneShapes(
            @Valid @RequestBody List<StoneShapeDto> stoneShapeDto) {

        stoneShapeDto.forEach(stoneShapeService::saveStoneShape);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @GetMapping("/stoneshapes/{id}")
    public ResponseEntity<ApiResponse<StoneShapeDto.ResponseSingle>> getStoneShape(
            @PathVariable(name = "id") Long stoneShapeId) {

        StoneShapeDto.ResponseSingle response = stoneShapeService.getStoneShape(stoneShapeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stoneshapes")
    public ResponseEntity<ApiResponse<List<StoneShapeDto.ResponseSingle>>> getStoneShapes(
            @RequestParam(name = "name", required = false) String stoneShapeName) {

        List<StoneShapeDto.ResponseSingle> stoneShapes = stoneShapeService.getStoneShapes(stoneShapeName);

        return ResponseEntity.ok(ApiResponse.success(stoneShapes));
    }

    @PatchMapping("/stoneshapes/{id}")
    public ResponseEntity<ApiResponse<String>> updateStoneShape(
            @PathVariable(name = "id") Long stoneShapeId,
            @Valid @RequestBody StoneShapeDto stoneShapeDto) {

        stoneShapeService.updateStoneShape(stoneShapeId, stoneShapeDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @DeleteMapping("/stoneshapes/{id}")
    public ResponseEntity<ApiResponse<String>> deleteStoneShape(
            @AccessToken String accessToken,
            @PathVariable(name = "id") Long stoneShapeId) {

        stoneShapeService.deleteStoneShape(accessToken, stoneShapeId);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }
}

