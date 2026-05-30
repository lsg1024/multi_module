package com.msa.jewelry.local.order.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.exception.ExceptionMessage;
import com.msa.jewelry.global.exception.InvalidOrderStatusException;
import com.msa.jewelry.global.exception.OrderNotFoundException;
import com.msa.jewelry.local.factory.dto.FactoryView;
import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.order.dto.DateDto;
import com.msa.jewelry.local.order.dto.FactoryDto;
import com.msa.jewelry.local.order.dto.OrderDto;
import com.msa.jewelry.local.order.dto.StoreDto;
import com.msa.jewelry.local.order.entity.OrderProduct;
import com.msa.jewelry.local.order.entity.OrderStone;
import com.msa.jewelry.local.order.entity.Orders;
import com.msa.jewelry.local.order.entity.StatusHistory;
import com.msa.jewelry.local.order.entity.order_enum.BusinessPhase;
import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.order.entity.order_enum.ProductStatus;
import com.msa.jewelry.local.order.entity.order_enum.SourceType;
import com.msa.jewelry.local.order.repository.CustomOrderRepository;
import com.msa.jewelry.local.order.repository.OrdersRepository;
import com.msa.jewelry.local.order.repository.StatusHistoryRepository;
import com.msa.jewelry.local.order.util.StatusHistoryHelper;
import com.msa.jewelry.local.priority.entity.Priority;
import com.msa.jewelry.local.priority.repository.PriorityRepository;
import com.msa.jewelry.local.product.service.ProductService;
import com.msa.jewelry.local.store.dto.StoreView;
import com.msa.jewelry.local.store.service.StoreService;
import org.junit.jupiter.api.*;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrdersService 단위 테스트")
class OrdersServiceTest {

    private static final String TOKEN     = "Bearer test-token";
    private static final String TENANT_ID = "tenant-001";
    private static final String NICKNAME  = "tester";
    private static final Long   FLOW_CODE = 9_990L;
    private static final Long   STORE_ID  = 10L;
    private static final Long   FACTORY_ID = 5L;

    @Mock JwtUtil jwtUtil;
    @Mock FactoryService factoryService;
    @Mock StoreService storeService;
    @Mock ProductService productService;
    @Mock OrdersRepository ordersRepository;
    @Mock CustomOrderRepository customOrderRepository;
    @Mock StatusHistoryRepository statusHistoryRepository;
    @Mock PriorityRepository priorityRepository;
    @Mock StatusHistoryHelper statusHistoryHelper;
    @Mock OrderCommandService orderCommandService;

    @InjectMocks
    OrdersService ordersService;

    @BeforeEach
    void commonStubs() {
        given(jwtUtil.getTenantId(anyString())).willReturn(TENANT_ID);
        given(jwtUtil.getNickname(anyString())).willReturn(NICKNAME);
        TenantContext.setTenant(TENANT_ID);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    // -----------------------------------------------------------------------
    // getOrder
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getOrder")
    class GetOrder {

        @Test
        @DisplayName("정상 — Orders 존재, storeName/factoryName 모두 조회됨")
        void 정상_조회() {
            Orders order = stubOrderForGetOrder();
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(storeService.getStoreInfoView(STORE_ID))
                    .willReturn(new StoreView(STORE_ID, "강남금은방", "A", "1.50", "SELL", false));
            given(factoryService.getFactoryInfo(FACTORY_ID))
                    .willReturn(new FactoryView(FACTORY_ID, "삼성공방", "A", "1.20"));

            OrderDto.ResponseDetail resp = ordersService.getOrder(FLOW_CODE);

            assertThat(resp).isNotNull();
            assertThat(resp.getFlowCode()).isEqualTo(FLOW_CODE.toString());
            assertThat(resp.getStoreName()).isEqualTo("강남금은방");
            assertThat(resp.getFactoryName()).isEqualTo("삼성공방");
        }

        @Test
        @DisplayName("Orders 없음 → OrderNotFoundException")
        void orders_없음() {
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ordersService.getOrder(FLOW_CODE))
                    .isInstanceOf(OrderNotFoundException.class);

            verifyNoInteractions(storeService, factoryService);
        }

        @Test
        @DisplayName("factoryId 가 null 이면 factoryService 호출 안 됨 — factoryName null 반환")
        void factoryId_null() {
            Orders order = stubOrderForGetOrder();
            given(order.getFactoryId()).willReturn(null);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));
            given(storeService.getStoreInfoView(STORE_ID))
                    .willReturn(new StoreView(STORE_ID, "강남금은방", "A", "1.50", "SELL", false));

            OrderDto.ResponseDetail resp = ordersService.getOrder(FLOW_CODE);

