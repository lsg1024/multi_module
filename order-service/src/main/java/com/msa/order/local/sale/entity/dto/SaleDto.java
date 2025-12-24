package com.msa.order.local.sale.entity.dto;

import com.msa.order.global.dto.StoneDto;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static com.msa.order.global.util.DateConversionUtil.OffsetDateTimeToLocalDate;

public class SaleDto {
    @Getter
    @NoArgsConstructor
    public static class Response {
        private Long flowCode;
        private String createAt;
        private String saleType;
        private String id;
        private String name;
        private String grade;
        private BigDecimal harry;
        private String productName;
        private String productSize;
        private String materialName;
        private String colorName;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String note;
        private Integer accountGoldPrice;
        private BigDecimal goldWeight;
        private BigDecimal stoneWeight;
        private Integer productLaborCost;
        private Integer productAddLaborCost;
        private Integer addStoneLaborCost;
        private Boolean assistantStone;
        private String assistantStoneId;
        private String assistantStoneName;
        private String assistantStoneCreateAt;
        private List<StoneDto.StoneInfo> stoneInfos;

        @Builder
        public Response(Long flowCode, String createAt, String saleType, String id, String name, String grade, BigDecimal harry, String productName, String productSize, String materialName, String colorName, String mainStoneNote, String assistanceStoneNote, String note, Integer accountGoldPrice, BigDecimal goldWeight, BigDecimal stoneWeight, Integer productLaborCost, Integer productAddLaborCost, Integer addStoneLaborCost, Boolean assistantStone, String assistantStoneId, String assistantStoneName, String assistantStoneCreateAt, List<StoneDto.StoneInfo> stoneInfos) {
            this.flowCode = flowCode;
            this.createAt = createAt;
            this.saleType = saleType;
            this.id = id;
            this.name = name;
            this.grade = grade;
            this.harry = harry;
            this.productName = productName;
            this.productSize = productSize;
            this.materialName = materialName;
            this.colorName = colorName;
            this.mainStoneNote = mainStoneNote;
            this.assistanceStoneNote = assistanceStoneNote;
            this.note = note;
            this.accountGoldPrice = accountGoldPrice;
            this.goldWeight = goldWeight;
            this.stoneWeight = stoneWeight;
            this.productLaborCost = productLaborCost;
            this.productAddLaborCost = productAddLaborCost;
            this.addStoneLaborCost = addStoneLaborCost;
            this.assistantStone = assistantStone;
            this.assistantStoneId = assistantStoneId;
            this.assistantStoneName = assistantStoneName;
            this.assistantStoneCreateAt = assistantStoneCreateAt;
            this.stoneInfos = stoneInfos;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class Request {
        private Integer accountGoldPrice;
        private Long id;
        private String name;
        private BigDecimal harry;
        private String grade;
        @NotBlank(message = "필수 입력값 입니다.")
        private String orderStatus;
        private String material;
        private String note;
        private String goldWeight; // 총중량으로 재질로 순금 값 계산
        private Integer payAmount;
    }

    @Getter
    @NoArgsConstructor
    public static class updateRequest {
        private String productSize;
        private boolean isProductWeightSale;
        private Integer productPurchaseCost;
        private Integer productLaborCost;
        private Integer productAddLaborCost;
        private String stockNote;
        private String storeHarry;
        private String goldWeight;
        private String stoneWeight;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private boolean assistantStone;
        private String assistantStoneId;
        private String assistantStoneName;
        private String assistantStoneCreateAt;
        private List<StoneDto.StoneInfo> stoneInfos;
        private Integer stoneAddLaborCost;

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
        private LocalDate assistantStoneCreateAt;
        private String storeName;

        @QueryProjection
        public SaleDetailDto(Long flowCode, String saleCreateAt, String productName, String productMaterial, String productColor, String stockMainStoneNote, String stockAssistanceStoneNote, String productSize, String stockNote, BigDecimal goldWeight, BigDecimal stoneWeight, Integer mainStoneQuantity, Integer assistanceStoneQuantity, Integer productLaborCost, Integer productAddLaborCost, Integer mainStoneLaborCost, Integer assistanceStoneLaborCost, Integer addStoneLaborCost, Boolean assistantStone, String assistantStoneName, OffsetDateTime assistantStoneCreateAt, String storeName) {
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
            this.assistantStoneCreateAt = OffsetDateTimeToLocalDate(assistantStoneCreateAt);
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
