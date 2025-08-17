package com.msa.order.global.kafka.dto;

import com.msa.order.global.exception.EnumValue;
import com.msa.order.local.domain.order.entity.order_enum.ProductStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class KafkaStockRequest {
    private String eventId;
    private Long orderId;
    private String tenantId;
    private Long storeId;
    private Long factoryId;
    private Long productId;
    private Long materialId;
    private Long colorId;
    private Long classificationId;
    @EnumValue(enumClass = ProductStatus.class)
    private String productStatus;
    private String nickname;
    private List<Long> stoneIds;

    @Builder
    public KafkaStockRequest(String eventId, Long orderId, String tenantId, Long storeId, Long factoryId, Long productId, Long materialId, Long colorId, Long classificationId, String productStatus, String nickname, List<Long> stoneIds) {
        this.eventId = eventId;
        this.orderId = orderId;
        this.tenantId = tenantId;
        this.storeId = storeId;
        this.factoryId = factoryId;
        this.productId = productId;
        this.materialId = materialId;
        this.colorId = colorId;
        this.classificationId = classificationId;
        this.productStatus = productStatus;
        this.nickname = nickname;
        this.stoneIds = stoneIds;
    }
}
