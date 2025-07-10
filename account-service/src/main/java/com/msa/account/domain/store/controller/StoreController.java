package com.msa.account.domain.store.controller;

import com.msa.account.domain.store.dto.StoreDto;
import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.domain.store.service.StoreService;
import com.msacommon.global.api.ApiResponse;
import com.msacommon.global.util.CustomPage;
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

    //상점 info
    @GetMapping("/store/{id}")
    public ResponseEntity<ApiResponse<AccountDto.accountInfo>> getStoreInfo(
            @PathVariable("id") String storeId) {

        AccountDto.accountInfo storeInfo = storeService.getStoreInfo(storeId);

        return ResponseEntity.ok(ApiResponse.success(storeInfo));
    }

    //상점 목록
    @GetMapping("/store/list")
    public ResponseEntity<ApiResponse<CustomPage<AccountDto.accountInfo>>> getStoreList(
            @PageableDefault(size = 30) Pageable pageable) {

        CustomPage<AccountDto.accountInfo> storeList = storeService.getStoreList(pageable);

        return ResponseEntity.ok(ApiResponse.success(storeList));
    }

    //상점 생성
    @PostMapping("/store")
    public ResponseEntity<ApiResponse<String>> addStore(
            @Valid @RequestBody StoreDto.StoreRequest storeInfo) {

        storeService.addStore(storeInfo);

        return ResponseEntity.ok(ApiResponse.success());
    }

    //상점 수정
    @PatchMapping("/store/{id}")
    public ResponseEntity<ApiResponse<String>> updateStore(
            @PathVariable("id") String storeId,
            @Valid @RequestBody StoreDto.StoreUpdate updateInfo) {

        storeService.updateStore(storeId, updateInfo);

        return ResponseEntity.ok(ApiResponse.success());
    }

    //상점 삭제
    @DeleteMapping("/store/{id}")
    public ResponseEntity<ApiResponse<String>> deleteStore(
            @PathVariable("id") String storeId) {

        storeService.deleteStore(storeId);

        return ResponseEntity.ok(ApiResponse.success());
    }

}
