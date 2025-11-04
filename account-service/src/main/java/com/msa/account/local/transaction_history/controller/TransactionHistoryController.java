package com.msa.account.local.transaction_history.controller;

import com.msa.account.local.transaction_history.domain.dto.TransactionDto;
import com.msa.account.local.transaction_history.service.TransactionHistoryService;
import com.msa.common.global.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransactionHistoryController {

    private final TransactionHistoryService transactionHistoryService;

    public TransactionHistoryController(TransactionHistoryService transactionHistoryService) {
        this.transactionHistoryService = transactionHistoryService;
    }

    // 상점 or 공장 잔액 조회
    @GetMapping("/current/balance")
    public ResponseEntity<ApiResponse<TransactionDto>> getCurrentBalance(
            @RequestParam("type") String type,
            @RequestParam("id") String id,
            @RequestParam("name") String name) {
        TransactionDto currentBalance = transactionHistoryService.getCurrentBalance(type, id, name);
        return ResponseEntity.ok(ApiResponse.success(currentBalance));
    }


    // 상점 or 공장 잔액 업데이트 (판매) -> 카프카에서 처리



}
