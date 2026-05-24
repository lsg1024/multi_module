package com.msa.jewelry.product.internal.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "상품 일괄 등록(배치) DTO — 외부 시스템/엑셀로부터의 대량 입력")
public class ProductBatchDto {

    @Schema(description = "제조사명 (ID 가 아닌 이름으로 매핑)", example = "한국주얼리")
    private String factoryName;
    @Schema(description = "제조사가 부여한 제품명", example = "R-2024-001")
    private String productFactoryName;
    @Schema(description = "상품명", example = "프로포즈 솔리테어 반지")
    private String productName;
    @Schema(description = "세트 타입명", example = "단품")
    private String setTypeName;
    @Schema(description = "분류명", example = "반지")
    private String classificationName;
    @Schema(description = "재질명", example = "18K")
    private String materialName; // materialId
    @Schema(description = "기준 무게 (그램)", example = "3.50")
    private String standardWeight;
    @Schema(description = "상품 비고", example = "메인 0.3ct")
    private String productNote;
    @Schema(description = "색상별 공임 정책 그룹 목록")
    private List<BatchPolicyGroup> productWorkGradePolicyGroupDto;
    @Schema(description = "상품 스톤 매핑 목록")
    private List<BatchStone> productStoneDtos;

    @Getter
    @NoArgsConstructor
    @Schema(description = "배치용 공임 정책 그룹 DTO — 색상명 기준")
    public static class BatchPolicyGroup {
        @Schema(description = "해당 색상의 상품 매입가 (원)", example = "300000")
        private Integer productPurchasePrice;
        @Schema(description = "색상명 (ID 가 아닌 이름으로 매핑)", example = "옐로골드")
        private String colorName;
        @Schema(description = "등급별 공임 정책 목록")
        private List<ProductWorkGradePolicyDto> policyDtos;
        @Schema(description = "그룹 비고", example = "옐로골드 전용")
        private String note;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "배치용 상품-스톤 매핑 DTO — 스톤명 기준")
    public static class BatchStone {
        @Schema(description = "스톤명 (ID 가 아닌 이름으로 매핑)", example = "다이아 라운드 0.3ct")
        private String stoneName;
        @Schema(description = "메인 스톤 여부", example = "true")
        private boolean mainStone;
        @Schema(description = "스톤 자체 가격 포함 여부", example = "true")
        private boolean includeStone;
        @Schema(description = "스톤 개수 가격 포함 여부", example = "true")
        private boolean includeQuantity;
        @Schema(description = "스톤 단가 가격 포함 여부", example = "true")
        private boolean includePrice;
        @Schema(description = "스톤 개수(알 수)", example = "4")
        private Integer stoneQuantity;
        @Schema(description = "상품-스톤 비고", example = "측면 보조석 4알")
        private String productStoneNote;
    }

}
