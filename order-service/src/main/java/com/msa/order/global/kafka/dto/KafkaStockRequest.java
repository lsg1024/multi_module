package com.msa.order.global.kafka.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
public class KafkaStockRequest {
    private String eventId;
    private Long flowCode;
    private String tenantId;
    private Long storeId;
    private Long factoryId;
    private Long productId;
    private Long materialId;
    private Long colorId;
    private Long setTypeId;
    private Long classificationId;
    private String nickname;
    private boolean assistantStone;
    private Long assistantStoneId;
    private OffsetDateTime assistantStoneCreateAt;
//    private List<Long> stoneIds;
//    private List<StoneDto.StoneInfo> stoneInfos;

    @Builder
    public KafkaStockRequest(String eventId, Long flowCode, String tenantId, Long storeId, Long factoryId, Long productId, Long materialId, Long colorId, Long setTypeId, Long classificationId, String nickname, boolean assistantStone, Long assistantStoneId, OffsetDateTime assistantStoneCreateAt) {
        this.eventId = eventId;
        this.flowCode = flowCode;
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.factoryId = factoryId;
        this.productId = productId;
        this.materialId = materialId;
        this.colorId = colorId;
        this.setTypeId = setTypeId;
        this.classificationId = classificationId;
        this.nickname = nickname;
        this.assistantStone = assistantStone;
        this.assistantStoneId = assistantStoneId;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
    }

}
