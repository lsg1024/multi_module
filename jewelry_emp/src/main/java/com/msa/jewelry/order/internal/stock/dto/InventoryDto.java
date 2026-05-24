package com.msa.jewelry.order.internal.stock.dto;

import com.querydsl.core.annotations.QueryProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "재고 조사(인벤토리) DTO 컨테이너 — 재고 조사 목록/검색/결과/통계용 inner DTO 모음.")
public class InventoryDto {

    /**
     * 재고 조사 목록 응답
     */
    @Getter
    @NoArgsConstructor
    @Schema(description = "재고 조사 목록 한 행 — 재고 한 건과 조사 상태를 함께 표현.")
    public static class Response {
        @Schema(description = "전역 흐름 코드 (TSID)", example = "445823472384938240")
        private String flowCode;
        @Schema(description = "상품 등록일 (최초 생성일, 문자열)", example = "2026-05-01 10:00")
        private String createAt;           // 상품 등록일 (최초생성일)
        @Schema(description = "재고 조사일 (문자열)", example = "2026-05-16 10:00")
        private String stockCheckedAt;     // 재고 조사일
        @Schema(description = "재고 조사 완료 여부", example = "true")
        private Boolean stockChecked;      // 재고 조사 여부
        @Schema(description = "원본 상태 (일반/주문 등)", example = "일반")
        private String originStatus;       // 원본 상태 (일반, 주문 등)
        @Schema(description = "재고 구분 (STOCK/RENTAL/RETURN/ORDER)", example = "STOCK")
        private String orderStatus;        // 재고 구분 (STOCK, RENTAL, RETURN, ORDER)
        @Schema(description = "상품 이름", example = "다이아 1ct 반지")
        private String productName;        // 상품명
        @Schema(description = "재질 이름", example = "18K")
        private String materialName;       // 재질
        @Schema(description = "색상 이름", example = "옐로우골드")
        private String colorName;          // 색상
        @Schema(description = "금 무게 (g, 문자열)", example = "3.250")
        private String goldWeight;         // 중량
        @Schema(description = "매입 비용 (원)", example = "500000")
        private Integer productPurchaseCost; // 매입가
        @Schema(description = "판매가(공임, 원)", example = "120000")
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
    @Schema(description = "재고 조사 결과 — 한 건의 조사 처리 결과(성공/중복/불가).")
    public static class CheckResponse {
        @Schema(description = "전역 흐름 코드", example = "445823472384938240")
        private String flowCode;
        @Schema(description = "상품 이름", example = "다이아 1ct 반지")
        private String productName;
        @Schema(description = "결과 상태 (SUCCESS/ALREADY_CHECKED/NOT_CHECKABLE)", example = "SUCCESS")
        private String status;           // SUCCESS, ALREADY_CHECKED, NOT_CHECKABLE
        @Schema(description = "결과 메시지", example = "재고 조사 처리됨")
        private String message;
        @Schema(description = "재고 조사일", example = "2026-05-16 10:00")
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
    @Schema(description = "재고 조사 검색 조건 — 필드별 검색/정렬/필터.")
    public static class Condition {
        @Schema(description = "검색 대상 필드 (productName/materialName/colorName 등)", example = "productName")
        private String searchField;      // 검색 필드 (productName, materialName, colorName 등)
        @Schema(description = "검색 값", example = "반지")
        private String searchValue;      // 검색 값
        @Schema(description = "정렬 필드", example = "createAt")
        private String sortField;        // 정렬 필드
        @Schema(description = "정렬 순서 (ASC/DESC)", example = "DESC")
        private String sortOrder;        // 정렬 순서 (ASC, DESC)
        @Schema(description = "재고 조사 여부 필터 (checked/unchecked/all)", example = "all")
        private String stockChecked;     // 재고 조사 여부 필터 (checked, unchecked, all)
        @Schema(description = "재고 구분 필터 (STOCK/RENTAL/RETURN/ORDER)", example = "STOCK")
        private String orderStatus;      // 재고 구분 필터 (STOCK, RENTAL, RETURN, ORDER)
        @Schema(description = "재질 필터", example = "18K")
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
    @Schema(description = "재고 조사 초기화 응답 — 초기화된 건수와 메시지.")
    public static class ResetResponse {
        @Schema(description = "초기화 처리 건수", example = "42")
        private Integer resetCount;
        @Schema(description = "결과 메시지", example = "재고 조사 초기화 완료")
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
    @Schema(description = "재고 조사 통계 — 검사/미검사 재질별 통계와 합계.")
    public static class StatisticsResponse {
        @Schema(description = "검사하지 않은 재질별 통계 목록")
        private List<MaterialStatistics> uncheckedStatistics;  // 검사하지 않은 재질별 통계
        @Schema(description = "검사한 재질별 통계 목록")
        private List<MaterialStatistics> checkedStatistics;    // 검사한 재질별 통계
        @Schema(description = "미검사 합계")
        private StatisticsSummary uncheckedSummary;            // 미검사 합계
        @Schema(description = "검사 합계")
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
    @Schema(description = "재질별 재고 통계 한 행.")
    public static class MaterialStatistics {
        @Schema(description = "재질 이름", example = "18K")
        private String materialName;      // 재질명
        @Schema(description = "총 금 무게 (g, 문자열)", example = "152.350")
        private String totalGoldWeight;   // 총 중량
        @Schema(description = "수량", example = "42")
        private Integer quantity;         // 수량
        @Schema(description = "총 매입가 (원)", example = "21000000")
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
    @Schema(description = "재고 조사 통계 합계.")
    public static class StatisticsSummary {
        @Schema(description = "총 금 무게 합계 (g, 문자열)", example = "300.500")
        private String totalGoldWeight;   // 총 중량 합계
        @Schema(description = "총 수량 합계", example = "80")
        private Integer totalQuantity;    // 총 수량 합계
        @Schema(description = "총 매입가 합계 (원)", example = "40000000")
        private Long totalPurchaseCost;   // 총 매입가 합계

        @Builder
        public StatisticsSummary(String totalGoldWeight, Integer totalQuantity, Long totalPurchaseCost) {
            this.totalGoldWeight = totalGoldWeight;
            this.totalQuantity = totalQuantity;
            this.totalPurchaseCost = totalPurchaseCost;
        }
    }
}
