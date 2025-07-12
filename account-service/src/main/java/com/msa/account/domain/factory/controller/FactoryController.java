package com.msa.account.domain.factory.controller;

import com.msa.account.domain.factory.dto.FactoryDto;
import com.msa.account.domain.factory.service.FactoryService;
import com.msacommon.global.api.ApiResponse;
import com.msacommon.global.jwt.AccessToken;
import com.msacommon.global.util.CustomPage;
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
    @GetMapping("/factory/list")
    public ResponseEntity<ApiResponse<CustomPage<FactoryDto.FactoryResponse>>> getFactoryList(
            @PageableDefault(size = 30) Pageable pageable) {

        CustomPage<FactoryDto.FactoryResponse> factoryList = factoryService.getFactoryList(pageable);

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
}
