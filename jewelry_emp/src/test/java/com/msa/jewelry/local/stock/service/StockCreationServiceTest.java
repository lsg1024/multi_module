package com.msa.jewelry.local.stock.service;

import com.msa.jewelry.global.exception.ExceptionMessage;
import com.msa.jewelry.local.assistant_stone.dto.AssistantStoneView;
import com.msa.jewelry.local.assistant_stone.service.AssistantStoneService;
import com.msa.jewelry.local.classification.service.ClassificationService;
import com.msa.jewelry.local.color.service.ColorService;
import com.msa.jewelry.local.factory.dto.FactoryView;
import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.material.service.MaterialService;
import com.msa.jewelry.local.order.entity.StatusHistory;
import com.msa.jewelry.local.order.entity.order_enum.BusinessPhase;
import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.order.entity.order_enum.SourceType;
import com.msa.jewelry.local.order.repository.StatusHistoryRepository;
import com.msa.jewelry.local.product.dto.ProductDetailView;
import com.msa.jewelry.local.product.service.ProductService;
import com.msa.jewelry.local.set.service.SetTypeService;
import com.msa.jewelry.local.stock.dto.StockCreationRequest;
import com.msa.jewelry.local.stock.entity.ProductSnapshot;
import com.msa.jewelry.local.stock.entity.Stock;
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

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StockCreationService 단위 테스트")
class StockCreationServiceTest {

    private static final Long   FLOW_CODE  = 9_990L;
    private static final Long   STORE_ID   = 100L;
    private static final Long   NEW_STORE_ID = 101L;
    private static final Long   FACTORY_ID = 200L;
    private static final Long   NEW_FACTORY_ID = 201L;
    private static final Long   PRODUCT_ID = 501L;
    private static final String NICKNAME   = "tester";

    @Mock StoreService storeService;
    @Mock FactoryService factoryService;
    @Mock ProductService productService;
    @Mock MaterialService materialService;
    @Mock ColorService colorService;
    @Mock SetTypeService setTypeService;
    @Mock ClassificationService classificationService;
    @Mock AssistantStoneService assistantStoneService;
    @Mock StockRepository stockRepository;
    @Mock StatusHistoryRepository statusHistoryRepository;

    @InjectMocks
    StockCreationService stockCreationService;

    // -----------------------------------------------------------------------
    // saveStock
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("saveStock")
    class SaveStock {

