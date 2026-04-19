package com.msa.account.local.expense.controller;

import com.msa.account.local.expense.domain.dto.ExpenseRecordDto;
import com.msa.account.local.expense.service.ExpenseRecordService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.util.CustomPage;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/expense/records")
public class ExpenseRecordController {

    private final ExpenseRecordService recordService;

    public ExpenseRecordController(ExpenseRecordService recordService) {
        this.recordService = recordService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CustomPage<ExpenseRecordDto.ListResponse>>> getExpenseRecords(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) String expenseType,
            @RequestParam(required = false) Long bankTypeId,
            @RequestParam(required = false) String counterparty,
            @PageableDefault(size = 20) Pageable pageable) {

        CustomPage<ExpenseRecordDto.ListResponse> records;

        if (expenseType != null && bankTypeId != null) {
            records = recordService.getExpenseRecordsByDateRangeAndFilters(
                    startDate, endDate, expenseType, bankTypeId, pageable);
        } else if (expenseType != null) {
            records = recordService.getExpenseRecordsByDateRangeAndType(
                    startDate, endDate, expenseType, pageable);
        } else if (bankTypeId != null) {
            records = recordService.getExpenseRecordsByDateRangeAndBankType(
                    startDate, endDate, bankTypeId, pageable);
        } else if (counterparty != null) {
            records = recordService.getExpenseRecordsByDateRangeAndCounterparty(
                    startDate, endDate, counterparty, pageable);
        } else {
            records = recordService.getExpenseRecordsByDateRange(
                    startDate, endDate, pageable);
        }

        return ResponseEntity.ok(ApiResponse.success(records));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseRecordDto.DetailResponse>> getExpenseRecordById(
            @PathVariable Long id) {
        ExpenseRecordDto.DetailResponse record = recordService.getExpenseRecordById(id);
        return ResponseEntity.ok(ApiResponse.success(record));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseRecordDto.DetailResponse>> createExpenseRecord(
            @RequestBody ExpenseRecordDto.CreateRequest request) {
        ExpenseRecordDto.DetailResponse response = recordService.createExpenseRecord(request);
        return ResponseEntity.ok(ApiResponse.success("Expense record created successfully", response));
    }

    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<String>> createRecordsBatch(
            @Valid @RequestBody List<ExpenseRecordDto.CreateRequest> requests) {
        recordService.createRecordsBatch(requests);
        return ResponseEntity.ok(ApiResponse.success("일괄 등록 완료 (" + requests.size() + "건)"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseRecordDto.DetailResponse>> updateExpenseRecord(
            @PathVariable Long id,
            @RequestBody ExpenseRecordDto.UpdateRequest request) {
        ExpenseRecordDto.DetailResponse response = recordService.updateExpenseRecord(id, request);
        return ResponseEntity.ok(ApiResponse.success("Expense record updated successfully", response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteExpenseRecord(
            @PathVariable Long id) {
        recordService.deleteExpenseRecord(id);
        return ResponseEntity.ok(ApiResponse.success("Expense record deleted successfully"));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ExpenseRecordDto.SummaryResponse>> getSummary(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        ExpenseRecordDto.SummaryResponse summary = recordService.getSummary(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}
