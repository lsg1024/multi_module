package com.msa.jewelry.order.internal.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "거래처(매장) DTO 컨테이너 — order 모듈이 account 모듈에서 거래처 정보를 조회할 때 사용.")
public class StoreDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처 조회 요청 — id 또는 name.")
    public static class Request {
        @Schema(description = "거래처 ID", example = "10")
        private Long storeId;
        @Schema(description = "거래처 이름", example = "ABC 보석상")
        private String storeName;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처 응답 — 단순 거래처 카드. 등급/수수료 포함.")
    public static class Response {
        @Schema(description = "거래처 ID", example = "10")
        private Long storeId;
        @Schema(description = "거래처 이름", example = "ABC 보석상")
        private String storeName;
        @Schema(description = "거래처 등급", example = "A")
        private String grade;
        @Schema(description = "거래처 수수료(허리, 문자열)", example = "1.50")
        private String storeHarry;
        @Schema(description = "거래처 등급 변경 시 과거 판매 적용 여부 옵션", example = "false")
        private boolean optionApplyPastSales;

        @Builder
        public Response(Long storeId, String storeName, String grade, String storeHarry, boolean optionApplyPastSales) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.grade = grade;
            this.storeHarry = storeHarry;
            this.optionApplyPastSales = optionApplyPastSales;
        }

    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처 상세(account) 응답 — 거래처/매입처 상세 잔고와 사업자 정보 포함.")
    public static class accountResponse {
        @Schema(description = "거래처 계정 ID", example = "10")
        private Long accountId;
        @Schema(description = "거래처 계정 이름", example = "ABC 보석상")
        private String accountName;
        @Schema(description = "거래 형태 (예: STORE/FACTORY)", example = "STORE")
        private String tradeType;
        @Schema(description = "거래처 등급", example = "A")
        private String grade;
        @Schema(description = "금 해리(허리) 손실률", example = "0.50")
        private String goldHarryLoss;
        @Schema(description = "이전 금 잔고 (g, 문자열)", example = "12.345")
        private String previousGoldBalance;
        @Schema(description = "이전 현금 잔고 (원, 문자열)", example = "1500000")
        private String previousMoneyBalance;
        @Schema(description = "변동 후 금 잔고 (g, 문자열)", example = "10.250")
        private String afterGoldBalance;
        @Schema(description = "변동 후 현금 잔고 (원, 문자열)", example = "1200000")
        private String afterMoneyBalance;
        @Schema(description = "마지막 판매 일자", example = "2026-05-10")
        private String lastSaleDate;
        @Schema(description = "마지막 결제 일자", example = "2026-05-15")
        private String lastPaymentDate;
        @Schema(description = "사업주 이름", example = "홍길동")
        private String businessOwnerName;
        @Schema(description = "사업주 전화번호", example = "010-1234-5678")
        private String businessOwnerNumber;
        @Schema(description = "사업장 연락처 1", example = "02-123-4567")
        private String businessNumber1;
        @Schema(description = "사업장 연락처 2", example = "031-987-6543")
        private String businessNumber2;
        @Schema(description = "팩스 번호", example = "02-123-4568")
        private String faxNumber;
        @Schema(description = "주소", example = "서울시 종로구 ...")
        private String address;
        @Schema(description = "비고", example = "VIP 거래처")
        private String note;
    }
}
