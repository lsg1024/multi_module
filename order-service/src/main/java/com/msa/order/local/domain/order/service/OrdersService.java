package com.msa.order.local.domain.order.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.kafka.KafkaProducer;
import com.msa.order.global.kafka.dto.OrderEnrichmentRequested;
import com.msa.order.local.domain.order.dto.OrderDto;
import com.msa.order.local.domain.order.dto.StoreDto;
import com.msa.order.local.domain.order.entity.*;
import com.msa.order.local.domain.order.external_client.dto.ProductDetailDto;
import com.msa.order.local.domain.order.repository.CustomOrderRepository;
import com.msa.order.local.domain.order.repository.OrdersRepository;
import com.msa.order.local.domain.priority.entitiy.Priority;
import com.msa.order.local.domain.priority.repository.PriorityRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;

@Slf4j
@Service
@Transactional
public class OrdersService {
    private final JwtUtil jwtUtil;
    private final KafkaProducer kafkaProducer;
    private final OrdersRepository ordersRepository;
    private final CustomOrderRepository customOrderRepository;
    private final PriorityRepository priorityRepository;

    public OrdersService(JwtUtil jwtUtil, KafkaProducer kafkaProducer, OrdersRepository ordersRepository, CustomOrderRepository customOrderRepository, PriorityRepository priorityRepository) {
        this.jwtUtil = jwtUtil;
        this.kafkaProducer = kafkaProducer;
        this.ordersRepository = ordersRepository;
        this.customOrderRepository = customOrderRepository;
        this.priorityRepository = priorityRepository;
    }

    //주문
    public void saveOrder(String accessToken, HttpServletRequest request, OrderDto.Request orderDto) {

        String nickname = jwtUtil.getNickname(accessToken);
        String tenantId = TenantContext.getTenant();

        Long storeId = Long.valueOf(orderDto.getStoreId());
        Long factoryId = Long.valueOf(orderDto.getFactoryId());
        Long productId = Long.valueOf(orderDto.getProductId());
        Long materialId = Long.valueOf(orderDto.getMaterialId());
        Long classificationId = Long.valueOf(orderDto.getClassificationId());
        Long colorId = Long.valueOf(orderDto.getColorId());

        // priority 추가
        Priority priority = priorityRepository.findByPriorityName(orderDto.getPriorityName())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        // productInfo 값에 있는 Stone 값을 스냅샷해 저장한다.
        Orders order = Orders.builder()
                .orderNote(orderDto.getOrderNote())
                .statusHistory(new ArrayList<>())
                .orderStatus(OrderStatus.ORDER_AWAIT)
                .build();

        // orderProduct 추가
        OrderProduct orderProduct = OrderProduct.builder()
                .productId(productId)
                .productAddLaborCost(orderDto.getProductAddLaborCost())
                .productSize(orderDto.getProductSize())
                .build();

        order.addOrderProduct(orderProduct);

        order.addPriority(priority);

        // statusHistory 추가
        StatusHistory statusHistory = StatusHistory.builder()
                .orderStatus(OrderStatus.ORDER_AWAIT)
                .createAt(OffsetDateTime.parse(orderDto.getCreateAt()))
                .userName(nickname)
                .build();

        order.addStatusHistory(statusHistory);

        // orderStone 추가
        List<ProductDetailDto.StoneInfo> storeInfos = orderDto.getStoneInfos();
        for (ProductDetailDto.StoneInfo stoneInfo : storeInfos) {
            OrderStone orderStone = OrderStone.builder()
                    .originStoneId(Long.valueOf(stoneInfo.getStoneId()))
                    .originStoneName(stoneInfo.getStoneName())
                    .originStoneWeight(new BigDecimal(stoneInfo.getStoneWeight()))
                    .stonePurchasePrice(stoneInfo.getPurchaseCost())
                    .stoneLaborCost(stoneInfo.getLaborCost())
                    .stoneQuantity(stoneInfo.getQuantity())
                    .productStoneMain(stoneInfo.isProductStoneMain())
                    .includeQuantity(stoneInfo.isIncludeQuantity())
                    .includeWeight(stoneInfo.isIncludeWeight())
                    .includeLabor(stoneInfo.isIncludeLabor())
                    .build();

            order.addOrderStone(orderStone);
        }

        ordersRepository.save(order);

        order.addOrderCode(String.format("J%07d", order.getOrderId()));
        ordersRepository.save(order);

        OrderEnrichmentRequested evt = OrderEnrichmentRequested.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .orderId(order.getOrderId())
                .tenantId(tenantId)
                .storeId(storeId)
                .factoryId(factoryId)
                .productId(productId)
                .materialId(materialId)
                .classificationId(classificationId)
                .colorId(colorId)
                .nickname(nickname)
                .build();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaProducer.publishOrderEnrichmentRequested(evt);
            }
        });
    }

    //주문 상태 변경 (주문, 취소)
    public List<String> getOrderStatusInfo(Long orderId) {

        List<OrderStatus> orderStatuses = Arrays.asList(OrderStatus.ORDER, OrderStatus.AWAIT);
        List<String> statusDtos = new ArrayList<>();
        for (OrderStatus orderStatus : orderStatuses) {
            statusDtos.add(orderStatus.getDisplayName());
        }

        boolean existsByOrderIdAndOrderStatusIn = ordersRepository.existsByOrderIdAndOrderStatusIn(orderId,
                orderStatuses);

        if (existsByOrderIdAndOrderStatusIn) {
            return statusDtos;
        }
        throw new IllegalArgumentException(NOT_FOUND);
    }

    public void updateOrderStatus(Long orderId, String status) {

        List<String> allowed = Arrays.asList("주문", "대기");

        if (!allowed.contains(status)) {
            throw new IllegalArgumentException("주문 상태를 변경할 수 없습니다.");
        }

        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        OrderStatus newStatus = OrderStatus.fromDisplayName(status)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        order.updateStatus(newStatus);
        ordersRepository.save(order);

    }

    //거래처 변경 -> account -> store 리스트 호출 /store/list
    public void updateOrderStore(Long orderId, StoreDto.UpdateRequest storeDto) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        order.updateStoreName(storeDto);
    }

    //제조사 변경 -> ?

    //기성 대체 -> ?

    // 주문 단건 조회
    public OrderDto.ResponseDetail getOrder(Long orderId) {
        return customOrderRepository.findByOrderId(orderId);
    }

    // 주문 전체 리스트 조회
    public CustomPage<OrderDto.Response> getOrderProducts(OrderDto.Condition condition, Pageable pageable) {
        return customOrderRepository.findByOrders(condition, pageable);

    }

    //판매등록

    //재고등록


}
