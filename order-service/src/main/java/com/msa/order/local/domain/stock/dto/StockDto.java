package com.msa.order.local.domain.stock.dto;

import com.msa.order.global.exception.EnumValue;
import com.msa.order.local.domain.order.entity.order_enum.ProductStatus;
import com.msa.order.local.domain.order.external_client.dto.ProductDetailDto;
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

    // 주문 -> 재고 등록 시
    @Getter
    @NoArgsConstructor
    public static class OrderStockRequest {
        @NotBlank(message = "상품 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상품 입력 값 오류.")
        private String productId;
        private String productSize;
        private Integer productAddLaborCost;
        private String materialId;
        private String classificationId;
        private String colorId;
        @EnumValue(enumClass = ProductStatus.class)
        private String productStatus; // 일반, 반품
        private String mainStoneNote;
        private String assistanceStoneNote;
        private String stockNote;
        private BigDecimal productWeight;
        private BigDecimal stoneWeight;

        @NotBlank(message = "상점 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "상점 입력 값 오류.")
        private String storeId;

        // factoryId 값은 노출 없이 저장
        @NotBlank(message = "공장 ID는 필수입니다.")
        @Pattern(regexp = "\\d+", message = "공장 입력 값 오류.")
        private String factoryId;

        @Valid
        private List<ProductDetailDto.StoneInfo> stoneInfos;
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

}
