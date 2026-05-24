package com.msa.jewelry.product.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 다른 모듈이 상품에 매핑된 스톤 정보를 조회할 때 사용하는 view.
 *
 * <p>기존 ProductClient.getProductStonesByName 의 동기 등가물.
 */
@Schema(description = "상품-스톤 매핑 view — 상품에 들어간 스톤의 가격·옵션 포함 정보")
public record ProductStoneView(
        @Schema(description = "스톤 ID", example = "301") String stoneId,
        @Schema(description = "스톤명", example = "다이아 라운드 0.3ct") String stoneName,
        @Schema(description = "스톤 무게(캐럿/그램)", example = "0.30") String stoneWeight,
        @Schema(description = "스톤 매입 단가 (원)", example = "150000") Integer purchaseCost,
        @Schema(description = "등급 기준 스톤 공임 (원)", example = "30000") Integer laborCost,
        @Schema(description = "스톤 개수(알 수)", example = "4") Integer quantity,
        @Schema(description = "메인 스톤 여부", example = "true") boolean mainStone,
        @Schema(description = "스톤 자체 가격 포함 여부", example = "true") boolean includeStone,
        @Schema(description = "스톤 개수 가격 포함 여부", example = "true") boolean includeQuantity,
        @Schema(description = "스톤 단가 가격 포함 여부", example = "true") boolean includePrice,
        @Schema(description = "상품-스톤 비고", example = "측면 보조석 4알") String stoneNote
) {
}
