package com.msa.product.local.material.controller;


import com.msa.product.local.material.dto.MaterialDto;
import com.msa.product.local.material.service.MaterialService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class MaterialController {

    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping("/materials")
    public ResponseEntity<ApiResponse<String>> createMaterial(
            @Valid @RequestBody MaterialDto materialDto) {

        materialService.saveMaterial(materialDto);

        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @PostMapping("/materialss")
    public ResponseEntity<ApiResponse<String>> createMaterials(
            @Valid @RequestBody List<MaterialDto> materialDto) {

        materialDto.forEach(materialService::saveMaterial);

        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @GetMapping("/materials/{id}")
    public ResponseEntity<ApiResponse<MaterialDto.ResponseSingle>> getMaterial(
            @PathVariable(name = "id") Long materialId) {

        MaterialDto.ResponseSingle material = materialService.getMaterial(materialId);

        return ResponseEntity.ok(ApiResponse.success(material));
    }

    @GetMapping("/materials")
    public ResponseEntity<ApiResponse<List<MaterialDto.ResponseSingle>>> getMaterials() {
        return ResponseEntity.ok(ApiResponse.success(materialService.getMaterials()));
    }

    @PatchMapping("/materials/{id}")
    public ResponseEntity<ApiResponse<String>> updateMaterial(
            @PathVariable(name = "id") Long materialId,
            @Valid @RequestBody MaterialDto materialDto) {

        materialService.updateMaterial(materialId, materialDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @DeleteMapping("/materials/{id}")
    public ResponseEntity<ApiResponse<String>> deletedMaterial(
            @AccessToken String accessToken,
            @PathVariable(name = "id") Long materialId) {

        materialService.deleteMaterial(accessToken, materialId);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }
}