package com.msa.jewelry.order.internal.global.kafka_dto_legacy;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "거래처 잔고 갱신 요청 컨테이너 (kafka legacy → 모놀로식 직접 호출 페이로드).")
public class AccountDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처 현재 잔고 업데이트 요청 — 판매/결제 시 거래처(매장 or 공장) 잔고를 갱신하기 위한 페이로드.")
    public static class updateCurrentBalance {
        @Schema(description = "멱등성 키 (중복 처리 방지)", example = "evt_abc123")
        private String eventId;
        @Schema(description = "판매 세션 코드", example = "445823472384938240")
        private String saleCode;
        @Schema(description = "테넌트 ID", example = "tenant-001")
        private String tenantId;
        @Schema(description = "거래 유형 (sale, 결제 등)", example = "sale")
        private String saleType; // sale or 결제...
        @Schema(description = "거래처 타입 (store / factory)", example = "store")
        private String type; // store or factory
        @Schema(description = "거래처 ID", example = "10")
        private Long id;
        @Schema(description = "거래처 이름", example = "ABC 보석상")
        private String name;
        @Schema(description = "금 재질 코드 (대문자)", example = "18K")
        private String material;
        @Schema(description = "순금 잔고 변동량(g) — 결제 시 음수", example = "-3.250")
        private BigDecimal pureGoldBalance;
        @Schema(description = "현금 잔고 변동량 (원)", example = "-100000")
        private Integer moneyBalance;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS")
        @Schema(description = "판매 발생 일시", example = "2026-05-16T14:30:00.000000000")
        private LocalDateTime SaleDate;

        @Builder
        public updateCurrentBalance(String eventId, String saleCode, String tenantId, String saleType, String type, Long id, String name, String material, BigDecimal pureGoldBalance, Integer moneyBalance, LocalDateTime saleDate) {
            this.eventId = eventId;
            this.saleCode = saleCode;
            this.tenantId = tenantId;
            this.saleType = saleType;
            this.type = type;
            this.id = id;
            this.name = name;
            this.material = material.toUpperCase();
            this.pureGoldBalance = pureGoldBalance;
            this.moneyBalance = moneyBalance;
            this.SaleDate = saleDate;
        }
    }
}
