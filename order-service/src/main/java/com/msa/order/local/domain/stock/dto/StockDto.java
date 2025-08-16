package com.msa.order.local.domain.stock.dto;

import com.msa.order.local.domain.order.entity.order_enum.ProductStatus;
import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StockDto {

    // 주문 -> 재고 등록 시
    @Getter
    @NoArgsConstructor
    public static class OrderRequest {

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
