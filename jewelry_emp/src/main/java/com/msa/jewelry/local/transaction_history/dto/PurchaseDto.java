package com.msa.jewelry.local.transaction_history.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "매입(Purchase) 단건 DTO - 매입 수기 등록(매입 생성) 요청 한 줄")
public class PurchaseDto {
    @Schema(description = "거래(등록) 일시. null 이면 서버 현재시각", example = "2026-05-16T00:00:00")
    private LocalDateTime transactionDate;
    @Schema(description = "거래 유형(enum name): PURCHASE/PAYMENT/RETURN/DISCOUNT 등", example = "PURCHASE")
    private String transactionType;
    @Schema(description = "재질 (14K/18K/24K 등)", example = "18K")
    private String material;
    @Schema(description = "순금 환산 중량(g) - 잔액에 가감되는 순금 값", example = "3.333")
    private BigDecimal goldAmount;
    @Schema(description = "현금 금액(원)", example = "500000")
    private Long moneyAmount;
    @Schema(description = "판매 세션 코드 (TSID). 수기 매입은 보통 null", example = "445823472384938240")
    private String saleCode;
    @Schema(description = "거래처(제조사) PK (문자열)", example = "10")
    private String accountId;
    @Schema(description = "거래 비고", example = "신규 매입")
    private String transactionNote;
}
