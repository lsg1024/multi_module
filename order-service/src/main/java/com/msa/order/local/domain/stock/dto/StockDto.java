package com.msa.order.local.domain.stock.dto;

import com.msa.order.local.domain.order.entity.order_enum.ProductStatus;
import com.querydsl.core.annotations.QueryProjection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
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
        private Integer productAddLaborCost;
        private boolean isProductWeightSale;
        private String materialId;
        private String classificationId;
        private String colorId;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String stockNote;
        private BigDecimal productWeight;
        private BigDecimal stoneWeight;

        @NotBlank(message = "상점 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상점 입력 값 오류.")
        private String storeId;

        @NotBlank(message = "공장 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "공장 입력 값 오류.")
        private String factoryId;

        @Valid
        private List<StockDto.StoneInfo> stoneInfos;
    }

    @Getter
    @NoArgsConstructor
    public static class orderStockRequest {
        private String productSize;
        private boolean isProductWeightSale;
        private Integer addProductLaborCost;
        private Integer addStoneLaborCost;
        private Integer productPurchaseCost;
        private Integer stonePurchaseCost;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String stockNote;
        private BigDecimal productWeight;
        private BigDecimal stoneWeight;

        @Valid
        private List<StockDto.StoneInfo> stoneInfos;
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
        private String productNote;
        private boolean isProductWeightSale;
        private String productWeight;
        private String stoneWeight;
        private Integer addProductLaborCost;
        private Integer addStoneLaborCost;
        @Valid
        private List<StockDto.StoneInfo> stoneInfos; // 개당 알수는 직접 수정 불가
    }

    @Getter
    @NoArgsConstructor
    public static class StoneInfo {
        @NotBlank(message = "스톤 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상점 ID는 숫자여야 합니다.")
        private String stoneId;
        private String stoneName;
        private String stoneWeight;
        private Integer purchaseCost;
        private Integer laborCost;
        private Integer quantity;
        private boolean productStoneMain;
        private boolean includeQuantity;
        private boolean includeWeight;
        private boolean includeLabor;
    }

    // 재고값 데이터
    @Getter
    @NoArgsConstructor
    public static class Response {
        private String orderCode;
        private String createAt;
        private ProductStatus productStatus;
        private String storeName;
        private String productSize;
        private String orderNote;
        private String materialName;
        private String classificationName;
        private String colorName;
        private String productLaborCost;
        private String productAddLaborCost;
        private String mainStoneLaborCost;
        private String assistanceLaborCost;
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String mainStoneQuantity;
        private String assistanceStoneQuantity;
        private String stoneWeight;
        private String totalWeight;
        private String goldHarry;
        private String productPurchaseCost;
        private String stonePurchaseCost;

        @QueryProjection
        public Response(String orderCode, String createAt, ProductStatus productStatus, String storeName, String productSize, String orderNote, String materialName, String classificationName, String colorName, String productLaborCost, String productAddLaborCost, String mainStoneLaborCost, String assistanceLaborCost, String mainStoneNote, String assistanceStoneNote, String mainStoneQuantity, String assistanceStoneQuantity, String stoneWeight, String totalWeight, String goldHarry, String productPurchaseCost, String stonePurchaseCost) {
            this.orderCode = orderCode;
            this.createAt = createAt;
            this.productStatus = productStatus;
            this.storeName = storeName;
            this.productSize = productSize;
            this.orderNote = orderNote;
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
            this.totalWeight = totalWeight;
            this.goldHarry = goldHarry;
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
