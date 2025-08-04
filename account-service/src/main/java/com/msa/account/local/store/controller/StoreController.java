package com.msa.account.local.store.controller;

import com.msa.account.local.store.dto.StoreDto;
import com.msa.account.local.store.service.StoreService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    //상점 단일 조회
    @GetMapping("/store/{id}")
    public ResponseEntity<ApiResponse<StoreDto.StoreSingleResponse>> getStoreInfo(
            @PathVariable("id") String storeId) {

        StoreDto.StoreSingleResponse storeInfo = storeService.getStoreInfo(storeId);

        return ResponseEntity.ok(ApiResponse.success(storeInfo));
    }

    //상점 목록 조회
    @GetMapping("/store/list")
    public ResponseEntity<ApiResponse<CustomPage<StoreDto.StoreResponse>>> getStoreList(
            @PageableDefault(size = 30) Pageable pageable) {

        CustomPage<StoreDto.StoreResponse> storeList = storeService.getStoreList(pageable);

        return ResponseEntity.ok(ApiResponse.success(storeList));
    }

    //상점 생성
    @PostMapping("/store")
    public ResponseEntity<ApiResponse<String>> createStore(
            @Valid @RequestBody StoreDto.StoreRequest storeInfo) {

        storeService.createStore(storeInfo);

        return ResponseEntity.ok(ApiResponse.success());
    }

    //상점 수정
    @PatchMapping("/store/{id}")
    public ResponseEntity<ApiResponse<String>> updateStore(
            @AccessToken String accessToken,
            @PathVariable("id") String storeId,
            @Valid @RequestBody StoreDto.StoreUpdate updateInfo) {

        storeService.updateStore(accessToken, storeId, updateInfo);

        return ResponseEntity.ok(ApiResponse.success());
    }

    //상점 삭제
    @DeleteMapping("/store/{id}")
    public ResponseEntity<ApiResponse<String>> deleteStore(
            @AccessToken String accessToken,
            @PathVariable("id") String storeId) {

        storeService.deleteStore(accessToken, storeId);

        return ResponseEntity.ok(ApiResponse.success());
    }

}
