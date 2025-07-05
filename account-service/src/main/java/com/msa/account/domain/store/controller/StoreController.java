package com.msa.account.domain.store.controller;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.domain.store.service.StoreService;
import com.msacommon.global.api.ApiResponse;
import com.msacommon.global.jwt.AccessToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    //상점 info
    @GetMapping("/store/{id}")
    public ResponseEntity<ApiResponse<AccountDto.accountInfo>> getStoreInfo(
            @AccessToken String accessToken,
            @PathVariable("id") String storeId) {

        AccountDto.accountInfo storeInfo = storeService.getStoreInfo(storeId);

        return ResponseEntity.ok(ApiResponse.success(storeInfo));
    }

    //상점 목록
//    @GetMapping("/store/list")
//    public ResponseEntity<ApiResponse<AccountDto.accountList>> getStoreList(
//            @AccessToken String accessToken) {
//
//        storeService.getStoreList(accessToken);
//    }

    //상점 생성

    //상점 수정

    //상점 삭제

}
