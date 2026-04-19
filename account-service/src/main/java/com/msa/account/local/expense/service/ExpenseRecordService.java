package com.msa.account.local.expense.service;

import com.msa.account.local.expense.domain.dto.ExpenseRecordDto;
import com.msa.account.local.expense.domain.entity.ExpenseBankType;
import com.msa.account.local.expense.domain.entity.ExpenseExpenseAccount;
import com.msa.account.local.expense.domain.entity.ExpenseIncomeAccount;
import com.msa.account.local.expense.domain.entity.ExpenseRecord;
import com.msa.account.local.expense.repository.ExpenseBankTypeRepository;
import com.msa.account.local.expense.repository.ExpenseExpenseAccountRepository;
import com.msa.account.local.expense.repository.ExpenseIncomeAccountRepository;
import com.msa.account.local.expense.repository.ExpenseRecordRepository;
import com.msa.common.global.common_enum.expense_enum.ExpenseType;
import com.msa.common.global.util.CustomPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class ExpenseRecordService {

    private final ExpenseRecordRepository recordRepository;
    private final ExpenseBankTypeRepository bankTypeRepository;
    private final ExpenseIncomeAccountRepository incomeAccountRepository;
    private final ExpenseExpenseAccountRepository expenseAccountRepository;

    public ExpenseRecordService(ExpenseRecordRepository recordRepository,
                                ExpenseBankTypeRepository bankTypeRepository,
                                ExpenseIncomeAccountRepository incomeAccountRepository,
                                ExpenseExpenseAccountRepository expenseAccountRepository) {
        this.recordRepository = recordRepository;
        this.bankTypeRepository = bankTypeRepository;
        this.incomeAccountRepository = incomeAccountRepository;
        this.expenseAccountRepository = expenseAccountRepository;
    }

    @Transactional(readOnly = true)
    public CustomPage<ExpenseRecordDto.ListResponse> getExpenseRecordsByDateRange(
            String startDate, String endDate, Pageable pageable) {
        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);

        Page<ExpenseRecord> records = recordRepository.findByDateRange(start, end, pageable);
        return convertToCustomPage(records);
    }

    @Transactional(readOnly = true)
    public CustomPage<ExpenseRecordDto.ListResponse> getExpenseRecordsByDateRangeAndType(
            String startDate, String endDate, String expenseType, Pageable pageable) {
        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);
        ExpenseType type = ExpenseType.valueOf(expenseType.toUpperCase());

        Page<ExpenseRecord> records = recordRepository.findByDateRangeAndExpenseType(start, end, type, pageable);
        return convertToCustomPage(records);
    }

    @Transactional(readOnly = true)
    public CustomPage<ExpenseRecordDto.ListResponse> getExpenseRecordsByDateRangeAndBankType(
            String startDate, String endDate, Long bankTypeId, Pageable pageable) {
        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);

        Page<ExpenseRecord> records = recordRepository.findByDateRangeAndBankType(start, end, bankTypeId, pageable);
        return convertToCustomPage(records);
    }

    @Transactional(readOnly = true)
    public CustomPage<ExpenseRecordDto.ListResponse> getExpenseRecordsByDateRangeAndCounterparty(
            String startDate, String endDate, String counterparty, Pageable pageable) {
        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);

        Page<ExpenseRecord> records = recordRepository.findByDateRangeAndCounterparty(start, end, counterparty, pageable);
        return convertToCustomPage(records);
    }

    @Transactional(readOnly = true)
    public CustomPage<ExpenseRecordDto.ListResponse> getExpenseRecordsByDateRangeAndFilters(
            String startDate, String endDate, String expenseType, Long bankTypeId, Pageable pageable) {
        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);
        ExpenseType type = ExpenseType.valueOf(expenseType.toUpperCase());

        Page<ExpenseRecord> records = recordRepository.findByDateRangeAndExpenseTypeAndBankType(
                start, end, type, bankTypeId, pageable);
        return convertToCustomPage(records);
    }

    @Transactional(readOnly = true)
    public ExpenseRecordDto.DetailResponse getExpenseRecordById(Long id) {
        ExpenseRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense record not found with id: " + id));
        return convertToDetailResponse(record);
    }

    public ExpenseRecordDto.DetailResponse createExpenseRecord(ExpenseRecordDto.CreateRequest request) {
        ExpenseBankType bankType = bankTypeRepository.findById(request.getBankTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Bank type not found"));

        ExpenseIncomeAccount incomeAccount = null;
        ExpenseExpenseAccount expenseAccount = null;

        String expenseTypeStr = request.getExpenseType().toUpperCase();
        if ("INCOME".equals(expenseTypeStr) && request.getIncomeAccountId() != null) {
            incomeAccount = incomeAccountRepository.findById(request.getIncomeAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Income account not found"));
        } else if ("EXPENSE".equals(expenseTypeStr) && request.getExpenseAccountId() != null) {
            expenseAccount = expenseAccountRepository.findById(request.getExpenseAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Expense account not found"));
        }

        LocalDateTime recordDate = parseRecordDate(request.getRecordDate());
        ExpenseType type = ExpenseType.valueOf(expenseTypeStr);

        ExpenseRecord record = ExpenseRecord.builder()
                .recordDate(recordDate)
                .expenseType(type)
                .bankType(bankType)
                .incomeAccount(incomeAccount)
                .expenseAccount(expenseAccount)
                .counterparty(request.getCounterparty())
                .description(request.getDescription())
                .material(request.getMaterial())
                .weight(request.getWeight())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .supplyAmount(request.getSupplyAmount())
                .taxAmount(request.getTaxAmount())
                .build();

        ExpenseRecord saved = recordRepository.save(record);
        return convertToDetailResponse(saved);
    }

    public ExpenseRecordDto.DetailResponse updateExpenseRecord(Long id, ExpenseRecordDto.UpdateRequest request) {
        ExpenseRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Expense record not found with id: " + id));

        ExpenseBankType bankType = bankTypeRepository.findById(request.getBankTypeId())
                .orElseThrow(() -> new IllegalArgumentException("Bank type not found"));

        ExpenseIncomeAccount incomeAccount = null;
        ExpenseExpenseAccount expenseAccount = null;

        String expenseTypeStr = request.getExpenseType().toUpperCase();
        if ("INCOME".equals(expenseTypeStr) && request.getIncomeAccountId() != null) {
            incomeAccount = incomeAccountRepository.findById(request.getIncomeAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Income account not found"));
        } else if ("EXPENSE".equals(expenseTypeStr) && request.getExpenseAccountId() != null) {
            expenseAccount = expenseAccountRepository.findById(request.getExpenseAccountId())
                    .orElseThrow(() -> new IllegalArgumentException("Expense account not found"));
        }

        LocalDateTime recordDate = parseRecordDate(request.getRecordDate());
        ExpenseType type = ExpenseType.valueOf(expenseTypeStr);

        record.update(recordDate, type, bankType, incomeAccount, expenseAccount,
                request.getCounterparty(), request.getDescription(),
                request.getMaterial(), request.getWeight(), request.getQuantity(),
                request.getUnitPrice(), request.getSupplyAmount(), request.getTaxAmount());

        ExpenseRecord updated = recordRepository.save(record);
        return convertToDetailResponse(updated);
    }

    public void deleteExpenseRecord(Long id) {
        if (!recordRepository.existsById(id)) {
            throw new IllegalArgumentException("Expense record not found with id: " + id);
        }
        recordRepository.deleteById(id);
    }

    @Transactional
    public void createRecordsBatch(List<ExpenseRecordDto.CreateRequest> requests) {
        for (ExpenseRecordDto.CreateRequest request : requests) {
            createExpenseRecord(request);
        }
    }

    @Transactional(readOnly = true)
    public ExpenseRecordDto.SummaryResponse getSummary(String startDate, String endDate) {
        LocalDateTime start = parseStartDate(startDate);
        LocalDateTime end = parseEndDate(endDate);

        BigDecimal incomeWeight = recordRepository.sumIncomeWeight(start, end);
        Long incomeAmount = recordRepository.sumIncomeAmount(start, end);
        BigDecimal expenseWeight = recordRepository.sumExpenseWeight(start, end);
        Long expenseAmount = recordRepository.sumExpenseAmount(start, end);

        incomeWeight = incomeWeight != null ? incomeWeight : BigDecimal.ZERO;
        incomeAmount = incomeAmount != null ? incomeAmount : 0L;
        expenseWeight = expenseWeight != null ? expenseWeight : BigDecimal.ZERO;
        expenseAmount = expenseAmount != null ? expenseAmount : 0L;

        BigDecimal netWeight = incomeWeight.subtract(expenseWeight);
        Long netAmount = incomeAmount - expenseAmount;

        return ExpenseRecordDto.SummaryResponse.builder()
                .totalIncomeWeight(incomeWeight)
                .totalIncomeAmount(incomeAmount)
                .totalExpenseWeight(expenseWeight)
                .totalExpenseAmount(expenseAmount)
                .netWeight(netWeight)
                .netAmount(netAmount)
                .build();
    }

    // Helper methods
    private LocalDateTime parseStartDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDate.now().atStartOfDay();
        }
        return LocalDate.parse(dateStr).atStartOfDay();
    }

    private LocalDateTime parseEndDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDate.now().atTime(23, 59, 59);
        }
        return LocalDate.parse(dateStr).atTime(23, 59, 59);
    }

    private LocalDateTime parseRecordDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            // Try ISO datetime first (2026-04-19T10:30:00)
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            // Fallback to date-only (2026-04-19)
            return LocalDate.parse(dateStr).atStartOfDay();
        }
    }

    private CustomPage<ExpenseRecordDto.ListResponse> convertToCustomPage(Page<ExpenseRecord> records) {
        return new CustomPage<>(
                records.getContent().stream()
                        .map(this::convertToListResponse)
                        .collect(Collectors.toList()),
                records.getPageable(),
                records.getTotalElements()
        );
    }

    private ExpenseRecordDto.ListResponse convertToListResponse(ExpenseRecord record) {
        String accountName = record.getExpenseType() == ExpenseType.INCOME
                ? (record.getIncomeAccount() != null ? record.getIncomeAccount().getIncomeAccountName() : "")
                : (record.getExpenseAccount() != null ? record.getExpenseAccount().getExpenseAccountName() : "");

        return ExpenseRecordDto.ListResponse.builder()
                .id(record.getExpenseRecordId())
                .recordDate(record.getRecordDate())
                .expenseType(record.getExpenseType().getCode())
                .bankTypeName(record.getBankType() != null ? record.getBankType().getBankTypeName() : "")
                .accountName(accountName)
                .counterparty(record.getCounterparty())
                .description(record.getDescription())
                .material(record.getMaterial())
                .weight(record.getWeight())
                .quantity(record.getQuantity())
                .unitPrice(record.getUnitPrice())
                .supplyAmount(record.getSupplyAmount())
                .taxAmount(record.getTaxAmount())
                .build();
    }

    private ExpenseRecordDto.DetailResponse convertToDetailResponse(ExpenseRecord record) {
        return ExpenseRecordDto.DetailResponse.builder()
                .id(record.getExpenseRecordId())
                .recordDate(record.getRecordDate())
                .expenseType(record.getExpenseType().getCode())
                .bankTypeId(record.getBankType() != null ? record.getBankType().getExpenseBankTypeId() : null)
                .bankTypeName(record.getBankType() != null ? record.getBankType().getBankTypeName() : "")
                .incomeAccountId(record.getIncomeAccount() != null ? record.getIncomeAccount().getExpenseIncomeAccountId() : null)
                .incomeAccountName(record.getIncomeAccount() != null ? record.getIncomeAccount().getIncomeAccountName() : "")
                .expenseAccountId(record.getExpenseAccount() != null ? record.getExpenseAccount().getExpenseExpenseAccountId() : null)
                .expenseAccountName(record.getExpenseAccount() != null ? record.getExpenseAccount().getExpenseAccountName() : "")
                .counterparty(record.getCounterparty())
                .description(record.getDescription())
                .material(record.getMaterial())
                .weight(record.getWeight())
                .quantity(record.getQuantity())
                .unitPrice(record.getUnitPrice())
                .supplyAmount(record.getSupplyAmount())
                .taxAmount(record.getTaxAmount())
                .build();
    }
}
