package com.msa.order.local.dashboard.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.dashboard.dto.DashboardDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DashboardRepository {

    List<DashboardDto.MaterialStockSummary> findMaterialStockSummary(int limit);

    List<DashboardDto.StockModelTop> findStockModelTop(int limit);

    CustomPage<DashboardDto.StockDetail> findAllStockDetails(Pageable pageable);

    CustomPage<DashboardDto.StockDetail> findStockDetailsWithSearch(DashboardDto.StockSearchCondition condition, Pageable pageable);

    DashboardDto.StockFilterOption findStockFilterOptions();

    List<DashboardDto.SaleModelTop> findMonthlySaleModelTop(int limit);

    List<DashboardDto.StoreLaborCostTop> findMonthlyStoreLaborCostTop(int limit);

    // 1. 당월 매출 현황
    DashboardDto.MonthlySalesSummary findMonthlySalesSummary();

    // 1-2. 거래처별 거래 통계 (검색 조건 적용)
    List<DashboardDto.StoreTradeStatistics> findStoreTradeStatistics(DashboardDto.StoreStatisticsSearchCondition condition);

    // 1-3. 거래처별 거래 통계 필터 옵션 조회
    DashboardDto.StoreStatisticsFilterOption findStoreStatisticsFilterOptions();

    // 2. 현 미수 현황
    DashboardDto.ReceivableSummary findReceivableSummary();

    // 3. 현 대여 현황
    DashboardDto.RentalSummary findRentalSummary();

    // 3-2. 대여 현황 상세보기
    List<DashboardDto.RentalDetail> findRentalDetails();

    // 4. 매입처 미결제 현황
    DashboardDto.FactoryUnpaidSummary findFactoryUnpaidSummary();
}
