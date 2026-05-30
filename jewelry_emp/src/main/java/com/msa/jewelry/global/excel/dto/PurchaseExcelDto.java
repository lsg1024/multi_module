package com.msa.jewelry.global.excel.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "매입처 엑셀 다운로드용 DTO — 매입처 잔고/비고 평탄화")
public class PurchaseExcelDto {

    @Schema(description = "거래처 PK", example = "10")
    private Long accountId;
    @Schema(description = "거래처명", example = "강남금은방")
    private String accountName;
    @Schema(description = "거래처 등급", example = "A")
    private String grade;
    @Schema(description = "금 미수 잔액 (문자열)", example = "12.345")
    private String goldWeight;
    @Schema(description = "현금 미수 잔액 (문자열)", example = "1500000")
    private String moneyAmount;
    @Schema(description = "비고", example = "VIP 거래처")
    private String note;

    @Builder
    @QueryProjection
    public PurchaseExcelDto(Long accountId, String accountName, String grade,
                            String goldWeight, String moneyAmount, String note) {
        this.accountId = accountId;
        this.accountName = accountName;
        this.grade = grade != null ? grade : "";
        this.goldWeight = goldWeight != null ? goldWeight : "0";
        this.moneyAmount = moneyAmount != null ? moneyAmount : "0";
        this.note = note != null ? note : "";
    }
}
