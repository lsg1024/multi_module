package com.msa.jewelry.local.order.service;

import com.msa.jewelry.global.exception.ExceptionMessage;
import com.msa.jewelry.local.assistant_stone.dto.AssistantStoneView;
import com.msa.jewelry.local.assistant_stone.service.AssistantStoneService;
import com.msa.jewelry.local.color.service.ColorService;
import com.msa.jewelry.local.factory.dto.FactoryView;
import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.material.service.MaterialService;
import com.msa.jewelry.local.order.dto.OrderAsyncRequested;
import com.msa.jewelry.local.order.dto.OrderUpdateRequest;
import com.msa.jewelry.local.order.entity.OrderProduct;
import com.msa.jewelry.local.order.entity.Orders;
import com.msa.jewelry.local.order.entity.StatusHistory;
import com.msa.jewelry.local.order.entity.order_enum.BusinessPhase;
import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.order.entity.order_enum.ProductStatus;
import com.msa.jewelry.local.order.entity.order_enum.SourceType;
import com.msa.jewelry.local.order.repository.OrdersRepository;
import com.msa.jewelry.local.order.repository.StatusHistoryRepository;
import com.msa.jewelry.local.product.dto.ProductDetailView;
import com.msa.jewelry.local.product.service.ProductService;
import com.msa.jewelry.local.store.dto.StoreView;
import com.msa.jewelry.local.store.service.StoreService;
import com.msa.jewelry.local.stone.service.StoneService;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * OrderProcessingService 단위 테스트.
 *
 * <p>외부 의존성을 전부 Mockito 로 대체해 createHandle / updateHandle 두 메서드의
 * 행복 경로 / NOT_FOUND / 외부 호출 실패 분기를 분리해 검증한다.
 *
 * <p>주요 검증:
 * <ul>
 *   <li>Orders/lastHistory NOT_FOUND → IllegalArgumentException</li>
 *   <li>order.orderStatus 가 WAIT 가 아니면 createHandle 은 일찍 return</li>
 *   <li>외부 서비스(productService 등) 실패 시 catch 블럭으로 빠져 StatusHistory.FAIL 저장</li>
 *   <li>storeId/factoryId 일치 시 update* 호출 안 됨 (스냅샷 보존)</li>
 *   <li>updateHandle 의 storeId/factoryId/productId 가 null 인 분기에서 각각 호출 skip</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderProcessingService 단위 테스트")
class OrderProcessingServiceTest {

    private static final Long FLOW_CODE = 9_990L;
    private static final Long STORE_ID = 10L;
    private static final Long FACTORY_ID = 5L;
    private static final Long PRODUCT_ID = 501L;
    private static final Long MATERIAL_ID = 1L;
    private static final Long COLOR_ID = 3L;
    private static final Long ASSIST_ID = 7L;

    @Mock StoreService storeService;
    @Mock StoneService stoneService;
    @Mock ProductService productService;
    @Mock FactoryService factoryService;
    @Mock MaterialService materialService;
    @Mock ColorService colorService;
    @Mock AssistantStoneService assistantStoneService;
    @Mock OrdersRepository ordersRepository;
    @Mock StatusHistoryRepository statusHistoryRepository;

    @InjectMocks
    OrderProcessingService orderProcessingService;

    // -----------------------------------------------------------------------
    // createHandle
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createHandle")
    class CreateHandle {

        @Test
        @DisplayName("Orders 없음 → IllegalArgumentException(NOT_FOUND)")
        void orders_없음() {
            OrderAsyncRequested evt = stubEvent(OrderStatus.STOCK.name());
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderProcessingService.createHandle(evt))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);

