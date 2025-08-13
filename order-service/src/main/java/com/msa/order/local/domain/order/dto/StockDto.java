package com.msa.order.local.domain.order.dto;

import com.msa.order.local.domain.order.entity.order_enum.ProductStatus;
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
    }

}
