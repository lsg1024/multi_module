package com.msa.order.local.sale.entity.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

}
