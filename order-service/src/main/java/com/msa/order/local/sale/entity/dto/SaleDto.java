package com.msa.order.local.sale.entity.dto;

import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class SaleDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        private Long storeId;
        private String storeName;
        @NotBlank(message = "필수 입력값 입니다.")
        private String type;
        private String material;
        private String saleNote;
        @Digits(integer=18, fraction=3) @PositiveOrZero
        private BigDecimal totalWeight;
        private Long totalPay;
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String createAt;
        private String saleType;
        private String name;
        private String saleCode;
        private String flowCode;
        private String productName;
        private String materialName;
        private String colorName;
        private String note;
        private Integer mainStoneQuantity;
        private Integer assistanceQuantity;
        private String productWeight; // 금 -> 24k X
        private String stoneWeight;
        private Integer mainProductCost;
        private Integer addProductCost;
        private Integer mainStoneCost;
        private Integer assistanceStoneCost;
        private Integer totalPurchaseCost; // 상품 매입 + 스톤 매입

        public static Response from(SaleRow r) {
            Response d = new Response();
            d.createAt = r.createAt()!=null ? r.createAt().toString() : null;
            d.saleType = r.saleType();
            d.name = r.name();
            d.saleCode = r.saleCode()!=null ? String.valueOf(r.saleCode()) : null;
            d.flowCode = r.flowCode()!=null ? String.valueOf(r.flowCode()) : null;
            d.productName = r.productName();
            d.materialName = r.materialName();
            d.colorName = r.colorName();
            d.note = r.note();
            d.mainStoneQuantity = r.mainStoneQuantity();
            d.assistanceQuantity = r.assistanceQuantity();
            d.productWeight = r.productWeight()!=null ? r.productWeight().toPlainString() : null;
            d.stoneWeight = r.stoneWeight()!=null ? r.stoneWeight().toPlainString() : null;
            d.mainProductCost = r.mainProductCost();
            d.addProductCost = r.addProductCost();
            d.mainStoneCost = r.mainStoneCost();
            d.assistanceStoneCost = r.assistanceStoneCost();
            d.totalPurchaseCost = r.totalPurchaseCost();
            return d;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition {
        private String date;
        private String input;
        private String type;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class SaleDetailDto {
        private Long flowCode;
        private OffsetDateTime saleCreateAt;
        private String productName;
        private String productMaterial;
        private String productColor;
        private String stockMainStoneNote;
        private String stockAssistanceStoneNote;
        private String productSize;
        private String stockNote;
        private BigDecimal goldWeight;
        private BigDecimal stoneWeight;
        private Integer mainStoneQuantity;
        private Integer assistanceStoneQuantity;
        private Integer productLaborCost;
        private Integer productAddLaborCost;
        private Integer mainStoneLaborCost;
        private Integer assistanceStoneLaborCost;
        private Integer addStoneLaborCost;
        private Boolean assistantStone;
        private String assistantStoneName;
        private OffsetDateTime assistantStoneCreateAt;
        private String storeName;

        @QueryProjection
        public SaleDetailDto(Long flowCode, OffsetDateTime saleCreateAt, String productName, String productMaterial, String productColor, String stockMainStoneNote, String stockAssistanceStoneNote, String productSize, String stockNote, BigDecimal goldWeight, BigDecimal stoneWeight, Integer mainStoneQuantity, Integer assistanceStoneQuantity, Integer productLaborCost, Integer productAddLaborCost, Integer mainStoneLaborCost, Integer assistanceStoneLaborCost, Integer addStoneLaborCost, Boolean assistantStone, String assistantStoneName, OffsetDateTime assistantStoneCreateAt, String storeName) {
            this.flowCode = flowCode;
            this.saleCreateAt = saleCreateAt;
            this.productName = productName;
            this.productMaterial = productMaterial;
            this.productColor = productColor;
            this.stockMainStoneNote = stockMainStoneNote;
            this.stockAssistanceStoneNote = stockAssistanceStoneNote;
            this.productSize = productSize;
            this.stockNote = stockNote;
            this.goldWeight = goldWeight;
            this.stoneWeight = stoneWeight;
            this.mainStoneQuantity = mainStoneQuantity;
            this.assistanceStoneQuantity = assistanceStoneQuantity;
            this.productLaborCost = productLaborCost;
            this.productAddLaborCost = productAddLaborCost;
            this.mainStoneLaborCost = mainStoneLaborCost;
            this.assistanceStoneLaborCost = assistanceStoneLaborCost;
            this.addStoneLaborCost = addStoneLaborCost;
            this.assistantStone = assistantStone;
            this.assistantStoneName = assistantStoneName;
            this.assistantStoneCreateAt = assistantStoneCreateAt;
            this.storeName = storeName;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class StoneCountDto {
        private Long flowCode;
        private Boolean mainStone;
        private Integer totalQuantity;

        @QueryProjection
        public StoneCountDto(Long flowCode, Boolean mainStone, Integer totalQuantity) {
            this.flowCode = flowCode;
            this.mainStone = mainStone;
            this.totalQuantity = totalQuantity;
        }
    }

}
