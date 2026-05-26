package com.msa.jewelry.local.stock.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.exception.InvalidOrderStatusException;
import com.msa.jewelry.global.exception.OrderNotFoundException;
import com.msa.jewelry.global.exception.StockNotFoundException;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.assistant_stone.dto.AssistantStoneView;
import com.msa.jewelry.local.assistant_stone.service.AssistantStoneService;
import com.msa.jewelry.local.factory.dto.FactoryView;
import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.order.entity.OrderProduct;
import com.msa.jewelry.local.order.entity.Orders;
import com.msa.jewelry.local.order.entity.StatusHistory;
import com.msa.jewelry.local.order.entity.order_enum.BusinessPhase;
import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.order.entity.order_enum.SourceType;
import com.msa.jewelry.local.order.repository.OrdersRepository;
import com.msa.jewelry.local.order.repository.StatusHistoryRepository;
import com.msa.jewelry.local.order.util.StatusHistoryHelper;
import com.msa.jewelry.local.stock.dto.InventoryDto;
import com.msa.jewelry.local.stock.dto.StockDto;
import com.msa.jewelry.local.stock.dto.StockView;
import com.msa.jewelry.local.stock.entity.ProductSnapshot;
import com.msa.jewelry.local.stock.entity.Stock;
import com.msa.jewelry.local.stock.repository.CustomStockRepository;
import com.msa.jewelry.local.stock.repository.StockRepository;
import com.msa.jewelry.local.store.dto.StoreView;
import com.msa.jewelry.local.store.service.StoreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * StockServiceImpl 단위 테스트.
 *
 * <p>외부 의존성(JwtUtil, AssistantStoneService, StoreService, FactoryService,
 * StockRepository, OrdersRepository, CustomStockRepository, StatusHistoryRepository,
 * StatusHistoryHelper, StockCreationService)을 모두 Mockito 로 대체하여
 * StockServiceImpl 의 분기 로직만 격리해 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StockServiceImpl 단위 테스트")
class StockServiceImplTest {

    private static final String TOKEN     = "Bearer test-token";
    private static final String TENANT_ID = "tenant-001";
    private static final String NICKNAME  = "tester";
    private static final Long   STORE_ID  = 100L;
    private static final Long   FACTORY_ID = 200L;
    private static final Long   FLOW_CODE = 9_990L;
    private static final Long   PRODUCT_ID = 501L;

    @Mock JwtUtil jwtUtil;
    @Mock StockCreationService stockCreationService;
    @Mock AssistantStoneService assistantStoneService;
    @Mock StoreService storeService;
    @Mock FactoryService factoryService;
    @Mock StockRepository stockRepository;
    @Mock OrdersRepository ordersRepository;
    @Mock CustomStockRepository customStockRepository;
    @Mock StatusHistoryRepository statusHistoryRepository;
    @Mock StatusHistoryHelper statusHistoryHelper;

    @InjectMocks
    StockServiceImpl stockService;

    // -----------------------------------------------------------------------
    // getStockCountByProductNames
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStockCountByProductNames")
    class GetStockCountByProductNames {

        @Test
        @DisplayName("productNames 가 null 이면 빈 Map 반환 — repo 호출 없음")
        void null_입력() {
            Map<String, Integer> result = stockService.getStockCountByProductNames(null);

            assertThat(result).isEmpty();
            verify(stockRepository, never()).countByProductNames(any());
        }

        @Test
        @DisplayName("productNames 가 빈 리스트면 빈 Map 반환 — repo 호출 없음")
        void 빈_리스트() {
            Map<String, Integer> result = stockService.getStockCountByProductNames(Collections.emptyList());

            assertThat(result).isEmpty();
            verify(stockRepository, never()).countByProductNames(any());
        }

        @Test
        @DisplayName("정상 — 결과 Object[] → Map<String,Integer> 변환")
        void 정상_변환() {
            List<String> names = List.of("반지", "목걸이");
            List<Object[]> rows = new ArrayList<>();
            rows.add(new Object[]{"반지", 3L});
            rows.add(new Object[]{"목걸이", 7L});
            given(stockRepository.countByProductNames(names)).willReturn(rows);

            Map<String, Integer> result = stockService.getStockCountByProductNames(names);

            assertThat(result).containsEntry("반지", 3).containsEntry("목걸이", 7);
        }

        @Test
        @DisplayName("repository 가 빈 결과 반환 → 빈 Map")
        void 결과_없음() {
            given(stockRepository.countByProductNames(anyList())).willReturn(Collections.emptyList());

            Map<String, Integer> result = stockService.getStockCountByProductNames(List.of("미존재"));

            assertThat(result).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getDetailStock
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getDetailStock")
    class GetDetailStock {

        @Test
        @DisplayName("flowCodes 빈 리스트 — 빈 결과 반환")
        void 빈_입력() {
            given(stockRepository.findByFlowCodeIn(Collections.emptyList())).willReturn(Collections.emptyList());
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDescIn(Collections.emptyList()))
                    .willReturn(Collections.emptyList());

            List<StockDto.ResponseDetail> result = stockService.getDetailStock(Collections.emptyList());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("정상 — stock 1건 + history 1건 매핑")
        void 정상_단건() {
            Stock stock = stubStock();
            given(stock.getFlowCode()).willReturn(FLOW_CODE);
            given(stock.getCreateDate()).willReturn(LocalDateTime.now());
            given(stock.getStockNote()).willReturn("재고비고");

            StatusHistory history = mock(StatusHistory.class);
            given(history.getFlowCode()).willReturn(FLOW_CODE);
            given(history.getId()).willReturn(1L);
            given(history.getSourceType()).willReturn(SourceType.NORMAL);

            given(stockRepository.findByFlowCodeIn(List.of(FLOW_CODE))).willReturn(List.of(stock));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDescIn(List.of(FLOW_CODE)))
                    .willReturn(List.of(history));
            given(storeService.getStoreInfoView(STORE_ID))
                    .willReturn(new StoreView(STORE_ID, "강남금은방", "A", "1.5", "SELL", false));
            given(factoryService.getFactoryInfo(FACTORY_ID))
                    .willReturn(new FactoryView(FACTORY_ID, "한빛공방", "A", "1.2"));

            List<StockDto.ResponseDetail> result = stockService.getDetailStock(List.of(FLOW_CODE));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFlowCode()).isEqualTo(FLOW_CODE.toString());
            assertThat(result.get(0).getStoreName()).isEqualTo("강남금은방");
            assertThat(result.get(0).getFactoryName()).isEqualTo("한빛공방");
            assertThat(result.get(0).getOriginalProductStatus()).isEqualTo(SourceType.NORMAL.getDisplayName());
        }

