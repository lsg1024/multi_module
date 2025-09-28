package com.msa.order.local.stock.dto;

import com.msa.order.global.dto.StoneDto;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

public class StockDto {

    // 직접 재고 등록 시
    @Getter
    @NoArgsConstructor
    public static class createStockRequest {
        @NotBlank(message = "상품 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상품 입력 값 오류.")
        private String productId;
        private String productSize;
        private Integer productPurchaseCost;
        private Integer addProductLaborCost;
        private Integer addStoneLaborCost;
        private Boolean isProductWeightSale;
        private String materialId;
        private String classificationId;
        private String colorId;
        private String setTypeId;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String stockNote;
        private BigDecimal goldWeight;
        private BigDecimal stoneWeight;
        // 보조석
        private boolean assistantStone;
        private String assistantStoneId;
        private String assistantStoneCreateAt;

        @NotBlank(message = "상점 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상점 입력 값 오류.")
        private String storeId;

        @NotBlank(message = "공장 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "공장 입력 값 오류.")
        private String factoryId;

        @Valid
        private List<StoneDto.StoneInfo> stoneInfos;
    }

    @Getter
    @NoArgsConstructor
    public static class stockRequest {
        private String productSize;
        private Boolean isProductWeightSale;
        private Integer addProductLaborCost;
        private Integer addStoneLaborCost;
        private Integer productPurchaseCost;
        private Integer stonePurchaseCost;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String stockNote;
        // 보조석
        private boolean assistantStone;
        private String assistantStoneId;
        private String assistantStoneCreateAt;
        private BigDecimal goldWeight;
        private BigDecimal stoneWeight;

        @Valid
        private List<StoneDto.StoneInfo> stoneInfos;
    }

    @Getter
    @NoArgsConstructor
    public static class stockResponse {
        private String productSize;
        private Boolean isProductWeightSale;
        private Integer addProductLaborCost;
        private Integer addStoneLaborCost;
        private Integer productPurchaseCost;
        private Integer stonePurchaseCost;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String stockNote;
        // 보조석
        private boolean assistantStone;
        private String assistantStoneId;
        private String assistantStoneCreateAt;
        private BigDecimal goldWeight;
        private BigDecimal stoneWeight;

        @Valid
        private List<StoneDto.StoneInfo> stoneInfos;
    }

    @Getter
    @NoArgsConstructor
    public static class StockRentalRequest {
        @NotBlank(message = "상점 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상점 입력 값 오류.")
        private String storeId;
        private String productSize;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String stockNote;
        private Boolean isProductWeightSale;
        private String goldWeight;
        private String stoneWeight;
        private Integer addProductLaborCost;
        private Integer addStoneLaborCost;
        @Valid
        private List<StoneDto.StoneInfo> stoneInfos; // 개당 알수는 직접 수정 불가
    }

    // 재고값 상세 조회 데이터
    @Getter
    @NoArgsConstructor
    public static class ResponseDetail {
        private String flowCode;
        private String createAt;
        private String originalProductStatus;
        private String classificationName;
        private String productName;
        private String storeName;
        private String materialName;
        private String colorName;
        private String setTypeName;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String productSize;
        private String stockNote;
        private Integer productLaborCost;
        private Integer productAddLaborCost;
        private Integer mainStoneLaborCost;
        private Integer assistanceStoneLaborCost;
        private Integer mainStoneQuantity;
        private Integer assistanceStoneQuantity;
        private String goldWeight;
        private String stoneWeight;
        private Integer productPurchaseCost;
        private Integer stonePurchaseCost;

        @Builder
        public ResponseDetail(String flowCode, String createAt, String originalProductStatus, String classificationName, String productName, String storeName, String materialName, String colorName, String setTypeName, String mainStoneNote, String assistanceStoneNote, String productSize, String stockNote, Integer productLaborCost, Integer productAddLaborCost, Integer mainStoneLaborCost, Integer assistanceStoneLaborCost, Integer mainStoneQuantity, Integer assistanceStoneQuantity, String goldWeight, String stoneWeight, Integer productPurchaseCost, Integer stonePurchaseCost) {
            this.flowCode = flowCode;
            this.createAt = createAt;
            this.originalProductStatus = originalProductStatus;
            this.classificationName = classificationName;
            this.productName = productName;
            this.storeName = storeName;
            this.materialName = materialName;
            this.colorName = colorName;
            this.setTypeName = setTypeName;
            this.mainStoneNote = mainStoneNote;
            this.assistanceStoneNote = assistanceStoneNote;
            this.productSize = productSize;
            this.stockNote = stockNote;
            this.productLaborCost = productLaborCost;
            this.productAddLaborCost = productAddLaborCost;
            this.mainStoneLaborCost = mainStoneLaborCost;
            this.assistanceStoneLaborCost = assistanceStoneLaborCost;
            this.mainStoneQuantity = mainStoneQuantity;
            this.assistanceStoneQuantity = assistanceStoneQuantity;
            this.goldWeight = goldWeight;
            this.stoneWeight = stoneWeight;
            this.productPurchaseCost = productPurchaseCost;
            this.stonePurchaseCost = stonePurchaseCost;
        }
    }

    // 재고값 조회 데이터
    @Getter
    @NoArgsConstructor
    public static class Response {
        private String flowCode;
        private String createAt;
        private String storeName;
        private String productSize;
        private String stockNote;
        private String materialName;
        private String classificationName;
        private String colorName;
        private Integer productLaborCost;
        private Integer productAddLaborCost;
        private Integer mainStoneLaborCost;
        private Integer assistanceLaborCost;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private Integer mainStoneQuantity;
        private Integer assistanceStoneQuantity;
        private String goldWeight;
        private String stoneWeight;
        private Integer productPurchaseCost;
        private Integer stonePurchaseCost;

        @QueryProjection
        public Response(String flowCode, String createAt, String storeName, String productSize, String stockNote, String materialName, String classificationName, String colorName, Integer productLaborCost, Integer productAddLaborCost, Integer mainStoneLaborCost, Integer assistanceLaborCost, String mainStoneNote, String assistanceStoneNote, Integer mainStoneQuantity, Integer assistanceStoneQuantity, String stoneWeight, String goldWeight, Integer productPurchaseCost, Integer stonePurchaseCost) {
            this.flowCode = flowCode;
            this.createAt = createAt;
            this.storeName = storeName;
            this.productSize = productSize;
            this.stockNote = stockNote;
            this.materialName = materialName;
            this.classificationName = classificationName;
            this.colorName = colorName;
            this.productLaborCost = productLaborCost;
            this.productAddLaborCost = productAddLaborCost;
            this.mainStoneLaborCost = mainStoneLaborCost;
            this.assistanceLaborCost = assistanceLaborCost;
            this.mainStoneNote = mainStoneNote;
            this.assistanceStoneNote = assistanceStoneNote;
            this.mainStoneQuantity = mainStoneQuantity;
            this.assistanceStoneQuantity = assistanceStoneQuantity;
            this.stoneWeight = stoneWeight;
            this.goldWeight = goldWeight;
            this.productPurchaseCost = productPurchaseCost;
            this.stonePurchaseCost = stonePurchaseCost;
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockCondition {
        private String startAt;
        private String endAt;
    }

    public static class StockStone {
        private Integer stonePurchaseCost;
        private Integer stoneLaborCost;
        private Integer stoneQuantity;
    }

}
