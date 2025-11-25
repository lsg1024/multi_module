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
    private String token;

    private Long storeId;
    private Long factoryId;
    private Long productId;
    private Long materialId;
    private Long colorId;
    private boolean assistantStone;
    private Long assistantStoneId;
    private OffsetDateTime assistantStoneCreateAt;

    private String nickname;

    @Builder
    public OrderUpdateRequest(String eventId, Long flowCode, String tenantId, String token, Long storeId, Long factoryId, Long productId, Long materialId, Long colorId, boolean assistantStone, Long assistantStoneId, OffsetDateTime assistantStoneCreateAt, String nickname) {
        this.eventId = eventId;
        this.flowCode = flowCode;
        this.tenantId = tenantId;
        this.token = token;
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
