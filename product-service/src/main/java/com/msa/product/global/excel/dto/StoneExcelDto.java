package com.msa.product.global.excel.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StoneExcelDto {

    private String stoneId;
    private String stoneName;
    private String stoneType;
    private String stoneShape;
    private String stoneSize;
    private String stoneWeight;
    private Integer stonePurchasePrice;
    private String stoneNote;
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
