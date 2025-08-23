package com.msa.order.global.kafka.dto;

import com.msa.order.local.domain.stock.dto.StockDto;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private Long classificationId;
    private String nickname;
    private Integer addProductLaborCost;
    private Integer addStoneLaborCost;
    private List<Long> stoneIds;
    private List<StockDto.StoneInfo> stoneInfos;

    @Builder
    public KafkaStockRequest(String eventId, Long flowCode, String tenantId, Long storeId, Long factoryId, Long productId, Long materialId, Long colorId, Long classificationId, String nickname, Integer addProductLaborCost, Integer addStoneLaborCost, List<Long> stoneIds, List<StockDto.StoneInfo> stoneInfos) {
        this.eventId = eventId;
        this.flowCode = flowCode;
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.factoryId = factoryId;
        this.productId = productId;
        this.materialId = materialId;
        this.colorId = colorId;
        this.classificationId = classificationId;
        this.nickname = nickname;
        this.addProductLaborCost = addProductLaborCost;
        this.addStoneLaborCost = addStoneLaborCost;
        this.stoneIds = stoneIds;
        this.stoneInfos = stoneInfos;
    }

}
