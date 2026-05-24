package com.msa.jewelry.order.internal.order.migration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

/**
 * 주문리스트.csv 한 행을 담는 DTO.
 * CSV 헤더: No,접수번호,매장,구분,단계,제조사,제조번호,접수일,출고일,거 래 처,모델번호,분류,수량,재질,색상,중심스톤,보조스톤,사이즈,비고
 */
@Getter
@Setter
@Schema(description = "주문 마이그레이션 CSV 한 행 — 레거시 주문리스트.csv 의 19개 컬럼을 그대로 매핑.")
public class OrderCsvRow {
    @Schema(description = "CSV 순번 (저장 불필요)", example = "1")
    private String no;               // CSV 순번 (저장 불필요)
    @Schema(description = "접수번호 (레거시 flowCode, J 접두어 포함)", example = "J1003775")
    private String receiptNumber;    // 접수번호 (레거시 flowCode, 예: J1003775)
    @Schema(description = "매장 이름 (마이그레이션 시 무시)", example = "본점")
    private String shopName;         // 매장 (무시)
    @Schema(description = "구분 (주문/수리)", example = "주문")
    private String category;         // 구분 (주문/수리)
    @Schema(description = "단계 (A/B/C/F/F1)", example = "A")
    private String phase;            // 단계 (A/B/C/F/F1)
    @Schema(description = "제조사 이름", example = "삼성공방")
    private String manufacturer;     // 제조사
    @Schema(description = "제조번호", example = "M-2026-0001")
    private String manufacturingNo;  // 제조번호
    @Schema(description = "접수 일자 (문자열)", example = "2026-05-16")
    private String receiptDate;      // 접수일
    @Schema(description = "출고 일자 (문자열)", example = "2026-05-20")
    private String shippingDate;     // 출고일
    @Schema(description = "거래처 (Store 매핑용)", example = "ABC 보석상")
    private String tradingPartner;   // 거 래 처 (Store 매핑용)
    @Schema(description = "모델번호 (상품 식별자)", example = "R-500")
    private String modelNumber;      // 모델번호
    @Schema(description = "분류", example = "반지")
    private String classification;   // 분류
    @Schema(description = "수량 (문자열)", example = "1")
    private String quantity;         // 수량
    @Schema(description = "재질", example = "18K")
    private String material;         // 재질
    @Schema(description = "색상", example = "옐로우골드")
    private String color;            // 색상
    @Schema(description = "중심(메인) 스톤", example = "1.0ct VS1")
    private String mainStone;        // 중심스톤
    @Schema(description = "보조 스톤", example = "0.05ct x 12")
    private String subStone;         // 보조스톤
    @Schema(description = "사이즈", example = "15호")
    private String size;             // 사이즈
    @Schema(description = "비고")
    private String note;             // 비고
}
