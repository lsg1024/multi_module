package com.msa.jewelry.local.order.service;

import com.msa.jewelry.global.exception.ExceptionMessage;
import com.msa.jewelry.local.order.dto.OrderAsyncRequested;
import com.msa.jewelry.local.order.dto.OrderDto;
import com.msa.jewelry.local.order.dto.OrderUpdateRequest;
import com.msa.jewelry.local.order.dto.StoneDto;
import com.msa.jewelry.local.order.entity.OrderProduct;
import com.msa.jewelry.local.order.entity.OrderStone;
import com.msa.jewelry.local.order.entity.Orders;
import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.order.repository.OrdersRepository;
import com.msa.jewelry.local.priority.entity.Priority;
import com.msa.jewelry.local.priority.repository.PriorityRepository;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderCommandService 단위 테스트")
class OrderCommandServiceTest {

    private static final String TOKEN     = "Bearer test-token";
    private static final String TENANT_ID = "tenant-001";
    private static final String NICKNAME  = "tester";
    private static final Long   FLOW_CODE = 9_990L;

    @Mock OrdersRepository ordersRepository;
    @Mock PriorityRepository priorityRepository;
    @Mock OrderProcessingService orderProcessingService;

    @InjectMocks
    OrderCommandService orderCommandService;

    // -----------------------------------------------------------------------
    // createOrder
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        @DisplayName("정상 — orders 저장 + orderProcessingService.createHandle 위임")
        void 정상_생성() {
            OrderDto.Request dto = stubRequest("10", "5", "501", "1", "3", "일반");
            given(dto.getStoneInfos()).willReturn(Collections.emptyList());

            Priority priority = mock(Priority.class);
            given(priority.getPriorityName()).willReturn("일반");
            given(priorityRepository.findByPriorityName("일반")).willReturn(Optional.of(priority));

            Orders saved = orderCommandService.createOrder(TENANT_ID, TOKEN, "ORDER", dto, NICKNAME);

            assertThat(saved).isNotNull();
            assertThat(saved.getStoreId()).isEqualTo(10L);
            assertThat(saved.getFactoryId()).isEqualTo(5L);
            assertThat(saved.getOrderStatus()).isEqualTo(OrderStatus.WAIT);
            // 저장 호출 검증
            verify(ordersRepository).save(any(Orders.class));
            // 후처리 위임 검증
            ArgumentCaptor<OrderAsyncRequested> evtCaptor = ArgumentCaptor.forClass(OrderAsyncRequested.class);
            verify(orderProcessingService).createHandle(evtCaptor.capture());
            OrderAsyncRequested evt = evtCaptor.getValue();
            assertThat(evt.getStoreId()).isEqualTo(10L);
            assertThat(evt.getFactoryId()).isEqualTo(5L);
            assertThat(evt.getProductId()).isEqualTo(501L);
            assertThat(evt.getOrderStatus()).isEqualTo("ORDER");
            assertThat(evt.getNickname()).isEqualTo(NICKNAME);
        }

        @Test
        @DisplayName("priorityName 매핑 실패 시 IllegalArgumentException(NOT_FOUND)")
        void priority_없음() {
            OrderDto.Request dto = stubRequest("10", "5", "501", "1", "3", "긴급");
            given(priorityRepository.findByPriorityName("긴급")).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    orderCommandService.createOrder(TENANT_ID, TOKEN, "ORDER", dto, NICKNAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);

            // priority 가 없으면 save / 후처리 모두 호출 안 됨
            verify(ordersRepository, never()).save(any(Orders.class));
            verifyNoInteractions(orderProcessingService);
        }

        @Test
        @DisplayName("stoneInfos 가 비어있어도 정상 처리 — stoneIds 빈 리스트로 이벤트 전달")
        void 스톤_빈리스트() {
            OrderDto.Request dto = stubRequest("10", "5", "501", "1", "3", "일반");
            given(dto.getStoneInfos()).willReturn(Collections.emptyList());

            Priority priority = mock(Priority.class);
            given(priorityRepository.findByPriorityName("일반")).willReturn(Optional.of(priority));

            orderCommandService.createOrder(TENANT_ID, TOKEN, "ORDER", dto, NICKNAME);

            ArgumentCaptor<OrderAsyncRequested> captor = ArgumentCaptor.forClass(OrderAsyncRequested.class);
            verify(orderProcessingService).createHandle(captor.capture());
            assertThat(captor.getValue().getStoneIds()).isEmpty();
        }

