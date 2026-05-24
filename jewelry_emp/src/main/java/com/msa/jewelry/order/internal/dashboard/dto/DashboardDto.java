package com.msa.jewelry.order.internal.dashboard.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "대시보드 응답 DTO 컨테이너 — 매출/재고/대여/미수 등 집계 결과를 담는 inner DTO 모음.")
public class DashboardDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "재질별 재고 요약 — 재질(material) 단위 총중량/건수 집계.")
    public static class MaterialStockSummary {
        @Schema(description = "재질 이름", example = "18K")
        private String material;
        @Schema(description = "재질별 총 무게 (g, 문자열)", example = "152.350")
        private String totalWeight;
        @Schema(description = "재질별 재고 건수", example = "42")
        private Long count;

        @QueryProjection
        public MaterialStockSummary(String material, BigDecimal totalWeight, Long count) {
            this.material = material;
            this.totalWeight = totalWeight != null ? totalWeight.toPlainString() : "0";
            this.count = count;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "재고 인기 상품 Top — 재고 보유량 기준 상품 랭킹.")
    public static class StockModelTop {
        @Schema(description = "상품 이름", example = "다이아 1ct 반지")
        private String productName;
        @Schema(description = "재고 보유 수량", example = "15")
        private Long stockCount;

        @QueryProjection
        public StockModelTop(String productName, Long stockCount) {
            this.productName = productName;
            this.stockCount = stockCount;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "판매 인기 상품 Top — 판매 건수 기준 상품 랭킹.")
    public static class SaleModelTop {
        @Schema(description = "상품 이름", example = "다이아 1ct 반지")
        private String productName;
        @Schema(description = "판매 건수", example = "30")
        private Long saleCount;

        @QueryProjection
        public SaleModelTop(String productName, Long saleCount) {
            this.productName = productName;
            this.saleCount = saleCount;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처(매장)별 공임 Top — 공임 합계 기준 거래처 랭킹.")
    public static class StoreLaborCostTop {
        @Schema(description = "거래처 이름", example = "ABC 보석상")
        private String storeName;
        @Schema(description = "거래처별 총 공임", example = "12500000")
        private Long totalLaborCost;

        @QueryProjection
        public StoreLaborCostTop(String storeName, Long totalLaborCost) {
            this.storeName = storeName;
            this.totalLaborCost = totalLaborCost;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "재고 상세 — 대시보드 재고 목록 한 행.")
    public static class StockDetail {
        @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
        private String flowCode;
        @Schema(description = "등록 일시 (포맷된 문자열)", example = "2026-05-16 14:30:00")
        private String createAt;
        @Schema(description = "상품 이름", example = "다이아 1ct 반지")
        private String productName;
        @Schema(description = "재질 이름", example = "18K")
        private String materialName;
        @Schema(description = "색상 이름", example = "옐로우골드")
        private String colorName;
        @Schema(description = "상품 사이즈", example = "15호")
        private String size;
        @Schema(description = "금 무게 (g, 문자열)", example = "3.250")
        private String goldWeight;
        @Schema(description = "스톤 무게 (g, 문자열)", example = "0.500")
        private String stoneWeight;
        @Schema(description = "제조사 이름", example = "삼성공방")
        private String factoryName;
        @Schema(description = "거래처(매장) 이름", example = "ABC 보석상")
        private String storeName;
        @Schema(description = "재고 비즈니스 상태", example = "STOCK")
        private String orderStatus;

        @QueryProjection
        @Builder
        public StockDetail(String flowCode, String createAt, String productName, String materialName,
                           String colorName, String size, BigDecimal goldWeight, BigDecimal stoneWeight,
                           String factoryName, String storeName, String orderStatus) {
            this.flowCode = flowCode;
            this.createAt = createAt;
            this.productName = productName;
            this.materialName = materialName;
            this.colorName = colorName;
            this.size = size;
            this.goldWeight = goldWeight != null ? goldWeight.toPlainString() : "0";
            this.stoneWeight = stoneWeight != null ? stoneWeight.toPlainString() : "0";
            this.factoryName = factoryName;
            this.storeName = storeName;
            this.orderStatus = orderStatus;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "재고 검색 조건 — 대시보드 재고 목록 필터.")
    public static class StockSearchCondition {
        @Schema(description = "상품 이름 검색어", example = "반지")
        private String productName;
        @Schema(description = "재질 이름 필터", example = "18K")
        private String materialName;
        @Schema(description = "색상 이름 필터", example = "옐로우골드")
        private String colorName;
        @Schema(description = "거래처(매장) 이름 필터", example = "ABC 보석상")
        private String storeName;

        @Builder
        public StockSearchCondition(String productName, String materialName, String colorName, String storeName) {
            this.productName = productName;
            this.materialName = materialName;
            this.colorName = colorName;
            this.storeName = storeName;
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "재고 필터 옵션 — 검색 UI 에 노출할 가능한 선택지 목록.")
    public static class StockFilterOption {
        @Schema(description = "재질 옵션 목록")
        private java.util.List<String> materials;
        @Schema(description = "색상 옵션 목록")
        private java.util.List<String> colors;
        @Schema(description = "거래처 옵션 목록")
        private java.util.List<String> stores;

        @Builder
        public StockFilterOption(java.util.List<String> materials, java.util.List<String> colors, java.util.List<String> stores) {
            this.materials = materials;
            this.colors = colors;
            this.stores = stores;
        }
    }

    /**
     * 1. 당월 매출 현황 DTO
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "당월 매출 현황 — 매출 순금/공임/마진 집계.")
    public static class MonthlySalesSummary {
        @Schema(description = "매출 순금 (g, 문자열)", example = "152.350")
        private String salePureGold;      // 매출 순금
        @Schema(description = "매출 공임 (원)", example = "12500000")
        private Long saleLaborCost;       // 매출 공임
        @Schema(description = "마진 공임 (원) — 매출 공임 - 매입 원가", example = "4200000")
        private Long marginLaborCost;     // 마진 공임

        @QueryProjection
        @Builder
        public MonthlySalesSummary(BigDecimal salePureGold, Long saleLaborCost, Long marginLaborCost) {
            this.salePureGold = salePureGold != null ? salePureGold.toPlainString() : "0";
            this.saleLaborCost = saleLaborCost != null ? saleLaborCost : 0L;
            this.marginLaborCost = marginLaborCost != null ? marginLaborCost : 0L;
        }
    }

    /**
     * 1-2. 거래처별 거래 통계 DTO
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처별 거래 통계 — 한 거래처의 판매/반품/DC/결제/매입/마진 집계.")
    public static class StoreTradeStatistics {
        @Schema(description = "거래처 ID", example = "10")
        private Long storeId;
        @Schema(description = "거래처 이름", example = "ABC 보석상")
        private String storeName;
        // 판매 상품
        @Schema(description = "판매 공임 합계 (원)", example = "5000000")
        private Long saleLaborCost;
        @Schema(description = "판매 순금 합계 (g, 문자열)", example = "85.250")
        private String salePureGold;
        @Schema(description = "판매 건수", example = "12")
        private Long saleCount;
        @Schema(description = "판매 메인 스톤 개수", example = "8")
        private Long saleMainStoneCount;
        @Schema(description = "판매 보조 스톤 개수", example = "24")
        private Long saleAssistStoneCount;
        // 반품 상품
        @Schema(description = "반품 공임 합계 (원)", example = "300000")
        private Long returnLaborCost;
        @Schema(description = "반품 순금 합계 (g, 문자열)", example = "5.000")
        private String returnPureGold;
        @Schema(description = "반품 건수", example = "1")
        private Long returnCount;
        // DC
        @Schema(description = "DC(할인) 공임 합계 (원)", example = "100000")
        private Long dcLaborCost;
        @Schema(description = "DC(할인) 순금 합계 (g, 문자열)", example = "0")
        private String dcPureGold;
        @Schema(description = "DC 건수", example = "2")
        private Long dcCount;
        // 매출 (판매 - 반품 - DC)
        @Schema(description = "총 매출 공임 (판매 - 반품 - DC, 원)", example = "4600000")
        private Long totalSaleLaborCost;
        @Schema(description = "총 매출 순금 (판매 - 반품 - DC, g 문자열)", example = "80.250")
        private String totalSalePureGold;
        @Schema(description = "총 매출 건수", example = "9")
        private Long totalSaleCount;
        // 실 입금 (결제)
        @Schema(description = "실 입금 금액 (원)", example = "3000000")
        private Long paymentAmount;
        @Schema(description = "실 입금 순금 (g, 문자열)", example = "40.500")
        private String paymentPureGold;
        // 매입원가
        @Schema(description = "매입 원가 합계 (원)", example = "2500000")
        private Long purchaseCost;
        // 마진 (매출 - 매입)
        @Schema(description = "마진 공임 (총 매출 - 매입, 원)", example = "2100000")
        private Long marginLaborCost;
        @Schema(description = "마진 순금 (g, 문자열)", example = "80.250")
        private String marginPureGold;

        @Builder
        public StoreTradeStatistics(Long storeId, String storeName,
                                    Long saleLaborCost, BigDecimal salePureGold, Long saleCount, Long saleMainStoneCount, Long saleAssistStoneCount,
                                    Long returnLaborCost, BigDecimal returnPureGold, Long returnCount,
                                    Long dcLaborCost, BigDecimal dcPureGold, Long dcCount,
                                    Long paymentAmount, BigDecimal paymentPureGold,
                                    Long purchaseCost) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.saleLaborCost = saleLaborCost != null ? saleLaborCost : 0L;
            this.salePureGold = salePureGold != null ? salePureGold.toPlainString() : "0";
            this.saleCount = saleCount != null ? saleCount : 0L;
            this.saleMainStoneCount = saleMainStoneCount != null ? saleMainStoneCount : 0L;
            this.saleAssistStoneCount = saleAssistStoneCount != null ? saleAssistStoneCount : 0L;
            this.returnLaborCost = returnLaborCost != null ? returnLaborCost : 0L;
            this.returnPureGold = returnPureGold != null ? returnPureGold.toPlainString() : "0";
            this.returnCount = returnCount != null ? returnCount : 0L;
            this.dcLaborCost = dcLaborCost != null ? dcLaborCost : 0L;
            this.dcPureGold = dcPureGold != null ? dcPureGold.toPlainString() : "0";
            this.dcCount = dcCount != null ? dcCount : 0L;

            // 매출 계산 (판매 - 반품 - DC)
            this.totalSaleLaborCost = this.saleLaborCost - this.returnLaborCost - this.dcLaborCost;
            BigDecimal totalPureGold = (salePureGold != null ? salePureGold : BigDecimal.ZERO)
                    .subtract(returnPureGold != null ? returnPureGold : BigDecimal.ZERO)
                    .subtract(dcPureGold != null ? dcPureGold : BigDecimal.ZERO);
            this.totalSalePureGold = totalPureGold.toPlainString();
            this.totalSaleCount = this.saleCount - this.returnCount - this.dcCount;

            this.paymentAmount = paymentAmount != null ? paymentAmount : 0L;
            this.paymentPureGold = paymentPureGold != null ? paymentPureGold.toPlainString() : "0";
            this.purchaseCost = purchaseCost != null ? purchaseCost : 0L;

            // 마진 계산 (매출공임 - 매입원가)
            this.marginLaborCost = this.totalSaleLaborCost - this.purchaseCost;
            this.marginPureGold = this.totalSalePureGold;
        }
    }

    /**
     * 2. 현 미수 현황 DTO
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "현재 미수금 현황 — 미수 순금/금액 집계.")
    public static class ReceivableSummary {
        @Schema(description = "미수 순금 (g, 문자열)", example = "25.500")
        private String totalPureGold;     // 미수 순금
        @Schema(description = "미수 금액 (원)", example = "3500000")
        private Long totalAmount;         // 미수 금액

        @QueryProjection
        @Builder
        public ReceivableSummary(BigDecimal totalPureGold, Long totalAmount) {
            this.totalPureGold = totalPureGold != null ? totalPureGold.toPlainString() : "0";
            this.totalAmount = totalAmount != null ? totalAmount : 0L;
        }
    }

    /**
     * 3. 현 대여 현황 DTO
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "현재 대여(렌탈) 현황 — 대여 순금/공임/건수.")
    public static class RentalSummary {
        @Schema(description = "대여 순금 (g, 문자열)", example = "12.500")
        private String totalPureGold;     // 대여 순금
        @Schema(description = "대여 공임 (원)", example = "1500000")
        private Long totalLaborCost;      // 대여 공임
        @Schema(description = "대여 건수", example = "5")
        private Long totalCount;          // 대여 수량

        @QueryProjection
        @Builder
        public RentalSummary(BigDecimal totalPureGold, Long totalLaborCost, Long totalCount) {
            this.totalPureGold = totalPureGold != null ? totalPureGold.toPlainString() : "0";
            this.totalLaborCost = totalLaborCost != null ? totalLaborCost : 0L;
            this.totalCount = totalCount != null ? totalCount : 0L;
        }
    }

    /**
     * 3-2. 대여 현황 상세보기 DTO
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "대여 현황 상세 — 거래처별 대여 한 행. 첫/마지막 대여일과 연락처 포함.")
    public static class RentalDetail {
        @Schema(description = "거래처 ID", example = "10")
        private Long storeId;
        @Schema(description = "거래처 이름", example = "ABC 보석상")
        private String storeName;
        @Schema(description = "대표 전화번호", example = "010-1234-5678")
        private String phoneNumber;
        @Schema(description = "보조 연락처 1", example = "02-123-4567")
        private String contactNumber1;
        @Schema(description = "보조 연락처 2", example = "031-987-6543")
        private String contactNumber2;
        @Schema(description = "대여 순금 (g, 문자열)", example = "5.250")
        private String pureGold;
        @Schema(description = "대여 공임 (원)", example = "300000")
        private Long laborCost;
        @Schema(description = "대여 건수", example = "2")
        private Long count;
        @Schema(description = "첫 대여 일자", example = "2026-04-01")
        private String firstRentalDate;
        @Schema(description = "마지막 대여 일자", example = "2026-05-16")
        private String lastRentalDate;

        @QueryProjection
        @Builder
        public RentalDetail(Long storeId, String storeName, BigDecimal pureGold, Long laborCost, Long count,
                            String firstRentalDate, String lastRentalDate) {
            this.storeId = storeId;
            this.storeName = storeName;
            this.pureGold = pureGold != null ? pureGold.toPlainString() : "0";
            this.laborCost = laborCost != null ? laborCost : 0L;
            this.count = count != null ? count : 0L;
            this.firstRentalDate = firstRentalDate;
            this.lastRentalDate = lastRentalDate;
        }

        public void setPhoneNumbers(String phoneNumber, String contactNumber1, String contactNumber2) {
            this.phoneNumber = phoneNumber;
            this.contactNumber1 = contactNumber1;
            this.contactNumber2 = contactNumber2;
        }
    }

    /**
     * 4. 매입처 미결제 현황 DTO
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "매입처(공장) 미결제 현황 — 미결제 순금/금액 집계.")
    public static class FactoryUnpaidSummary {
        @Schema(description = "미결제 순금 (g, 문자열)", example = "18.250")
        private String totalPureGold;     // 미결제 순금
        @Schema(description = "미결제 금액 (원)", example = "2200000")
        private Long totalAmount;         // 미결제 금액

        @QueryProjection
        @Builder
        public FactoryUnpaidSummary(BigDecimal totalPureGold, Long totalAmount) {
            this.totalPureGold = totalPureGold != null ? totalPureGold.toPlainString() : "0";
            this.totalAmount = totalAmount != null ? totalAmount : 0L;
        }
    }

    /**
     * 거래처별 거래 통계 검색 조건 DTO
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처별 거래 통계 검색 조건 — 기간/거래처/등급/거래형태 등 필터.")
    public static class StoreStatisticsSearchCondition {
        @Schema(description = "시작 일자 (yyyy-MM-dd)", example = "2026-05-01")
        private String start;           // 시작일 (yyyy-MM-dd)
        @Schema(description = "종료 일자 (yyyy-MM-dd)", example = "2026-05-31")
        private String end;             // 종료일 (yyyy-MM-dd)
        @Schema(description = "거래처 검색어", example = "ABC")
        private String storeName;       // 거래처 검색
        @Schema(description = "거래처 등급 필터", example = "A")
        private String storeGrade;      // 매장구분 (등급)
        @Schema(description = "거래 형태 필터 (예: 판매/대여)", example = "SALE")
        private String tradeType;       // 거래형태
        @Schema(description = "재질 필터", example = "18K")
        private String materialName;    // 재질구분
        @Schema(description = "분류 필터", example = "반지")
        private String classificationName; // 분류구분
        @Schema(description = "매입처(공장) 필터", example = "삼성공방")
        private String factoryName;     // 매입처구분
        @Schema(description = "등록 관리자 필터", example = "홍길동")
        private String createdBy;       // 관리자구분
        @Schema(description = "통계 유형 (STORE / FACTORY)", example = "STORE")
        private String statisticsType;  // 통계선택 (STORE, FACTORY)

        @Builder
        public StoreStatisticsSearchCondition(String start, String end, String storeName,
                                               String storeGrade, String tradeType,
                                               String materialName, String classificationName,
                                               String factoryName, String createdBy, String statisticsType) {
            this.start = start;
            this.end = end;
            this.storeName = storeName;
            this.storeGrade = storeGrade;
            this.tradeType = tradeType;
            this.materialName = materialName;
            this.classificationName = classificationName;
            this.factoryName = factoryName;
            this.createdBy = createdBy;
            this.statisticsType = statisticsType;
        }
    }

    /**
     * 거래처별 거래 통계 필터 옵션 DTO
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "거래처별 거래 통계 필터 옵션 — 검색 UI 에 노출할 가능한 선택지 목록.")
    public static class StoreStatisticsFilterOption {
        @Schema(description = "거래처 등급 옵션")
        private java.util.List<String> storeGrades;      // 매장구분 (등급) 옵션
        @Schema(description = "거래 형태 옵션")
        private java.util.List<String> tradeTypes;       // 거래형태 옵션
        @Schema(description = "재질 옵션")
        private java.util.List<String> materials;        // 재질구분 옵션
        @Schema(description = "분류 옵션")
        private java.util.List<String> classifications;  // 분류구분 옵션
        @Schema(description = "매입처(공장) 옵션")
        private java.util.List<String> factories;        // 매입처구분 옵션
        @Schema(description = "관리자 옵션")
        private java.util.List<String> managers;         // 관리자구분 옵션
        @Schema(description = "통계 유형 옵션")
        private java.util.List<String> statisticsTypes;  // 통계선택 옵션

        @Builder
        public StoreStatisticsFilterOption(java.util.List<String> storeGrades,
                                            java.util.List<String> tradeTypes,
                                            java.util.List<String> materials,
                                            java.util.List<String> classifications,
                                            java.util.List<String> factories,
                                            java.util.List<String> managers,
                                            java.util.List<String> statisticsTypes) {
            this.storeGrades = storeGrades;
            this.tradeTypes = tradeTypes;
            this.materials = materials;
            this.classifications = classifications;
            this.factories = factories;
            this.managers = managers;
            this.statisticsTypes = statisticsTypes;
        }
    }
}
