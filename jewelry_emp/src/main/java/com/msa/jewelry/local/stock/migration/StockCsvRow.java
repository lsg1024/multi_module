package com.msa.jewelry.local.stock.migration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 재고 CSV 한 행을 담는 DTO.
 * CSV 헤더: No,매장,매대구분,원재고구분,현재고구분,등록일,변경일,시리얼,접수번호,모델,모델분류,재질,색상,중심스톤,보조스톤,사이즈,기타설명,
 *          단가상품,총중량,금중량,알중량,수량,메인알수/EA,보조알수/EA,공임고정,기본공임/EA,추가공임/EA,중심공임/EA,보조공임/EA,공임합계,매입처,매입해리,기본공임원가/EA,스톤원가/EA,원가합계
 */
@Getter
@Setter
@Schema(description = "재고 마이그레이션 CSV 한 행 — 레거시 재고 CSV 의 34개 컬럼을 그대로 매핑.")
public class StockCsvRow {
    @Schema(description = "CSV 순번", example = "1")
    private String no;                      // 0: CSV 순번
    @Schema(description = "매장 이름", example = "본점")
    private String storeName;               // 1: 매장
    @Schema(description = "매대 구분 (거래처 등급)", example = "A")
    private String storeGrade;              // 2: 매대구분
    @Schema(description = "원재고 구분 (주문/일반/수리)", example = "일반")
    private String sourceType;              // 3: 원재고구분 (주문/일반/수리)
    @Schema(description = "현재고 구분 (주문/판매/반납/삭제/수리/일반/대여)", example = "일반")
    private String currentStockType;        // 4: 현재고구분 (주문/판매/반납/삭제/수리/일반/대여)
    @Schema(description = "등록일 (문자열)", example = "2026-05-01")
    private String createdDate;             // 5: 등록일
    @Schema(description = "변경일 (문자열)", example = "2026-05-10")
    private String changedDate;             // 6: 변경일
    @Schema(description = "시리얼 번호", example = "SN-12345")
    private String serialNumber;            // 7: 시리얼
    @Schema(description = "접수번호 (레거시 flowCode)", example = "J1003775")
    private String receiptNumber;           // 8: 접수번호
    @Schema(description = "모델명", example = "다이아 1ct 반지")
    private String modelName;               // 9: 모델
    @Schema(description = "모델 분류", example = "반지")
    private String classification;          // 10: 모델분류
    @Schema(description = "재질", example = "18K")
    private String material;                // 11: 재질
    @Schema(description = "색상", example = "옐로우골드")
    private String color;                   // 12: 색상
    @Schema(description = "중심(메인) 스톤", example = "1.0ct VS1")
    private String mainStone;               // 13: 중심스톤
    @Schema(description = "보조 스톤", example = "0.05ct x 12")
    private String subStone;                // 14: 보조스톤
    @Schema(description = "사이즈", example = "15호")
    private String size;                    // 15: 사이즈
    @Schema(description = "재고 기타 설명 / 비고")
    private String stockNote;               // 16: 기타설명
    @Schema(description = "단가 상품 여부 (Y/N)", example = "N")
    private String unitProduct;             // 17: 단가상품
    @Schema(description = "총 무게 (g, 문자열)", example = "3.750")
    private String totalWeight;             // 18: 총중량
    @Schema(description = "금 무게 (g, 문자열)", example = "3.250")
    private String goldWeight;              // 19: 금중량
    @Schema(description = "알(스톤) 무게 (g, 문자열)", example = "0.500")
    private String stoneWeight;             // 20: 알중량
    @Schema(description = "수량", example = "1")
    private String quantity;                // 21: 수량
    @Schema(description = "메인 알수 / EA", example = "1")
    private String mainStoneQuantity;       // 22: 메인알수/EA
    @Schema(description = "보조 알수 / EA", example = "12")
    private String subStoneQuantity;        // 23: 보조알수/EA
    @Schema(description = "공임 고정 여부 (Y/N)", example = "Y")
    private String laborCostFixed;          // 24: 공임고정
    @Schema(description = "기본 공임 / EA", example = "120000")
    private String productLaborCost;        // 25: 기본공임/EA
    @Schema(description = "추가 공임 / EA", example = "20000")
    private String productAddLaborCost;     // 26: 추가공임/EA
    @Schema(description = "중심(메인) 스톤 공임 / EA", example = "200000")
    private String stoneMainLaborCost;      // 27: 중심공임/EA
    @Schema(description = "보조 스톤 공임 / EA", example = "50000")
    private String stoneSubLaborCost;       // 28: 보조공임/EA
    @Schema(description = "공임 합계", example = "390000")
    private String totalLaborCost;          // 29: 공임합계
    @Schema(description = "매입처(공장) 이름", example = "삼성공방")
    private String factoryName;             // 30: 매입처
    @Schema(description = "매입 해리(수수료)", example = "1.20")
    private String factoryHarry;            // 31: 매입해리
    @Schema(description = "기본 공임 원가 / EA", example = "100000")
    private String productPurchaseCost;     // 32: 기본공임원가/EA
    @Schema(description = "스톤 원가 / EA", example = "150000")
    private String totalStonePurchaseCost;  // 33: 스톤원가/EA
    // (34 컬럼 중 마지막 컬럼인 "원가합계"는 파싱 불필요)
}
