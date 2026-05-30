package com.msa.jewelry.local.order.dto;

import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.order.entity.order_enum.ProductStatus;
import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "주문 목록 쿼리 프로젝션 DTO — Querydsl 로 주문 목록을 한 행씩 가져오는 read-model.")
public class OrderQueryDto {
    @Schema(description = "상품 ID", example = "501")
    private Long productId;
    @Schema(description = "주문 접수 일시 (문자열)", example = "2026-05-16 14:30")
    private String createAt;
    @Schema(description = "출고 예정 일시 (문자열)", example = "2026-05-20 10:00")
    private String shippingAt;
    @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
    private String flowCode;
    @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
    private String storeName;
    @Schema(description = "상품 이름", example = "다이아 1ct 반지")
    private String productName;
    @Schema(description = "상품 제조사 표기명 (스냅샷)", example = "삼성공방")
    private String productFactoryName;
    @Schema(description = "재질 이름", example = "18K")
    private String materialName;
    @Schema(description = "색상 이름", example = "옐로우골드")
    private String colorName;
    @Schema(description = "세트 타입 이름", example = "단품")
    private String setType;
    @Schema(description = "상품 사이즈", example = "15호")
    private String productSize;
    @Schema(description = "주문에 연결된 재고 수량", example = "1")
    private Integer stockQuantity;
    @Schema(description = "메인 스톤 비고", example = "1.0ct VS1")
    private String mainStoneNote;
    @Schema(description = "보조 스톤 비고", example = "0.05ct x 12")
    private String assistanceStoneNote;
    @Schema(description = "보조석 포함 여부", example = "true")
    private boolean assistantStone;
    @Schema(description = "보조석 이름", example = "큐빅")
    private String assistantStoneName;
    @Schema(description = "보조석 생성 일시 (문자열)", example = "2026-05-16T14:30:00")
    private String assistantStoneCreateAt;
    @Schema(description = "연결된 재고 flowCode 목록")
    private List<String> stockFlowCodes;
    @Schema(description = "주문 비고", example = "포장 정중하게")
    private String orderNote;
    @Schema(description = "제조사 이름", example = "삼성공방")
    private String factoryName;
    @Schema(description = "우선순위 이름", example = "일반")
    private String priority;
    @Schema(description = "상품 진행 상태")
    private ProductStatus productStatus;
    @Schema(description = "주문 비즈니스 상태")
    private OrderStatus orderStatus;

    public void setStockFlowCodes(List<String> stockFlowCodes) {
        this.stockFlowCodes = stockFlowCodes;
    }

    @QueryProjection
    public OrderQueryDto(Long productId, String createAt, String shippingAt, String flowCode, String storeName, String productName, String productFactoryName, String materialName, String colorName, String setType, String productSize, Integer stockQuantity, String mainStoneNote, String assistanceStoneNote, boolean assistantStone, String assistantStoneName, String assistantStoneCreateAt, String orderNote, String factoryName, String priority, ProductStatus productStatus, OrderStatus orderStatus) {
        this.productId = productId;
        this.createAt = createAt;
        this.shippingAt = shippingAt;
        this.flowCode = flowCode;
        this.storeName = storeName;
        this.productName = productName;
        this.productFactoryName = productFactoryName;
        this.materialName = materialName;
        this.colorName = colorName;
        this.setType = setType;
        this.productSize = productSize;
        this.stockQuantity = stockQuantity;
        this.mainStoneNote = mainStoneNote;
        this.assistanceStoneNote = assistanceStoneNote;
        this.assistantStone = assistantStone;
        this.assistantStoneName = assistantStoneName;
        this.assistantStoneCreateAt = assistantStoneCreateAt;
        this.orderNote = orderNote;
        this.factoryName = factoryName;
        this.priority = priority;
        this.productStatus = productStatus;
        this.orderStatus = orderStatus;
    }
}
