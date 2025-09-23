package com.msa.order.global.kafka.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
public class OrderUpdateRequest {
    private String eventId;       // 멱등 처리용
    private Long flowCode;
    private String tenantId;

    private Long storeId;
    private Long factoryId;
    private Long productId;
    private Long materialId;
    private Long colorId;
    private boolean assistantStone;
    private Long assistantStoneId;
    private OffsetDateTime assistantStoneCreateAt;

    private String nickname;

    @Override
    public String toString() {
        return "OrderUpdateRequest{" +
                "eventId='" + eventId + '\'' +
                ", flowCode=" + flowCode +
                ", tenantId='" + tenantId + '\'' +
                ", storeId=" + storeId +
                ", factoryId=" + factoryId +
                ", productId=" + productId +
                ", materialId=" + materialId +
                ", colorId=" + colorId +
                ", assistantStone=" + assistantStone +
                ", assistantStoneId=" + assistantStoneId +
                ", assistantStoneCreateAt=" + assistantStoneCreateAt +
                ", nickname='" + nickname + '\'' +
                '}';
    }

    @Builder
    public OrderUpdateRequest(String eventId, Long flowCode, String tenantId, Long storeId, Long factoryId, Long productId, Long materialId, Long colorId, boolean assistantStone, Long assistantStoneId, OffsetDateTime assistantStoneCreateAt, String nickname) {
        this.eventId = eventId;
        this.flowCode = flowCode;
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.factoryId = factoryId;
        this.productId = productId;
        this.materialId = materialId;
        this.colorId = colorId;
        this.assistantStone = assistantStone;
        this.assistantStoneId = assistantStoneId;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
        this.nickname = nickname;
    }
}