        @Test
        @DisplayName("stoneInfos 가 있으면 stoneIds 가 이벤트에 포함됨")
        void 스톤_여러개() {
            OrderDto.Request dto = stubRequest("10", "5", "501", "1", "3", "일반");
            List<StoneDto.StoneInfo> stones = List.of(
                    stoneInfo("101", "다이아", "0.250"),
                    stoneInfo("102", "사파이어", "0.120")
            );
            given(dto.getStoneInfos()).willReturn(stones);

            Priority priority = mock(Priority.class);
            given(priorityRepository.findByPriorityName("일반")).willReturn(Optional.of(priority));

            orderCommandService.createOrder(TENANT_ID, TOKEN, "ORDER", dto, NICKNAME);

            ArgumentCaptor<OrderAsyncRequested> captor = ArgumentCaptor.forClass(OrderAsyncRequested.class);
            verify(orderProcessingService).createHandle(captor.capture());
            assertThat(captor.getValue().getStoneIds()).containsExactly(101L, 102L);
        }

        @Test
        @DisplayName("assistantStoneCreateAt 가 비어있으면 이벤트 timestamp 도 null")
        void 보조석_생성일_없음() {
            OrderDto.Request dto = stubRequest("10", "5", "501", "1", "3", "일반");
            given(dto.getStoneInfos()).willReturn(Collections.emptyList());
            given(dto.getAssistantStoneCreateAt()).willReturn("");

            Priority priority = mock(Priority.class);
            given(priorityRepository.findByPriorityName("일반")).willReturn(Optional.of(priority));

            orderCommandService.createOrder(TENANT_ID, TOKEN, "ORDER", dto, NICKNAME);

            ArgumentCaptor<OrderAsyncRequested> captor = ArgumentCaptor.forClass(OrderAsyncRequested.class);
            verify(orderProcessingService).createHandle(captor.capture());
            assertThat(captor.getValue().getAssistantStoneCreateAt()).isNull();
        }

