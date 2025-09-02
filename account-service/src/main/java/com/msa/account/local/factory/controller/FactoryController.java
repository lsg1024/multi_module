package com.msa.account.local.factory.controller;

import com.msa.account.local.factory.dto.FactoryDto;
import com.msa.account.local.factory.service.FactoryService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class FactoryController {

    private final FactoryService factoryService;

    public FactoryController(FactoryService factoryService) {
        this.factoryService = factoryService;
    }

    //단일 조회
    @GetMapping("/factory/{id}")
    public ResponseEntity<ApiResponse<FactoryDto.FactorySingleResponse>> getFactoryInfo(
            @PathVariable("id") String factoryId) {

        FactoryDto.FactorySingleResponse factoryInfo = factoryService.getFactoryInfo(factoryId);

        return ResponseEntity.ok(ApiResponse.success(factoryInfo));
    }

    //목록 조회
    @GetMapping("/factories")
    public ResponseEntity<ApiResponse<CustomPage<FactoryDto.FactoryResponse>>> getFactoryList(
            @RequestParam(name = "search", required = false) String name,
            @PageableDefault(size = 12) Pageable pageable) {

        CustomPage<FactoryDto.FactoryResponse> factoryList = factoryService.getFactoryList(name, pageable);

        return ResponseEntity.ok(ApiResponse.success(factoryList));
    }

    //생성
    @PostMapping("/factory")
    public ResponseEntity<ApiResponse<String>> createFactory(
            @Valid @RequestBody FactoryDto.FactoryRequest factoryInfo) {

        factoryService.createFactory(factoryInfo);

        return ResponseEntity.ok(ApiResponse.success());
    }

    //수정
    @PatchMapping("/factory/{id}")
    public ResponseEntity<ApiResponse<String>> updateFactory(
            @AccessToken String accessToken,
            @PathVariable("id") String factoryId,
            @Valid @RequestBody FactoryDto.FactoryUpdate factoryInfo) {

        factoryService.updateFactory(accessToken, factoryId, factoryInfo);

        return ResponseEntity.ok(ApiResponse.success());
    }

    //삭제
    @DeleteMapping("/factory/{id}")
    public ResponseEntity<ApiResponse<String>> deleteFactory(
            @AccessToken String accessToken,
            @PathVariable("id") String factoryId) {

        factoryService.deleteFactory(accessToken, factoryId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    //공장 검증
    @GetMapping("/api/factory/{id}")
    public ResponseEntity<ApiResponse<FactoryDto.ApiFactoryInfo>> getFactoryInfo(@PathVariable Long id) {
        FactoryDto.ApiFactoryInfo factoryIdAndName = factoryService.getFactoryIdAndName(id);
        return ResponseEntity.ok(ApiResponse.success(factoryIdAndName));
    }
}
