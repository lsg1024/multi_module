package com.msa.jewelry.local.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "지출 계정 DTO 묶음 — 지출 계정 CRUD 시 요청/응답 형태")
public class ExpenseExpenseAccountDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "지출 계정 등록/수정 요청")
    public static class Request {
        @Schema(description = "지출 계정명", example = "사무용품비")
        private String name;
        @Schema(description = "지출 계정 비고", example = "운영 잡비 카테고리")
        private String note;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "지출 계정 응답")
    public static class Response {
        @Schema(description = "지출 계정 PK", example = "1")
        private Long id;
        @Schema(description = "지출 계정명", example = "사무용품비")
        private String name;
        @Schema(description = "지출 계정 비고", example = "운영 잡비 카테고리")
        private String note;
    }
}
