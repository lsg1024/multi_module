package com.msa.order.local.stock.migration;

import lombok.Getter;
import lombok.Setter;

/**
 * 재고 CSV 한 행을 담는 DTO.
 * CSV 헤더: No,매장,매대구분,원재고구분,현재고구분,등록일,변경일,시리얼,접수번호,모델,모델분류,재질,색상,중심스톤,보조스톤,사이즈,기타설명,
 *          단가상품,총중량,금중량,알중량,수량,메인알수/EA,보조알수/EA,공임고정,기본공임/EA,추가공임/EA,중심공임/EA,보조공임/EA,공임합계,매입처,매입해리,기본공임원가/EA,스톤원가/EA,원가합계
 */
@Getter
@Setter
public class StockCsvRow {
    private String no;                      // 0: CSV 순번
    private String storeName;               // 1: 매장
    private String storeGrade;              // 2: 매대구분
    private String sourceType;              // 3: 원재고구분 (주문/일반/수리)
    private String currentStockType;        // 4: 현재고구분 (주문/판매/반납/삭제/수리/일반/대여)
    private String createdDate;             // 5: 등록일
    private String changedDate;             // 6: 변경일
    private String serialNumber;            // 7: 시리얼
    private String receiptNumber;           // 8: 접수번호
    private String modelName;               // 9: 모델
    private String classification;          // 10: 모델분류
    private String material;                // 11: 재질
    private String color;                   // 12: 색상
    private String mainStone;               // 13: 중심스톤
    private String subStone;                // 14: 보조스톤
    private String size;                    // 15: 사이즈
    private String stockNote;               // 16: 기타설명
    private String unitProduct;             // 17: 단가상품
    private String totalWeight;             // 18: 총중량
    private String goldWeight;              // 19: 금중량
    private String stoneWeight;             // 20: 알중량
    private String quantity;                // 21: 수량
    private String mainStoneQuantity;       // 22: 메인알수/EA
    private String subStoneQuantity;        // 23: 보조알수/EA
    private String laborCostFixed;          // 24: 공임고정
    private String productLaborCost;        // 25: 기본공임/EA
    private String productAddLaborCost;     // 26: 추가공임/EA
    private String stoneMainLaborCost;      // 27: 중심공임/EA
    private String stoneSubLaborCost;       // 28: 보조공임/EA
    private String totalLaborCost;          // 29: 공임합계
    private String factoryName;             // 30: 매입처
    private String factoryHarry;            // 31: 매입해리
    private String productPurchaseCost;     // 32: 기본공임원가/EA
    private String totalStonePurchaseCost;  // 33: 스톤원가/EA
    // (34 컬럼 중 마지막 컬럼인 "원가합계"는 파싱 불필요)
}
