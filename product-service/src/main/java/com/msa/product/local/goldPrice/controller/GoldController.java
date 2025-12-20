package com.msa.product.local.goldPrice.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.product.local.goldPrice.service.GoldService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GoldController {

    private final GoldService goldService;

    public GoldController(GoldService goldService) {
        this.goldService = goldService;
    }

    @GetMapping("/gold-price")
    public ResponseEntity<ApiResponse<Integer>> getGoldPrice() {
        Integer goldPrice = goldService.getGoldPrice();
        return ResponseEntity.ok(ApiResponse.success(goldPrice));
    }

    @PostMapping("/gold-price")
    public ResponseEntity<ApiResponse<String>> createGoldPrice(
            @RequestParam(name = "price") Integer price) {
        goldService.createGoldPrice(price);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

}
