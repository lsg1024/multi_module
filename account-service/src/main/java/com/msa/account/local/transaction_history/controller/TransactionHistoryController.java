package com.msa.account.local.transaction_history.controller;

import com.msa.account.local.transaction_history.domain.dto.TransactionDto;
import com.msa.account.local.transaction_history.domain.dto.TransactionPage;
import com.msa.account.local.transaction_history.dto.PurchaseDto;
import com.msa.account.local.transaction_history.service.TransactionHistoryService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    //매입 목록 호출 기능 -> 오늘 판매된 상품에 대한 공장 미수 데이터를 말함
    @GetMapping("/purchase")
    public ResponseEntity<ApiResponse<CustomPage<TransactionPage>>> getAccountPurchases(
            @RequestParam("start") String start,
            @RequestParam("end") String end,
            @RequestParam(value = "accountType", required = false) String accountType,
            @RequestParam(value = "accountName", required = false) String accountName,
            @PageableDefault(size = 20) Pageable pageable) {

        CustomPage<TransactionPage> accountPurchasePage = transactionHistoryService.findAccountPurchase(start, end, accountType, accountName, pageable);

        return ResponseEntity.ok(ApiResponse.success(accountPurchasePage));
    }

    @GetMapping("/purchase/factory")
    public ResponseEntity<ApiResponse<CustomPage<TransactionPage>>> getFactoryPurchase(
            @RequestParam("start") String start,
            @RequestParam("end") String end,
            @RequestParam(value = "accountType", required = false) String accountType,
            @RequestParam(value = "accountName", required = false) String accountName,
            @PageableDefault(size = 20) Pageable pageable) {
        CustomPage<TransactionPage> accountPurchasePage = transactionHistoryService.findFactoryPurchase(start, end, accountType, accountName, pageable);

        return ResponseEntity.ok(ApiResponse.success(accountPurchasePage));
    }

    @PostMapping("/purchase/factory")
    public ResponseEntity<ApiResponse<String>> createFactoryPurchase(
            @RequestBody PurchaseDto purchaseDto) {
        transactionHistoryService.savePurchase(purchaseDto);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }


}
