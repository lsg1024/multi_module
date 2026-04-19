package com.msa.account.local.expense.service;

import com.msa.account.local.expense.domain.dto.ExpenseBankTypeDto;
import com.msa.account.local.expense.domain.dto.ExpenseExpenseAccountDto;
import com.msa.account.local.expense.domain.dto.ExpenseIncomeAccountDto;
import com.msa.account.local.expense.domain.entity.ExpenseBankType;
import com.msa.account.local.expense.domain.entity.ExpenseExpenseAccount;
import com.msa.account.local.expense.domain.entity.ExpenseIncomeAccount;
import com.msa.account.local.expense.repository.ExpenseBankTypeRepository;
import com.msa.account.local.expense.repository.ExpenseExpenseAccountRepository;
import com.msa.account.local.expense.repository.ExpenseIncomeAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ExpenseMasterService {

    private final ExpenseBankTypeRepository bankTypeRepository;
    private final ExpenseIncomeAccountRepository incomeAccountRepository;
    private final ExpenseExpenseAccountRepository expenseAccountRepository;

    public ExpenseMasterService(ExpenseBankTypeRepository bankTypeRepository,
                                ExpenseIncomeAccountRepository incomeAccountRepository,
                                ExpenseExpenseAccountRepository expenseAccountRepository) {
        this.bankTypeRepository = bankTypeRepository;
        this.incomeAccountRepository = incomeAccountRepository;
        this.expenseAccountRepository = expenseAccountRepository;
    }

    // Bank Type CRUD
    @Transactional(readOnly = true)
    public List<ExpenseBankTypeDto.Response> getAllBankTypes() {
        return bankTypeRepository.findAllByDeletedFalse().stream()
                .map(this::convertBankTypeToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseBankTypeDto.Response getBankTypeById(Long id) {
        ExpenseBankType bankType = bankTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bank type not found with id: " + id));
        return convertBankTypeToResponse(bankType);
    }

    public ExpenseBankTypeDto.Response createBankType(ExpenseBankTypeDto.Request request) {
        ExpenseBankType bankType = ExpenseBankType.builder()
                .bankTypeName(request.getName())
                .bankTypeNote(request.getNote())
                .build();
        ExpenseBankType saved = bankTypeRepository.save(bankType);
        return convertBankTypeToResponse(saved);
    }

    public ExpenseBankTypeDto.Response updateBankType(Long id, ExpenseBankTypeDto.Request request) {
        ExpenseBankType bankType = bankTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bank type not found with id: " + id));
        bankType.update(request.getName(), request.getNote());
        ExpenseBankType updated = bankTypeRepository.save(bankType);
        return convertBankTypeToResponse(updated);
    }

    public void deleteBankType(Long id) {
        ExpenseBankType bankType = bankTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bank type not found with id: " + id));
        bankType.softDelete();
        bankTypeRepository.save(bankType);
    }

    // Income Account CRUD
    @Transactional(readOnly = true)
    public List<ExpenseIncomeAccountDto.Response> getAllIncomeAccounts() {
        return incomeAccountRepository.findAllByDeletedFalse().stream()
                .map(this::convertIncomeAccountToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseIncomeAccountDto.Response getIncomeAccountById(Long id) {
        ExpenseIncomeAccount account = incomeAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Income account not found with id: " + id));
        return convertIncomeAccountToResponse(account);
    }

    public ExpenseIncomeAccountDto.Response createIncomeAccount(ExpenseIncomeAccountDto.Request request) {
        ExpenseIncomeAccount account = ExpenseIncomeAccount.builder()
                .incomeAccountName(request.getName())
                .incomeAccountNote(request.getNote())
                .build();
        ExpenseIncomeAccount saved = incomeAccountRepository.save(account);
        return convertIncomeAccountToResponse(saved);
    }

    public ExpenseIncomeAccountDto.Response updateIncomeAccount(Long id, ExpenseIncomeAccountDto.Request request) {
        ExpenseIncomeAccount account = incomeAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Income account not found with id: " + id));
        account.update(request.getName(), request.getNote());
        ExpenseIncomeAccount updated = incomeAccountRepository.save(account);
        return convertIncomeAccountToResponse(updated);
    }

    public void deleteIncomeAccount(Long id) {
        ExpenseIncomeAccount account = incomeAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Income account not found with id: " + id));
        account.softDelete();
        incomeAccountRepository.save(account);
    }

    // Expense Account CRUD
    @Transactional(readOnly = true)
    public List<ExpenseExpenseAccountDto.Response> getAllExpenseAccounts() {
        return expenseAccountRepository.findAllByDeletedFalse().stream()
                .map(this::convertExpenseAccountToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExpenseExpenseAccountDto.Response getExpenseAccountById(Long id) {
        ExpenseExpenseAccount account = expenseAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense account not found with id: " + id));
        return convertExpenseAccountToResponse(account);
    }

    public ExpenseExpenseAccountDto.Response createExpenseAccount(ExpenseExpenseAccountDto.Request request) {
        ExpenseExpenseAccount account = ExpenseExpenseAccount.builder()
                .expenseAccountName(request.getName())
                .expenseAccountNote(request.getNote())
                .build();
        ExpenseExpenseAccount saved = expenseAccountRepository.save(account);
        return convertExpenseAccountToResponse(saved);
    }

    public ExpenseExpenseAccountDto.Response updateExpenseAccount(Long id, ExpenseExpenseAccountDto.Request request) {
        ExpenseExpenseAccount account = expenseAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense account not found with id: " + id));
        account.update(request.getName(), request.getNote());
        ExpenseExpenseAccount updated = expenseAccountRepository.save(account);
        return convertExpenseAccountToResponse(updated);
    }

    public void deleteExpenseAccount(Long id) {
        ExpenseExpenseAccount account = expenseAccountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense account not found with id: " + id));
        account.softDelete();
        expenseAccountRepository.save(account);
    }

    // Helper methods
    private ExpenseBankTypeDto.Response convertBankTypeToResponse(ExpenseBankType bankType) {
        return ExpenseBankTypeDto.Response.builder()
                .id(bankType.getExpenseBankTypeId())
                .name(bankType.getBankTypeName())
                .note(bankType.getBankTypeNote())
                .build();
    }

    private ExpenseIncomeAccountDto.Response convertIncomeAccountToResponse(ExpenseIncomeAccount account) {
        return ExpenseIncomeAccountDto.Response.builder()
                .id(account.getExpenseIncomeAccountId())
                .name(account.getIncomeAccountName())
                .note(account.getIncomeAccountNote())
                .build();
    }

    private ExpenseExpenseAccountDto.Response convertExpenseAccountToResponse(ExpenseExpenseAccount account) {
        return ExpenseExpenseAccountDto.Response.builder()
                .id(account.getExpenseExpenseAccountId())
                .name(account.getExpenseAccountName())
                .note(account.getExpenseAccountNote())
                .build();
    }
}
