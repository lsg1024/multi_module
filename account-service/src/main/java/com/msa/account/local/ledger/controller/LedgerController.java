package com.msa.account.local.ledger.controller;

import com.msa.account.local.ledger.domain.dto.LedgerDto;
import com.msa.account.local.ledger.domain.entity.AssetType;
import com.msa.account.local.ledger.service.LedgerService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.util.CustomPage;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ledger")
public class LedgerController {

    private final LedgerService ledgerService;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> createLedger(
            @RequestHeader(value = "X-User-ID", required = false) String userId,
            @Valid @RequestBody LedgerDto.CreateRequest request) {

        ledgerService.createLedger(request, userId);
        return ResponseEntity.ok(ApiResponse.success("등록 완료"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> updateLedger(
            @PathVariable("id") Long ledgerId,
            @Valid @RequestBody LedgerDto.UpdateRequest request) {

        ledgerService.updateLedger(ledgerId, request);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteLedger(
            @PathVariable("id") Long ledgerId) {

        ledgerService.deleteLedger(ledgerId);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LedgerDto.LedgerResponse>> getLedger(
            @PathVariable("id") Long ledgerId) {

        LedgerDto.LedgerResponse response = ledgerService.getLedger(ledgerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<CustomPage<LedgerDto.LedgerResponse>>> getLedgerList(
            @RequestParam(name = "assetType", required = false) AssetType assetType,
            @RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {

        CustomPage<LedgerDto.LedgerResponse> list = ledgerService.getLedgerList(assetType, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<LedgerDto.BalanceResponse>> getBalance() {

        LedgerDto.BalanceResponse balance = ledgerService.getBalance();
        return ResponseEntity.ok(ApiResponse.success(balance));
    }
}
