package com.msa.jewelry.local.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "지출/수입 기록 DTO 묶음 — 등록/수정 요청 및 목록/상세/요약 응답")
public class ExpenseRecordDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "지출/수입 기록 등록 요청")
    public static class CreateRequest {
        @Schema(description = "기록 일자 (문자열, 예: 2026-05-16)", example = "2026-05-16")
        private String recordDate;
        @Schema(description = "지출 유형 (수입/지출 구분)", example = "EXPENSE")
        private String expenseType;
        @Schema(description = "은행 유형 ID", example = "1")
        private Long bankTypeId;
        @Schema(description = "수입 계정 ID", example = "1")
        private Long incomeAccountId;
        @Schema(description = "지출 계정 ID", example = "2")
        private Long expenseAccountId;
        @Schema(description = "거래 상대방", example = "ABC상사")
        private String counterparty;
        @Schema(description = "적요/설명", example = "월세 정산")
        private String description;
        @Schema(description = "재질 (14K/18K/24K 등)", example = "18K")
        private String material;
        @Schema(description = "중량(g)", example = "12.345")
        private BigDecimal weight;
        @Schema(description = "수량", example = "1")
        private Integer quantity;
        @Schema(description = "단가", example = "100000")
        private Long unitPrice;
        @Schema(description = "공급가액", example = "1000000")
        private Long supplyAmount;
        @Schema(description = "부가세액", example = "100000")
        private Long taxAmount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "지출/수입 기록 수정 요청")
    public static class UpdateRequest {
        @Schema(description = "기록 일자 (문자열)", example = "2026-05-16")
        private String recordDate;
        @Schema(description = "지출 유형 (수입/지출 구분)", example = "EXPENSE")
        private String expenseType;
        @Schema(description = "은행 유형 ID", example = "1")
        private Long bankTypeId;
        @Schema(description = "수입 계정 ID", example = "1")
        private Long incomeAccountId;
        @Schema(description = "지출 계정 ID", example = "2")
        private Long expenseAccountId;
        @Schema(description = "거래 상대방", example = "ABC상사")
        private String counterparty;
        @Schema(description = "적요/설명", example = "월세 정산")
        private String description;
        @Schema(description = "재질", example = "18K")
        private String material;
        @Schema(description = "중량(g)", example = "12.345")
        private BigDecimal weight;
        @Schema(description = "수량", example = "1")
        private Integer quantity;
        @Schema(description = "단가", example = "100000")
        private Long unitPrice;
        @Schema(description = "공급가액", example = "1000000")
        private Long supplyAmount;
        @Schema(description = "부가세액", example = "100000")
        private Long taxAmount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "지출/수입 기록 목록 응답")
    public static class ListResponse {
        @Schema(description = "지출 기록 PK", example = "1001")
        private Long id;
        @Schema(description = "기록 일시", example = "2026-05-16T10:30:00")
        private LocalDateTime recordDate;
        @Schema(description = "지출 유형", example = "EXPENSE")
        private String expenseType;
        @Schema(description = "은행 유형명", example = "국민은행")
        private String bankTypeName;
        @Schema(description = "계정명 (수입/지출 계정 표시)", example = "사무용품비")
        private String accountName;
        @Schema(description = "거래 상대방", example = "ABC상사")
        private String counterparty;
        @Schema(description = "적요/설명", example = "월세 정산")
        private String description;
        @Schema(description = "재질", example = "18K")
        private String material;
        @Schema(description = "중량(g)", example = "12.345")
        private BigDecimal weight;
        @Schema(description = "수량", example = "1")
        private Integer quantity;
        @Schema(description = "단가", example = "100000")
        private Long unitPrice;
        @Schema(description = "공급가액", example = "1000000")
        private Long supplyAmount;
        @Schema(description = "부가세액", example = "100000")
        private Long taxAmount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "지출/수입 기록 상세 응답")
    public static class DetailResponse {
        @Schema(description = "지출 기록 PK", example = "1001")
        private Long id;
        @Schema(description = "기록 일시", example = "2026-05-16T10:30:00")
        private LocalDateTime recordDate;
        @Schema(description = "지출 유형", example = "EXPENSE")
        private String expenseType;
        @Schema(description = "은행 유형 ID", example = "1")
        private Long bankTypeId;
        @Schema(description = "은행 유형명", example = "국민은행")
        private String bankTypeName;
        @Schema(description = "수입 계정 ID", example = "1")
        private Long incomeAccountId;
        @Schema(description = "수입 계정명", example = "판매수입")
        private String incomeAccountName;
        @Schema(description = "지출 계정 ID", example = "2")
        private Long expenseAccountId;
        @Schema(description = "지출 계정명", example = "사무용품비")
        private String expenseAccountName;
        @Schema(description = "거래 상대방", example = "ABC상사")
        private String counterparty;
        @Schema(description = "적요/설명", example = "월세 정산")
        private String description;
        @Schema(description = "재질", example = "18K")
        private String material;
        @Schema(description = "중량(g)", example = "12.345")
        private BigDecimal weight;
        @Schema(description = "수량", example = "1")
        private Integer quantity;
        @Schema(description = "단가", example = "100000")
        private Long unitPrice;
        @Schema(description = "공급가액", example = "1000000")
        private Long supplyAmount;
        @Schema(description = "부가세액", example = "100000")
        private Long taxAmount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "지출/수입 기록 합계 요약")
    public static class SummaryResponse {
        @Schema(description = "총 수입 중량(g)", example = "30.500")
        private BigDecimal totalIncomeWeight;
        @Schema(description = "총 수입 금액(원)", example = "5000000")
        private Long totalIncomeAmount;
        @Schema(description = "총 지출 중량(g)", example = "15.250")
        private BigDecimal totalExpenseWeight;
        @Schema(description = "총 지출 금액(원)", example = "2500000")
        private Long totalExpenseAmount;
        @Schema(description = "순 중량 (수입 - 지출)", example = "15.250")
        private BigDecimal netWeight;
        @Schema(description = "순 금액 (수입 - 지출)", example = "2500000")
        private Long netAmount;
    }
}
