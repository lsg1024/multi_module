package com.msa.account.global.domain.controller;

import com.msa.account.global.domain.dto.GoldHarryDto;
import com.msa.account.global.domain.service.GoldHarryService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GoldHarryController {

    private final GoldHarryService goldHarryService;

    public GoldHarryController(GoldHarryService goldHarryService) {
        this.goldHarryService = goldHarryService;
    }

    @GetMapping("/gold-harries")
    public ResponseEntity<ApiResponse<List<GoldHarryDto.Response>>> getHarries() {
        List<GoldHarryDto.Response> goldHarries = goldHarryService.getGoldHarries();
        return ResponseEntity.ok(ApiResponse.success(goldHarries));
    }

    @PostMapping("/gold-harry")
    public ResponseEntity<ApiResponse<String>> createHarry(
            @AccessToken String accessToken,
            @RequestBody GoldHarryDto.Request request) {
        goldHarryService.createHarry(accessToken, request);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @PatchMapping("/gold-harry/{id}")
    public ResponseEntity<ApiResponse<String>> updateLoss(
            @AccessToken String accessToken,
            @PathVariable("id") Long id,
            @RequestBody GoldHarryDto.Update request) {
        goldHarryService.updateLoss(accessToken, id, request);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @DeleteMapping("/gold-harry/{id}")
    public ResponseEntity<ApiResponse<String>> deleteGoldHarry(
            @AccessToken String accessToken,
            @PathVariable("id") String id) {

        goldHarryService.delete(accessToken, id);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }
}