        @Test
        @DisplayName("history 가 null flowCode 면 무시(continue) — NPE 안 남")
        void history_null_flowCode_무시() {
            Stock stock = stubStock();
            given(stock.getFlowCode()).willReturn(FLOW_CODE);

            StatusHistory bad = mock(StatusHistory.class);
            given(bad.getFlowCode()).willReturn(null);

            given(stockRepository.findByFlowCodeIn(any())).willReturn(List.of(stock));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDescIn(any()))
                    .willReturn(Arrays.asList(bad, null));
            given(storeService.getStoreInfoView(STORE_ID))
                    .willReturn(new StoreView(STORE_ID, "store", "A", "1.5", "SELL", false));
            given(factoryService.getFactoryInfo(FACTORY_ID))
                    .willReturn(new FactoryView(FACTORY_ID, "factory", "A", "1.2"));

            List<StockDto.ResponseDetail> result = stockService.getDetailStock(List.of(FLOW_CODE));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOriginalProductStatus()).isNull();
        }

        @Test
        @DisplayName("같은 flowCode 의 여러 history 중 id 최대값을 선택")
        void 최신_history_선택() {
            Stock stock = stubStock();
            given(stock.getFlowCode()).willReturn(FLOW_CODE);

            StatusHistory h1 = mock(StatusHistory.class);
            given(h1.getFlowCode()).willReturn(FLOW_CODE);
            given(h1.getId()).willReturn(1L);
            given(h1.getSourceType()).willReturn(SourceType.ORDER);

            StatusHistory h2 = mock(StatusHistory.class);
            given(h2.getFlowCode()).willReturn(FLOW_CODE);
            given(h2.getId()).willReturn(5L);
            given(h2.getSourceType()).willReturn(SourceType.RENTAL);

            given(stockRepository.findByFlowCodeIn(any())).willReturn(List.of(stock));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDescIn(any()))
                    .willReturn(List.of(h1, h2));
            given(storeService.getStoreInfoView(STORE_ID))
                    .willReturn(new StoreView(STORE_ID, "s", "A", "1.5", "SELL", false));
            given(factoryService.getFactoryInfo(FACTORY_ID))
                    .willReturn(new FactoryView(FACTORY_ID, "f", "A", "1.2"));

            List<StockDto.ResponseDetail> result = stockService.getDetailStock(List.of(FLOW_CODE));

            assertThat(result.get(0).getOriginalProductStatus()).isEqualTo(SourceType.RENTAL.getDisplayName());
        }

        @Test
        @DisplayName("storeId 가 null 이면 storeName 조회 안 함")
        void storeId_null() {
            Stock stock = mock(Stock.class);
            given(stock.getFlowCode()).willReturn(FLOW_CODE);
            given(stock.getStoreId()).willReturn(null);
            given(stock.getFactoryId()).willReturn(null);
            given(stock.getOrderStones()).willReturn(new ArrayList<>());
            given(stock.getProduct()).willReturn(null);

            given(stockRepository.findByFlowCodeIn(any())).willReturn(List.of(stock));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDescIn(any()))
                    .willReturn(Collections.emptyList());

            List<StockDto.ResponseDetail> result = stockService.getDetailStock(List.of(FLOW_CODE));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStoreName()).isNull();
            assertThat(result.get(0).getFactoryName()).isNull();
            verify(storeService, never()).getStoreInfoView(any());
            verify(factoryService, never()).getFactoryInfo(any());
        }
    }

    // -----------------------------------------------------------------------
    // getStocks
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStocks")
    class GetStocks {

        @Test
        @DisplayName("빈 결과 — history 조회 없음 / 빈 페이지 반환")
        void 빈결과() {
            Pageable pageable = PageRequest.of(0, 20);
            CustomPage<StockDto.Response> emptyPage = new CustomPage<>(Collections.emptyList(), pageable, 0L);

            given(customStockRepository.findByStockProducts(any(), any(), eq(pageable)))
                    .willReturn(emptyPage);
            given(statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(anyList()))
                    .willReturn(Collections.emptyList());

            CustomPage<StockDto.Response> result = stockService.getStocks(
                    "input", "field", "2026-05-01", "2026-05-31",
                    "공장", "매장", "단품", "옐로우", "반지", "18K",
                    "createAt", "DESC", "STOCK", pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("결과 1건 — 각 dto 에 history 가 주입됨(updateHistory 호출)")
        void 결과_1건_history_주입() {
            Pageable pageable = PageRequest.of(0, 20);
            StockDto.Response dto = mock(StockDto.Response.class);
            given(dto.getFlowCode()).willReturn(FLOW_CODE.toString());
            CustomPage<StockDto.Response> page = new CustomPage<>(List.of(dto), pageable, 1L);

            given(customStockRepository.findByStockProducts(any(), any(), eq(pageable))).willReturn(page);
            given(statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(List.of(FLOW_CODE)))
                    .willReturn(Collections.emptyList());

            CustomPage<StockDto.Response> result = stockService.getStocks(
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            verify(dto).updateHistory(anyList());
        }
    }

    // -----------------------------------------------------------------------
    // getPastRentalHistory
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getPastRentalHistory")
    class GetPastRentalHistory {

        @Test
        @DisplayName("빈 결과 — 그대로 반환")
        void 빈결과() {
            Pageable pageable = PageRequest.of(0, 20);
            CustomPage<StockDto.Response> empty = new CustomPage<>(Collections.emptyList(), pageable, 0L);
            given(customStockRepository.findStocksByHistoricalPhase(any(), any(), eq(pageable))).willReturn(empty);

            CustomPage<StockDto.Response> result = stockService.getPastRentalHistory(
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("결과 1건 — origin/current Status 가 displayName 으로 치환")
        void 상태_변환() {
            Pageable pageable = PageRequest.of(0, 20);
            StockDto.Response dto = mock(StockDto.Response.class);
            given(dto.getOriginStatus()).willReturn(SourceType.NORMAL.name());
            given(dto.getCurrentStatus()).willReturn(OrderStatus.STOCK.name());
            CustomPage<StockDto.Response> page = new CustomPage<>(List.of(dto), pageable, 1L);
            given(customStockRepository.findStocksByHistoricalPhase(any(), any(), eq(pageable))).willReturn(page);

            stockService.getPastRentalHistory(
                    null, null, null, null, null, null, null, null, null, null,
                    null, null, pageable);

            verify(dto).updateStatus(SourceType.NORMAL.getDisplayName(), OrderStatus.STOCK.getDisplayName());
        }
    }

    // -----------------------------------------------------------------------
    // updateStock
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateStock")
    class UpdateStock {

        @Test
        @DisplayName("stock 없음 → StockNotFoundException")
        void stock_없음() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            StockDto.updateStockRequest dto = mock(StockDto.updateStockRequest.class);

            assertThatThrownBy(() -> stockService.updateStock(TOKEN, FLOW_CODE, dto))
                    .isInstanceOf(StockNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // updateOrderToStock
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateOrderToStock")
    class UpdateOrderToStock {

        @Test
        @DisplayName("order 없음 → OrderNotFoundException")
        void order_없음() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            StockDto.StockRegisterRequest dto = mock(StockDto.StockRegisterRequest.class);

            assertThatThrownBy(() -> stockService.updateOrderToStock(TOKEN, FLOW_CODE, "STOCK", dto))
                    .isInstanceOf(OrderNotFoundException.class);
        }

        @Test
        @DisplayName("이미 stock 이 order 로 매핑되어 있으면 READY_TO_EXPECT 예외 — 멱등성")
        void 이미_재고화됨_existsByOrder() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            Orders order = mock(Orders.class);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(stockRepository.existsByOrder(order)).willReturn(true);

            StockDto.StockRegisterRequest dto = mock(StockDto.StockRegisterRequest.class);

            assertThatThrownBy(() -> stockService.updateOrderToStock(TOKEN, FLOW_CODE, "STOCK", dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 출고");
        }

        @Test
        @DisplayName("동일 flowCode 의 stock 이 존재하면 READY_TO_EXPECT 예외")
        void 동일_flowCode_stock_존재() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            Orders order = mock(Orders.class);
            given(order.getFlowCode()).willReturn(FLOW_CODE);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(stockRepository.existsByOrder(order)).willReturn(false);
            given(stockRepository.existsByFlowCode(FLOW_CODE)).willReturn(true);

            StockDto.StockRegisterRequest dto = mock(StockDto.StockRegisterRequest.class);

            assertThatThrownBy(() -> stockService.updateOrderToStock(TOKEN, FLOW_CODE, "STOCK", dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("잘못된 orderType — OrderStatus.valueOf 실패로 예외")
        void 잘못된_orderType() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            Orders order = orderStubForOrderToStock();
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(stockRepository.existsByOrder(order)).willReturn(false);
            given(stockRepository.existsByFlowCode(any())).willReturn(false);
            given(assistantStoneService.getAssistantStoneView(any()))
                    .willReturn(new AssistantStoneView(null, null, ""));

            StockDto.StockRegisterRequest dto = mock(StockDto.StockRegisterRequest.class);
            given(dto.getAssistantStoneId()).willReturn(null);
            given(dto.getGoldWeight()).willReturn(null);
            given(dto.getStoneWeight()).willReturn(null);
            given(dto.getProductSize()).willReturn("15호");
            given(dto.getStoneInfos()).willReturn(new ArrayList<>());
            given(dto.getMainStoneNote()).willReturn("m");
            given(dto.getAssistanceStoneNote()).willReturn("a");
            given(dto.getOrderNote()).willReturn("note");
            given(dto.getStoneAddLaborCost()).willReturn(0);

            assertThatThrownBy(() -> stockService.updateOrderToStock(TOKEN, FLOW_CODE, "NOT_A_REAL_STATUS", dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // saveStock
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("saveStock")
    class SaveStock {

        @Test
        @DisplayName("정상 — stockRepository.save, statusHistoryHelper.saveCreate, stockCreationService.saveStock 호출")
        void 정상_저장() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            StockDto.Request dto = mock(StockDto.Request.class);
            given(dto.getStoreId()).willReturn(STORE_ID.toString());
            given(dto.getFactoryId()).willReturn(FACTORY_ID.toString());
            given(dto.getProductId()).willReturn(PRODUCT_ID.toString());
            given(dto.getMaterialId()).willReturn("1");
            given(dto.getClassificationId()).willReturn("2");
            given(dto.getColorId()).willReturn("3");
            given(dto.getSetTypeId()).willReturn("4");
            given(dto.getProductName()).willReturn("반지");
            given(dto.getProductFactoryName()).willReturn("공방");
            given(dto.getProductSize()).willReturn("15호");
            given(dto.getMaterialName()).willReturn("18K");
            given(dto.getClassificationName()).willReturn("반지");
            given(dto.getColorName()).willReturn("옐로우");
            given(dto.getSetTypeName()).willReturn("단품");
            given(dto.getIsProductWeightSale()).willReturn(false);
            given(dto.getGoldWeight()).willReturn(new BigDecimal("3.250"));
            given(dto.getStoneWeight()).willReturn(new BigDecimal("0.500"));
            given(dto.getProductPurchaseCost()).willReturn(500_000);
            given(dto.getProductLaborCost()).willReturn(120_000);
            given(dto.getProductAddLaborCost()).willReturn(0);
            given(dto.getAssistantStoneId()).willReturn(null);
            given(dto.getAssistantStoneName()).willReturn(null);
            given(dto.getStoreGrade()).willReturn("A");
            given(dto.getStoreHarry()).willReturn("1.5");
            given(dto.getFactoryHarry()).willReturn("1.2");
            given(dto.getStockNote()).willReturn("재고비고");
            given(dto.getMainStoneNote()).willReturn("m");
            given(dto.getAssistanceStoneNote()).willReturn("a");
            given(dto.getStoneAddLaborCost()).willReturn(0);
            given(dto.getStoneInfos()).willReturn(new ArrayList<>());
            given(dto.isAssistantStone()).willReturn(false);
            given(dto.getAssistantStoneCreateAt()).willReturn(null);

            stockService.saveStock(TOKEN, "NORMAL", dto);

            verify(stockRepository).save(any(Stock.class));
            verify(statusHistoryHelper).saveCreate(any(), eq(SourceType.NORMAL), eq(BusinessPhase.WAITING),
                    anyString(), eq(NICKNAME));
            verify(stockCreationService).saveStock(any());
        }

        @Test
        @DisplayName("잘못된 orderType → SourceType.valueOf 예외")
        void 잘못된_orderType() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            StockDto.Request dto = mock(StockDto.Request.class);
            given(dto.getStoneInfos()).willReturn(new ArrayList<>());
            given(dto.getStoneAddLaborCost()).willReturn(0);
            given(dto.getIsProductWeightSale()).willReturn(false);

            assertThatThrownBy(() -> stockService.saveStock(TOKEN, "NOT_A_TYPE", dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // stockToRental
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("stockToRental")
    class StockToRental {

        @Test
        @DisplayName("stock 없음 → StockNotFoundException")
        void stock_없음() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            StockDto.StockRentalRequest dto = mock(StockDto.StockRentalRequest.class);

            assertThatThrownBy(() -> stockService.stockToRental(TOKEN, FLOW_CODE, dto))
                    .isInstanceOf(StockNotFoundException.class);
        }

        @Test
        @DisplayName("OrderStatus 가 STOCK/NORMAL 이 아니면 IllegalArgumentException")
        void 잘못된_상태() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            Stock stock = mock(Stock.class);
            given(stock.getOrderStatus()).willReturn(OrderStatus.RENTAL);
            given(stock.getFlowCode()).willReturn(FLOW_CODE);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            StockDto.StockRentalRequest dto = mock(StockDto.StockRentalRequest.class);

            assertThatThrownBy(() -> stockService.stockToRental(TOKEN, FLOW_CODE, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("일반 또는 재고");
        }

        @Test
        @DisplayName("SALE 상태 → IllegalArgumentException")
        void sale_상태() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            Stock stock = mock(Stock.class);
            given(stock.getOrderStatus()).willReturn(OrderStatus.SALE);
            given(stock.getFlowCode()).willReturn(FLOW_CODE);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            StockDto.StockRentalRequest dto = mock(StockDto.StockRentalRequest.class);

            assertThatThrownBy(() -> stockService.stockToRental(TOKEN, FLOW_CODE, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // stockToDelete
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("stockToDelete")
    class StockToDelete {

        @Test
        @DisplayName("stock 없음 → StockNotFoundException")
        void stock_없음() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> stockService.stockToDelete(TOKEN, FLOW_CODE))
                    .isInstanceOf(StockNotFoundException.class);
        }

        @Test
        @DisplayName("마지막 history 없음 → IllegalArgumentException(NOT_FOUND)")
        void history_없음() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            Stock stock = mock(Stock.class);
            given(stock.getFlowCode()).willReturn(FLOW_CODE);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> stockService.stockToDelete(TOKEN, FLOW_CODE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("정상 — order null 이면 order 갱신 호출 없이 stock.updateOrderStatus(DELETED) 만 호출")
        void 정상_order_없음() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            Stock stock = mock(Stock.class);
            given(stock.getFlowCode()).willReturn(FLOW_CODE).willReturn(999L);
            given(stock.getOrder()).willReturn(null);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            StatusHistory last = mock(StatusHistory.class);
            given(last.getSourceType()).willReturn(SourceType.NORMAL);
            given(last.getToValue()).willReturn(BusinessPhase.STOCK.name());
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE)).willReturn(Optional.of(last));

            stockService.stockToDelete(TOKEN, FLOW_CODE);

            verify(stock).updateFlowCode();
            verify(statusHistoryHelper).copyAllHistories(eq(FLOW_CODE), anyLong());
            verify(statusHistoryHelper).savePhaseChange(anyLong(), eq(SourceType.NORMAL),
                    eq(BusinessPhase.STOCK), eq(BusinessPhase.DELETED), anyString(), eq(NICKNAME));
            verify(stock).unlinkOrder();
            verify(stock).updateOrderStatus(OrderStatus.DELETED);
        }

        @Test
        @DisplayName("정상 — order 가 있으면 ProductStatus.WAITING / OrderStatus.ORDER 로 되돌림")
        void 정상_order_복구() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            Stock stock = mock(Stock.class);
            given(stock.getFlowCode()).willReturn(FLOW_CODE).willReturn(999L);
            Orders order = mock(Orders.class);
            given(stock.getOrder()).willReturn(order);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            StatusHistory last = mock(StatusHistory.class);
            given(last.getSourceType()).willReturn(SourceType.ORDER);
            given(last.getToValue()).willReturn(BusinessPhase.STOCK.name());
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE)).willReturn(Optional.of(last));

            stockService.stockToDelete(TOKEN, FLOW_CODE);

            verify(order).updateProductStatus(any());
            verify(order).updateOrderStatus(OrderStatus.ORDER);
        }
    }

    // -----------------------------------------------------------------------
    // rentalToReturn
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("rentalToReturn")
    class RentalToReturn {

        @Test
        @DisplayName("stock 없음 → StockNotFoundException")
        void stock_없음() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> stockService.rentalToReturn(TOKEN, FLOW_CODE, "RETURN"))
                    .isInstanceOf(StockNotFoundException.class);
        }

        @Test
        @DisplayName("OrderStatus 가 RENTAL 이 아니면 InvalidOrderStatusException")
        void 잘못된_상태() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            Stock stock = mock(Stock.class);
            given(stock.getOrderStatus()).willReturn(OrderStatus.STOCK);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            assertThatThrownBy(() -> stockService.rentalToReturn(TOKEN, FLOW_CODE, "RETURN"))
                    .isInstanceOf(InvalidOrderStatusException.class);
        }

        @Test
        @DisplayName("정상 — stock.updateOrderStatus(RETURN) + history 추가")
        void 정상() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            Stock stock = mock(Stock.class);
            given(stock.getOrderStatus()).willReturn(OrderStatus.RENTAL);
            given(stock.getFlowCode()).willReturn(FLOW_CODE);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            stockService.rentalToReturn(TOKEN, FLOW_CODE, "RETURN");

            verify(stock).updateOrderStatus(OrderStatus.RETURN);
            verify(statusHistoryHelper).savePhaseChangeFromLast(FLOW_CODE, BusinessPhase.RETURN,
                    "대여 반납", NICKNAME);
        }
    }

    // -----------------------------------------------------------------------
    // rollBackStock
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("rollBackStock")
    class RollBackStock {

        @Test
        @DisplayName("target 이 RETURN/DELETED 가 아니면 InvalidOrderStatusException")
        void 잘못된_target() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);

            assertThatThrownBy(() -> stockService.rollBackStock(TOKEN, FLOW_CODE, "SALE"))
                    .isInstanceOf(InvalidOrderStatusException.class);

            verify(stockRepository, never()).findByFlowCode(any());
        }

        @Test
        @DisplayName("존재하지 않는 OrderStatus → IllegalArgumentException")
        void 알수없는_target() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);

            assertThatThrownBy(() -> stockService.rollBackStock(TOKEN, FLOW_CODE, "NOT_A_STATUS"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("RETURN — stock 없음 → StockNotFoundException")
        void stock_없음() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> stockService.rollBackStock(TOKEN, FLOW_CODE, "RETURN"))
                    .isInstanceOf(StockNotFoundException.class);
        }

        @Test
        @DisplayName("정상 RETURN → stock.updateOrderStatus(STOCK) + history")
        void 정상_RETURN() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            Stock stock = mock(Stock.class);
            given(stock.getFlowCode()).willReturn(FLOW_CODE);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            stockService.rollBackStock(TOKEN, FLOW_CODE, "RETURN");

            verify(stock).updateOrderStatus(OrderStatus.STOCK);
            verify(statusHistoryHelper).savePhaseChangeFromLast(FLOW_CODE, BusinessPhase.STOCK,
                    "반납 재고", NICKNAME);
        }

        @Test
        @DisplayName("정상 DELETED → stock.updateOrderStatus(STOCK)")
        void 정상_DELETED() {
            given(jwtUtil.getNickname(TOKEN)).willReturn(NICKNAME);
            Stock stock = mock(Stock.class);
            given(stock.getFlowCode()).willReturn(FLOW_CODE);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            stockService.rollBackStock(TOKEN, FLOW_CODE, "DELETED");

            verify(stock).updateOrderStatus(OrderStatus.STOCK);
        }
    }

    // -----------------------------------------------------------------------
    // getFilterFactories
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFilterFactories")
    class GetFilterFactories {

        @Test
        @DisplayName("위임 — condition 으로 customStockRepository.findByFilterFactories 호출")
        void 단순_위임() {
            given(customStockRepository.findByFilterFactories(any())).willReturn(List.of("공방1", "공방2"));

            List<String> result = stockService.getFilterFactories("2026-05-01", "2026-05-31", "STOCK");

            assertThat(result).containsExactly("공방1", "공방2");
            verify(customStockRepository).findByFilterFactories(any());
        }

        @Test
        @DisplayName("빈 결과 — 빈 리스트 반환")
        void 빈_결과() {
            given(customStockRepository.findByFilterFactories(any())).willReturn(Collections.emptyList());

            assertThat(stockService.getFilterFactories(null, null, null)).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getFilterStores
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFilterStores")
    class GetFilterStores {

        @Test
        @DisplayName("위임")
        void 위임() {
            given(customStockRepository.findByFilterStores(any())).willReturn(List.of("매장A"));

            List<String> result = stockService.getFilterStores("2026-05-01", "2026-05-31", "STOCK");

            assertThat(result).containsExactly("매장A");
            verify(customStockRepository).findByFilterStores(any());
        }

        @Test
        @DisplayName("빈 결과")
        void 빈() {
            given(customStockRepository.findByFilterStores(any())).willReturn(Collections.emptyList());

            assertThat(stockService.getFilterStores(null, null, null)).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getFilterSetType
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFilterSetType")
    class GetFilterSetType {

        @Test
        @DisplayName("위임")
        void 위임() {
            given(customStockRepository.findByFilterSetType(any())).willReturn(List.of("단품"));

            assertThat(stockService.getFilterSetType("s", "e", "STOCK")).containsExactly("단품");
        }

        @Test
        @DisplayName("빈 결과")
        void 빈() {
            given(customStockRepository.findByFilterSetType(any())).willReturn(Collections.emptyList());
            assertThat(stockService.getFilterSetType(null, null, null)).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getFilterColors
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFilterColors")
    class GetFilterColors {

        @Test
        @DisplayName("위임")
        void 위임() {
            given(customStockRepository.findByFilterColor(any())).willReturn(List.of("옐로우"));

            assertThat(stockService.getFilterColors("s", "e", "STOCK")).containsExactly("옐로우");
        }

        @Test
        @DisplayName("빈 결과")
        void 빈() {
            given(customStockRepository.findByFilterColor(any())).willReturn(Collections.emptyList());
            assertThat(stockService.getFilterColors(null, null, null)).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getFilterClassifications
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFilterClassifications")
    class GetFilterClassifications {

        @Test
        @DisplayName("위임")
        void 위임() {
            given(customStockRepository.findByFilterClassification(any())).willReturn(List.of("반지"));

            assertThat(stockService.getFilterClassifications("s", "e", "STOCK")).containsExactly("반지");
        }

        @Test
        @DisplayName("빈 결과")
        void 빈() {
            given(customStockRepository.findByFilterClassification(any())).willReturn(Collections.emptyList());
            assertThat(stockService.getFilterClassifications(null, null, null)).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getFilterMaterials
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFilterMaterials")
    class GetFilterMaterials {

        @Test
        @DisplayName("위임")
        void 위임() {
            given(customStockRepository.findByFilterMaterial(any())).willReturn(List.of("18K"));

            assertThat(stockService.getFilterMaterials("s", "e", "STOCK")).containsExactly("18K");
        }

        @Test
        @DisplayName("빈 결과")
        void 빈() {
            given(customStockRepository.findByFilterMaterial(any())).willReturn(Collections.emptyList());
            assertThat(stockService.getFilterMaterials(null, null, null)).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getInventoryStocks
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getInventoryStocks")
    class GetInventoryStocks {

        @Test
        @DisplayName("빈 결과 — 빈 페이지 반환")
        void 빈() {
            Pageable pageable = PageRequest.of(0, 20);
            CustomPage<InventoryDto.Response> emptyPage = new CustomPage<>(Collections.emptyList(), pageable, 0L);
            given(customStockRepository.findInventoryStocks(any(), eq(pageable))).willReturn(emptyPage);

            CustomPage<InventoryDto.Response> result = stockService.getInventoryStocks(
                    "productName", "반지", "createAt", "DESC",
                    "all", "STOCK", "18K", pageable);

            assertThat(result.getContent()).isEmpty();
            verify(customStockRepository).findInventoryStocks(any(), eq(pageable));
        }

        @Test
        @DisplayName("결과 1건 — 그대로 반환")
        void 결과_1건() {
            Pageable pageable = PageRequest.of(0, 20);
            InventoryDto.Response item = mock(InventoryDto.Response.class);
            CustomPage<InventoryDto.Response> page = new CustomPage<>(List.of(item), pageable, 1L);
            given(customStockRepository.findInventoryStocks(any(), eq(pageable))).willReturn(page);

            CustomPage<InventoryDto.Response> result = stockService.getInventoryStocks(
                    null, null, null, null, null, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
        }
    }

    // -----------------------------------------------------------------------
    // getInventoryMaterials
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getInventoryMaterials")
    class GetInventoryMaterials {

        @Test
        @DisplayName("위임")
        void 위임() {
            given(customStockRepository.findInventoryMaterials()).willReturn(List.of("18K", "14K"));

            assertThat(stockService.getInventoryMaterials()).containsExactly("18K", "14K");
        }

        @Test
        @DisplayName("빈 결과")
        void 빈() {
            given(customStockRepository.findInventoryMaterials()).willReturn(Collections.emptyList());
            assertThat(stockService.getInventoryMaterials()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // prepareInventoryCheck
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("prepareInventoryCheck")
    class PrepareInventoryCheck {

        @Test
        @DisplayName("정상 — resetCount 가 ResetResponse 에 담겨 반환, message 에 건수 포함")
        void 정상() {
            given(customStockRepository.resetAllStockChecks()).willReturn(42);

            InventoryDto.ResetResponse result = stockService.prepareInventoryCheck();

            assertThat(result.getResetCount()).isEqualTo(42);
            assertThat(result.getMessage()).contains("42");
        }

        @Test
        @DisplayName("0 건 — message 가 0건으로 나옴")
        void 영건() {
            given(customStockRepository.resetAllStockChecks()).willReturn(0);

            InventoryDto.ResetResponse result = stockService.prepareInventoryCheck();

            assertThat(result.getResetCount()).isZero();
            assertThat(result.getMessage()).contains("0");
        }
    }

    // -----------------------------------------------------------------------
    // checkStock
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("checkStock")
    class CheckStock {

        @Test
        @DisplayName("stock 없음 → status=NOT_FOUND")
        void 없음() {
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            InventoryDto.CheckResponse result = stockService.checkStock(FLOW_CODE);

            assertThat(result.getStatus()).isEqualTo("NOT_FOUND");
            assertThat(result.getProductName()).isNull();
        }

        @Test
        @DisplayName("재고 조사 불가 상태(SALE) → NOT_CHECKABLE")
        void 불가_상태() {
            Stock stock = mock(Stock.class);
            ProductSnapshot product = mock(ProductSnapshot.class);
            given(product.getProductName()).willReturn("반지");
            given(stock.getProduct()).willReturn(product);
            given(stock.getOrderStatus()).willReturn(OrderStatus.SALE);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            InventoryDto.CheckResponse result = stockService.checkStock(FLOW_CODE);

            assertThat(result.getStatus()).isEqualTo("NOT_CHECKABLE");
            assertThat(result.getMessage()).contains("SALE");
        }

        @Test
        @DisplayName("이미 조사 완료 → ALREADY_CHECKED")
        void 이미_완료() {
            Stock stock = mock(Stock.class);
            ProductSnapshot product = mock(ProductSnapshot.class);
            given(product.getProductName()).willReturn("반지");
            given(stock.getProduct()).willReturn(product);
            given(stock.getOrderStatus()).willReturn(OrderStatus.STOCK);
            given(stock.getStockChecked()).willReturn(Boolean.TRUE);
            given(stock.getStockCheckedAt()).willReturn(LocalDateTime.now());
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            InventoryDto.CheckResponse result = stockService.checkStock(FLOW_CODE);

            assertThat(result.getStatus()).isEqualTo("ALREADY_CHECKED");
            assertThat(result.getStockCheckedAt()).isNotNull();
        }

        @Test
        @DisplayName("정상 처리 → SUCCESS + markAsChecked 호출")
        void 정상() {
            Stock stock = mock(Stock.class);
            ProductSnapshot product = mock(ProductSnapshot.class);
            given(product.getProductName()).willReturn("반지");
            given(stock.getProduct()).willReturn(product);
            given(stock.getOrderStatus()).willReturn(OrderStatus.STOCK);
            given(stock.getStockChecked()).willReturn(Boolean.FALSE);
            given(stock.getStockCheckedAt()).willReturn(LocalDateTime.now());
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            InventoryDto.CheckResponse result = stockService.checkStock(FLOW_CODE);

            assertThat(result.getStatus()).isEqualTo("SUCCESS");
            verify(stock).markAsChecked();
        }

        @Test
        @DisplayName("product 가 null 이면 productName='알 수 없음'")
        void product_null() {
            Stock stock = mock(Stock.class);
            given(stock.getProduct()).willReturn(null);
            given(stock.getOrderStatus()).willReturn(OrderStatus.RENTAL);
            given(stock.getStockChecked()).willReturn(Boolean.FALSE);
            given(stock.getStockCheckedAt()).willReturn(LocalDateTime.now());
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            InventoryDto.CheckResponse result = stockService.checkStock(FLOW_CODE);

            assertThat(result.getProductName()).isEqualTo("알 수 없음");
            assertThat(result.getStatus()).isEqualTo("SUCCESS");
        }
    }

    // -----------------------------------------------------------------------
    // getInventoryStatistics
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getInventoryStatistics")
    class GetInventoryStatistics {

        @Test
        @DisplayName("빈 결과 — summary 합계 모두 0/0/'0.000'")
        void 빈() {
            given(customStockRepository.findInventoryStatistics(false)).willReturn(Collections.emptyList());
            given(customStockRepository.findInventoryStatistics(true)).willReturn(Collections.emptyList());

            InventoryDto.StatisticsResponse result = stockService.getInventoryStatistics();

            assertThat(result.getUncheckedStatistics()).isEmpty();
            assertThat(result.getCheckedStatistics()).isEmpty();
            assertThat(result.getUncheckedSummary().getTotalQuantity()).isZero();
            assertThat(result.getCheckedSummary().getTotalQuantity()).isZero();
            assertThat(result.getUncheckedSummary().getTotalPurchaseCost()).isZero();
            assertThat(result.getUncheckedSummary().getTotalGoldWeight()).isEqualTo("0.000");
        }

        @Test
        @DisplayName("정상 합계 — 미검사 2건 / 검사 1건 가중치 합산")
        void 정상_합계() {
            InventoryDto.MaterialStatistics unchecked1 = mock(InventoryDto.MaterialStatistics.class);
            given(unchecked1.getTotalGoldWeight()).willReturn("10.500");
            given(unchecked1.getQuantity()).willReturn(3);
            given(unchecked1.getTotalPurchaseCost()).willReturn(100_000L);

            InventoryDto.MaterialStatistics unchecked2 = mock(InventoryDto.MaterialStatistics.class);
            given(unchecked2.getTotalGoldWeight()).willReturn("5.250");
            given(unchecked2.getQuantity()).willReturn(2);
            given(unchecked2.getTotalPurchaseCost()).willReturn(50_000L);

            InventoryDto.MaterialStatistics checked1 = mock(InventoryDto.MaterialStatistics.class);
            given(checked1.getTotalGoldWeight()).willReturn("3.000");
            given(checked1.getQuantity()).willReturn(1);
            given(checked1.getTotalPurchaseCost()).willReturn(30_000L);

            given(customStockRepository.findInventoryStatistics(false)).willReturn(List.of(unchecked1, unchecked2));
            given(customStockRepository.findInventoryStatistics(true)).willReturn(List.of(checked1));

            InventoryDto.StatisticsResponse result = stockService.getInventoryStatistics();

            assertThat(result.getUncheckedSummary().getTotalQuantity()).isEqualTo(5);
            assertThat(result.getUncheckedSummary().getTotalPurchaseCost()).isEqualTo(150_000L);
            assertThat(result.getUncheckedSummary().getTotalGoldWeight()).isEqualTo("15.750");

            assertThat(result.getCheckedSummary().getTotalQuantity()).isEqualTo(1);
            assertThat(result.getCheckedSummary().getTotalPurchaseCost()).isEqualTo(30_000L);
            assertThat(result.getCheckedSummary().getTotalGoldWeight()).isEqualTo("3.000");
        }

        @Test
        @DisplayName("totalGoldWeight 가 비정상 문자열 → NumberFormatException 무시하고 0 으로 처리")
        void 비정상_문자열_무시() {
            InventoryDto.MaterialStatistics bad = mock(InventoryDto.MaterialStatistics.class);
            given(bad.getTotalGoldWeight()).willReturn("not-a-number");
            given(bad.getQuantity()).willReturn(2);
            given(bad.getTotalPurchaseCost()).willReturn(10_000L);

            given(customStockRepository.findInventoryStatistics(false)).willReturn(List.of(bad));
            given(customStockRepository.findInventoryStatistics(true)).willReturn(Collections.emptyList());

            InventoryDto.StatisticsResponse result = stockService.getInventoryStatistics();

            // 숫자 변환 실패는 무시되므로 weight 누적이 0 인 상태로 format
            assertThat(result.getUncheckedSummary().getTotalGoldWeight()).isEqualTo("0.000");
            assertThat(result.getUncheckedSummary().getTotalQuantity()).isEqualTo(2);
            assertThat(result.getUncheckedSummary().getTotalPurchaseCost()).isEqualTo(10_000L);
        }

        @Test
        @DisplayName("null 필드들이 섞여있어도 NPE 없이 통과")
        void null_필드_안전() {
            InventoryDto.MaterialStatistics nullish = mock(InventoryDto.MaterialStatistics.class);
            given(nullish.getTotalGoldWeight()).willReturn(null);
            given(nullish.getQuantity()).willReturn(null);
            given(nullish.getTotalPurchaseCost()).willReturn(null);

            given(customStockRepository.findInventoryStatistics(false)).willReturn(List.of(nullish));
            given(customStockRepository.findInventoryStatistics(true)).willReturn(Collections.emptyList());

            InventoryDto.StatisticsResponse result = stockService.getInventoryStatistics();

            assertThat(result.getUncheckedSummary().getTotalQuantity()).isZero();
            assertThat(result.getUncheckedSummary().getTotalPurchaseCost()).isZero();
            assertThat(result.getUncheckedSummary().getTotalGoldWeight()).isEqualTo("0.000");
        }
    }

    // -----------------------------------------------------------------------
    // getStock
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getStock")
    class GetStock {

        @Test
        @DisplayName("stock 없음 → NotFoundException")
        void 없음() {
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> stockService.getStock(FLOW_CODE))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Stock not found");
        }

        @Test
        @DisplayName("정상 — StockView 의 모든 필드 매핑 확인")
        void 정상() {
            Stock stock = mock(Stock.class);
            ProductSnapshot product = mock(ProductSnapshot.class);
            given(stock.getFlowCode()).willReturn(FLOW_CODE);
            given(stock.getOrderStatus()).willReturn(OrderStatus.STOCK);
            given(stock.getProduct()).willReturn(product);
            given(product.getProductName()).willReturn("반지");
            given(product.getMaterialName()).willReturn("18K");
            given(product.getColorName()).willReturn("옐로우");
            given(product.getGoldWeight()).willReturn(new BigDecimal("3.250"));
            given(product.getStoneWeight()).willReturn(new BigDecimal("0.500"));
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            StockView result = stockService.getStock(FLOW_CODE);

            assertThat(result.flowCode()).isEqualTo(FLOW_CODE);
            assertThat(result.orderStatus()).isEqualTo("STOCK");
            assertThat(result.productName()).isEqualTo("반지");
            assertThat(result.materialName()).isEqualTo("18K");
            assertThat(result.colorName()).isEqualTo("옐로우");
            assertThat(result.goldWeight()).isEqualTo(new BigDecimal("3.250"));
        }

        @Test
        @DisplayName("product / orderStatus 가 null 이어도 NPE 없이 처리")
        void null_안전() {
            Stock stock = mock(Stock.class);
            given(stock.getFlowCode()).willReturn(FLOW_CODE);
            given(stock.getOrderStatus()).willReturn(null);
            given(stock.getProduct()).willReturn(null);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            StockView result = stockService.getStock(FLOW_CODE);

            assertThat(result.flowCode()).isEqualTo(FLOW_CODE);
            assertThat(result.orderStatus()).isNull();
            assertThat(result.productName()).isNull();
            assertThat(result.materialName()).isNull();
            assertThat(result.colorName()).isNull();
            assertThat(result.goldWeight()).isNull();
            assertThat(result.stoneWeight()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------
    private static Stock stubStock() {
        Stock stock = mock(Stock.class);
        ProductSnapshot product = mock(ProductSnapshot.class);
        given(stock.getStoreId()).willReturn(STORE_ID);
        given(stock.getFactoryId()).willReturn(FACTORY_ID);
        given(stock.getStoreHarry()).willReturn(new BigDecimal("1.50"));
        given(stock.getStoreGrade()).willReturn("A");
        given(stock.getOrderStatus()).willReturn(OrderStatus.STOCK);
        given(stock.getOrderStones()).willReturn(new ArrayList<>());
        given(stock.getProduct()).willReturn(product);
        given(product.getMaterialName()).willReturn("18K");
        given(product.getGoldWeight()).willReturn(new BigDecimal("3.250"));
        return stock;
    }

    private static Orders orderStubForOrderToStock() {
        Orders order = mock(Orders.class);
        OrderProduct op = mock(OrderProduct.class);
        given(order.getFlowCode()).willReturn(FLOW_CODE);
        given(order.getStoreId()).willReturn(STORE_ID);
        given(order.getFactoryId()).willReturn(FACTORY_ID);
        given(order.getStoreHarry()).willReturn(new BigDecimal("1.50"));
        given(order.getStoreGrade()).willReturn("A");
        given(order.getFactoryHarry()).willReturn(new BigDecimal("1.20"));
        given(order.getOrderProduct()).willReturn(op);
        given(op.getProductId()).willReturn(PRODUCT_ID);
        given(op.getProductName()).willReturn("반지");
        given(op.getProductFactoryName()).willReturn("공방");
        given(op.getProductLaborCost()).willReturn(120_000);
        given(op.getMaterialId()).willReturn(1L);
        given(op.getMaterialName()).willReturn("18K");
        given(op.getSetTypeId()).willReturn(4L);
        given(op.getSetTypeName()).willReturn("단품");
        given(op.getClassificationId()).willReturn(2L);
        given(op.getClassificationName()).willReturn("반지");
        given(op.getColorId()).willReturn(3L);
        given(op.getColorName()).willReturn("옐로우");
        return order;
    }
}
