package com.msa.jewelry.global.batch.product.legacy;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStoneCsvRow {
    private String no;                 // A - 순서
    private String modelNumber;        // B - 모델번호 → Product.productName 기반 조회
    private String mainStone;          // C - 메인 ("메인" → MAIN_STONE = true)
    private String stoneName;          // D - 스톤종류 → Stone.stoneName 기반 조회
    private String stoneNote;          // E - 스톤비고 → PRODUCT_STONE_NOTE
    private String includeQuantity;    // F - 알개수적용 ("Y" → INCLUDE_QUANTITY = true)
    private String stoneQuantity;      // G - 알개수 → STONE_QUANTITY
    private String includeStone;       // H - 알차감적용 ("Y" → INCLUDE_STONE = true)
    private String stoneWeight;        // I - 개당스톤중량
    private String stonePurchasePrice; // J - 스톤원단가
    private String includePrice;       // K - 공임적용여부 ("Y" → INCLUDE_PRICE = true)
    private String gradePrice1;        // L - 스톤공임단가1
    private String gradePrice2;        // M - 스톤공임단가2
    private String gradePrice3;        // N - 스톤공임단가3
    private String gradePrice4;        // O - 스톤공임단가4
}
