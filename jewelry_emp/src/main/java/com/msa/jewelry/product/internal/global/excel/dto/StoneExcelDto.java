package com.msa.jewelry.product.internal.global.excel.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스톤 엑셀 다운로드용 DTO — 스톤 목록 엑셀의 한 행")
public class StoneExcelDto {

    @Schema(description = "스톤 ID", example = "301")
    private String stoneId;
    @Schema(description = "스톤명", example = "다이아 라운드 0.3ct")
    private String stoneName;
    @Schema(description = "스톤 타입명", example = "다이아몬드")
    private String stoneType;
    @Schema(description = "스톤 모양명", example = "라운드")
    private String stoneShape;
    @Schema(description = "스톤 사이즈(표기)", example = "0.3ct")
    private String stoneSize;
    @Schema(description = "스톤 무게", example = "0.30")
    private String stoneWeight;
    @Schema(description = "스톤 매입 단가 (원)", example = "150000")
    private Integer stonePurchasePrice;
    @Schema(description = "스톤 비고", example = "VS1 등급")
    private String stoneNote;
    @Schema(description = "해당 스톤을 사용 중인 상품 수", example = "12")
    private Integer productCount;

    @Builder
    @QueryProjection
    public StoneExcelDto(String stoneId, String stoneName, String stoneType, String stoneShape,
                         String stoneSize, String stoneWeight, Integer stonePurchasePrice,
                         String stoneNote, Integer productCount) {
        this.stoneId = stoneId;
        this.stoneName = stoneName;
        this.stoneType = stoneType != null ? stoneType : "";
        this.stoneShape = stoneShape != null ? stoneShape : "";
        this.stoneSize = stoneSize != null ? stoneSize : "";
        this.stoneWeight = stoneWeight != null ? stoneWeight : "";
        this.stonePurchasePrice = stonePurchasePrice != null ? stonePurchasePrice : 0;
        this.stoneNote = stoneNote != null ? stoneNote : "";
        this.productCount = productCount != null ? productCount : 0;
    }
}
