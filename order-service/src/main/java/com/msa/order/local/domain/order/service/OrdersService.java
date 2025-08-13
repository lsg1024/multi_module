package com.msa.order.local.domain.order.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.kafka.KafkaProducer;
import com.msa.order.global.kafka.dto.OrderAsyncRequested;
import com.msa.order.local.domain.order.dto.DateDto;
import com.msa.order.local.domain.order.dto.FactoryDto;
import com.msa.order.local.domain.order.dto.OrderDto;
import com.msa.order.local.domain.order.dto.StoreDto;
import com.msa.order.local.domain.order.entity.OrderProduct;
import com.msa.order.local.domain.order.entity.OrderStone;
import com.msa.order.local.domain.order.entity.Orders;
import com.msa.order.local.domain.order.entity.StatusHistory;
import com.msa.order.local.domain.order.entity.order_enum.OrderStatus;
import com.msa.order.local.domain.order.entity.order_enum.ProductStatus;
import com.msa.order.local.domain.order.external_client.FactoryClient;
import com.msa.order.local.domain.order.external_client.StoreClient;
import com.msa.order.local.domain.order.external_client.dto.ProductDetailDto;
import com.msa.order.local.domain.order.repository.CustomOrderRepository;
import com.msa.order.local.domain.order.repository.OrdersRepository;
import com.msa.order.local.domain.order.util.DateUtil;
import com.msa.order.local.domain.priority.entitiy.Priority;
import com.msa.order.local.domain.priority.repository.PriorityRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.msa.order.global.exception.ExceptionMessage.NOT_ACCESS;
import static com.msa.order.global.exception.ExceptionMessage.NOT_FOUND;

