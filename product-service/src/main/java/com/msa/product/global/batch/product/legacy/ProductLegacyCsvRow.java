package com.msa.product.global.batch.product.legacy;

import lombok.Getter;
import lombok.Setter;

/**
 * 레거시 기본정보 CSV 행 매핑 DTO.
 *
 * CSV 컬럼 순서:
 * A=No, B=등록일, C=모델번호, D=제조사, E=제조번호, F=세트구분,
 * G=모델분류, H=기본재질, I=기본중량, J=공개여부, K=단종여부, L=단가제,
 * M=비고사항, N=기본색상, O=기본공임원가, P=기본공임1등급,
 * Q=기본공임2등급, R=기본공임3등급, S=기본공임4등급, T=공임설명
 */
@Getter
@Setter
public class ProductLegacyCsvRow {
    private String no;              // A - No
    private String registerDate;    // B - 등록일
    private String modelNumber;     // C - 모델번호 → PRODUCT_NAME
    private String manufacturer;    // D - 제조사 → FACTORY_NAME
    private String manufacturingNo; // E - 제조번호 → PRODUCT_FACTORY_NAME
    private String setType;         // F - 세트구분 → SET_TYPE_NAME
    private String classification;  // G - 모델분류 → CLASSIFICATION_NAME
    private String material;        // H - 기본재질 → MATERIAL_NAME
    private String standardWeight;  // I - 기본중량 → STANDARD_WEIGHT
    private String isPublic;        // J - 공개여부
    private String discontinued;    // K - 단종여부 (Y이면 저장 안함)
    private String unitPrice;       // L - 단가제
    private String note;            // M - 비고사항 → PRODUCT_NOTE
    private String defaultColor;    // N - 기본색상 → COLOR_NAME
    private String purchasePrice;   // O - 기본공임원가 → PRODUCT_PURCHASE_PRICE
    private String grade1LaborCost; // P - 기본공임1등급 → LABOR_COST (GRADE_1)
    private String grade2LaborCost; // Q - 기본공임2등급 → LABOR_COST (GRADE_2)
    private String grade3LaborCost; // R - 기본공임3등급 → LABOR_COST (GRADE_3)
    private String grade4LaborCost; // S - 기본공임4등급 → LABOR_COST (GRADE_4)
    private String laborCostNote;   // T - 공임설명 → PolicyGroup.NOTE
}
