package com.msa.jewelry.global.excel.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@Schema(description = "판매 엑셀 출력용 쿼리 프로젝션 DTO — 판매 한 건의 엑셀 한 행을 표현.")
public class SaleExcelDto {

    @Schema(description = "거래 일시 (포맷된 문자열)", example = "2026-05-16 14:30:00")
    private String createAt;
    @Schema(description = "거래 등록자", example = "홍길동")
    private String createBy;
    @Schema(description = "거래 유형 (판매/반품/결제 등 표시명)", example = "판매")
    private String saleType;
    @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
    private String storeName;
    @Schema(description = "판매 세션 코드 (TSID)", example = "445823472384938240")
    private String saleCode;
    @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
    private String flowCode;
    @Schema(description = "상품 이름 (스냅샷)", example = "다이아 1ct 반지")
    private String productName;
    @Schema(description = "재질 이름", example = "18K")
    private String materialName;
    @Schema(description = "색상 이름", example = "옐로우골드")
    private String colorName;
    @Schema(description = "금 무게 (g)", example = "3.250")
    private BigDecimal goldWeight;
    @Schema(description = "스톤 무게 (g)", example = "0.500")
    private BigDecimal stoneWeight;
    @Schema(description = "순금 환산 무게 (g) — 해리 적용 후", example = "2.500")
    private BigDecimal pureGoldWeight;
    @Schema(description = "총 공임 합계", example = "200000")
    private Integer totalLaborCost;
    @Schema(description = "거래 비고", example = "샘플 반품")
    private String note;

    @Builder
    @QueryProjection
    public SaleExcelDto(String createAt, String createBy, String saleType, String storeName,
                        String saleCode, String flowCode, String productName, String materialName,
                        String colorName, BigDecimal goldWeight, BigDecimal stoneWeight,
                        BigDecimal pureGoldWeight, Integer totalLaborCost, String note) {
        this.createAt = createAt;
        this.createBy = createBy;
        this.saleType = saleType;
        this.storeName = storeName;
        this.saleCode = saleCode;
        this.flowCode = flowCode;
        this.productName = productName;
        this.materialName = materialName;
        this.colorName = colorName;
        this.goldWeight = goldWeight != null ? goldWeight : BigDecimal.ZERO;
        this.stoneWeight = stoneWeight != null ? stoneWeight : BigDecimal.ZERO;
        this.pureGoldWeight = pureGoldWeight != null ? pureGoldWeight : BigDecimal.ZERO;
        this.totalLaborCost = totalLaborCost != null ? totalLaborCost : 0;
        this.note = note;
    }
}
