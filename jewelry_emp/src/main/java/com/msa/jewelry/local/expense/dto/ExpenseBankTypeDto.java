package com.msa.jewelry.local.expense.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "지출 은행 유형 DTO 묶음 — 은행 유형 CRUD 시 요청/응답 형태")
public class ExpenseBankTypeDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "지출 은행 유형 등록/수정 요청")
    public static class Request {
        @Schema(description = "은행 유형명", example = "국민은행")
        private String name;
        @Schema(description = "은행 유형 비고", example = "법인 운영 계좌")
        private String note;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "지출 은행 유형 응답")
    public static class Response {
        @Schema(description = "지출 은행 유형 PK", example = "1")
        private Long id;
        @Schema(description = "은행 유형명", example = "국민은행")
        private String name;
        @Schema(description = "은행 유형 비고", example = "법인 운영 계좌")
        private String note;
    }
}
