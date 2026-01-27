package com.msa.order.local.dashboard.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

public class DashboardDto {

    @Getter
    @NoArgsConstructor
    public static class MaterialStockSummary {
        private String material;
        private String totalWeight;
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
    public static class StockModelTop {
        private String productName;
        private Long stockCount;

        @QueryProjection
        public StockModelTop(String productName, Long stockCount) {
            this.productName = productName;
            this.stockCount = stockCount;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class SaleModelTop {
        private String productName;
        private Long saleCount;

        @QueryProjection
        public SaleModelTop(String productName, Long saleCount) {
            this.productName = productName;
            this.saleCount = saleCount;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class StoreLaborCostTop {
        private String storeName;
        private Long totalLaborCost;

        @QueryProjection
        public StoreLaborCostTop(String storeName, Long totalLaborCost) {
            this.storeName = storeName;
            this.totalLaborCost = totalLaborCost;
        }
    }

    @Getter
    @NoArgsConstructor
    public static class StockDetail {
        private String flowCode;
        private String createAt;
        private String productName;
        private String materialName;
        private String colorName;
        private String size;
        private String goldWeight;
        private String stoneWeight;
        private String factoryName;
        private String storeName;
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
    public static class StockSearchCondition {
        private String productName;
        private String materialName;
        private String colorName;
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
    public static class StockFilterOption {
        private java.util.List<String> materials;
        private java.util.List<String> colors;
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
    public static class MonthlySalesSummary {
        private String salePureGold;      // 매출 순금
        private Long saleLaborCost;       // 매출 공임
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
    public static class StoreTradeStatistics {
        private Long storeId;
        private String storeName;
        // 판매 상품
        private Long saleLaborCost;
        private String salePureGold;
        private Long saleCount;
        private Long saleMainStoneCount;
        private Long saleAssistStoneCount;
        // 반품 상품
        private Long returnLaborCost;
        private String returnPureGold;
        private Long returnCount;
        // DC
        private Long dcLaborCost;
        private String dcPureGold;
        private Long dcCount;
        // 매출 (판매 - 반품 - DC)
        private Long totalSaleLaborCost;
        private String totalSalePureGold;
        private Long totalSaleCount;
        // 실 입금 (결제)
        private Long paymentAmount;
        private String paymentPureGold;
        // 매입원가
        private Long purchaseCost;
        // 마진 (매출 - 매입)
        private Long marginLaborCost;
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
    public static class ReceivableSummary {
        private String totalPureGold;     // 미수 순금
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
    public static class RentalSummary {
        private String totalPureGold;     // 대여 순금
        private Long totalLaborCost;      // 대여 공임
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
    public static class RentalDetail {
        private Long storeId;
        private String storeName;
        private String phoneNumber;
        private String contactNumber1;
        private String contactNumber2;
        private String pureGold;
        private Long laborCost;
        private Long count;
        private String firstRentalDate;
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
    public static class FactoryUnpaidSummary {
        private String totalPureGold;     // 미결제 순금
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
    public static class StoreStatisticsSearchCondition {
        private String start;           // 시작일 (yyyy-MM-dd)
        private String end;             // 종료일 (yyyy-MM-dd)
        private String storeName;       // 거래처 검색
        private String storeGrade;      // 매장구분 (등급)
        private String tradeType;       // 거래형태
        private String materialName;    // 재질구분
        private String classificationName; // 분류구분
        private String factoryName;     // 매입처구분
        private String createdBy;       // 관리자구분
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
    public static class StoreStatisticsFilterOption {
        private java.util.List<String> storeGrades;      // 매장구분 (등급) 옵션
        private java.util.List<String> tradeTypes;       // 거래형태 옵션
        private java.util.List<String> materials;        // 재질구분 옵션
        private java.util.List<String> classifications;  // 분류구분 옵션
        private java.util.List<String> factories;        // 매입처구분 옵션
        private java.util.List<String> managers;         // 관리자구분 옵션
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
