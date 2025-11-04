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

public class SaleDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        private Long id;
        private String name;
        @NotBlank(message = "필수 입력값 입니다.")
        private String orderStatus;
        private String material;
        private String saleNote;
        @Digits(integer=18, fraction=3)
        @PositiveOrZero
        private BigDecimal goldWeight;
        private Integer payAmount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Condition {
        private String input;
        private String startAt;
        private String endAt;
        private String material;
    }

    @Setter
    @Getter
    @NoArgsConstructor
    public static class SaleDetailDto {
        private Long flowCode;
        private String saleCreateAt;
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
        private String assistantStoneCreateAt;
        private String storeName;

        @QueryProjection
        public SaleDetailDto(Long flowCode, String saleCreateAt, String productName, String productMaterial, String productColor, String stockMainStoneNote, String stockAssistanceStoneNote, String productSize, String stockNote, BigDecimal goldWeight, BigDecimal stoneWeight, Integer mainStoneQuantity, Integer assistanceStoneQuantity, Integer productLaborCost, Integer productAddLaborCost, Integer mainStoneLaborCost, Integer assistanceStoneLaborCost, Integer addStoneLaborCost, Boolean assistantStone, String assistantStoneName, String assistantStoneCreateAt, String storeName) {
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
