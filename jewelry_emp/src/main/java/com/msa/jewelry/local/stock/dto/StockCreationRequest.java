package com.msa.jewelry.local.stock.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Schema(description = "재고 생성 요청 페이로드. 주문 완료 시 재고 모듈에 전달.")
public class StockCreationRequest {
    @Schema(description = "멱등성 키", example = "evt_abc123")
    private String eventId;
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
    @Schema(description = "세트 타입 ID", example = "4")
    private Long setTypeId;
    @Schema(description = "분류 ID", example = "2")
    private Long classificationId;
    @Schema(description = "이력 발생자 닉네임", example = "홍길동")
    private String nickname;
    @Schema(description = "보조석 포함 여부", example = "false")
    private boolean assistantStone;
    @Schema(description = "보조석 ID", example = "10")
    private Long assistantStoneId;
    @Schema(description = "보조석 생성 일시", example = "2026-05-16T14:30:00")
    private LocalDateTime assistantStoneCreateAt;

    @Builder
    public StockCreationRequest(String eventId, Long flowCode, String tenantId, String token, Long storeId, Long factoryId, Long productId, Long materialId, Long colorId, Long setTypeId, Long classificationId, String nickname, boolean assistantStone, Long assistantStoneId, LocalDateTime assistantStoneCreateAt) {
        this.eventId = eventId;
        this.flowCode = flowCode;
        this.tenantId = tenantId;
        this.token = token;
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
