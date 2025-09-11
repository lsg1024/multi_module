package com.msa.product.local.set.controller;

import com.msa.product.local.set.dto.SetTypeDto;
import com.msa.product.local.set.service.SetTypeService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SetTypeController {
    private final SetTypeService setTypeService;

    public SetTypeController(SetTypeService setTypeService) {
        this.setTypeService = setTypeService;
    }

    @PostMapping("/set-types")
    public ResponseEntity<ApiResponse<String>> createSetType(
            @Valid @RequestBody SetTypeDto setTypeDto) {

        setTypeService.saveSetType(setTypeDto);

        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @PostMapping("/set-typess")
    public ResponseEntity<ApiResponse<String>> createSetTypes(
            @Valid @RequestBody List<SetTypeDto> setTypeDto) {

        setTypeDto.forEach(setTypeService::saveSetType);

        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @GetMapping("/set-types/{id}")
    public ResponseEntity<ApiResponse<SetTypeDto.ResponseSingle>> getSetType(
            @PathVariable(name = "id") Long setTypeId) {
        SetTypeDto.ResponseSingle setType = setTypeService.getSetType(setTypeId);

        return ResponseEntity.ok(ApiResponse.success(setType));
    }

    @GetMapping("/set-types")
    public ResponseEntity<ApiResponse<List<SetTypeDto.ResponseSingle>>> getSetTypes() {
        return ResponseEntity.ok(ApiResponse.success(setTypeService.getSetTypes()));
    }

    @PatchMapping("/set-types")
    public ResponseEntity<ApiResponse<String>> updateSetType(
            @RequestParam(name = "id") Long setTypeId,
            @Valid @RequestBody SetTypeDto setTypeDto) {
        setTypeService.updateSetType(setTypeId, setTypeDto);

        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @DeleteMapping("/set-types/{id}")
    public ResponseEntity<ApiResponse<String>> deletedSetType(
            @AccessToken String accessToken,
            @PathVariable(name = "id") Long setTypeId) {
        setTypeService.deletedSetType(accessToken, setTypeId);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }

    @GetMapping("/api/set-type/{id}")
    public ResponseEntity<ApiResponse<String>> getMaterialInfo(@PathVariable Long id) {
        String setTypeName = setTypeService.getSetTypeName(id);
        return ResponseEntity.ok(ApiResponse.success(setTypeName));
    }

}
