package com.msa.order.local.order.repository;

import com.msa.order.local.order.entity.OrderProduct;
import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.ProductStatus;
import com.msa.order.local.priority.entitiy.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrdersRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OrdersRepository ordersRepository;

    private Orders savedOrder;
    private Priority priority;

    @BeforeEach
    void setUp() {
        priority = Priority.builder()
                .priorityName("일반")
                .priorityDate(7)
                .build();
        entityManager.persist(priority);

        Orders order = Orders.builder()
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

        OrderProduct orderProduct = OrderProduct.builder()
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

        order.addOrderProduct(orderProduct);
        order.addPriority(priority);

        savedOrder = entityManager.persistAndFlush(order);
        entityManager.clear();
    }

    @Test
    @DisplayName("flowCode로 주문 조회 성공")
    void findByFlowCode_success() {
        // given
        Long flowCode = savedOrder.getFlowCode();

        // when
        Optional<Orders> result = ordersRepository.findByFlowCode(flowCode);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getStoreName()).isEqualTo("테스트매장");
        assertThat(result.get().getFactoryName()).isEqualTo("테스트공장");
    }

    @Test
    @DisplayName("존재하지 않는 flowCode로 조회 시 빈 Optional 반환")
    void findByFlowCode_notFound() {
        // given
        Long invalidFlowCode = 999999L;

        // when
        Optional<Orders> result = ordersRepository.findByFlowCode(invalidFlowCode);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("flowCode 목록으로 상세 조회")
    void findWithDetailsByFlowCodeIn_success() {
        // given
        List<Long> flowCodes = List.of(savedOrder.getFlowCode());

        // when
        List<Orders> result = ordersRepository.findWithDetailsByFlowCodeIn(flowCodes);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderProduct()).isNotNull();
        assertThat(result.get(0).getOrderProduct().getProductName()).isEqualTo("테스트상품");
    }

    @Test
    @DisplayName("flowCode와 ProductStatus로 존재 여부 확인")
    void existsByFlowCodeAndProductStatusIn_success() {
        // given
        Long flowCode = savedOrder.getFlowCode();
        List<ProductStatus> statuses = List.of(ProductStatus.RECEIPT, ProductStatus.WAITING);

        // when
        boolean exists = ordersRepository.existsByFlowCodeAndProductStatusIn(flowCode, statuses);

        // then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("주문 상태 업데이트")
    void updateOrderStatus_success() {
        // given
        Orders order = ordersRepository.findByFlowCode(savedOrder.getFlowCode()).get();

        // when
        order.updateOrderStatus(OrderStatus.FIX);
        ordersRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // then
        Orders updatedOrder = ordersRepository.findByFlowCode(savedOrder.getFlowCode()).get();
        assertThat(updatedOrder.getOrderStatus()).isEqualTo(OrderStatus.FIX);
    }

    @Test
    @DisplayName("주문 삭제 (소프트 삭제)")
    void deleteOrder_softDelete() {
        // given
        Orders order = ordersRepository.findByFlowCode(savedOrder.getFlowCode()).get();

        // when
        order.updateOrderStatus(OrderStatus.DELETED);
        order.deletedOrder(OffsetDateTime.now());
        ordersRepository.save(order);
        entityManager.flush();
        entityManager.clear();

        // then
        Orders deletedOrder = ordersRepository.findByFlowCode(savedOrder.getFlowCode()).get();
        assertThat(deletedOrder.getOrderStatus()).isEqualTo(OrderStatus.DELETED);
        assertThat(deletedOrder.isOrderDeleted()).isTrue();
    }
}
