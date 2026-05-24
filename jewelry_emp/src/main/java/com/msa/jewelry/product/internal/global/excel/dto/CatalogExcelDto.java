package com.msa.jewelry.product.internal.global.excel.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "카탈로그 엑셀 다운로드용 DTO — 상품 목록 엑셀의 한 행")
public class CatalogExcelDto {

    @Schema(description = "상품 ID", example = "1001")
    private String productId;
    @Schema(description = "상품명", example = "프로포즈 솔리테어 반지")
    private String productName;
    @Schema(description = "상품 무게(그램)", example = "3.50")
    private String productWeight;
    @Schema(description = "세트 타입명", example = "단품")
    private String setTypeName;
    @Schema(description = "분류명", example = "반지")
    private String classificationName;
    @Schema(description = "재질명", example = "18K")
    private String materialName;
    @Schema(description = "색상명", example = "옐로골드")
    private String colorName;
    @Schema(description = "관련번호", example = "R-2024-SERIES")
    private String relatedNumber;
    @Schema(description = "상품 비고", example = "메인 0.3ct")
    private String productNote;

    @Builder
    @QueryProjection
    public CatalogExcelDto(String productId, String productName, String productWeight,
                           String setTypeName, String classificationName, String materialName,
                           String colorName, String relatedNumber, String productNote) {
        this.productId = productId;
        this.productName = productName;
        this.productWeight = productWeight;
        this.setTypeName = setTypeName;
        this.classificationName = classificationName;
        this.materialName = materialName;
        this.colorName = colorName;
        this.relatedNumber = relatedNumber;
        this.productNote = productNote != null ? productNote : "";
    }
}
