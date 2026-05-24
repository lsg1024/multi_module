package com.msa.jewelry.order.internal.global.kafka_dto_legacy;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "주문 비동기 처리 요청 페이로드 (kafka legacy → 모놀로식 직접 호출). 주문 생성/이력 적재용.")
public class OrderAsyncRequested {
    @Schema(description = "멱등성 키 (중복 처리 방지)", example = "evt_abc123")
    private String eventId;       // 멱등 처리용
    @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
    private Long flowCode;
    @Schema(description = "테넌트 ID", example = "tenant-001")
    private String tenantId;
    @Schema(description = "인증 토큰", example = "eyJhbGciOi...")
    private String token;
    @Schema(description = "거래처(매장) ID", example = "10")
    private Long storeId;
    @Schema(description = "제조사 ID", example = "5")
    private Long factoryId;
    @Schema(description = "상품 ID", example = "501")
    private Long productId;
    @Schema(description = "재질 ID", example = "1")
    private Long materialId;
    @Schema(description = "색상 ID", example = "3")
    private Long colorId;
    @Schema(description = "보조석 포함 여부", example = "false")
    private boolean assistantStone;
    @Schema(description = "보조석 ID", example = "10")
    private Long assistantStoneId;
    @Schema(description = "보조석 생성 일시", example = "2026-05-16T14:30:00")
    private LocalDateTime assistantStoneCreateAt;
    @Schema(description = "주문에 포함된 스톤 ID 목록")
    private List<Long> stoneIds;
    @Schema(description = "주문 비즈니스 상태", example = "WAIT")
    private String orderStatus;

    @Schema(description = "처리자 닉네임", example = "홍길동")
    private String nickname;

    @Builder
    public OrderAsyncRequested(String eventId, Long flowCode, String tenantId, String token, Long storeId, Long factoryId, Long productId, Long materialId, Long colorId, boolean assistantStone, Long assistantStoneId, LocalDateTime assistantStoneCreateAt, List<Long> stoneIds, String orderStatus, String nickname) {
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
        this.stoneIds = stoneIds;
        this.orderStatus = orderStatus;
        this.nickname = nickname;
    }
}
