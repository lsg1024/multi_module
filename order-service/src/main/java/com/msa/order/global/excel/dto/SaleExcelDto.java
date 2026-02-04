package com.msa.order.global.excel.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class SaleExcelDto {

    private String createAt;
    private String createBy;
    private String saleType;
    private String storeName;
    private String saleCode;
    private String flowCode;
    private String productName;
    private String materialName;
    private String colorName;
    private BigDecimal goldWeight;
    private BigDecimal stoneWeight;
    private BigDecimal pureGoldWeight;
    private Integer totalLaborCost;
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
