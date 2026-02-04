package com.msa.order.local.stock.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class InventoryDto {

    /**
     * 재고 조사 목록 응답
     */
    @Getter
    @NoArgsConstructor
    public static class Response {
        private String flowCode;
        private String createAt;           // 상품 등록일 (최초생성일)
        private String stockCheckedAt;     // 재고 조사일
        private Boolean stockChecked;      // 재고 조사 여부
        private String originStatus;       // 원본 상태 (일반, 주문 등)
        private String orderStatus;        // 재고 구분 (STOCK, RENTAL, RETURN, ORDER)
        private String productName;        // 상품명
        private String materialName;       // 재질
        private String colorName;          // 색상
        private String goldWeight;         // 중량
        private Integer productPurchaseCost; // 매입가
        private Integer productLaborCost;    // 판매가

        @QueryProjection
        public Response(String flowCode, String createAt, String stockCheckedAt, Boolean stockChecked,
                        String originStatus, String orderStatus, String productName, String materialName,
                        String colorName, String goldWeight, Integer productPurchaseCost, Integer productLaborCost) {
            this.flowCode = flowCode;
            this.createAt = createAt;
            this.stockCheckedAt = stockCheckedAt;
            this.stockChecked = stockChecked;
            this.originStatus = originStatus;
            this.orderStatus = orderStatus;
            this.productName = productName;
            this.materialName = materialName;
            this.colorName = colorName;
            this.goldWeight = goldWeight;
            this.productPurchaseCost = productPurchaseCost;
            this.productLaborCost = productLaborCost;
        }

        public void updateStatus(String originStatus, String currentStatus) {
            this.originStatus = originStatus;
            this.orderStatus = currentStatus;
        }
    }

    /**
     * 재고 조사 결과 응답
     */
    @Getter
    @NoArgsConstructor
    public static class CheckResponse {
        private String flowCode;
        private String productName;
        private String status;           // SUCCESS, ALREADY_CHECKED, NOT_CHECKABLE
        private String message;
        private String stockCheckedAt;   // 재고 조사일

        @Builder
        public CheckResponse(String flowCode, String productName, String status, String message, String stockCheckedAt) {
            this.flowCode = flowCode;
            this.productName = productName;
            this.status = status;
            this.message = message;
            this.stockCheckedAt = stockCheckedAt;
        }
    }

    /**
     * 재고 조사 검색 조건
     */
    @Getter
    @NoArgsConstructor
    public static class Condition {
        private String searchField;      // 검색 필드 (productName, materialName, colorName 등)
        private String searchValue;      // 검색 값
        private String sortField;        // 정렬 필드
        private String sortOrder;        // 정렬 순서 (ASC, DESC)
        private String stockChecked;     // 재고 조사 여부 필터 (checked, unchecked, all)
        private String orderStatus;      // 재고 구분 필터 (STOCK, RENTAL, RETURN, ORDER)
        private String materialName;     // 재질 필터

        @Builder
        public Condition(String searchField, String searchValue, String sortField, String sortOrder,
                         String stockChecked, String orderStatus, String materialName) {
            this.searchField = searchField;
            this.searchValue = searchValue;
            this.sortField = sortField;
            this.sortOrder = sortOrder;
            this.stockChecked = stockChecked;
            this.orderStatus = orderStatus;
            this.materialName = materialName;
        }
    }

    /**
     * 재고 조사 초기화 응답
     */
    @Getter
    @NoArgsConstructor
    public static class ResetResponse {
        private Integer resetCount;
        private String message;

        @Builder
        public ResetResponse(Integer resetCount, String message) {
            this.resetCount = resetCount;
            this.message = message;
        }
    }

    /**
     * 재고 조사 통계 응답
     */
    @Getter
    @NoArgsConstructor
    public static class StatisticsResponse {
        private List<MaterialStatistics> uncheckedStatistics;  // 검사하지 않은 재질별 통계
        private List<MaterialStatistics> checkedStatistics;    // 검사한 재질별 통계
        private StatisticsSummary uncheckedSummary;            // 미검사 합계
        private StatisticsSummary checkedSummary;              // 검사 합계

        @Builder
        public StatisticsResponse(List<MaterialStatistics> uncheckedStatistics,
                                  List<MaterialStatistics> checkedStatistics,
                                  StatisticsSummary uncheckedSummary,
                                  StatisticsSummary checkedSummary) {
            this.uncheckedStatistics = uncheckedStatistics;
            this.checkedStatistics = checkedStatistics;
            this.uncheckedSummary = uncheckedSummary;
            this.checkedSummary = checkedSummary;
        }
    }

    /**
     * 재질별 통계
     */
    @Getter
    @NoArgsConstructor
    public static class MaterialStatistics {
        private String materialName;      // 재질명
        private String totalGoldWeight;   // 총 중량
        private Integer quantity;         // 수량
        private Long totalPurchaseCost;   // 총 매입가

        @QueryProjection
        public MaterialStatistics(String materialName, String totalGoldWeight,
                                  Integer quantity, Long totalPurchaseCost) {
            this.materialName = materialName;
            this.totalGoldWeight = totalGoldWeight;
            this.quantity = quantity;
            this.totalPurchaseCost = totalPurchaseCost;
        }
    }

    /**
     * 통계 합계
     */
    @Getter
    @NoArgsConstructor
    public static class StatisticsSummary {
        private String totalGoldWeight;   // 총 중량 합계
        private Integer totalQuantity;    // 총 수량 합계
        private Long totalPurchaseCost;   // 총 매입가 합계

        @Builder
        public StatisticsSummary(String totalGoldWeight, Integer totalQuantity, Long totalPurchaseCost) {
            this.totalGoldWeight = totalGoldWeight;
            this.totalQuantity = totalQuantity;
            this.totalPurchaseCost = totalPurchaseCost;
        }
    }
}
