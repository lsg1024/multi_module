package com.msa.order.local.order.migration;

import lombok.Getter;
import lombok.Setter;

/**
 * 주문리스트.csv 한 행을 담는 DTO.
 * CSV 헤더: No,접수번호,매장,구분,단계,제조사,제조번호,접수일,출고일,거 래 처,모델번호,분류,수량,재질,색상,중심스톤,보조스톤,사이즈,비고
 */
@Getter
@Setter
public class OrderCsvRow {
    private String no;               // CSV 순번 (저장 불필요)
    private String receiptNumber;    // 접수번호 (레거시 flowCode, 예: J1003775)
    private String shopName;         // 매장 (무시)
    private String category;         // 구분 (주문/수리)
    private String phase;            // 단계 (A/B/C/F/F1)
    private String manufacturer;     // 제조사
    private String manufacturingNo;  // 제조번호
    private String receiptDate;      // 접수일
    private String shippingDate;     // 출고일
    private String tradingPartner;   // 거 래 처 (Store 매핑용)
    private String modelNumber;      // 모델번호
    private String classification;   // 분류
    private String quantity;         // 수량
    private String material;         // 재질
    private String color;            // 색상
    private String mainStone;        // 중심스톤
    private String subStone;         // 보조스톤
    private String size;             // 사이즈
    private String note;             // 비고
}
