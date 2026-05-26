package com.msa.jewelry.local.dashboard.service;

import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.local.dashboard.dto.DashboardDto;
import com.msa.jewelry.local.dashboard.repository.DashboardRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * DashboardService 단위 테스트.
 *
 * <p>DashboardService 는 DashboardRepository 위임 위주의 단순 facade.
 * 검증 포인트:
 * <ul>
 *   <li>TOP_LIMIT(5) 가 정확히 repository 에 전달되는지</li>
 *   <li>Pageable / Condition 이 그대로 전달되는지 (ArgumentCaptor)</li>
 *   <li>repository 빈 결과/정상 결과 둘 다 그대로 통과되는지</li>
 *   <li>null 결과 통과 — facade 가 임의 변환하지 않는지</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DashboardService 단위 테스트")
class DashboardServiceTest {

    private static final int TOP_LIMIT = 5;

    @Mock DashboardRepository dashboardRepository;

    @InjectMocks DashboardService dashboardService;

    // -----------------------------------------------------------------------
    // getMaterialStockSummary
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getMaterialStockSummary")
    class GetMaterialStockSummary {

        @Test
        @DisplayName("정상 — TOP_LIMIT(5) 위임 + 결과 그대로 반환")
        void 정상_위임() {
            DashboardDto.MaterialStockSummary item = mock(DashboardDto.MaterialStockSummary.class);
            given(dashboardRepository.findMaterialStockSummary(TOP_LIMIT))
                    .willReturn(List.of(item));

            List<DashboardDto.MaterialStockSummary> result = dashboardService.getMaterialStockSummary();

            assertThat(result).hasSize(1).containsExactly(item);
            verify(dashboardRepository).findMaterialStockSummary(TOP_LIMIT);
        }

