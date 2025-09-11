package com.msa.order.global.kafka.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class OrderAsyncRequested {
    private String eventId;       // 멱등 처리용
    private Long flowCode;
    private String tenantId;

    private Long storeId;
    private Long factoryId;
    private Long productId;
    private Long materialId;
    private Long classificationId;
    private Long colorId;
    private Long setTypeId;
    private List<Long> stoneIds;
    private String orderStatus;

    private String nickname;

    @Builder
    public OrderAsyncRequested(String eventId, Long flowCode, String tenantId, Long storeId, Long factoryId, Long productId, Long materialId, Long classificationId, Long colorId, Long setTypeId, List<Long> stoneIds, String orderStatus, String nickname) {
        this.eventId = eventId;
        this.flowCode = flowCode;
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.factoryId = factoryId;
        this.productId = productId;
        this.materialId = materialId;
        this.classificationId = classificationId;
        this.colorId = colorId;
        this.setTypeId = setTypeId;
        this.stoneIds = stoneIds;
        this.orderStatus = orderStatus;
        this.nickname = nickname;
    }
}