        @Test
        @DisplayName("Stock 없음 → IllegalArgumentException(NOT_FOUND)")
        void stock_없음() {
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            StockCreationRequest req = StockCreationRequest.builder()
                    .flowCode(FLOW_CODE)
                    .nickname(NICKNAME)
                    .build();

            assertThatThrownBy(() -> stockCreationService.saveStock(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("OrderStatus 가 WAIT 가 아니면 조용히 return — 어떤 서비스도 호출되지 않음")
        void 이미_재고화됨_멱등() {
            Stock stock = mock(Stock.class);
            given(stock.getOrderStatus()).willReturn(OrderStatus.STOCK);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            StockCreationRequest req = StockCreationRequest.builder()
                    .flowCode(FLOW_CODE)
                    .nickname(NICKNAME)
                    .build();

            stockCreationService.saveStock(req);

            // history 조회도 안 함
            verify(statusHistoryRepository, never()).findTopByFlowCodeOrderByIdDesc(any());
            verify(productService, never()).getProductDetail(any(), any());
            verify(stockRepository, never()).save(any());
        }

        @Test
        @DisplayName("마지막 history 없음 → IllegalArgumentException(NOT_FOUND)")
        void history_없음() {
            Stock stock = mock(Stock.class);
            given(stock.getOrderStatus()).willReturn(OrderStatus.WAIT);
            given(stock.getFlowCode()).willReturn(FLOW_CODE);
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.empty());

            StockCreationRequest req = StockCreationRequest.builder()
                    .flowCode(FLOW_CODE)
                    .nickname(NICKNAME)
                    .build();

            assertThatThrownBy(() -> stockCreationService.saveStock(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("정상 — store/factory/product 모두 변경 없음 → 동일 정보로 STOCK 상태 전이")
        void 정상_변경없음() {
            Stock stock = stubStockReadyToCommit();
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            StatusHistory last = mock(StatusHistory.class);
            given(last.getSourceType()).willReturn(SourceType.NORMAL);
            given(last.getToValue()).willReturn(BusinessPhase.WAITING.name());
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(last));

            // 동일 정보 — 외부 서비스 호출은 productService 만 발생
            given(productService.getProductDetail(PRODUCT_ID, "A"))
                    .willReturn(new ProductDetailView(PRODUCT_ID, "반지", "공방", 2L, "반지",
                            4L, "단품", 300_000, 50_000));

            StockCreationRequest req = StockCreationRequest.builder()
                    .flowCode(FLOW_CODE)
                    .nickname(NICKNAME)
                    .storeId(STORE_ID)     // 동일
                    .factoryId(FACTORY_ID) // 동일
                    .productId(PRODUCT_ID)
                    .materialId(1L)
                    .classificationId(2L)
                    .colorId(3L)
                    .setTypeId(4L)
                    .assistantStoneId(null)
                    .build();

            stockCreationService.saveStock(req);

            verify(stock).updateOrderStatus(OrderStatus.STOCK);
            verify(stockRepository).save(stock);
            // 동일 정보이므로 store/factory 갱신 호출 없음
            verify(stock, never()).updateStore(any(), any(), any());
            verify(stock, never()).updateFactory(any(), any());
            // 변경 사항 없으면 product.updateProduct 도 호출 안 됨
            // (전부 동일하므로)
        }

        @Test
        @DisplayName("정상 — storeId 변경 시 stock.updateStore 호출됨")
        void store_변경() {
            Stock stock = stubStockReadyToCommit();
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            StatusHistory last = mock(StatusHistory.class);
            given(last.getSourceType()).willReturn(SourceType.NORMAL);
            given(last.getToValue()).willReturn(BusinessPhase.WAITING.name());
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(last));

            given(storeService.getStoreInfoView(NEW_STORE_ID))
                    .willReturn(new StoreView(NEW_STORE_ID, "새매장", "B", "1.7", "SELL", false));
            given(productService.getProductDetail(PRODUCT_ID, "B"))
                    .willReturn(new ProductDetailView(PRODUCT_ID, "반지", "공방", 2L, "반지",
                            4L, "단품", 300_000, 50_000));

            StockCreationRequest req = StockCreationRequest.builder()
                    .flowCode(FLOW_CODE)
                    .nickname(NICKNAME)
                    .storeId(NEW_STORE_ID)
                    .factoryId(FACTORY_ID)
                    .productId(PRODUCT_ID)
                    .build();

            stockCreationService.saveStock(req);

            verify(stock).updateStore(any(), any(), any());
            verify(stock).updateOrderStatus(OrderStatus.STOCK);
        }

        @Test
        @DisplayName("정상 — factoryId 변경 시 stock.updateFactory 호출됨")
        void factory_변경() {
            Stock stock = stubStockReadyToCommit();
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            StatusHistory last = mock(StatusHistory.class);
            given(last.getSourceType()).willReturn(SourceType.NORMAL);
            given(last.getToValue()).willReturn(BusinessPhase.WAITING.name());
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(last));

            given(factoryService.getFactoryInfo(NEW_FACTORY_ID))
                    .willReturn(new FactoryView(NEW_FACTORY_ID, "새공방", "B", "1.4"));
            given(productService.getProductDetail(PRODUCT_ID, "A"))
                    .willReturn(new ProductDetailView(PRODUCT_ID, "반지", "공방", 2L, "반지",
                            4L, "단품", 300_000, 50_000));

            StockCreationRequest req = StockCreationRequest.builder()
                    .flowCode(FLOW_CODE)
                    .nickname(NICKNAME)
                    .storeId(STORE_ID)
                    .factoryId(NEW_FACTORY_ID)
                    .productId(PRODUCT_ID)
                    .build();

            stockCreationService.saveStock(req);

            verify(stock).updateFactory(any(), any());
            verify(stock).updateOrderStatus(OrderStatus.STOCK);
        }

        @Test
        @DisplayName("정상 — material/classification/color/setType 변경 시 product.updateProduct 호출")
        void product_변경() {
            Stock stock = stubStockReadyToCommit();
            ProductSnapshot product = stock.getProduct();
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            StatusHistory last = mock(StatusHistory.class);
            given(last.getSourceType()).willReturn(SourceType.NORMAL);
            given(last.getToValue()).willReturn(BusinessPhase.WAITING.name());
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(last));

            given(materialService.getMaterialName(99L)).willReturn("14K");

            given(productService.getProductDetail(PRODUCT_ID, "A"))
                    .willReturn(new ProductDetailView(PRODUCT_ID, "반지-개명", "공방",
                            2L, "반지", 4L, "단품", 300_000, 50_000));

            StockCreationRequest req = StockCreationRequest.builder()
                    .flowCode(FLOW_CODE)
                    .nickname(NICKNAME)
                    .storeId(STORE_ID)
                    .factoryId(FACTORY_ID)
                    .productId(PRODUCT_ID)
                    .materialId(99L) // 변경
                    .classificationId(2L)
                    .colorId(3L)
                    .setTypeId(4L)
                    .build();

            stockCreationService.saveStock(req);

            verify(product, atLeastOnce()).updateProduct(any(), any(), any(), any(), any(),
                    org.mockito.ArgumentMatchers.anyBoolean(), any(), any(), any());
            verify(stock).updateOrderStatus(OrderStatus.STOCK);
        }

        @Test
        @DisplayName("외부 서비스 예외 발생 시 FAIL 상태 history 저장 + 정상 종료(예외 밖으로 안 새어 나감)")
        void 실패_시_FAIL_히스토리_저장() {
            Stock stock = stubStockReadyToCommit();
            given(stockRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(stock));

            StatusHistory last = mock(StatusHistory.class);
            given(last.getSourceType()).willReturn(SourceType.NORMAL);
            given(last.getToValue()).willReturn(BusinessPhase.WAITING.name());
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(last));

            // productService 가 예외를 던지는 시나리오
            given(productService.getProductDetail(any(), any()))
                    .willThrow(new RuntimeException("외부 서비스 장애"));

            StockCreationRequest req = StockCreationRequest.builder()
                    .flowCode(FLOW_CODE)
                    .nickname(NICKNAME)
                    .storeId(STORE_ID)
                    .factoryId(FACTORY_ID)
                    .productId(PRODUCT_ID)
                    .build();

            // 예외가 밖으로 새어 나가지 않아야 함
            stockCreationService.saveStock(req);

            // FAIL phase 로 history 저장됨
            verify(statusHistoryRepository).save(any(StatusHistory.class));
            // 정상 STOCK 전이는 호출되지 않음
            verify(stock, never()).updateOrderStatus(OrderStatus.STOCK);
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------
    private static Stock stubStockReadyToCommit() {
        Stock stock = mock(Stock.class);
        ProductSnapshot product = mock(ProductSnapshot.class);
        given(stock.getOrderStatus()).willReturn(OrderStatus.WAIT);
        given(stock.getFlowCode()).willReturn(FLOW_CODE);
        given(stock.getStoreId()).willReturn(STORE_ID);
        given(stock.getStoreGrade()).willReturn("A");
        given(stock.getFactoryId()).willReturn(FACTORY_ID);
        given(stock.getProduct()).willReturn(product);

        given(product.getProductName()).willReturn("반지");
        given(product.getMaterialId()).willReturn(1L);
        given(product.getMaterialName()).willReturn("18K");
        given(product.getClassificationId()).willReturn(2L);
        given(product.getClassificationName()).willReturn("반지");
        given(product.getColorId()).willReturn(3L);
        given(product.getColorName()).willReturn("옐로우");
        given(product.getSetTypeId()).willReturn(4L);
        given(product.getSetTypeName()).willReturn("단품");
        given(product.getAssistantStoneId()).willReturn(null);
        given(product.getAssistantStoneName()).willReturn(null);
        return stock;
    }
}
