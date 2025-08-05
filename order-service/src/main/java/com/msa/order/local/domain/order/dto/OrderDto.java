package com.msa.order.local.domain.order.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OrderDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        private String storeId;
        private String productId;
        private String productSize;
        private String productLaborCost;
        private String orderNote;
        private String factoryId;
        private String materialId;
        private String colorId;
        private Integer quantity;
        private Integer orderMainStoneQuantity;
        private Integer orderAuxiliaryStoneQuantity;
        private String priorityName;
        private String createAt;

        @Builder
        public Request(String storeId, String productId, String productSize, String productLaborCost, String orderNote, String factoryId, String materialId, String colorId, Integer quantity, Integer orderMainStoneQuantity, Integer orderAuxiliaryStoneQuantity, String priorityName, String createAt) {
            this.storeId = storeId;
            this.productId = productId;
            this.productSize = productSize;
            this.productLaborCost = productLaborCost;
            this.orderNote = orderNote;
            this.factoryId = factoryId;
            this.materialId = materialId;
            this.colorId = colorId;
            this.quantity = quantity;
            this.orderMainStoneQuantity = orderMainStoneQuantity;
            this.orderAuxiliaryStoneQuantity = orderAuxiliaryStoneQuantity;
            this.priorityName = priorityName;
            this.createAt = createAt;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class Response {
        private String orderId;
        private String storeName;
        private String productName;
        private String productSize;
        private String productLaborCost;
        private String orderNote;
        private String factoryName;
        private String materialName;
        private String colorName;
        private Integer quantity;
        private Integer orderMainStoneQuantity;
        private Integer orderAuxiliaryStoneQuantity;
        private String priority;
        private String createAt;

        @QueryProjection
        public Response(String orderId, String storeName, String productName, String productSize, String productLaborCost, String orderNote, String factoryName, String materialName, String colorName, Integer quantity, Integer orderMainStoneQuantity, Integer orderAuxiliaryStoneQuantity, String priority, String createAt) {
            this.orderId = orderId;
            this.storeName = storeName;
            this.productName = productName;
            this.productSize = productSize;
            this.productLaborCost = productLaborCost;
            this.orderNote = orderNote;
            this.factoryName = factoryName;
            this.materialName = materialName;
            this.colorName = colorName;
            this.quantity = quantity;
            this.orderMainStoneQuantity = orderMainStoneQuantity;
            this.orderAuxiliaryStoneQuantity = orderAuxiliaryStoneQuantity;
            this.priority = priority;
            this.createAt = createAt;
        }
    }
}
