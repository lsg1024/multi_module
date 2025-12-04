package com.msa.order.local.sale.entity.dto;

import com.msa.order.global.util.GoldUtils;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
@NoArgsConstructor
public class SaleItemResponse {

    private String createAt;
    private String createBy;
    private String saleType;
    private String storeId;
    private String storeName;
    private String saleCode;
    private String flowCode;
    private String productName;
    private String materialName;
    private String colorName;
    private String note;
    private Boolean assistantStone;
    private String assistantName;
    private String assistantCreateAt;
    private BigDecimal totalWeight;
    private BigDecimal pureGoldWeight;
    private Integer totalProductLaborCost;
    private Long mainStoneLaborCost;
    private Long assistanceStoneLaborCost;
    private Integer stoneAddLaborCost;
    private Long mainStoneQuantity;
    private Long assistanceStoneQuantity;

    @QueryProjection
    public SaleItemResponse(String createAt, String createBy, String saleType, String storeId, String storeName, String saleCode, String flowCode, String productName, String materialName, String colorName, String stockNote, String mainNote, String subNote, Boolean assistantStone, String assistantName, BigDecimal goldWeight, BigDecimal stoneWeight, BigDecimal harry, Integer productLabor, Integer productAddLabor, String assistantCreateAt, Long mainStoneLabor, Long asstStoneLabor, Integer stoneAddLabor, Long mainQty, Long asstQty) {
        this.createAt = createAt;
        this.createBy = createBy;
        this.saleType = saleType;
        this.storeId = storeId;
        this.storeName = storeName;
        this.saleCode = saleCode;
        this.flowCode = flowCode;
        this.productName = productName;
        this.materialName = materialName;
        this.colorName = colorName;
        this.assistantCreateAt = assistantCreateAt;
        this.note = Stream.of(stockNote, mainNote, subNote)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));

        this.assistantStone = assistantStone;
        this.assistantName = assistantName;
        BigDecimal validGoldWeight = (goldWeight == null) ? BigDecimal.ZERO : goldWeight;
        BigDecimal validStoneWeight = (stoneWeight == null) ? BigDecimal.ZERO : stoneWeight;
        this.totalWeight = validGoldWeight.add(validStoneWeight);
        this.pureGoldWeight = GoldUtils.calculatePureGoldWeightAndHarry(validGoldWeight, materialName, harry);
        int pLabor = (productLabor == null) ? 0 : productLabor;
        int pAddLabor = (productAddLabor == null) ? 0 : productAddLabor;
        this.totalProductLaborCost = pLabor + pAddLabor;
        this.mainStoneLaborCost = (mainStoneLabor == null) ? 0 : mainStoneLabor;
        this.assistanceStoneLaborCost = (asstStoneLabor == null) ? 0 : asstStoneLabor;
        this.stoneAddLaborCost = (stoneAddLabor == null) ? 0 : stoneAddLabor;
        this.mainStoneQuantity = (mainQty == null) ? 0 : mainQty;
        this.assistanceStoneQuantity = (asstQty == null) ? 0 : asstQty;
    }

}