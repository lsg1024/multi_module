package com.msa.order.local.order.dto;

import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.ProductStatus;
import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class OrderQueryDto {
    private Long productId;
    private String createAt;
    private String shippingAt;
    private String flowCode;
    private String storeName;
    private String productName;
    private String materialName;
    private String colorName;
    private String setType;
    private String productSize;
    private Integer stockQuantity;
    private String mainStoneNote;
    private String assistanceStoneNote;
    private List<String> stockFlowCodes;
    private String orderNote;
    private String factoryName;
    private String priority;
    private ProductStatus productStatus;
    private OrderStatus orderStatus;

    public void setStockFlowCodes(List<String> stockFlowCodes) {
        this.stockFlowCodes = stockFlowCodes;
    }

    @QueryProjection
    public OrderQueryDto(Long productId, String createAt, String shippingAt, String flowCode, String storeName, String productName, String materialName, String colorName, String setType, String productSize, Integer stockQuantity, String mainStoneNote, String assistanceStoneNote, String orderNote, String factoryName, String priority, ProductStatus productStatus, OrderStatus orderStatus) {
        this.productId = productId;
        this.createAt = createAt;
        this.shippingAt = shippingAt;
        this.flowCode = flowCode;
        this.storeName = storeName;
        this.productName = productName;
        this.materialName = materialName;
        this.colorName = colorName;
        this.setType = setType;
        this.productSize = productSize;
        this.stockQuantity = stockQuantity;
        this.mainStoneNote = mainStoneNote;
        this.assistanceStoneNote = assistanceStoneNote;
        this.orderNote = orderNote;
        this.factoryName = factoryName;
        this.priority = priority;
        this.productStatus = productStatus;
        this.orderStatus = orderStatus;
    }
}
