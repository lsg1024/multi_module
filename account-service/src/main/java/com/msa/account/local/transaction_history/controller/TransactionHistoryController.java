package com.msa.account.local.transaction_history.controller;

import com.msa.account.local.transaction_history.domain.dto.TransactionDto;
import com.msa.account.local.transaction_history.domain.dto.TransactionPage;
import com.msa.account.local.transaction_history.service.TransactionHistoryService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    //매입 목록 호출 기능 -> 오늘 판매된 상품에 대한 공장 미수 데이터를 말함
    @GetMapping("/account/purchase")
    public ResponseEntity<ApiResponse<CustomPage<TransactionPage>>> getAccountPurchases(
            @RequestParam("start") String start,
            @RequestParam("end") String end,
            @RequestParam(value = "accountType", required = false) String accountType,
            @RequestParam(value = "accountName", required = false) String accountName,
            @PageableDefault(size = 20) Pageable pageable) {

        CustomPage<TransactionPage> accountPurchasePage = transactionHistoryService.findAccountPurchase(start, end, accountType, accountName, pageable);

        return ResponseEntity.ok(ApiResponse.success(accountPurchasePage));
    }

    //매입 추가 -> 결제, 반품, 판매에 대한 order 서버와 별도로 구현된 독립 구간 - order와 묶여 있으면 거기서 취소에 관한 로직과 여기서 추가외 관한 로직이 얽힐 수 있음


}
