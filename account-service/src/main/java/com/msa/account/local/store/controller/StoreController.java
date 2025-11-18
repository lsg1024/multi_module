package com.msa.account.local.store.controller;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.local.store.domain.dto.StoreDto;
import com.msa.account.local.store.service.StoreService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
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
    @GetMapping("/stores")
    public ResponseEntity<ApiResponse<CustomPage<StoreDto.StoreResponse>>> getStoreList(
            @RequestParam(name = "search", required = false) String name,
            @PageableDefault(size = 12) Pageable pageable) {

        CustomPage<StoreDto.StoreResponse> storeList = storeService.getStoreList(name, pageable);

        return ResponseEntity.ok(ApiResponse.success(storeList));
    }

    //상점 미수 금액 조회
    @GetMapping("/stores/attempt")
    public ResponseEntity<ApiResponse<CustomPage<AccountDto.accountResponse>>> getStoreAttempt(
            @RequestParam(name = "search", required = false) String name,
            @PageableDefault(size = 12) Pageable pageable) {
        CustomPage<AccountDto.accountResponse> storeAttemptList = storeService.getStoreAttempt(name, pageable);
        return ResponseEntity.ok(ApiResponse.success(storeAttemptList));
    }

    //상점 미수 금액 상세조회
    @GetMapping("/stores/attempt/{id}")
    public ResponseEntity<ApiResponse<AccountDto.accountResponse>> getStoreAttemptDetail(
            @PathVariable(name = "id") String storeId) {

        AccountDto.accountResponse storeAttemptDetail = storeService.getStoreAttemptDetail(storeId);
        return ResponseEntity.ok(ApiResponse.success(storeAttemptDetail));
    }

    //상점 생성
    @PostMapping("/store")

    public ResponseEntity<ApiResponse<String>> createStore(
            @Valid @RequestBody StoreDto.StoreRequest storeInfo) {

        storeService.createStore(storeInfo);

        return ResponseEntity.ok(ApiResponse.success());
    }

    //상점 수정
    @PatchMapping("/stores/{id}")
    public ResponseEntity<ApiResponse<String>> updateStore(
            @AccessToken String accessToken,
            @PathVariable("id") String storeId,
            @Valid @RequestBody StoreDto.StoreUpdate updateInfo) {

        storeService.updateStore(accessToken, storeId, updateInfo);

        return ResponseEntity.ok(ApiResponse.success());
    }

    //상점 삭제
    @DeleteMapping("/stores/{id}")
    public ResponseEntity<ApiResponse<String>> deleteStore(
            @AccessToken String accessToken,
            @PathVariable("id") String storeId) {

        storeService.deleteStore(accessToken, storeId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    //상점 등급 조회
    @GetMapping("/stores/grade")
    public ResponseEntity<ApiResponse<String>> getStoreGrade(
            @RequestParam(name = "id") String storeId) {
        String grade = storeService.getStoreGrade(storeId);
        return ResponseEntity.ok(ApiResponse.success(grade));
    }

    @GetMapping("/api/store/{id}")
    public ResponseEntity<ApiResponse<StoreDto.ApiStoreInfo>> getStoreInfo(@PathVariable Long id) {
        StoreDto.ApiStoreInfo storeInfo = storeService.getStoreInfo(id);
        return ResponseEntity.ok(ApiResponse.success(storeInfo));
    }

}
