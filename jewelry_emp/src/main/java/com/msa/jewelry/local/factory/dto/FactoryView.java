package com.msa.jewelry.local.factory.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 다른 모듈이 제조사를 참조할 때 사용하는 불변 view DTO.
 *
 * <p>{@code goldHarryLoss} 는 String — 원본 엔티티 그대로.
 */
@Schema(description = "제조사(공장) 정보 외부 노출용 view — 다른 모듈이 제조사를 참조할 때 사용")
public record FactoryView(
        @Schema(description = "제조사 PK", example = "5")
        Long factoryId,
        @Schema(description = "제조사명", example = "한빛제조사")
        String factoryName,
        @Schema(description = "제조사 등급 (A/B/C 등)", example = "A")
        String grade,
        @Schema(description = "금 손모율 — 가공 시 손실되는 금 비율 (문자열로 보관)", example = "1.5")
        String goldHarryLoss
) {
}
