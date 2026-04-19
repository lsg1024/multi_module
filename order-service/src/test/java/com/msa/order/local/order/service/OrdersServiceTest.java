package com.msa.order.local.order.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.order.global.exception.OrderNotFoundException;
import com.msa.order.local.order.dto.OrderDto;
import com.msa.order.local.order.entity.OrderProduct;
import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.ProductStatus;
import com.msa.order.local.order.repository.OrdersRepository;
import com.msa.order.local.order.repository.StatusHistoryRepository;
import com.msa.order.local.order.util.StatusHistoryHelper;
import com.msa.order.local.priority.entitiy.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdersServiceTest {

    @InjectMocks
    private OrdersService ordersService;

    @Mock
    private OrdersRepository ordersRepository;

    @Mock
    private StatusHistoryRepository statusHistoryRepository;

    @Mock
    private StatusHistoryHelper statusHistoryHelper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private OrderCommandService orderCommandService;

    private Orders testOrder;
    private OrderProduct testOrderProduct;
    private Priority testPriority;

    @BeforeEach
    void setUp() {
        testPriority = Priority.builder()
                .priorityName("일반")
                .priorityDate(7)
                .build();

        testOrder = Orders.builder()
                .storeId(1L)
                .storeName("테스트매장")
                .storeGrade("1")
                .storeHarry(new BigDecimal("1.10"))
                .factoryId(1L)
                .factoryName("테스트공장")
                .orderNote("테스트 메모")
                .productStatus(ProductStatus.RECEIPT)
                .orderStatus(OrderStatus.ORDER)
                .createAt(OffsetDateTime.now())
                .shippingAt(OffsetDateTime.now().plusDays(7))
                .build();

        testOrderProduct = OrderProduct.builder()
                .productId(1L)
                .productName("테스트상품")
                .productSize("M")
                .productFactoryName("공장명")
                .classificationId(1L)
                .classificationName("분류")
                .setTypeId(1L)
                .setTypeName("세트타입")
                .colorId(1L)
                .colorName("골드")
                .materialId(1L)
                .materialName("14K")
                .isProductWeightSale(false)
                .productPurchaseCost(10000)
                .productLaborCost(5000)
                .productAddLaborCost(1000)
                .goldWeight(new BigDecimal("10.5"))
                .stoneWeight(new BigDecimal("0.5"))
                .build();

        testOrder.addOrderProduct(testOrderProduct);
        testOrder.addPriority(testPriority);
    }

    @Nested
    @DisplayName("주문 단건 조회")
    class GetOrder {

        @Test
        @DisplayName("성공")
        void getOrder_success() {
            // given
            Long flowCode = 1L;
            given(ordersRepository.findByFlowCode(flowCode)).willReturn(Optional.of(testOrder));

            // when
            OrderDto.ResponseDetail result = ordersService.getOrder(flowCode);

            // then
            assertThat(result.getStoreName()).isEqualTo("테스트매장");
            assertThat(result.getFactoryName()).isEqualTo("테스트공장");
            assertThat(result.getProductName()).isEqualTo("테스트상품");
            verify(ordersRepository).findByFlowCode(flowCode);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 주문")
        void getOrder_notFound() {
            // given
            Long flowCode = 999L;
            given(ordersRepository.findByFlowCode(flowCode)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> ordersService.getOrder(flowCode))
                    .isInstanceOf(OrderNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("주문 상태 변경")
    class UpdateOrderStatus {

        @Test
        @DisplayName("성공 - RECEIPT 상태로 변경")
        void updateOrderStatus_toReceipt() {
            // given
            String accessToken = "test-token";
            String id = "1";
            String status = "RECEIPT";

            given(ordersRepository.findByFlowCode(1L)).willReturn(Optional.of(testOrder));
            given(jwtUtil.getNickname(accessToken)).willReturn("테스터");

            // when
            ordersService.updateOrderStatus(accessToken, id, status);

            // then
            assertThat(testOrder.getProductStatus()).isEqualTo(ProductStatus.RECEIPT);
            verify(ordersRepository).findByFlowCode(1L);
        }

        @Test
        @DisplayName("실패 - 허용되지 않은 상태")
        void updateOrderStatus_invalidStatus() {
            // given
            String accessToken = "test-token";
            String id = "1";
            String status = "INVALID_STATUS";

            // when & then
            assertThatThrownBy(() -> ordersService.updateOrderStatus(accessToken, id, status))
                    .isInstanceOf(Exception.class);
        }
    }

    @Nested
    @DisplayName("주문 삭제")
    class DeleteOrder {

        @Test
        @DisplayName("성공")
        void deletedOrders_success() {
            // given
            String accessToken = "test-token";
            String id = "1";

            given(jwtUtil.getNickname(accessToken)).willReturn("테스터");
            given(jwtUtil.getRole(accessToken)).willReturn("ADMIN");
            doNothing().when(orderCommandService).deleteOrder(1L, "ADMIN");

            // when
            ordersService.deletedOrders(accessToken, id);

            // then
            verify(orderCommandService).deleteOrder(1L, "ADMIN");
            verify(statusHistoryHelper).savePhaseChangeFromLast(any(), any(), any(), any());
        }
    }
}
