package com.account.domain.store.controller;

import com.account.domain.store.dto.StoreDto;
import com.account.domain.store.service.StoreService;
import jakarta.ws.rs.QueryParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    //상점 info
//    @GetMapping("/store/{id}")
//    public StoreDto.storeInfo getStoreInfo(@QueryParam("id") String storeId) {
//
//    }

    //상점 목록

    //상점 생성

    //상점 수정

    //상점 삭제

}