            verifyNoInteractions(storeService, factoryService, productService,
                    materialService, colorService, assistantStoneService, stoneService);
        }

        @Test
        @DisplayName("OrderStatus 가 WAIT 가 아니면 조용히 early return — 외부 서비스 호출 없음")
        void 이미_처리됨_early_return() {
            OrderAsyncRequested evt = stubEvent(OrderStatus.STOCK.name());

            Orders order = mock(Orders.class);
            given(order.getOrderStatus()).willReturn(OrderStatus.STOCK); // 이미 STOCK
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));

            orderProcessingService.createHandle(evt);

            verifyNoInteractions(storeService, factoryService, productService,
                    materialService, colorService, assistantStoneService, stoneService);
            verify(ordersRepository, never()).save(any(Orders.class));
        }

        @Test
        @DisplayName("lastHistory 없음 → IllegalArgumentException")
        void lastHistory_없음() {
            OrderAsyncRequested evt = stubEvent(OrderStatus.STOCK.name());

            Orders order = stubOrder(OrderStatus.WAIT);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> orderProcessingService.createHandle(evt))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("정상 — 모든 스냅샷 갱신 후 OrderStatus = STOCK 로 전이")
        void 정상_처리() {
            OrderAsyncRequested evt = stubEvent(OrderStatus.STOCK.name());

            Orders order = stubOrder(OrderStatus.WAIT);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(stubLastHistory()));

            stubAllExternalServicesOk();

            orderProcessingService.createHandle(evt);

            verify(order).updateOrderStatus(OrderStatus.STOCK);
            verify(ordersRepository).save(order);
            // FAIL 이력 저장 안 됨
            verify(statusHistoryRepository, never()).save(any(StatusHistory.class));
        }

        @Test
        @DisplayName("storeId 가 기존과 동일하면 updateStore 호출 안 됨 (스냅샷 보존)")
        void storeId_동일_skip() {
            OrderAsyncRequested evt = stubEvent(OrderStatus.STOCK.name());

            Orders order = stubOrder(OrderStatus.WAIT);
            // storeId 가 일치하도록 stub
            given(order.getStoreId()).willReturn(STORE_ID);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(stubLastHistory()));

            stubAllExternalServicesOk();

            orderProcessingService.createHandle(evt);

            verify(order, never()).updateStore(any(), any(), any());
        }

        @Test
        @DisplayName("stoneIds 중 미존재 stone 발견 → catch 분기로 FAIL StatusHistory 저장")
        void stone_없음_FAIL_이력() {
            OrderAsyncRequested evt = stubEvent(OrderStatus.STOCK.name());

            Orders order = stubOrder(OrderStatus.WAIT);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(stubLastHistory()));

            stubAllExternalServicesOk();
            // stoneService 가 false 반환하도록 override
            given(stoneService.existsStoneId(anyLong())).willReturn(false);

            orderProcessingService.createHandle(evt);

            // catch 로 빠져 FAIL 이력 저장
            ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
            verify(statusHistoryRepository).save(captor.capture());
            assertThat(captor.getValue()).isNotNull();
            // 저장이 한 번도 안 되었어야 함 (catch 전에 save 도달 못 함)
            verify(ordersRepository, never()).save(order);
        }

        @Test
        @DisplayName("외부 storeService 실패 시 catch 분기로 FAIL 이력 저장")
        void 외부서비스_예외_FAIL_이력() {
            OrderAsyncRequested evt = stubEvent(OrderStatus.STOCK.name());

            Orders order = stubOrder(OrderStatus.WAIT);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(stubLastHistory()));

            willThrow(new RuntimeException("store down"))
                    .given(storeService).getStoreInfoView(STORE_ID);

            orderProcessingService.createHandle(evt);

            verify(statusHistoryRepository, times(1)).save(any(StatusHistory.class));
            verify(ordersRepository, never()).save(order);
        }
    }

    // -----------------------------------------------------------------------
    // updateHandle
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateHandle")
    class UpdateHandle {

        @Test
        @DisplayName("Orders 없음 → IllegalArgumentException(NOT_FOUND)")
        void orders_없음() {
            OrderUpdateRequest req = stubUpdateRequest(OrderStatus.STOCK.name());
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderProcessingService.updateHandle(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("lastHistory 없음 → IllegalArgumentException")
        void lastHistory_없음() {
            OrderUpdateRequest req = stubUpdateRequest(OrderStatus.STOCK.name());

            Orders order = stubOrder(OrderStatus.WAIT);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> orderProcessingService.updateHandle(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("정상 — storeId/factoryId/productId 모두 있으면 각각의 update 호출, WAIT 상태면 OrderStatus 전이")
        void 정상_모든값_있음() {
            OrderUpdateRequest req = stubUpdateRequest(OrderStatus.STOCK.name());

            Orders order = stubOrder(OrderStatus.WAIT);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(stubLastHistory()));

            stubAllExternalServicesOk();

            orderProcessingService.updateHandle(req);

            verify(order).updateStore(eq(STORE_ID), anyString(), any());
            verify(order).updateFactory(eq(FACTORY_ID), any());
            verify(order).updateOrderStatus(OrderStatus.STOCK);
            verify(ordersRepository).save(order);
        }

        @Test
        @DisplayName("storeId == null 이면 store 업데이트 skip — productInfo 도 store 분기 안 들어감")
        void storeId_null() {
            OrderUpdateRequest req = mock(OrderUpdateRequest.class);
            given(req.getFlowCode()).willReturn(FLOW_CODE);
            given(req.getOrderStatus()).willReturn(OrderStatus.STOCK.name());
            given(req.getStoreId()).willReturn(null);
            given(req.getFactoryId()).willReturn(null);
            given(req.getProductId()).willReturn(null);
            given(req.getMaterialId()).willReturn(null);
            given(req.getColorId()).willReturn(null);
            given(req.getAssistantStoneId()).willReturn(null);
            given(req.isAssistantStone()).willReturn(false);
            given(req.getAssistantStoneCreateAt()).willReturn(null);
            given(req.getNickname()).willReturn("tester");

            Orders order = stubOrder(OrderStatus.WAIT);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(stubLastHistory()));

            orderProcessingService.updateHandle(req);

            verify(order, never()).updateStore(any(), any(), any());
            verify(order, never()).updateFactory(any(), any());
            verify(storeService, never()).getStoreInfoView(any());
            verify(factoryService, never()).getFactoryInfo(any());
            verify(productService, never()).getProductDetail(any(), any());
        }

        @Test
        @DisplayName("WAIT 가 아닌 상태에서는 OrderStatus 전이 호출 안 됨 — 그 외 update 는 수행")
        void 상태_WAIT_아님_전이_skip() {
            OrderUpdateRequest req = stubUpdateRequest(OrderStatus.STOCK.name());

            Orders order = stubOrder(OrderStatus.STOCK); // 이미 STOCK
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(stubLastHistory()));

            stubAllExternalServicesOk();

            orderProcessingService.updateHandle(req);

            verify(order, never()).updateOrderStatus(any(OrderStatus.class));
            verify(ordersRepository).save(order);
        }

        @Test
        @DisplayName("외부 서비스(productService) 실패 시 CHANG_FAILED 상태 + FAIL 이력 저장")
        void 외부서비스_예외_FAIL_이력() {
            OrderUpdateRequest req = stubUpdateRequest(OrderStatus.STOCK.name());

            Orders order = stubOrder(OrderStatus.WAIT);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(stubLastHistory()));

            given(storeService.getStoreInfoView(STORE_ID)).willReturn(stubStoreView());
            given(factoryService.getFactoryInfo(FACTORY_ID)).willReturn(stubFactoryView());
            willThrow(new RuntimeException("product down"))
                    .given(productService).getProductDetail(any(), any());

            orderProcessingService.updateHandle(req);

            verify(order).updateProductStatus(ProductStatus.CHANG_FAILED);
            ArgumentCaptor<StatusHistory> captor = ArgumentCaptor.forClass(StatusHistory.class);
            verify(statusHistoryRepository).save(captor.capture());
            assertThat(captor.getValue()).isNotNull();
            verify(ordersRepository, never()).save(order);
        }

        @Test
        @DisplayName("assistantStoneId 가 null 이면 assistantStoneService 호출 안 함")
        void 보조석ID_null() {
            OrderUpdateRequest req = mock(OrderUpdateRequest.class);
            given(req.getFlowCode()).willReturn(FLOW_CODE);
            given(req.getOrderStatus()).willReturn(OrderStatus.STOCK.name());
            given(req.getStoreId()).willReturn(STORE_ID);
            given(req.getFactoryId()).willReturn(FACTORY_ID);
            given(req.getProductId()).willReturn(PRODUCT_ID);
            given(req.getMaterialId()).willReturn(MATERIAL_ID);
            given(req.getColorId()).willReturn(COLOR_ID);
            given(req.getAssistantStoneId()).willReturn(null);
            given(req.isAssistantStone()).willReturn(false);
            given(req.getAssistantStoneCreateAt()).willReturn(null);
            given(req.getNickname()).willReturn("tester");

            Orders order = stubOrder(OrderStatus.WAIT);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(FLOW_CODE))
                    .willReturn(Optional.of(stubLastHistory()));

            stubAllExternalServicesOk();

            orderProcessingService.updateHandle(req);

            verify(assistantStoneService, never()).getAssistantStoneView(any());
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------

    private static OrderAsyncRequested stubEvent(String orderStatus) {
        return OrderAsyncRequested.builder()
                .eventId("evt-001")
                .flowCode(FLOW_CODE)
                .tenantId("tenant-001")
                .token("Bearer tk")
                .storeId(STORE_ID)
                .factoryId(FACTORY_ID)
                .productId(PRODUCT_ID)
                .materialId(MATERIAL_ID)
                .colorId(COLOR_ID)
                .assistantStone(true)
                .assistantStoneId(ASSIST_ID)
                .stoneIds(List.of(101L, 102L))
                .orderStatus(orderStatus)
                .nickname("tester")
                .build();
    }

    private static OrderUpdateRequest stubUpdateRequest(String orderStatus) {
        return OrderUpdateRequest.builder()
                .eventId("evt-001")
                .flowCode(FLOW_CODE)
                .tenantId("tenant-001")
                .token("Bearer tk")
                .orderStatus(orderStatus)
                .storeId(STORE_ID)
                .factoryId(FACTORY_ID)
                .productId(PRODUCT_ID)
                .materialId(MATERIAL_ID)
                .colorId(COLOR_ID)
                .assistantStone(true)
                .assistantStoneId(ASSIST_ID)
                .nickname("tester")
                .build();
    }

    private static Orders stubOrder(OrderStatus status) {
        Orders order = mock(Orders.class);
        OrderProduct op = mock(OrderProduct.class);
        given(order.getOrderStatus()).willReturn(status);
        given(order.getOrderProduct()).willReturn(op);
        given(order.getStoreId()).willReturn(999L); // 항상 evt 와 다른 값으로 — update 호출 유도
        given(order.getFactoryId()).willReturn(998L);
        given(order.getStoreGrade()).willReturn("A");
        given(order.getFlowCode()).willReturn(FLOW_CODE);
        given(op.getProductName()).willReturn("old-name");
        given(op.getProductFactoryName()).willReturn("old-factory");
        given(op.getMaterialName()).willReturn("old-mat");
        given(op.getColorName()).willReturn("old-col");
        given(op.getClassificationName()).willReturn("old-cls");
        given(op.getSetTypeName()).willReturn("old-set");
        given(op.getMaterialId()).willReturn(99L);
        given(op.getColorId()).willReturn(98L);
        given(op.getProductId()).willReturn(PRODUCT_ID);
        return order;
    }

    private static StatusHistory stubLastHistory() {
        StatusHistory h = mock(StatusHistory.class);
        given(h.getSourceType()).willReturn(SourceType.ORDER);
        given(h.getToValue()).willReturn(BusinessPhase.WAITING.name());
        given(h.getPhase()).willReturn(BusinessPhase.WAITING);
        return h;
    }

    private static StoreView stubStoreView() {
        return new StoreView(STORE_ID, "강남금은방", "A", "1.50", "SELL", false);
    }

    private static FactoryView stubFactoryView() {
        return new FactoryView(FACTORY_ID, "삼성공방", "A", "1.20");
    }

    private static ProductDetailView stubProductView() {
        return new ProductDetailView(PRODUCT_ID, "P-Name", "P-Factory",
                2L, "반지", 4L, "단품", 100_000, 50_000);
    }

    private static AssistantStoneView stubAssistantView() {
        return new AssistantStoneView(ASSIST_ID, "큐빅", "큐빅 지르코니아");
    }

    /**
     * 외부 서비스 행복 경로 stub 묶음.
     */
    private void stubAllExternalServicesOk() {
        given(storeService.getStoreInfoView(STORE_ID)).willReturn(stubStoreView());
        given(factoryService.getFactoryInfo(FACTORY_ID)).willReturn(stubFactoryView());
        given(productService.getProductDetail(any(), any())).willReturn(stubProductView());
        given(materialService.getMaterialName(MATERIAL_ID)).willReturn("18K");
        given(colorService.getColorName(COLOR_ID)).willReturn("옐로우골드");
        given(assistantStoneService.getAssistantStoneView(ASSIST_ID)).willReturn(stubAssistantView());
        given(stoneService.existsStoneId(anyLong())).willReturn(true);
    }
}