            assertThat(resp.getFactoryName()).isNull();
            verify(factoryService, never()).getFactoryInfo(anyLong());
        }
    }

    // -----------------------------------------------------------------------
    // getOrderProducts
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getOrderProducts")
    class GetOrderProducts {

        @Test
        @DisplayName("빈 결과 — content 비어있고 정상 응답")
        void 빈결과() {
            Pageable pageable = PageRequest.of(0, 20);
            given(customOrderRepository.findByOrders(any(), any(), eq(pageable)))
                    .willReturn(new CustomPage<>(Collections.emptyList(), pageable, 0L));
            given(productService.getProductImages(anyList())).willReturn(Map.of());
            given(statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(anyList()))
                    .willReturn(Collections.emptyList());

            CustomPage<OrderDto.Response> result = ordersService.getOrderProducts(
                    TOKEN, "input", "field", "2026-05-01", "2026-05-31",
                    null, null, null, null, null, null, null, null, "WAIT", pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }

    // -----------------------------------------------------------------------
    // saveOrder
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("saveOrder")
    class SaveOrder {

        @Test
        @DisplayName("정상 — orderCommandService.createOrder 위임 + statusHistoryHelper.saveCreate 호출")
        void 정상_위임() {
            OrderDto.Request dto = mock(OrderDto.Request.class);
            Orders saved = mock(Orders.class);
            given(saved.getFlowCode()).willReturn(FLOW_CODE);
            given(orderCommandService.createOrder(eq(TENANT_ID), eq(TOKEN), eq("ORDER"), eq(dto), eq(NICKNAME)))
                    .willReturn(saved);

            ordersService.saveOrder(TOKEN, "ORDER", dto);

            verify(orderCommandService).createOrder(TENANT_ID, TOKEN, "ORDER", dto, NICKNAME);
            verify(statusHistoryHelper).saveCreate(
                    eq(FLOW_CODE), eq(SourceType.ORDER), eq(BusinessPhase.WAITING),
                    eq("주문 등록"), eq(NICKNAME));
        }

        @Test
        @DisplayName("orderStatus 가 SourceType 에 없는 값이면 IllegalArgumentException 전파")
        void 잘못된_orderStatus() {
            OrderDto.Request dto = mock(OrderDto.Request.class);
            Orders saved = mock(Orders.class);
            given(saved.getFlowCode()).willReturn(FLOW_CODE);
            given(orderCommandService.createOrder(any(), any(), any(), any(), any()))
                    .willReturn(saved);

            assertThatThrownBy(() -> ordersService.saveOrder(TOKEN, "NOT_REAL_SOURCE", dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    // updateOrder
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateOrder")
    class UpdateOrder {

        @Test
        @DisplayName("정상 — 변경 사항 트래킹 후 commandService 위임")
        void 정상_위임() {
            OrderDto.Request dto = stubUpdateDto();
            Orders beforeOrder = stubExistingOrder();
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(beforeOrder));
            Orders after = mock(Orders.class);
            given(after.getFlowCode()).willReturn(FLOW_CODE);
            given(orderCommandService.updateOrder(eq(TENANT_ID), eq(TOKEN), eq(FLOW_CODE), eq("ORDER"), eq(dto), eq(NICKNAME)))
                    .willReturn(after);

            ordersService.updateOrder(TOKEN, FLOW_CODE, "ORDER", dto);

            verify(orderCommandService).updateOrder(TENANT_ID, TOKEN, FLOW_CODE, "ORDER", dto, NICKNAME);
            verify(statusHistoryHelper).savePhaseChangeFromLast(
                    eq(FLOW_CODE), eq(BusinessPhase.UPDATE), anyString(), eq(NICKNAME));
        }

        @Test
        @DisplayName("Orders 못 찾으면 OrderNotFoundException — commandService 호출 안 됨")
        void orders_없음() {
            OrderDto.Request dto = stubUpdateDto();
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ordersService.updateOrder(TOKEN, FLOW_CODE, "ORDER", dto))
                    .isInstanceOf(OrderNotFoundException.class);

            verifyNoInteractions(orderCommandService, statusHistoryHelper);
        }
    }

    // -----------------------------------------------------------------------
    // getOrderStatusInfo
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getOrderStatusInfo")
    class GetOrderStatusInfo {

        @Test
        @DisplayName("flowCode 존재 → ProductStatus displayName 리스트 반환")
        void 정상() {
            given(ordersRepository.existsByFlowCodeAndProductStatusIn(eq(1L), anyList())).willReturn(true);

            List<String> result = ordersService.getOrderStatusInfo("1");

            assertThat(result).containsExactlyInAnyOrder(
                    ProductStatus.RECEIPT.getDisplayName(),
                    ProductStatus.RECEIPT_FAILED.getDisplayName(),
                    ProductStatus.WAITING.getDisplayName());
        }

        @Test
        @DisplayName("flowCode 없음 → OrderNotFoundException")
        void 없음() {
            given(ordersRepository.existsByFlowCodeAndProductStatusIn(eq(1L), anyList())).willReturn(false);

            assertThatThrownBy(() -> ordersService.getOrderStatusInfo("1"))
                    .isInstanceOf(OrderNotFoundException.class);
        }

        @Test
        @DisplayName("숫자가 아닌 id → NumberFormatException")
        void 비숫자_id() {
            assertThatThrownBy(() -> ordersService.getOrderStatusInfo("abc"))
                    .isInstanceOf(NumberFormatException.class);
        }
    }

    // -----------------------------------------------------------------------
    // updateOrderStatus
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateOrderStatus")
    class UpdateOrderStatus {

        @Test
        @DisplayName("정상 — RECEIPT 로 변경, status 업데이트 + history 저장")
        void 정상_RECEIPT() {
            Orders order = mock(Orders.class);
            given(order.getOrderStatus()).willReturn(OrderStatus.ORDER);
            given(order.getProductStatus()).willReturn(ProductStatus.WAITING);
            given(order.getFlowCode()).willReturn(1L);
            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.of(order));

            ordersService.updateOrderStatus(TOKEN, "1", "RECEIPT");

            verify(order).updateProductStatus(ProductStatus.RECEIPT);
            verify(ordersRepository).save(order);
            verify(statusHistoryHelper).savePhaseChangeFromLast(
                    eq(1L), eq(BusinessPhase.UPDATE), anyString(), eq(NICKNAME));
        }

        @Test
        @DisplayName("소문자 status 도 정상 처리 (toUpperCase)")
        void 소문자_status() {
            Orders order = mock(Orders.class);
            given(order.getOrderStatus()).willReturn(OrderStatus.ORDER);
            given(order.getProductStatus()).willReturn(ProductStatus.WAITING);
            given(order.getFlowCode()).willReturn(1L);
            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.of(order));

            ordersService.updateOrderStatus(TOKEN, "1", "receipt");

            verify(order).updateProductStatus(ProductStatus.RECEIPT);
        }

        @Test
        @DisplayName("status 가 허용 목록에 없으면 InvalidOrderStatusException")
        void 잘못된_status() {
            assertThatThrownBy(() -> ordersService.updateOrderStatus(TOKEN, "1", "DELETED"))
                    .isInstanceOf(InvalidOrderStatusException.class)
                    .hasMessageContaining("상태를 변경할 수 없");
        }

        @Test
        @DisplayName("Orders 없음 → OrderNotFoundException")
        void orders_없음() {
            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ordersService.updateOrderStatus(TOKEN, "1", "RECEIPT"))
                    .isInstanceOf(OrderNotFoundException.class);
        }

        @Test
        @DisplayName("현재 OrderStatus 가 SALE 등 허용 외 → InvalidOrderStatusException")
        void 현재상태_변경불가() {
            Orders order = mock(Orders.class);
            given(order.getOrderStatus()).willReturn(OrderStatus.SALE);
            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.of(order));

            assertThatThrownBy(() -> ordersService.updateOrderStatus(TOKEN, "1", "RECEIPT"))
                    .isInstanceOf(InvalidOrderStatusException.class)
                    .hasMessageContaining("주문, 수리, 일반");
        }
    }

    // -----------------------------------------------------------------------
    // updateOrderStore
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateOrderStore")
    class UpdateOrderStore {

        @Test
        @DisplayName("정상 — store 변경 호출 + history 저장")
        void 정상() {
            Orders order = mock(Orders.class);
            given(order.getStoreId()).willReturn(10L);
            given(order.getFlowCode()).willReturn(1L);
            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.of(order));
            given(storeService.getStoreInfoView(10L))
                    .willReturn(new StoreView(10L, "기존매장", "A", "1.50", "SELL", false));
            given(storeService.getStoreInfoView(20L))
                    .willReturn(new StoreView(20L, "변경매장", "B", "2.00", "SELL", false));

            StoreDto.Request req = mock(StoreDto.Request.class);
            given(req.getStoreId()).willReturn(20L);

            ordersService.updateOrderStore(TOKEN, "1", req);

            verify(order).updateStore(eq(20L), eq("B"), any(BigDecimal.class));
            verify(statusHistoryHelper).savePhaseChangeFromLast(
                    eq(1L), eq(BusinessPhase.UPDATE), anyString(), eq(NICKNAME));
        }

        @Test
        @DisplayName("기존 storeId 가 null 이면 기존 매장 조회 안 함")
        void 기존_storeId_null() {
            Orders order = mock(Orders.class);
            given(order.getStoreId()).willReturn(null);
            given(order.getFlowCode()).willReturn(1L);
            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.of(order));
            given(storeService.getStoreInfoView(20L))
                    .willReturn(new StoreView(20L, "변경매장", "B", "2.00", "SELL", false));

            StoreDto.Request req = mock(StoreDto.Request.class);
            given(req.getStoreId()).willReturn(20L);

            ordersService.updateOrderStore(TOKEN, "1", req);

            verify(storeService).getStoreInfoView(20L);
            verify(order).updateStore(eq(20L), any(), any());
        }

        @Test
        @DisplayName("Orders 없음 → OrderNotFoundException")
        void orders_없음() {
            StoreDto.Request req = mock(StoreDto.Request.class);
            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ordersService.updateOrderStore(TOKEN, "1", req))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // updateOrderFactory
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateOrderFactory")
    class UpdateOrderFactory {

        @Test
        @DisplayName("정상 — factory 변경 호출 + history 저장")
        void 정상() {
            Orders order = mock(Orders.class);
            given(order.getFactoryId()).willReturn(5L);
            given(order.getFlowCode()).willReturn(1L);
            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.of(order));
            given(factoryService.getFactoryInfo(5L))
                    .willReturn(new FactoryView(5L, "기존공방", "A", "1.20"));
            given(factoryService.getFactoryInfo(7L))
                    .willReturn(new FactoryView(7L, "신규공방", "B", "1.30"));

            FactoryDto.Request req = mock(FactoryDto.Request.class);
            given(req.getFactoryId()).willReturn(7L);

            ordersService.updateOrderFactory(TOKEN, "1", req);

            verify(order).updateFactory(eq(7L), any(BigDecimal.class));
            verify(statusHistoryHelper).savePhaseChangeFromLast(
                    eq(1L), eq(BusinessPhase.UPDATE), anyString(), eq(NICKNAME));
        }

        @Test
        @DisplayName("기존 factoryId 가 null 이면 기존 제조사 조회 안 함")
        void 기존_factoryId_null() {
            Orders order = mock(Orders.class);
            given(order.getFactoryId()).willReturn(null);
            given(order.getFlowCode()).willReturn(1L);
            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.of(order));
            given(factoryService.getFactoryInfo(7L))
                    .willReturn(new FactoryView(7L, "신규공방", "B", "1.30"));

            FactoryDto.Request req = mock(FactoryDto.Request.class);
            given(req.getFactoryId()).willReturn(7L);

            ordersService.updateOrderFactory(TOKEN, "1", req);

            verify(factoryService).getFactoryInfo(7L);
        }

        @Test
        @DisplayName("Orders 없음 → OrderNotFoundException")
        void orders_없음() {
            FactoryDto.Request req = mock(FactoryDto.Request.class);
            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ordersService.updateOrderFactory(TOKEN, "1", req))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // updateOrderDeliveryDate
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateOrderDeliveryDate")
    class UpdateOrderDeliveryDate {

        @Test
        @DisplayName("정상 — shippingDate 업데이트 + history 저장")
        void 정상() {
            Orders order = mock(Orders.class);
            given(order.getShippingAt()).willReturn(LocalDateTime.of(2026, 5, 20, 10, 0));
            given(order.getFlowCode()).willReturn(1L);
            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.of(order));

            DateDto dto = mock(DateDto.class);
            LocalDateTime newDate = LocalDateTime.of(2026, 5, 25, 10, 0);
            given(dto.getDeliveryDate()).willReturn(newDate);

            ordersService.updateOrderDeliveryDate(TOKEN, "1", dto);

            verify(order).updateShippingDate(newDate);
            verify(statusHistoryHelper).savePhaseChangeFromLast(
                    eq(1L), eq(BusinessPhase.UPDATE), anyString(), eq(NICKNAME));
        }

        @Test
        @DisplayName("기존 shippingAt 이 null 이어도 NPE 없이 처리")
        void 기존_shippingAt_null() {
            Orders order = mock(Orders.class);
            given(order.getShippingAt()).willReturn(null);
            given(order.getFlowCode()).willReturn(1L);
            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.of(order));

            DateDto dto = mock(DateDto.class);
            LocalDateTime newDate = LocalDateTime.of(2026, 5, 25, 10, 0);
            given(dto.getDeliveryDate()).willReturn(newDate);

            ordersService.updateOrderDeliveryDate(TOKEN, "1", dto);

            verify(order).updateShippingDate(newDate);
        }

        @Test
        @DisplayName("Orders 없음 → OrderNotFoundException")
        void orders_없음() {
            DateDto dto = mock(DateDto.class);
            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> ordersService.updateOrderDeliveryDate(TOKEN, "1", dto))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    // -----------------------------------------------------------------------
    // deletedOrders
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("deletedOrders")
    class DeletedOrders {

        @Test
        @DisplayName("정상 — orderCommandService.deleteOrder 위임 + history 저장")
        void 정상_위임() {
            given(jwtUtil.getRole(TOKEN)).willReturn("USER");

            ordersService.deletedOrders(TOKEN, "1");

            verify(orderCommandService).deleteOrder(1L, "USER");
            verify(statusHistoryHelper).savePhaseChangeFromLast(
                    eq(1L), eq(BusinessPhase.DELETED), eq("주문 삭제"), eq(NICKNAME));
        }

        @Test
        @DisplayName("commandService 가 NOT_ACCESS 던지면 예외 전파, history 호출 안 됨")
        void 권한_거부_전파() {
            given(jwtUtil.getRole(TOKEN)).willReturn("WAIT");
            org.mockito.BDDMockito.willThrow(new IllegalArgumentException(ExceptionMessage.NOT_ACCESS))
                    .given(orderCommandService).deleteOrder(1L, "WAIT");

            assertThatThrownBy(() -> ordersService.deletedOrders(TOKEN, "1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_ACCESS);

            verifyNoInteractions(statusHistoryHelper);
        }
    }

    // -----------------------------------------------------------------------
    // getFixProducts
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFixProducts")
    class GetFixProducts {

        @Test
        @DisplayName("빈 결과 → 빈 페이지 반환")
        void 빈결과() {
            Pageable pageable = PageRequest.of(0, 20);
            given(customOrderRepository.findByFixOrders(any(), any(), eq(pageable)))
                    .willReturn(new CustomPage<>(Collections.emptyList(), pageable, 0L));
            given(productService.getProductImages(anyList())).willReturn(Map.of());
            given(statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(anyList()))
                    .willReturn(Collections.emptyList());

            CustomPage<OrderDto.Response> result = ordersService.getFixProducts(
                    TOKEN, "input", "field", "2026-05-01", "2026-05-31",
                    null, null, null, null, null, null, null, null, "FIX", pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getDeliveryProducts
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getDeliveryProducts")
    class GetDeliveryProducts {

        @Test
        @DisplayName("빈 결과 → 빈 페이지 반환")
        void 빈결과() {
            Pageable pageable = PageRequest.of(0, 20);
            given(customOrderRepository.findByDeliveryOrders(any(), any(), eq(pageable)))
                    .willReturn(new CustomPage<>(Collections.emptyList(), pageable, 0L));
            given(productService.getProductImages(anyList())).willReturn(Map.of());
            given(statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(anyList()))
                    .willReturn(Collections.emptyList());

            CustomPage<OrderDto.Response> result = ordersService.getDeliveryProducts(
                    TOKEN, "input", "field", "2026-05-31",
                    null, null, null, null, null, null, null, null, pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getDeletedProducts
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getDeletedProducts")
    class GetDeletedProducts {

        @Test
        @DisplayName("빈 결과 → 빈 페이지 반환")
        void 빈결과() {
            Pageable pageable = PageRequest.of(0, 20);
            given(customOrderRepository.findByDeletedOrders(any(), any(), eq(pageable)))
                    .willReturn(new CustomPage<>(Collections.emptyList(), pageable, 0L));
            given(productService.getProductImages(anyList())).willReturn(Map.of());
            given(statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(anyList()))
                    .willReturn(Collections.emptyList());

            CustomPage<OrderDto.Response> result = ordersService.getDeletedProducts(
                    TOKEN, "input", "field", "2026-05-01", "2026-05-31",
                    null, null, null, null, null, null, null, null, "DELETED", pageable);

            assertThat(result.getContent()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getExcel
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getExcel")
    class GetExcel {

        @Test
        @DisplayName("Repository 위임 — condition 그대로 전달 후 결과 그대로 반환")
        void 위임검증() {
            given(customOrderRepository.findByExcelData(any(), any())).willReturn(Collections.emptyList());

            List<?> result = ordersService.getExcel(
                    "2026-05-01", "2026-05-31", "search", "field",
                    null, null, null, null, null, null, "WAIT");

            assertThat(result).isEmpty();
            verify(customOrderRepository).findByExcelData(any(OrderDto.InputCondition.class), any(OrderDto.OrderCondition.class));
        }
    }

    // -----------------------------------------------------------------------
    // getFilterFactories / Stores / SetType / Colors / Classifications / Materials
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFilter* (단순 위임)")
    class GetFilters {

        @Test
        @DisplayName("getFilterFactories — repository 위임")
        void factories() {
            given(customOrderRepository.findByFilterFactories(any())).willReturn(List.of("삼성공방"));

            List<String> result = ordersService.getFilterFactories(
                    "2026-05-01", "2026-05-31", null, null, null, null, null, null, "WAIT");

            assertThat(result).containsExactly("삼성공방");
            verify(customOrderRepository).findByFilterFactories(any(OrderDto.OrderCondition.class));
        }

        @Test
        @DisplayName("getFilterStores — repository 위임")
        void stores() {
            given(customOrderRepository.findByFilterStores(any())).willReturn(List.of("강남금은방"));

            List<String> result = ordersService.getFilterStores(
                    "2026-05-01", "2026-05-31", null, null, null, null, null, null, "WAIT");

            assertThat(result).containsExactly("강남금은방");
        }

        @Test
        @DisplayName("getFilterSetType — repository 위임")
        void setType() {
            given(customOrderRepository.findByFilterSetType(any())).willReturn(List.of("단품"));

            List<String> result = ordersService.getFilterSetType(
                    "2026-05-01", "2026-05-31", null, null, null, null, null, null, "WAIT");

            assertThat(result).containsExactly("단품");
        }

        @Test
        @DisplayName("getFilterColors — repository 위임")
        void colors() {
            given(customOrderRepository.findByFilterColor(any())).willReturn(List.of("옐로우골드"));

            List<String> result = ordersService.getFilterColors(
                    "2026-05-01", "2026-05-31", null, null, null, null, null, null, "WAIT");

            assertThat(result).containsExactly("옐로우골드");
        }

        @Test
        @DisplayName("getFilterClassifications — repository 위임")
        void classifications() {
            given(customOrderRepository.findByFilterClassification(any())).willReturn(List.of("반지"));

            List<String> result = ordersService.getFilterClassifications(
                    "2026-05-01", "2026-05-31", null, null, null, null, null, null, "WAIT");

            assertThat(result).containsExactly("반지");
        }

        @Test
        @DisplayName("getFilterMaterials — repository 위임")
        void materials() {
            given(customOrderRepository.findByFilterMaterial(any())).willReturn(List.of("18K"));

            List<String> result = ordersService.getFilterMaterials(
                    "2026-05-01", "2026-05-31", null, null, null, null, null, null, "WAIT");

            assertThat(result).containsExactly("18K");
        }

        @Test
        @DisplayName("빈 결과도 정상 처리")
        void 빈결과() {
            given(customOrderRepository.findByFilterFactories(any())).willReturn(Collections.emptyList());

            assertThat(ordersService.getFilterFactories(
                    "2026-05-01", "2026-05-31", null, null, null, null, null, null, "WAIT"))
                    .isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getOrderRegisterStock
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getOrderRegisterStock")
    class GetOrderRegisterStock {

        @Test
        @DisplayName("빈 flowCodes 입력 — 빈 결과")
        void 빈입력() {
            given(ordersRepository.findWithDetailsByFlowCodeIn(anyList())).willReturn(Collections.emptyList());
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDescIn(anyList()))
                    .willReturn(Collections.emptyList());

            List<?> result = ordersService.getOrderRegisterStock(Collections.emptyList());

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("정상 — Orders 한 건, 매핑 결과 정상 반환")
        void 정상_한건() {
            Orders order = stubOrderForGetRegisterStock();
            given(ordersRepository.findWithDetailsByFlowCodeIn(anyList())).willReturn(List.of(order));

            StatusHistory hist = mock(StatusHistory.class);
            given(hist.getFlowCode()).willReturn(FLOW_CODE);
            given(hist.getId()).willReturn(1L);
            given(hist.getSourceType()).willReturn(SourceType.ORDER);
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDescIn(anyList()))
                    .willReturn(List.of(hist));

            given(storeService.getStoreInfoView(STORE_ID))
                    .willReturn(new StoreView(STORE_ID, "강남금은방", "A", "1.50", "SELL", false));
            given(factoryService.getFactoryInfo(FACTORY_ID))
                    .willReturn(new FactoryView(FACTORY_ID, "삼성공방", "A", "1.20"));

            List<?> result = ordersService.getOrderRegisterStock(List.of(FLOW_CODE));

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("StatusHistory 가 여러 건이어도 flowCode 별 id 최대값만 매핑 — IndexOutOfBoundsException 방어")
        void 여러_히스토리_최대id_선택() {
            Orders order = stubOrderForGetRegisterStock();
            given(ordersRepository.findWithDetailsByFlowCodeIn(anyList())).willReturn(List.of(order));

            StatusHistory older = mock(StatusHistory.class);
            given(older.getFlowCode()).willReturn(FLOW_CODE);
            given(older.getId()).willReturn(1L);
            given(older.getSourceType()).willReturn(SourceType.ORDER);

            StatusHistory newer = mock(StatusHistory.class);
            given(newer.getFlowCode()).willReturn(FLOW_CODE);
            given(newer.getId()).willReturn(5L);
            given(newer.getSourceType()).willReturn(SourceType.FIX);

            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDescIn(anyList()))
                    .willReturn(List.of(older, newer));

            given(storeService.getStoreInfoView(STORE_ID))
                    .willReturn(new StoreView(STORE_ID, "강남금은방", "A", "1.50", "SELL", false));
            given(factoryService.getFactoryInfo(FACTORY_ID))
                    .willReturn(new FactoryView(FACTORY_ID, "삼성공방", "A", "1.20"));

            List<?> result = ordersService.getOrderRegisterStock(List.of(FLOW_CODE));

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Orders 의 storeId/factoryId 가 null 이면 외부 호출 안 함, null 매핑 안전")
        void null필드_안전() {
            Orders order = mock(Orders.class);
            OrderProduct op = mock(OrderProduct.class);
            given(order.getOrderProduct()).willReturn(op);
            given(order.getOrderStones()).willReturn(new ArrayList<>());
            given(order.getFlowCode()).willReturn(FLOW_CODE);
            given(order.getStoreId()).willReturn(null);
            given(order.getFactoryId()).willReturn(null);
            given(order.getCreateAt()).willReturn(LocalDateTime.now());
            given(ordersRepository.findWithDetailsByFlowCodeIn(anyList())).willReturn(List.of(order));
            given(statusHistoryRepository.findTopByFlowCodeOrderByIdDescIn(anyList()))
                    .willReturn(Collections.emptyList());

            List<?> result = ordersService.getOrderRegisterStock(List.of(FLOW_CODE));

            assertThat(result).hasSize(1);
            verify(storeService, never()).getStoreInfoView(any());
            verify(factoryService, never()).getFactoryInfo((String) any());
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------
    private static Orders stubOrderForGetOrder() {
        Orders order = mock(Orders.class);
        OrderProduct op = mock(OrderProduct.class);
        Priority priority = mock(Priority.class);

        given(order.getFlowCode()).willReturn(FLOW_CODE);
        given(order.getStoreId()).willReturn(STORE_ID);
        given(order.getFactoryId()).willReturn(FACTORY_ID);
        given(order.getCreateAt()).willReturn(LocalDateTime.of(2026, 5, 16, 14, 30));
        given(order.getShippingAt()).willReturn(LocalDateTime.of(2026, 5, 20, 10, 0));
        given(order.getStoreGrade()).willReturn("A");
        given(order.getStoreHarry()).willReturn(new BigDecimal("1.50"));
        given(order.getOrderNote()).willReturn("test-note");
        given(order.getPriority()).willReturn(priority);
        given(order.getProductStatus()).willReturn(ProductStatus.RECEIPT);
        given(order.getOrderStatus()).willReturn(OrderStatus.WAIT);
        given(order.getOrderStones()).willReturn(new ArrayList<>());
        given(order.getOrderProduct()).willReturn(op);
        given(priority.getPriorityName()).willReturn("일반");

        given(op.getProductId()).willReturn(501L);
        given(op.getProductName()).willReturn("다이아 반지");
        given(op.getProductFactoryName()).willReturn("삼성공방");
        given(op.getProductSize()).willReturn("15호");
        given(op.getProductLaborCost()).willReturn(120_000);
        given(op.getProductAddLaborCost()).willReturn(20_000);
        given(op.getClassificationId()).willReturn(2L);
        given(op.getClassificationName()).willReturn("반지");
        given(op.getMaterialId()).willReturn(1L);
        given(op.getMaterialName()).willReturn("18K");
        given(op.getColorId()).willReturn(3L);
        given(op.getColorName()).willReturn("옐로우골드");
        given(op.getSetTypeId()).willReturn(4L);
        given(op.getSetTypeName()).willReturn("단품");
        given(op.getOrderMainStoneNote()).willReturn("main");
        given(op.getOrderAssistanceStoneNote()).willReturn("assist");
        given(op.getStoneAddLaborCost()).willReturn(30_000);
        given(op.isAssistantStone()).willReturn(true);
        given(op.getAssistantStoneId()).willReturn(10L);
        given(op.getAssistantStoneName()).willReturn("큐빅");
        given(op.getAssistantStoneCreateAt()).willReturn(LocalDateTime.of(2026, 5, 16, 14, 30));
        return order;
    }
    private static OrderDto.Request stubUpdateDto() {
        OrderDto.Request dto = mock(OrderDto.Request.class);
        given(dto.getCreateAt()).willReturn("2026-05-16T14:30");
        given(dto.getShippingAt()).willReturn("2026-05-20T10:00");
        given(dto.getProductSize()).willReturn("15호");
        given(dto.getProductAddLaborCost()).willReturn(20_000);
        given(dto.getStoneWeight()).willReturn(new BigDecimal("0.500"));
        given(dto.getMainStoneNote()).willReturn("main");
        given(dto.getAssistanceStoneNote()).willReturn("assist");
        given(dto.getPriorityName()).willReturn("일반");
        given(dto.getOrderNote()).willReturn("note");
        given(dto.getColorName()).willReturn("옐로우골드");
        given(dto.getMaterialName()).willReturn("18K");
        given(dto.getAssistantStoneId()).willReturn("10");
        given(dto.getAssistantStoneName()).willReturn("큐빅");
        return dto;
    }
    private static Orders stubExistingOrder() {
        Orders order = mock(Orders.class);
        OrderProduct op = mock(OrderProduct.class);
        Priority priority = mock(Priority.class);
        given(order.getCreateAt()).willReturn(LocalDateTime.of(2026, 5, 16, 14, 30));
        given(order.getShippingAt()).willReturn(LocalDateTime.of(2026, 5, 20, 10, 0));
        given(order.getOrderProduct()).willReturn(op);
        given(order.getPriority()).willReturn(priority);
        given(order.getOrderNote()).willReturn("old-note");
        given(order.getFlowCode()).willReturn(FLOW_CODE);
        given(priority.getPriorityName()).willReturn("일반");
        given(op.getProductSize()).willReturn("15호");
        given(op.getProductAddLaborCost()).willReturn(20_000);
        given(op.getStoneWeight()).willReturn(new BigDecimal("0.500"));
        given(op.getOrderMainStoneNote()).willReturn("main");
        given(op.getOrderAssistanceStoneNote()).willReturn("assist");
        given(op.getColorName()).willReturn("옐로우골드");
        given(op.getMaterialName()).willReturn("18K");
        given(op.getAssistantStoneId()).willReturn(10L);
        given(op.getAssistantStoneName()).willReturn("큐빅");
        return order;
    }

    private static Orders stubOrderForGetRegisterStock() {
        Orders order = mock(Orders.class);
        OrderProduct op = mock(OrderProduct.class);
        given(order.getFlowCode()).willReturn(FLOW_CODE);
        given(order.getStoreId()).willReturn(STORE_ID);
        given(order.getFactoryId()).willReturn(FACTORY_ID);
        given(order.getStoreGrade()).willReturn("A");
        given(order.getStoreHarry()).willReturn(new BigDecimal("1.50"));
        given(order.getOrderNote()).willReturn("note");
        given(order.getCreateAt()).willReturn(LocalDateTime.now());
        given(order.getOrderProduct()).willReturn(op);
        given(order.getOrderStones()).willReturn(new ArrayList<OrderStone>());
        given(op.getProductId()).willReturn(501L);
        given(op.getProductName()).willReturn("다이아 반지");
        given(op.getProductSize()).willReturn("15호");
        given(op.getColorId()).willReturn(3L);
        given(op.getColorName()).willReturn("옐로우골드");
        given(op.getMaterialId()).willReturn(1L);
        given(op.getMaterialName()).willReturn("18K");
        given(op.isProductWeightSale()).willReturn(false);
        given(op.getProductPurchaseCost()).willReturn(100_000);
        given(op.getProductLaborCost()).willReturn(120_000);
        given(op.getProductAddLaborCost()).willReturn(20_000);
        given(op.getGoldWeight()).willReturn(new BigDecimal("3.250"));
        given(op.getStoneWeight()).willReturn(new BigDecimal("0.500"));
        given(op.getOrderMainStoneNote()).willReturn("main");
        given(op.getOrderAssistanceStoneNote()).willReturn("assist");
        given(op.isAssistantStone()).willReturn(false);
        given(op.getAssistantStoneId()).willReturn(null);
        given(op.getAssistantStoneName()).willReturn(null);
        given(op.getAssistantStoneCreateAt()).willReturn(null);
        given(op.getStoneAddLaborCost()).willReturn(30_000);
        return order;
    }
}
