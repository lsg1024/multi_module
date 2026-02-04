package com.msa.product.global.excel.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CatalogExcelDto {

    private String productId;
    private String productName;
    private String productWeight;
    private String setTypeName;
    private String classificationName;
    private String materialName;
    private String colorName;
    private String relatedNumber;
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
