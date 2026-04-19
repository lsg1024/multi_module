package com.msa.account.local.expense.controller;

import com.msa.account.local.expense.domain.dto.ExpenseBankTypeDto;
import com.msa.account.local.expense.domain.dto.ExpenseExpenseAccountDto;
import com.msa.account.local.expense.domain.dto.ExpenseIncomeAccountDto;
import com.msa.account.local.expense.service.ExpenseMasterService;
import com.msa.common.global.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/expense/master")
public class ExpenseMasterController {

    private final ExpenseMasterService masterService;

    public ExpenseMasterController(ExpenseMasterService masterService) {
        this.masterService = masterService;
    }

    // Bank Type Endpoints
    @GetMapping("/bank-types")
    public ResponseEntity<ApiResponse<List<ExpenseBankTypeDto.Response>>> getAllBankTypes() {
        List<ExpenseBankTypeDto.Response> bankTypes = masterService.getAllBankTypes();
        return ResponseEntity.ok(ApiResponse.success(bankTypes));
    }

    @GetMapping("/bank-types/{id}")
    public ResponseEntity<ApiResponse<ExpenseBankTypeDto.Response>> getBankTypeById(
            @PathVariable Long id) {
        ExpenseBankTypeDto.Response bankType = masterService.getBankTypeById(id);
        return ResponseEntity.ok(ApiResponse.success(bankType));
    }

    @PostMapping("/bank-types")
    public ResponseEntity<ApiResponse<ExpenseBankTypeDto.Response>> createBankType(
            @RequestBody ExpenseBankTypeDto.Request request) {
        ExpenseBankTypeDto.Response response = masterService.createBankType(request);
        return ResponseEntity.ok(ApiResponse.success("Bank type created successfully", response));
    }

    @PatchMapping("/bank-types/{id}")
    public ResponseEntity<ApiResponse<ExpenseBankTypeDto.Response>> updateBankType(
            @PathVariable Long id,
            @RequestBody ExpenseBankTypeDto.Request request) {
        ExpenseBankTypeDto.Response response = masterService.updateBankType(id, request);
        return ResponseEntity.ok(ApiResponse.success("Bank type updated successfully", response));
    }

    @DeleteMapping("/bank-types/{id}")
    public ResponseEntity<ApiResponse<String>> deleteBankType(
            @PathVariable Long id) {
        masterService.deleteBankType(id);
        return ResponseEntity.ok(ApiResponse.success("Bank type deleted successfully"));
    }

    // Income Account Endpoints
    @GetMapping("/income-accounts")
    public ResponseEntity<ApiResponse<List<ExpenseIncomeAccountDto.Response>>> getAllIncomeAccounts() {
        List<ExpenseIncomeAccountDto.Response> accounts = masterService.getAllIncomeAccounts();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/income-accounts/{id}")
    public ResponseEntity<ApiResponse<ExpenseIncomeAccountDto.Response>> getIncomeAccountById(
            @PathVariable Long id) {
        ExpenseIncomeAccountDto.Response account = masterService.getIncomeAccountById(id);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping("/income-accounts")
    public ResponseEntity<ApiResponse<ExpenseIncomeAccountDto.Response>> createIncomeAccount(
            @RequestBody ExpenseIncomeAccountDto.Request request) {
        ExpenseIncomeAccountDto.Response response = masterService.createIncomeAccount(request);
        return ResponseEntity.ok(ApiResponse.success("Income account created successfully", response));
    }

    @PatchMapping("/income-accounts/{id}")
    public ResponseEntity<ApiResponse<ExpenseIncomeAccountDto.Response>> updateIncomeAccount(
            @PathVariable Long id,
            @RequestBody ExpenseIncomeAccountDto.Request request) {
        ExpenseIncomeAccountDto.Response response = masterService.updateIncomeAccount(id, request);
        return ResponseEntity.ok(ApiResponse.success("Income account updated successfully", response));
    }

    @DeleteMapping("/income-accounts/{id}")
    public ResponseEntity<ApiResponse<String>> deleteIncomeAccount(
            @PathVariable Long id) {
        masterService.deleteIncomeAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Income account deleted successfully"));
    }

    // Expense Account Endpoints
    @GetMapping("/expense-accounts")
    public ResponseEntity<ApiResponse<List<ExpenseExpenseAccountDto.Response>>> getAllExpenseAccounts() {
        List<ExpenseExpenseAccountDto.Response> accounts = masterService.getAllExpenseAccounts();
        return ResponseEntity.ok(ApiResponse.success(accounts));
    }

    @GetMapping("/expense-accounts/{id}")
    public ResponseEntity<ApiResponse<ExpenseExpenseAccountDto.Response>> getExpenseAccountById(
            @PathVariable Long id) {
        ExpenseExpenseAccountDto.Response account = masterService.getExpenseAccountById(id);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping("/expense-accounts")
    public ResponseEntity<ApiResponse<ExpenseExpenseAccountDto.Response>> createExpenseAccount(
            @RequestBody ExpenseExpenseAccountDto.Request request) {
        ExpenseExpenseAccountDto.Response response = masterService.createExpenseAccount(request);
        return ResponseEntity.ok(ApiResponse.success("Expense account created successfully", response));
    }

    @PatchMapping("/expense-accounts/{id}")
    public ResponseEntity<ApiResponse<ExpenseExpenseAccountDto.Response>> updateExpenseAccount(
            @PathVariable Long id,
            @RequestBody ExpenseExpenseAccountDto.Request request) {
        ExpenseExpenseAccountDto.Response response = masterService.updateExpenseAccount(id, request);
        return ResponseEntity.ok(ApiResponse.success("Expense account updated successfully", response));
    }

    @DeleteMapping("/expense-accounts/{id}")
    public ResponseEntity<ApiResponse<String>> deleteExpenseAccount(
            @PathVariable Long id) {
        masterService.deleteExpenseAccount(id);
        return ResponseEntity.ok(ApiResponse.success("Expense account deleted successfully"));
    }
}