        @Test
        @DisplayName("빈 결과 — empty list 통과")
        void 빈결과() {
            given(dashboardRepository.findMaterialStockSummary(anyInt()))
                    .willReturn(Collections.emptyList());

            assertThat(dashboardService.getMaterialStockSummary()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getStockModelTop5
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStockModelTop5")
    class GetStockModelTop5 {

        @Test
        @DisplayName("정상 — TOP_LIMIT 전달")
        void 정상() {
            DashboardDto.StockModelTop item = mock(DashboardDto.StockModelTop.class);
            given(dashboardRepository.findStockModelTop(TOP_LIMIT)).willReturn(List.of(item));

            assertThat(dashboardService.getStockModelTop5()).containsExactly(item);
            verify(dashboardRepository).findStockModelTop(TOP_LIMIT);
        }

        @Test
        @DisplayName("빈 결과")
        void 빈결과() {
            given(dashboardRepository.findStockModelTop(anyInt())).willReturn(Collections.emptyList());

            assertThat(dashboardService.getStockModelTop5()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getAllStockDetails
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getAllStockDetails")
    class GetAllStockDetails {

        @Test
        @DisplayName("Pageable 그대로 위임")
        void Pageable_위임() {
            Pageable pageable = PageRequest.of(0, 20);
            @SuppressWarnings("unchecked")
            CustomPage<DashboardDto.StockDetail> page = mock(CustomPage.class);
            given(dashboardRepository.findAllStockDetails(pageable)).willReturn(page);

            CustomPage<DashboardDto.StockDetail> result = dashboardService.getAllStockDetails(pageable);

            assertThat(result).isSameAs(page);
            verify(dashboardRepository).findAllStockDetails(pageable);
        }

        @Test
        @DisplayName("repository 가 null 반환해도 그대로 통과 — facade 는 변환하지 않음")
        void null_통과() {
            Pageable pageable = PageRequest.of(0, 20);
            given(dashboardRepository.findAllStockDetails(pageable)).willReturn(null);

            assertThat(dashboardService.getAllStockDetails(pageable)).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // searchStockDetails
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("searchStockDetails")
    class SearchStockDetails {

        @Test
        @DisplayName("Condition + Pageable 둘 다 그대로 전달")
        void 위임() {
            Pageable pageable = PageRequest.of(1, 10);
            DashboardDto.StockSearchCondition condition = DashboardDto.StockSearchCondition.builder()
                    .productName("반지")
                    .materialName("18K")
                    .colorName("옐로우골드")
                    .storeName("ABC")
                    .build();

            @SuppressWarnings("unchecked")
            CustomPage<DashboardDto.StockDetail> page = mock(CustomPage.class);
            given(dashboardRepository.findStockDetailsWithSearch(eq(condition), eq(pageable))).willReturn(page);

            assertThat(dashboardService.searchStockDetails(condition, pageable)).isSameAs(page);

            ArgumentCaptor<DashboardDto.StockSearchCondition> captor =
                    ArgumentCaptor.forClass(DashboardDto.StockSearchCondition.class);
            verify(dashboardRepository).findStockDetailsWithSearch(captor.capture(), eq(pageable));
            assertThat(captor.getValue().getProductName()).isEqualTo("반지");
            assertThat(captor.getValue().getMaterialName()).isEqualTo("18K");
        }

        @Test
        @DisplayName("null condition 도 그대로 위임 (방어 책임은 repository)")
        void null_condition() {
            Pageable pageable = PageRequest.of(0, 5);
            @SuppressWarnings("unchecked")
            CustomPage<DashboardDto.StockDetail> page = mock(CustomPage.class);
            given(dashboardRepository.findStockDetailsWithSearch(null, pageable)).willReturn(page);

            assertThat(dashboardService.searchStockDetails(null, pageable)).isSameAs(page);
        }
    }

    // -----------------------------------------------------------------------
    // getStockFilterOptions
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStockFilterOptions")
    class GetStockFilterOptions {

        @Test
        @DisplayName("repository 위임 + 그대로 반환")
        void 정상() {
            DashboardDto.StockFilterOption opt = DashboardDto.StockFilterOption.builder()
                    .materials(List.of("18K", "14K"))
                    .colors(List.of("옐로우"))
                    .stores(List.of("ABC"))
                    .build();
            given(dashboardRepository.findStockFilterOptions()).willReturn(opt);

            assertThat(dashboardService.getStockFilterOptions()).isSameAs(opt);
        }

        @Test
        @DisplayName("repository 가 null 반환 — null 통과")
        void null_통과() {
            given(dashboardRepository.findStockFilterOptions()).willReturn(null);
            assertThat(dashboardService.getStockFilterOptions()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // getMonthlySaleModelTop5
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getMonthlySaleModelTop5")
    class GetMonthlySaleModelTop5 {

        @Test
        @DisplayName("TOP_LIMIT 전달")
        void 정상() {
            DashboardDto.SaleModelTop top = mock(DashboardDto.SaleModelTop.class);
            given(dashboardRepository.findMonthlySaleModelTop(TOP_LIMIT)).willReturn(List.of(top));

            assertThat(dashboardService.getMonthlySaleModelTop5()).containsExactly(top);
            verify(dashboardRepository).findMonthlySaleModelTop(TOP_LIMIT);
        }

        @Test
        @DisplayName("빈 결과")
        void 빈결과() {
            given(dashboardRepository.findMonthlySaleModelTop(TOP_LIMIT)).willReturn(Collections.emptyList());
            assertThat(dashboardService.getMonthlySaleModelTop5()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getMonthlyStoreLaborCostTop5
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getMonthlyStoreLaborCostTop5")
    class GetMonthlyStoreLaborCostTop5 {

        @Test
        @DisplayName("정상 위임")
        void 정상() {
            DashboardDto.StoreLaborCostTop top = mock(DashboardDto.StoreLaborCostTop.class);
            given(dashboardRepository.findMonthlyStoreLaborCostTop(TOP_LIMIT)).willReturn(List.of(top));

            assertThat(dashboardService.getMonthlyStoreLaborCostTop5()).containsExactly(top);
        }

        @Test
        @DisplayName("빈 결과")
        void 빈결과() {
            given(dashboardRepository.findMonthlyStoreLaborCostTop(TOP_LIMIT)).willReturn(Collections.emptyList());
            assertThat(dashboardService.getMonthlyStoreLaborCostTop5()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getMonthlySalesSummary
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getMonthlySalesSummary")
    class GetMonthlySalesSummary {

        @Test
        @DisplayName("정상 — facade 가 변환 없이 통과")
        void 정상() {
            DashboardDto.MonthlySalesSummary summary = mock(DashboardDto.MonthlySalesSummary.class);
            given(dashboardRepository.findMonthlySalesSummary()).willReturn(summary);

            assertThat(dashboardService.getMonthlySalesSummary()).isSameAs(summary);
        }

        @Test
        @DisplayName("null 반환 — 그대로")
        void null_통과() {
            given(dashboardRepository.findMonthlySalesSummary()).willReturn(null);
            assertThat(dashboardService.getMonthlySalesSummary()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // getStoreTradeStatistics
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStoreTradeStatistics")
    class GetStoreTradeStatistics {

        @Test
        @DisplayName("Condition 통째로 위임")
        void 위임() {
            DashboardDto.StoreStatisticsSearchCondition condition =
                    DashboardDto.StoreStatisticsSearchCondition.builder()
                            .start("2026-05-01")
                            .end("2026-05-31")
                            .storeName("ABC")
                            .storeGrade("A")
                            .tradeType("SALE")
                            .materialName("18K")
                            .classificationName("반지")
                            .factoryName("삼성공방")
                            .createdBy("admin")
                            .statisticsType("STORE")
                            .build();

            DashboardDto.StoreTradeStatistics row = mock(DashboardDto.StoreTradeStatistics.class);
            given(dashboardRepository.findStoreTradeStatistics(condition)).willReturn(List.of(row));

            assertThat(dashboardService.getStoreTradeStatistics(condition)).containsExactly(row);

            ArgumentCaptor<DashboardDto.StoreStatisticsSearchCondition> captor =
                    ArgumentCaptor.forClass(DashboardDto.StoreStatisticsSearchCondition.class);
            verify(dashboardRepository).findStoreTradeStatistics(captor.capture());
            assertThat(captor.getValue().getStart()).isEqualTo("2026-05-01");
            assertThat(captor.getValue().getStatisticsType()).isEqualTo("STORE");
        }

        @Test
        @DisplayName("빈 결과")
        void 빈결과() {
            DashboardDto.StoreStatisticsSearchCondition condition =
                    DashboardDto.StoreStatisticsSearchCondition.builder().build();
            given(dashboardRepository.findStoreTradeStatistics(any())).willReturn(Collections.emptyList());

            assertThat(dashboardService.getStoreTradeStatistics(condition)).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getStoreStatisticsFilterOptions
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStoreStatisticsFilterOptions")
    class GetStoreStatisticsFilterOptions {

        @Test
        @DisplayName("정상")
        void 정상() {
            DashboardDto.StoreStatisticsFilterOption opt =
                    DashboardDto.StoreStatisticsFilterOption.builder()
                            .storeGrades(List.of("A", "B"))
                            .tradeTypes(List.of("SALE"))
                            .materials(List.of("18K"))
                            .classifications(List.of("반지"))
                            .factories(List.of("삼성공방"))
                            .managers(List.of("admin"))
                            .statisticsTypes(List.of("STORE", "FACTORY"))
                            .build();

            given(dashboardRepository.findStoreStatisticsFilterOptions()).willReturn(opt);

            assertThat(dashboardService.getStoreStatisticsFilterOptions()).isSameAs(opt);
        }

        @Test
        @DisplayName("null 통과")
        void null_통과() {
            given(dashboardRepository.findStoreStatisticsFilterOptions()).willReturn(null);
            assertThat(dashboardService.getStoreStatisticsFilterOptions()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // getReceivableSummary
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getReceivableSummary")
    class GetReceivableSummary {

        @Test
        @DisplayName("정상")
        void 정상() {
            DashboardDto.ReceivableSummary summary = mock(DashboardDto.ReceivableSummary.class);
            given(dashboardRepository.findReceivableSummary()).willReturn(summary);
            assertThat(dashboardService.getReceivableSummary()).isSameAs(summary);
        }

        @Test
        @DisplayName("null 통과")
        void null_통과() {
            given(dashboardRepository.findReceivableSummary()).willReturn(null);
            assertThat(dashboardService.getReceivableSummary()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // getRentalSummary
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getRentalSummary")
    class GetRentalSummary {

        @Test
        @DisplayName("정상")
        void 정상() {
            DashboardDto.RentalSummary summary = mock(DashboardDto.RentalSummary.class);
            given(dashboardRepository.findRentalSummary()).willReturn(summary);
            assertThat(dashboardService.getRentalSummary()).isSameAs(summary);
        }

        @Test
        @DisplayName("null 통과")
        void null_통과() {
            given(dashboardRepository.findRentalSummary()).willReturn(null);
            assertThat(dashboardService.getRentalSummary()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // getRentalDetails
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getRentalDetails")
    class GetRentalDetails {

        @Test
        @DisplayName("정상")
        void 정상() {
            DashboardDto.RentalDetail r = mock(DashboardDto.RentalDetail.class);
            given(dashboardRepository.findRentalDetails()).willReturn(List.of(r));
            assertThat(dashboardService.getRentalDetails()).containsExactly(r);
            verify(dashboardRepository).findRentalDetails();
            verifyNoMoreInteractions(dashboardRepository);
        }

        @Test
        @DisplayName("빈 결과")
        void 빈결과() {
            given(dashboardRepository.findRentalDetails()).willReturn(Collections.emptyList());
            assertThat(dashboardService.getRentalDetails()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getFactoryUnpaidSummary
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFactoryUnpaidSummary")
    class GetFactoryUnpaidSummary {

        @Test
        @DisplayName("정상")
        void 정상() {
            DashboardDto.FactoryUnpaidSummary summary = mock(DashboardDto.FactoryUnpaidSummary.class);
            given(dashboardRepository.findFactoryUnpaidSummary()).willReturn(summary);
            assertThat(dashboardService.getFactoryUnpaidSummary()).isSameAs(summary);
        }

        @Test
        @DisplayName("null 통과")
        void null_통과() {
            given(dashboardRepository.findFactoryUnpaidSummary()).willReturn(null);
            assertThat(dashboardService.getFactoryUnpaidSummary()).isNull();
        }
    }
}