        @Test
        @DisplayName("선택 필드(setTypeId/classificationId) 가 null 이어도 IllegalArgumentException 없이 통과")
        void 선택필드_null_허용() {
            OrderDto.Request dto = stubRequest("10", "5", "501", "1", "3", "일반");
            given(dto.getStoneInfos()).willReturn(Collections.emptyList());
            given(dto.getSetTypeId()).willReturn(null);
            given(dto.getClassificationId()).willReturn(null);
            given(dto.getAssistantStoneId()).willReturn(null);

            Priority priority = mock(Priority.class);
            given(priorityRepository.findByPriorityName("일반")).willReturn(Optional.of(priority));

            Orders saved = orderCommandService.createOrder(TENANT_ID, TOKEN, "ORDER", dto, NICKNAME);

            assertThat(saved).isNotNull();
            verify(orderProcessingService).createHandle(any(OrderAsyncRequested.class));
        }
    }

    // -----------------------------------------------------------------------
    // updateOrder
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateOrder")
    class UpdateOrder {

        @Test
        @DisplayName("정상 — priority/orders 모두 존재하면 업데이트 + updateHandle 위임")
        void 정상_수정() {
            OrderDto.Request dto = stubRequest("10", "5", "501", "1", "3", "일반");
            given(dto.getStoneInfos()).willReturn(Collections.emptyList());

            Priority priority = mock(Priority.class);
            given(priorityRepository.findByPriorityName("일반")).willReturn(Optional.of(priority));

            Orders order = stubExistingOrder(10L, 5L, 501L, 1L, 3L);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));

            Orders result = orderCommandService.updateOrder(TENANT_ID, TOKEN, FLOW_CODE, "ORDER", dto, NICKNAME);

            assertThat(result).isSameAs(order);
            verify(ordersRepository).save(order);
            verify(orderProcessingService).updateHandle(any(OrderUpdateRequest.class));
        }

        @Test
        @DisplayName("priorityName 매핑 실패 시 IllegalArgumentException — 메시지에 등급 포함")
        void priority_없음() {
            OrderDto.Request dto = stubRequest("10", "5", "501", "1", "3", "긴급");
            given(priorityRepository.findByPriorityName("긴급")).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    orderCommandService.updateOrder(TENANT_ID, TOKEN, FLOW_CODE, "ORDER", dto, NICKNAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("등급")
                    .hasMessageContaining(ExceptionMessage.NOT_FOUND);

            verify(ordersRepository, never()).save(any(Orders.class));
            verifyNoInteractions(orderProcessingService);
        }

        @Test
        @DisplayName("flowCode 로 Orders 못 찾으면 IllegalArgumentException — 메시지에 '주문 수정' 포함")
        void orders_없음() {
            OrderDto.Request dto = stubRequest("10", "5", "501", "1", "3", "일반");

            Priority priority = mock(Priority.class);
            given(priorityRepository.findByPriorityName("일반")).willReturn(Optional.of(priority));
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() ->
                    orderCommandService.updateOrder(TENANT_ID, TOKEN, FLOW_CODE, "ORDER", dto, NICKNAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("주문 수정")
                    .hasMessageContaining(ExceptionMessage.NOT_FOUND);

            verifyNoInteractions(orderProcessingService);
        }

        @Test
        @DisplayName("storeId/factoryId 가 빈 문자열이면 store/factory 업데이트 호출 안 됨")
        void 빈문자열_매장_공장() {
            OrderDto.Request dto = stubRequest("", "", "501", "1", "3", "일반");
            given(dto.getStoneInfos()).willReturn(Collections.emptyList());

            Priority priority = mock(Priority.class);
            given(priorityRepository.findByPriorityName("일반")).willReturn(Optional.of(priority));

            Orders order = stubExistingOrder(10L, 5L, 501L, 1L, 3L);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));

            orderCommandService.updateOrder(TENANT_ID, TOKEN, FLOW_CODE, "ORDER", dto, NICKNAME);

            verify(order, never()).updateStore(any(), any(), any());
            verify(order, never()).updateFactory(any(), any());
            verify(orderProcessingService).updateHandle(any(OrderUpdateRequest.class));
        }

        @Test
        @DisplayName("productId 가 변경되었을 때 OrderProduct 의 productId 포함 update 가 호출됨")
        void productId_변경() {
            // 신규 productId = 999, 기존 productId = 501
            OrderDto.Request dto = stubRequest("10", "5", "999", "1", "3", "일반");
            given(dto.getStoneInfos()).willReturn(Collections.emptyList());

            Priority priority = mock(Priority.class);
            given(priorityRepository.findByPriorityName("일반")).willReturn(Optional.of(priority));

            Orders order = stubExistingOrder(10L, 5L, 501L, 1L, 3L);
            OrderProduct op = order.getOrderProduct();
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));

            orderCommandService.updateOrder(TENANT_ID, TOKEN, FLOW_CODE, "ORDER", dto, NICKNAME);

            // productId 포함 6필드 + 1필드(productId) = 8 인자 시그니처가 호출돼야 한다
            verify(op).updateOrderProductInfo(eqLong(999L),
                    any(), any(), any(), any(), any(), any(), any());
            // productId 없는 시그니처는 호출되면 안 됨
            verify(op, never()).updateOrderProductInfo(
                    any(), any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("productId 변경 없으면 productId 미포함 시그니처가 호출됨")
        void productId_동일() {
            OrderDto.Request dto = stubRequest("10", "5", "501", "1", "3", "일반");
            given(dto.getStoneInfos()).willReturn(Collections.emptyList());

            Priority priority = mock(Priority.class);
            given(priorityRepository.findByPriorityName("일반")).willReturn(Optional.of(priority));

            Orders order = stubExistingOrder(10L, 5L, 501L, 1L, 3L);
            OrderProduct op = order.getOrderProduct();
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));

            orderCommandService.updateOrder(TENANT_ID, TOKEN, FLOW_CODE, "ORDER", dto, NICKNAME);

            verify(op).updateOrderProductInfo(
                    any(), any(), any(), any(), any(), any(), any());
        }
    }

    // -----------------------------------------------------------------------
    // deleteOrder
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("deleteOrder")
    class DeleteOrder {

        @Test
        @DisplayName("정상 — role=USER, Orders 존재 → DELETED 상태로 전이")
        void 정상_삭제_USER() {
            Orders order = mock(Orders.class);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));

            orderCommandService.deleteOrder(FLOW_CODE, "USER");

            verify(order).updateOrderStatus(OrderStatus.DELETED);
            verify(order).deletedOrder(any());
        }

        @Test
        @DisplayName("정상 — role=ADMIN 도 통과")
        void 정상_삭제_ADMIN() {
            Orders order = mock(Orders.class);
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.of(order));

            orderCommandService.deleteOrder(FLOW_CODE, "ADMIN");

            verify(order).updateOrderStatus(OrderStatus.DELETED);
        }

        @Test
        @DisplayName("role=WAIT 면 NOT_ACCESS 예외 — 권한 거부, repository 호출 없음")
        void 권한_없음_WAIT() {
            assertThatThrownBy(() -> orderCommandService.deleteOrder(FLOW_CODE, "WAIT"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_ACCESS);

            verifyNoInteractions(ordersRepository);
        }

        @Test
        @DisplayName("role=GUEST 같이 알 수 없는 값도 차단")
        void 권한_없음_GUEST() {
            assertThatThrownBy(() -> orderCommandService.deleteOrder(FLOW_CODE, "GUEST"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_ACCESS);
        }

        @Test
        @DisplayName("role 이 'admin' 처럼 소문자면 거부됨 (대소문자 구분)")
        void 권한_거부_소문자() {
            assertThatThrownBy(() -> orderCommandService.deleteOrder(FLOW_CODE, "admin"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_ACCESS);
        }

        @Test
        @DisplayName("flowCode 로 Orders 못 찾으면 IllegalArgumentException — 메시지에 flowCode 포함")
        void orders_없음() {
            given(ordersRepository.findByFlowCode(FLOW_CODE)).willReturn(Optional.empty());

            assertThatThrownBy(() -> orderCommandService.deleteOrder(FLOW_CODE, "USER"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("flowCode")
                    .hasMessageContaining(ExceptionMessage.NOT_FOUND);
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------
    private static Long eqLong(Long v) {
        return org.mockito.ArgumentMatchers.eq(v);
    }
    private static OrderDto.Request stubRequest(String storeId, String factoryId, String productId,
                                                String materialId, String colorId, String priorityName) {
        OrderDto.Request dto = mock(OrderDto.Request.class);
        given(dto.getStoreId()).willReturn(storeId);
        given(dto.getFactoryId()).willReturn(factoryId);
        given(dto.getProductId()).willReturn(productId);
        given(dto.getMaterialId()).willReturn(materialId);
        given(dto.getColorId()).willReturn(colorId);
        given(dto.getPriorityName()).willReturn(priorityName);
        given(dto.getStoreGrade()).willReturn("A");
        given(dto.getStoreHarry()).willReturn("1.50");
        given(dto.getFactoryHarry()).willReturn("1.20");
        given(dto.getOrderNote()).willReturn("test-note");
        given(dto.getProductName()).willReturn("test-product");
        given(dto.getProductFactoryName()).willReturn("삼성공방");
        given(dto.getProductSize()).willReturn("15호");
        given(dto.getMaterialName()).willReturn("18K");
        given(dto.getColorName()).willReturn("옐로우골드");
        given(dto.getClassificationId()).willReturn("2");
        given(dto.getClassificationName()).willReturn("반지");
        given(dto.getSetTypeId()).willReturn("4");
        given(dto.getSetTypeName()).willReturn("단품");
        given(dto.getIsProductWeightSale()).willReturn(false);
        given(dto.getProductPurchaseCost()).willReturn(100_000);
        given(dto.getProductLaborCost()).willReturn(120_000);
        given(dto.getProductAddLaborCost()).willReturn(20_000);
        given(dto.getStoneWeight()).willReturn(java.math.BigDecimal.ZERO);
        given(dto.getMainStoneNote()).willReturn("main");
        given(dto.getAssistanceStoneNote()).willReturn("assist");
        given(dto.isAssistantStone()).willReturn(false);
        given(dto.getAssistantStoneId()).willReturn("10");
        given(dto.getAssistantStoneName()).willReturn("큐빅");
        given(dto.getAssistantStoneCreateAt()).willReturn("2026-05-16");
        given(dto.getCreateAt()).willReturn("2026-05-16");
        given(dto.getShippingAt()).willReturn("2026-05-20");
        given(dto.getStoneAddLaborCost()).willReturn(0);
        return dto;
    }

    private static StoneDto.StoneInfo stoneInfo(String stoneId, String name, String weight) {
        return new StoneDto.StoneInfo(
                stoneId, name, weight,
                10_000, 20_000, 0,
                1, false, true
        );
    }
    private static Orders stubExistingOrder(Long storeId, Long factoryId, Long productId,
                                            Long materialId, Long colorId) {
        Orders order = mock(Orders.class);
        OrderProduct op = mock(OrderProduct.class);
        List<OrderStone> stones = new ArrayList<>();
        given(order.getStoreId()).willReturn(storeId);
        given(order.getFactoryId()).willReturn(factoryId);
        given(order.getStoreGrade()).willReturn("A");
        given(order.getOrderProduct()).willReturn(op);
        given(order.getOrderStones()).willReturn(stones);
        given(order.getFlowCode()).willReturn(FLOW_CODE);
        given(order.getOrderStatus()).willReturn(OrderStatus.WAIT);

        given(op.getProductId()).willReturn(productId);
        given(op.getMaterialId()).willReturn(materialId);
        given(op.getColorId()).willReturn(colorId);
        return order;
    }
}
