package com.msa.account.local.expense.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ExpenseRecordDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        private String recordDate;
        private String expenseType;
        private Long bankTypeId;
        private Long incomeAccountId;
        private Long expenseAccountId;
        private String counterparty;
        private String description;
        private String material;
        private BigDecimal weight;
        private Integer quantity;
        private Long unitPrice;
        private Long supplyAmount;
        private Long taxAmount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateRequest {
        private String recordDate;
        private String expenseType;
        private Long bankTypeId;
        private Long incomeAccountId;
        private Long expenseAccountId;
        private String counterparty;
        private String description;
        private String material;
        private BigDecimal weight;
        private Integer quantity;
        private Long unitPrice;
        private Long supplyAmount;
        private Long taxAmount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListResponse {
        private Long id;
        private LocalDateTime recordDate;
        private String expenseType;
        private String bankTypeName;
        private String accountName;
        private String counterparty;
        private String description;
        private String material;
        private BigDecimal weight;
        private Integer quantity;
        private Long unitPrice;
        private Long supplyAmount;
        private Long taxAmount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DetailResponse {
        private Long id;
        private LocalDateTime recordDate;
        private String expenseType;
        private Long bankTypeId;
        private String bankTypeName;
        private Long incomeAccountId;
        private String incomeAccountName;
        private Long expenseAccountId;
        private String expenseAccountName;
        private String counterparty;
        private String description;
        private String material;
        private BigDecimal weight;
        private Integer quantity;
        private Long unitPrice;
        private Long supplyAmount;
        private Long taxAmount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryResponse {
        private BigDecimal totalIncomeWeight;
        private Long totalIncomeAmount;
        private BigDecimal totalExpenseWeight;
        private Long totalExpenseAmount;
        private BigDecimal netWeight;
        private Long netAmount;
    }
}