@Slf4j
@Service
@Transactional
public class OrdersService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Seoul");
    private EntityManager em;
    private final JwtUtil jwtUtil;
    private final FactoryClient factoryClient;
    private final StoreClient storeClient;
    private final KafkaProducer kafkaProducer;
    private final OrdersRepository ordersRepository;
    private final CustomOrderRepository customOrderRepository;
    private final PriorityRepository priorityRepository;

    public OrdersService(JwtUtil jwtUtil, FactoryClient factoryClient, StoreClient storeClient, KafkaProducer kafkaProducer, OrdersRepository ordersRepository, CustomOrderRepository customOrderRepository, PriorityRepository priorityRepository) {
        this.jwtUtil = jwtUtil;
        this.factoryClient = factoryClient;
        this.storeClient = storeClient;
        this.kafkaProducer = kafkaProducer;
        this.ordersRepository = ordersRepository;
        this.customOrderRepository = customOrderRepository;
        this.priorityRepository = priorityRepository;
    }

    // 주문 단건 조회
    @Transactional(readOnly = true)
    public OrderDto.ResponseDetail getOrder(Long orderId) {
        return customOrderRepository.findByOrderId(orderId);
    }

    // 주문 전체 리스트 조회
    @Transactional(readOnly = true)
    public CustomPage<OrderDto.Response> getOrderProducts(OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable) {
        return customOrderRepository.findByOrders(inputCondition, orderCondition, pageable);

    }

    //주문
    public void saveOrder(String accessToken, String orderType, OrderDto.Request orderDto) {

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

        Integer priorityDate = priority.getPriorityDate();

        OffsetDateTime received = orderDto.getCreateAt();
        OffsetDateTime receivedUtc = received.withOffsetSameInstant(ZoneOffset.UTC);

        OffsetDateTime expectUtc =
                DateUtil.plusBusinessDay(receivedUtc, priorityDate, BUSINESS_ZONE);

        // productInfo 값에 있는 Stone 값을 스냅샷해 저장한다. /
        Orders order = Orders.builder()
                .orderNote(orderDto.getOrderNote())
                .statusHistory(new ArrayList<>())
                .productStatus(ProductStatus.valueOf(orderType))
                .orderStatus(OrderStatus.valueOf(orderDto.getOrderStatus()))
                .orderDate(receivedUtc)
                .orderExpectDate(expectUtc)
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
                .productStatus(ProductStatus.valueOf(orderType))
                .orderStatus(OrderStatus.valueOf(orderDto.getOrderStatus()))
                .createAt(receivedUtc)
                .userName(nickname)
                .build();

        order.addStatusHistory(statusHistory);

        // orderStone 추가
        List<Long> stoneIds = new ArrayList<>();
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

            stoneIds.add(Long.valueOf(stoneInfo.getStoneId()));
            order.addOrderStone(orderStone);
        }

        ordersRepository.save(order);

        OrderAsyncRequested evt = OrderAsyncRequested.builder()
                .eventId(UUID.randomUUID().toString())
                .orderId(order.getOrderId())
                .tenantId(tenantId)
                .storeId(storeId)
                .factoryId(factoryId)
                .productId(productId)
                .materialId(materialId)
                .classificationId(classificationId)
                .colorId(colorId)
                .nickname(nickname)
                .stoneIds(stoneIds)
                .productStatus(orderType)
                .orderStatus(orderDto.getOrderStatus())
                .build();

        order.addOrderCode(String.format("J%07d", order.getOrderId()));
        ordersRepository.save(order);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaProducer.publishOrderAsyncRequested(evt);
            }
        });
    }

    //주문 상태 변경 (주문, 취소)
    public List<String> getOrderStatusInfo(Long orderId) {

        List<OrderStatus> orderStatuses = Arrays.asList(OrderStatus.RECEIPT, OrderStatus.RECEIPT_FAILED, OrderStatus.WAITING);
        List<String> statusDtos = new ArrayList<>();
        for (OrderStatus productStatus : orderStatuses) {
            statusDtos.add(productStatus.getDisplayName());
        }

        boolean existsByOrderIdAndOrderStatusIn = ordersRepository.existsByOrderIdAndOrderStatusIn(orderId, orderStatuses);

        if (existsByOrderIdAndOrderStatusIn) {
            return statusDtos;
        }
        throw new IllegalArgumentException(NOT_FOUND);
    }

    //주문 상태 변경
    public void updateOrderStatus(Long orderId, String status) {

        List<String> allowed = Arrays.asList(
                OrderStatus.RECEIPT.getDisplayName(),
                OrderStatus.WAITING.getDisplayName());

        log.info("status {} {}", status, allowed.contains(status));
        if (!allowed.contains(status)) {
            throw new IllegalArgumentException("주문 상태를 변경할 수 없습니다.");
        }

        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        OrderStatus newStatus = OrderStatus.fromDisplayName(status)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        order.updateOrderStatus(newStatus);
        ordersRepository.save(order);

    }

    //거래처 변경 -> account -> store 리스트 호출 /store/list
    public void updateOrderStore(String accessToken, Long orderId, StoreDto.Request storeDto) {
        String tenantId = jwtUtil.getTenantId(accessToken);
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StoreDto.Response storeInfo = storeClient.getStoreInfo(tenantId, storeDto.getStoreId());

        order.updateStore(storeInfo);
    }

    //제조사 변경 ->
    public void updateOrderFactory(String accessToken, Long orderId, FactoryDto.Request factoryDto) {
        String tenantId = jwtUtil.getTenantId(accessToken);
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        FactoryDto.Response factoryInfo = factoryClient.getFactoryInfo(tenantId, factoryDto.getFactoryId());

        order.updateFactory(factoryInfo);
    }

    //출고일 변경
    public void updateOrderExpectDate(Long orderId, DateDto newDate) {
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        OffsetDateTime received = newDate.getExpectDate();
        OffsetDateTime receivedUtc = received.withOffsetSameInstant(ZoneOffset.UTC);

        order.updateExceptDate(receivedUtc);
    }

    //기성 대체 -> 재고에 있는 제품 (이름, 색상, 재질 동일)

    //주문 -> 삭제
    public void deletedOrder(String accessToken, Long orderId) {
        String nickname = jwtUtil.getNickname(accessToken);
        String role = jwtUtil.getRole(accessToken);

        if (role.equals("ADMIN") || role.equals("USER")) {
            Orders order = ordersRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

            OffsetDateTime now = OffsetDateTime.now();
            StatusHistory statusHistory = StatusHistory.builder()
                    .productStatus(ProductStatus.DELETE)
                    .orderStatus(OrderStatus.NONE)
                    .createAt(now)
                    .userName(nickname)
                    .build();

            order.addStatusHistory(statusHistory);
            order.deletedOrder(OffsetDateTime.now());
            return;
        }
        throw new IllegalArgumentException(NOT_ACCESS);
    }

    //주문 -> 재고 변경
    public void updateOrderStatusToStock(String accessToken, Long orderId) {
        String nickname = jwtUtil.getNickname(accessToken);
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        OffsetDateTime now = OffsetDateTime.now();
        StatusHistory statusHistory = StatusHistory.builder()
                .productStatus(ProductStatus.STOCK)
                .orderStatus(OrderStatus.NONE)
                .createAt(now)
                .userName(nickname)
                .build();

        order.addStatusHistory(statusHistory);
        order.updateProductStatus(ProductStatus.STOCK);
    }

    //주문 -> 판매 변경
    public void updateOrderStatusSale(String accessToken, Long orderId) {
        String nickname = jwtUtil.getNickname(accessToken);
        Orders order = ordersRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        OffsetDateTime now = OffsetDateTime.now();
        StatusHistory statusHistory = StatusHistory.builder()
                .productStatus(ProductStatus.STOCK)
                .orderStatus(OrderStatus.NONE)
                .createAt(now)
                .userName(nickname)
                .build();

        order.addStatusHistory(statusHistory);
        order.updateProductStatus(ProductStatus.SALE);
    }


    // 출고 예정 목록 출력
    @Transactional(readOnly = true)
    public CustomPage<OrderDto.Response> getExpectProducts(OrderDto.InputCondition inputCondition, OrderDto.ExpectCondition expectCondition, Pageable pageable) {
        return customOrderRepository.findByExpectOrders(inputCondition, expectCondition, pageable);
    }

    // 주문 상태에서 취소된 목록 출력
    @Transactional(readOnly = true)
    public CustomPage<OrderDto.Response> getDeletedProducts(OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable) {
        return customOrderRepository.findByDeletedOrders(inputCondition, orderCondition, pageable);
    }

}
