package com.msa.order.global.kafka.dto;

import com.msa.order.global.dto.StoneDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

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
    private Integer addProductLaborCost;
    private Integer addStoneLaborCost;
    private boolean assistantStone;
    private Long assistantStoneId;
    private OffsetDateTime assistantStoneCreateAt;
    private List<Long> stoneIds;
    private List<StoneDto.StoneInfo> stoneInfos;

    @Builder
    public KafkaStockRequest(String eventId, Long flowCode, String tenantId, Long storeId, Long factoryId, Long productId, Long materialId, Long colorId, Long setTypeId, Long classificationId, String nickname, Integer addProductLaborCost, Integer addStoneLaborCost, boolean assistantStone, Long assistantStoneId, OffsetDateTime assistantStoneCreateAt, List<Long> stoneIds, List<StoneDto.StoneInfo> stoneInfos) {
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
        this.addProductLaborCost = addProductLaborCost;
        this.addStoneLaborCost = addStoneLaborCost;
        this.assistantStone = assistantStone;
        this.assistantStoneId = assistantStoneId;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
        this.stoneIds = stoneIds;
        this.stoneInfos = stoneInfos;
    }

}
