package com.msa.order.global.kafka.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class OrderAsyncRequested {
    private String eventId;       // 멱등 처리용
    private Long orderId;
    private String tenantId;

    private Long storeId;
    private Long factoryId;
    private Long productId;
    private Long materialId;
    private Long classificationId;
    private Long colorId;
    private List<Long> stoneIds;

    private String nickname;

    @Builder
    public OrderAsyncRequested(String eventId, Long orderId, String tenantId, Long storeId, Long factoryId, Long productId, Long materialId, Long classificationId, Long colorId, List<Long> stoneIds, String nickname) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.factoryId = factoryId;
        this.productId = productId;
        this.materialId = materialId;
        this.classificationId = classificationId;
        this.colorId = colorId;
        this.stoneIds = stoneIds;
        this.nickname = nickname;
    }
}
