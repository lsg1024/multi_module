package com.msa.order.global.kafka.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
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
    private Long colorId;
    private boolean assistantStone;
    private Long assistantStoneId;
    private OffsetDateTime assistantStoneCreateAt;
    private List<Long> stoneIds;
    private String orderStatus;

    private String nickname;

    @Override
    public String toString() {
        return "OrderAsyncRequested{" +
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
                ", stoneIds=" + stoneIds +
                ", orderStatus='" + orderStatus + '\'' +
                ", nickname='" + nickname + '\'' +
                '}';
    }

    @Builder
    public OrderAsyncRequested(String eventId, Long flowCode, String tenantId, Long storeId, Long factoryId, Long productId, Long materialId, Long colorId, boolean assistantStone, Long assistantStoneId, OffsetDateTime assistantStoneCreateAt, List<Long> stoneIds, String orderStatus, String nickname) {
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
        this.stoneIds = stoneIds;
        this.orderStatus = orderStatus;
        this.nickname = nickname;
    }
}
