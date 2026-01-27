package com.msa.order.local.dashboard.service;

import com.msa.common.global.util.CustomPage;
import com.msa.order.local.dashboard.dto.DashboardDto;
import com.msa.order.local.dashboard.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    private static final int TOP_LIMIT = 5;

    public List<DashboardDto.MaterialStockSummary> getMaterialStockSummary() {
        return dashboardRepository.findMaterialStockSummary(TOP_LIMIT);
    }

    public List<DashboardDto.StockModelTop> getStockModelTop5() {
        return dashboardRepository.findStockModelTop(TOP_LIMIT);
    }

    public CustomPage<DashboardDto.StockDetail> getAllStockDetails(Pageable pageable) {
        return dashboardRepository.findAllStockDetails(pageable);
    }

    public CustomPage<DashboardDto.StockDetail> searchStockDetails(DashboardDto.StockSearchCondition condition, Pageable pageable) {
        return dashboardRepository.findStockDetailsWithSearch(condition, pageable);
    }

    public DashboardDto.StockFilterOption getStockFilterOptions() {
        return dashboardRepository.findStockFilterOptions();
    }

    public List<DashboardDto.SaleModelTop> getMonthlySaleModelTop5() {
        return dashboardRepository.findMonthlySaleModelTop(TOP_LIMIT);
    }

    public List<DashboardDto.StoreLaborCostTop> getMonthlyStoreLaborCostTop5() {
        return dashboardRepository.findMonthlyStoreLaborCostTop(TOP_LIMIT);
    }

    // 1. 당월 매출 현황
    public DashboardDto.MonthlySalesSummary getMonthlySalesSummary() {
        return dashboardRepository.findMonthlySalesSummary();
    }

    // 1-2. 거래처별 거래 통계 (검색 조건 적용)
    public List<DashboardDto.StoreTradeStatistics> getStoreTradeStatistics(DashboardDto.StoreStatisticsSearchCondition condition) {
        return dashboardRepository.findStoreTradeStatistics(condition);
    }

    // 1-3. 거래처별 거래 통계 필터 옵션 조회
    public DashboardDto.StoreStatisticsFilterOption getStoreStatisticsFilterOptions() {
        return dashboardRepository.findStoreStatisticsFilterOptions();
    }

    // 2. 현 미수 현황
    public DashboardDto.ReceivableSummary getReceivableSummary() {
        return dashboardRepository.findReceivableSummary();
    }

    // 3. 현 대여 현황
    public DashboardDto.RentalSummary getRentalSummary() {
        return dashboardRepository.findRentalSummary();
    }

    // 3-2. 대여 현황 상세보기
    public List<DashboardDto.RentalDetail> getRentalDetails() {
        return dashboardRepository.findRentalDetails();
    }

    // 4. 매입처 미결제 현황
    public DashboardDto.FactoryUnpaidSummary getFactoryUnpaidSummary() {
        return dashboardRepository.findFactoryUnpaidSummary();
    }
}
