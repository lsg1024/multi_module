package com.msa.order.local.dashboard.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.util.CustomPage;
import com.msa.order.local.dashboard.dto.DashboardDto;
import com.msa.order.local.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 1. 재질별 현 재고 현황 API (최대 5개)
     * - 재질(material), 중량(goldWeight 합), 수량(count)
     */
    @GetMapping("/stocks/material-summary")
    public ResponseEntity<ApiResponse<List<DashboardDto.MaterialStockSummary>>> getMaterialStockSummary() {
        List<DashboardDto.MaterialStockSummary> result = dashboardService.getMaterialStockSummary();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 2. 재고 Top5 모델 API
     * - 모델번호(productName), 재고 개수
     */
    @GetMapping("/stocks/top5-models")
    public ResponseEntity<ApiResponse<List<DashboardDto.StockModelTop>>> getStockModelTop5() {
        List<DashboardDto.StockModelTop> result = dashboardService.getStockModelTop5();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 2-2. 재고 상세보기 API (페이징 + 검색)
     * - 모든 재고 제품 목록 조회
     * - 검색 조건: 모델번호, 재질, 색상, 판매처
     */
    @GetMapping("/stocks/details")
    public ResponseEntity<ApiResponse<CustomPage<DashboardDto.StockDetail>>> getAllStockDetails(
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String colorName,
            @RequestParam(required = false) String storeName,
            @PageableDefault(size = 20) Pageable pageable) {

        DashboardDto.StockSearchCondition condition = DashboardDto.StockSearchCondition.builder()
                .productName(productName)
                .materialName(materialName)
                .colorName(colorName)
                .storeName(storeName)
                .build();

        CustomPage<DashboardDto.StockDetail> result = dashboardService.searchStockDetails(condition, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 2-3. 재고 필터 옵션 조회 API
     * - 현재 재고에 존재하는 재질, 색상, 판매처 목록 조회
     */
    @GetMapping("/stocks/filter-options")
    public ResponseEntity<ApiResponse<DashboardDto.StockFilterOption>> getStockFilterOptions() {
        DashboardDto.StockFilterOption result = dashboardService.getStockFilterOptions();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 3. 당월 판매 Top5 모델 API
     * - 모델번호(productName), 판매 개수
     */
    @GetMapping("/sales/top5-models")
    public ResponseEntity<ApiResponse<List<DashboardDto.SaleModelTop>>> getMonthlySaleModelTop5() {
        List<DashboardDto.SaleModelTop> result = dashboardService.getMonthlySaleModelTop5();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 4. 당월 매출공임 Top5 거래처 API
     * - 거래처(storeName), 매출공임 합계
     * - 매출공임 = productLaborCost + productAddLaborCost + stoneMainLaborCost + stoneAssistanceLaborCost + stoneAddLaborCost
     */
    @GetMapping("/sales/top5-labor-cost")
    public ResponseEntity<ApiResponse<List<DashboardDto.StoreLaborCostTop>>> getMonthlyStoreLaborCostTop5() {
        List<DashboardDto.StoreLaborCostTop> result = dashboardService.getMonthlyStoreLaborCostTop5();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 5. 당월 매출 현황 API
     * - 매출 순금, 매출 공임, 마진 공임
     */
    @GetMapping("/sales/monthly-summary")
    public ResponseEntity<ApiResponse<DashboardDto.MonthlySalesSummary>> getMonthlySalesSummary() {
        DashboardDto.MonthlySalesSummary result = dashboardService.getMonthlySalesSummary();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 5-2. 거래처별 거래 통계 API
     * - 거래처별 판매/반품/DC/매출/실입금/매입/마진 통계
     * - 검색 조건: 날짜 범위, 거래처명, 매장구분, 거래형태, 재질, 분류, 매입처, 관리자
     */
    @GetMapping("/sales/store-statistics")
    public ResponseEntity<ApiResponse<List<DashboardDto.StoreTradeStatistics>>> getStoreTradeStatistics(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String storeName,
            @RequestParam(required = false) String storeGrade,
            @RequestParam(required = false) String tradeType,
            @RequestParam(required = false) String materialName,
            @RequestParam(required = false) String classificationName,
            @RequestParam(required = false) String factoryName,
            @RequestParam(required = false) String createdBy,
            @RequestParam(required = false) String statisticsType) {

        DashboardDto.StoreStatisticsSearchCondition condition = DashboardDto.StoreStatisticsSearchCondition.builder()
                .start(start)
                .end(end)
                .storeName(storeName)
                .storeGrade(storeGrade)
                .tradeType(tradeType)
                .materialName(materialName)
                .classificationName(classificationName)
                .factoryName(factoryName)
                .createdBy(createdBy)
                .statisticsType(statisticsType)
                .build();

        List<DashboardDto.StoreTradeStatistics> result = dashboardService.getStoreTradeStatistics(condition);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 5-3. 거래처별 거래 통계 필터 옵션 조회 API
     * - 매장구분, 거래형태, 재질, 분류, 매입처, 관리자, 통계선택 옵션 목록
     */
    @GetMapping("/sales/store-statistics/filter-options")
    public ResponseEntity<ApiResponse<DashboardDto.StoreStatisticsFilterOption>> getStoreStatisticsFilterOptions() {
        DashboardDto.StoreStatisticsFilterOption result = dashboardService.getStoreStatisticsFilterOptions();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 6. 현 미수 현황 API
     * - 미수 순금, 미수 금액
     */
    @GetMapping("/receivable/summary")
    public ResponseEntity<ApiResponse<DashboardDto.ReceivableSummary>> getReceivableSummary() {
        DashboardDto.ReceivableSummary result = dashboardService.getReceivableSummary();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 7. 현 대여 현황 API
     * - 대여 순금, 대여 공임, 대여 수량
     */
    @GetMapping("/rental/summary")
    public ResponseEntity<ApiResponse<DashboardDto.RentalSummary>> getRentalSummary() {
        DashboardDto.RentalSummary result = dashboardService.getRentalSummary();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 7-2. 대여 현황 상세보기 API
     * - 거래처별 대여 현황 (거래처, 전화번호, 순금, 공임, 수량, 최초/최종 대여일)
     */
    @GetMapping("/rental/details")
    public ResponseEntity<ApiResponse<List<DashboardDto.RentalDetail>>> getRentalDetails() {
        List<DashboardDto.RentalDetail> result = dashboardService.getRentalDetails();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 8. 매입처 미결제 현황 API
     * - 미결제 순금, 미결제 금액
     */
    @GetMapping("/factory/unpaid-summary")
    public ResponseEntity<ApiResponse<DashboardDto.FactoryUnpaidSummary>> getFactoryUnpaidSummary() {
        DashboardDto.FactoryUnpaidSummary result = dashboardService.getFactoryUnpaidSummary();
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
