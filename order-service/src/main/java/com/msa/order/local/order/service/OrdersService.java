package com.msa.order.local.order.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.dto.StoneDto;
import com.msa.order.global.kafka.KafkaProducer;
import com.msa.order.global.kafka.dto.OrderAsyncRequested;
import com.msa.order.global.kafka.dto.OrderUpdateRequest;
import com.msa.order.local.order.dto.*;
import com.msa.order.local.order.entity.OrderProduct;
import com.msa.order.local.order.entity.OrderStone;
import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.*;
import com.msa.order.local.order.external_client.FactoryClient;
import com.msa.order.local.order.external_client.ProductClient;
import com.msa.order.local.order.external_client.StoreClient;
import com.msa.order.local.order.external_client.dto.ProductImageDto;
import com.msa.order.local.order.repository.CustomOrderRepository;
import com.msa.order.local.order.repository.OrdersRepository;
import com.msa.order.local.order.repository.StatusHistoryRepository;
import com.msa.order.local.priority.entitiy.Priority;
import com.msa.order.local.priority.repository.PriorityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static com.msa.order.global.exception.ExceptionMessage.*;
import static com.msa.order.global.util.DateConversionUtil.StringToOffsetDateTime;
import static com.msa.order.global.util.DateConversionUtil.plusBusinessDay;

@Slf4j
@Service
@Transactional
public class OrdersService {
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final JwtUtil jwtUtil;
    private final FactoryClient factoryClient;
    private final StoreClient storeClient;
    private final ProductClient productClient;
    private final KafkaProducer kafkaProducer;
    private final OrdersRepository ordersRepository;
    private final CustomOrderRepository customOrderRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final PriorityRepository priorityRepository;

    public OrdersService(JwtUtil jwtUtil, FactoryClient factoryClient, StoreClient storeClient, ProductClient productClient, KafkaProducer kafkaProducer, OrdersRepository ordersRepository, CustomOrderRepository customOrderRepository, StatusHistoryRepository statusHistoryRepository, PriorityRepository priorityRepository) {
        this.jwtUtil = jwtUtil;
        this.factoryClient = factoryClient;
        this.storeClient = storeClient;
        this.productClient = productClient;
        this.kafkaProducer = kafkaProducer;
        this.ordersRepository = ordersRepository;
        this.customOrderRepository = customOrderRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.priorityRepository = priorityRepository;
    }

    // 주문 단건 조회
    @Transactional(readOnly = true)
    public OrderDto.ResponseDetail getOrder(Long flowCode) {
        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        List<OrderStone> orderStones = order.getOrderStones();

        List<StoneDto.StoneResponse> stonesDtos = new ArrayList<>();
        for (OrderStone orderStone : orderStones) {
            StoneDto.StoneResponse stoneDto = new StoneDto.StoneResponse(
                orderStone.getOriginStoneName(),
                orderStone.getOriginStoneWeight().toPlainString(),
                orderStone.getStonePurchaseCost(),
                orderStone.getStoneLaborCost(),
                orderStone.getStoneAddLaborCost(),
                orderStone.getStoneQuantity(),
                orderStone.getMainStone(),
                orderStone.getIncludeStone()
            );
            stonesDtos.add(stoneDto);
        }

        OrderProduct orderProduct = order.getOrderProduct();

        return OrderDto.ResponseDetail.builder()
                .createAt(order.getCreateAt().toString())
                .shippingAt(order.getShippingAt().toString())
                .flowCode(order.getFlowCode().toString())
                .storeName(order.getStoreName())
                .productName(orderProduct.getProductName())
                .classification(orderProduct.getClassificationName())
                .materialName(orderProduct.getMaterialName())
                .colorName(orderProduct.getColorName())
                .productSize(orderProduct.getProductSize())
                .orderNote(order.getOrderNote())
                .factoryName(order.getFactoryName())
                .priority(order.getPriority().getPriorityName())
                .productStatus(order.getProductStatus().getDisplayName())
                .orderStatus(order.getOrderStatus().getDisplayName())
                .stoneInfos(stonesDtos)
                .assistantStone(orderProduct.isAssistantStone())
                .assistantStoneName(orderProduct.getAssistantStoneName())
                .assistantStoneCreateAt(orderProduct.getAssistantStoneCreateAt())
                .build();

    }

    // 주문 전체 리스트 조회
    @Transactional(readOnly = true)
    public CustomPage<OrderDto.Response> getOrderProducts(String accessToken, OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable) {
        String tenantId = jwtUtil.getTenantId(accessToken);

        CustomPage<OrderQueryDto> queryDtoPage = customOrderRepository.findByOrders(inputCondition, orderCondition, pageable);
        List<OrderQueryDto> queryDtos = queryDtoPage.getContent();

        List<Long> productIds = queryDtos.stream()
                .map(OrderQueryDto::getProductId)
                .distinct()
                .toList();

        Map<Long, ProductImageDto> productImages = productClient.getProductImages(tenantId, productIds);

        List<OrderDto.Response> finalResponse = queryDtos.stream()
                .map(queryDto -> {
                    ProductImageDto image = productImages.get(queryDto.getProductId());
                    String imagePath = (image != null) ? image.getImagePath() : null;

                    return OrderDto.Response.from(queryDto, imagePath);
                })
                .toList();

        return new CustomPage<>(finalResponse, pageable, queryDtoPage.getTotalElements());
    }
    //주문
    public void saveOrder(String accessToken, String orderType, OrderDto.Request orderDto) {

        String nickname = jwtUtil.getNickname(accessToken);
        String tenantId = TenantContext.getTenant();

        Long storeId = Long.valueOf(orderDto.getStoreId());
        Long factoryId = Long.valueOf(orderDto.getFactoryId());
        Long productId = Long.valueOf(orderDto.getProductId());
        Long materialId = Long.valueOf(orderDto.getMaterialId());
        Long colorId = Long.valueOf(orderDto.getColorId());
        Long assistantId = Long.valueOf(orderDto.getAssistantStoneId());

        // priority 추가
        Priority priority = priorityRepository.findByPriorityName(orderDto.getPriorityName())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        Integer priorityDate = priority.getPriorityDate();

        OffsetDateTime createAt = StringToOffsetDateTime(orderDto.getCreateAt());
        OffsetDateTime expectAt = plusBusinessDay(createAt, priorityDate);

        ProductStatus productStatus = ProductStatus.fromDisplayName(orderDto.getProductStatus())
                .orElseThrow(() -> new IllegalArgumentException(WRONG_STATUS));

        Orders order = Orders.builder()
                .orderNote(orderDto.getOrderNote())
                .productStatus(productStatus)
                .orderStatus(OrderStatus.valueOf(orderType))
                .createAt(createAt)
                .shippingAt(expectAt)
                .build();

        // orderStone 추가
        int totalStoneCost = 0;
        List<Long> stoneIds = new ArrayList<>();
        List<StoneDto.StoneInfo> storeInfos = orderDto.getStoneInfos();
        for (StoneDto.StoneInfo stoneInfo : storeInfos) {
            OrderStone orderStone = OrderStone.builder()
                    .originStoneId(Long.valueOf(stoneInfo.getStoneId()))
                    .originStoneName(stoneInfo.getStoneName())
                    .originStoneWeight(new BigDecimal(stoneInfo.getStoneWeight()))
                    .stonePurchaseCost(stoneInfo.getPurchaseCost())
                    .stoneLaborCost(stoneInfo.getLaborCost())
                    .stoneAddLaborCost(stoneInfo.getAddLaborCost())
                    .stoneQuantity(stoneInfo.getQuantity())
                    .mainStone(stoneInfo.isMainStone())
                    .includeStone(stoneInfo.isIncludeStone())
                    .build();

            stoneIds.add(Long.valueOf(stoneInfo.getStoneId()));
            order.addOrderStone(orderStone);
            totalStoneCost += stoneInfo.getLaborCost() * stoneInfo.getQuantity() + stoneInfo.getAddLaborCost();
        }

        // orderProduct 추가
        OrderProduct orderProduct = OrderProduct.builder()
                .productId(productId)
                .isGoldWeightSale(orderDto.isProductWeightSale())
                .goldWeight(new BigDecimal(BigInteger.ZERO))
                .stoneWeight(orderDto.getStoneWeight())
                .productAddLaborCost(orderDto.getProductAddLaborCost())
                .stoneTotalAddLaborCost(totalStoneCost)
                .orderMainStoneNote(orderDto.getMainStoneNote())
                .orderAssistanceStoneNote(orderDto.getAssistanceStoneNote())
                .productSize(orderDto.getProductSize())
                .classificationName(orderDto.getClassificationName())
                .setType(orderDto.getSetTypeName())
                .build();

        order.addOrderProduct(orderProduct);
        order.addPriority(priority);

        ordersRepository.save(order);

        // statusHistory 추가
        StatusHistory statusHistory = StatusHistory.create(
                order.getFlowCode(),
                SourceType.ORDER,
                BusinessPhase.WAITING,
                Kind.CREATE,
                nickname
        );

        statusHistoryRepository.save(statusHistory);

        OrderAsyncRequested evt;
        if (orderDto.isAssistantStone()) {
            OffsetDateTime assistantStoneCreateAt = StringToOffsetDateTime(orderDto.getAssistantStoneCreateAt());
            evt = OrderAsyncRequested.builder()
                    .eventId(UUID.randomUUID().toString())
                    .flowCode(order.getFlowCode())
                    .tenantId(tenantId)
                    .storeId(storeId)
                    .factoryId(factoryId)
                    .productId(productId)
                    .materialId(materialId)
                    .colorId(colorId)
                    .assistantStone(orderDto.isAssistantStone())
                    .assistantStoneId(assistantId)
                    .assistantStoneCreateAt(assistantStoneCreateAt)
                    .nickname(nickname)
                    .stoneIds(stoneIds)
                    .orderStatus(orderType)
                    .build();
        } else {
            evt = OrderAsyncRequested.builder()
                    .eventId(UUID.randomUUID().toString())
                    .flowCode(order.getFlowCode())
                    .tenantId(tenantId)
                    .storeId(storeId)
                    .factoryId(factoryId)
                    .productId(productId)
                    .materialId(materialId)
                    .colorId(colorId)
                    .assistantStone(false)
                    .nickname(nickname)
                    .stoneIds(stoneIds)
                    .orderStatus(orderType)
                    .build();
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaProducer.orderDetailAsync(evt);
            }
        });
    }

    //주문 수정
    public void updateOrder(String accessToken, Long flowCode, OrderDto.Request orderDto) {
        String nickname = jwtUtil.getNickname(accessToken);
        String tenantId = TenantContext.getTenant();

        Long storeId = Long.valueOf(orderDto.getStoreId());
        Long factoryId = Long.valueOf(orderDto.getFactoryId());
        Long productId = Long.valueOf(orderDto.getProductId());
        Long materialId = Long.valueOf(orderDto.getMaterialId());
        Long colorId = Long.valueOf(orderDto.getColorId());
        Long assistantId = Long.valueOf(orderDto.getAssistantStoneId());

        // priority 추가
        Priority priority = priorityRepository.findByPriorityName(orderDto.getPriorityName())
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        Integer priorityDate = priority.getPriorityDate();

        OffsetDateTime createAt = StringToOffsetDateTime(orderDto.getCreateAt());
        OffsetDateTime shippingAt = plusBusinessDay(createAt, priorityDate);

        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException("주문" + NOT_FOUND));

        order.updateOrderNote(order.getOrderNote());
        order.updateCreateDate(createAt);
        order.updateShippingDate(shippingAt);
        order.addPriority(priority);

        // 스톤 값을 업데이트 (기존 그대로,추가,삭제)


        // orderProduct 추가
        OrderProduct orderProduct = order.getOrderProduct();
        orderProduct.updateOrderProductInfo(
                productId,
                orderDto.getStoneWeight(),
                orderDto.getProductAddLaborCost(),
                null, // 스톤 업데이트 후
                orderDto.getMainStoneNote(),
                orderDto.getAssistanceStoneNote(),
                orderDto.getProductSize(),
                orderDto.getClassificationName(),
                orderDto.getSetTypeName()
        );

        ordersRepository.save(order);

        // statusHistory 추가
        StatusHistory statusHistory = StatusHistory.create(
                order.getFlowCode(),
                SourceType.ORDER,
                BusinessPhase.WAITING,
                Kind.UPDATE,
                nickname
        );

        statusHistoryRepository.save(statusHistory);

        OrderUpdateRequest updateRequest;
        if (orderDto.isAssistantStone()) {
            OffsetDateTime assistantStoneCreateAt = StringToOffsetDateTime(orderDto.getAssistantStoneCreateAt());
            updateRequest = OrderUpdateRequest.builder()
                    .eventId(UUID.randomUUID().toString())
                    .flowCode(order.getFlowCode())
                    .tenantId(tenantId)
                    .storeId(storeId)
                    .factoryId(factoryId)
                    .productId(productId)
                    .materialId(materialId)
                    .colorId(colorId)
                    .assistantStone(orderDto.isAssistantStone())
                    .assistantStoneId(assistantId)
                    .assistantStoneCreateAt(assistantStoneCreateAt)
                    .nickname(nickname)
//                    .stoneIds(stoneIds)
                    .build();
        } else {
            updateRequest = OrderUpdateRequest.builder()
                    .eventId(UUID.randomUUID().toString())
                    .flowCode(order.getFlowCode())
                    .tenantId(tenantId)
                    .storeId(storeId)
                    .factoryId(factoryId)
                    .productId(productId)
                    .materialId(materialId)
                    .colorId(colorId)
                    .assistantStone(false)
                    .nickname(nickname)
//                    .stoneIds(stoneIds)
                    .build();
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                kafkaProducer.orderDetailUpdateAsync(updateRequest);
            }
        });
    }

    //주문 상태 변경 (주문, 취소)
    public List<String> getOrderStatusInfo(String id) {
        List<ProductStatus> productStatuses = Arrays.asList(ProductStatus.RECEIPT, ProductStatus.RECEIPT_FAILED, ProductStatus.WAITING);
        List<String> statusDtos = new ArrayList<>();
        for (ProductStatus productStatus : productStatuses) {
            statusDtos.add(productStatus.getDisplayName());
        }

        long flowCode = Long.parseLong(id);
        boolean existsByOrderIdAndOrderStatusIn = ordersRepository.existsByFlowCodeAndProductStatusIn(flowCode, productStatuses);

        if (existsByOrderIdAndOrderStatusIn) {
            return statusDtos;
        }
        throw new IllegalArgumentException(NOT_FOUND);
    }

    //주문 상태 변경
    public void updateOrderStatus(String id, String status) {

        List<String> allowed = Arrays.asList(
                ProductStatus.RECEIPT.name(),
                ProductStatus.WAITING.name());

        if (!allowed.contains(status.toUpperCase())) {
            throw new IllegalArgumentException("주문 상태를 변경할 수 없습니다.");
        }

        long flowCode = Long.parseLong(id);
        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        ProductStatus newStatus = ProductStatus.valueOf(status.toUpperCase());

        order.updateProductStatus(newStatus);
        ordersRepository.save(order);

    }

    //판매처 변경 -> account -> store 리스트 호출 /store/list
    public void updateOrderStore(String accessToken, String id, StoreDto.Request storeDto) {
        String tenantId = jwtUtil.getTenantId(accessToken);

        long flowCode = Long.parseLong(id);
        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        StoreDto.Response storeInfo = storeClient.getStoreInfo(tenantId, storeDto.getStoreId());

        order.updateStore(storeInfo);
    }

    //제조사 변경 ->
    public void updateOrderFactory(String accessToken, String id, FactoryDto.Request factoryDto) {
        String tenantId = jwtUtil.getTenantId(accessToken);
        long flowCode = Long.parseLong(id);
        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        FactoryDto.Response factoryInfo = factoryClient.getFactoryInfo(tenantId, factoryDto.getFactoryId());

        order.updateFactory(factoryInfo);
    }

    //출고일 변경
    public void updateOrderExpectDate(String id, DateDto newDate) {

        long flowCode = Long.parseLong(id);
        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        OffsetDateTime received = newDate.getExpectDate();
        OffsetDateTime receivedKst  = received.withOffsetSameInstant(ZoneOffset.ofHours(9));

        order.updateShippingDate(receivedKst);
    }

    //기성 대체 -> 재고에 있는 제품 (이름, 색상, 재질 동일)

    //주문 -> 삭제
    public void deletedOrder(String accessToken, String id) {
        String nickname = jwtUtil.getNickname(accessToken);
        String role = jwtUtil.getRole(accessToken);

        if (role.equals("ADMIN") || role.equals("USER")) {
            long flowCode = Long.parseLong(id);
            Orders order = ordersRepository.findByFlowCode(flowCode)
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

            StatusHistory lastHistory = statusHistoryRepository.findTopByFlowCodeOrderByIdDesc(order.getFlowCode())
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

            StatusHistory statusHistory = StatusHistory.phaseChange(
                    order.getFlowCode(),
                    lastHistory.getSourceType(),
                    BusinessPhase.valueOf(lastHistory.getFromValue()),
                    BusinessPhase.DELETE,
                    nickname
            );

            order.deletedOrder(OffsetDateTime.now());
            statusHistoryRepository.save(statusHistory);
            return;
        }
        throw new IllegalArgumentException(NOT_ACCESS);
    }

    // 출고 예정 목록 출력
    @Transactional(readOnly = true)
    public CustomPage<OrderDto.Response> getExpectProducts(String accessToken, OrderDto.InputCondition inputCondition, OrderDto.ExpectCondition expectCondition, Pageable pageable) {
        String tenantId = jwtUtil.getTenantId(accessToken);
        CustomPage<OrderQueryDto> expectOrderPages = customOrderRepository.findByExpectOrders(inputCondition, expectCondition, pageable);

        List<Long> productIds = expectOrderPages.stream()
                .map(OrderQueryDto::getProductId)
                .distinct()
                .toList();

        Map<Long, ProductImageDto> productImages = productClient.getProductImages(tenantId, productIds);

        List<OrderDto.Response> finalResponse = expectOrderPages.stream()
                .map(queryDto -> {
                    ProductImageDto imageDto = productImages.get(queryDto.getProductId());
                    String imagePath = (imageDto != null) ? imageDto.getImagePath() : null;

                    return OrderDto.Response.from(queryDto, imagePath);
                })
                .toList();

        return new CustomPage<>(finalResponse, pageable, expectOrderPages.getTotalElements());
    }

    // 주문 상태에서 취소된 목록 출력
    @Transactional(readOnly = true)
    public CustomPage<OrderDto.Response> getDeletedProducts(String accessToken, OrderDto.InputCondition inputCondition, OrderDto.OrderCondition orderCondition, Pageable pageable) {
        String tenantId = jwtUtil.getTenantId(accessToken);
        CustomPage<OrderQueryDto> expectOrderPages = customOrderRepository.findByDeletedOrders(inputCondition, orderCondition, pageable);

        List<Long> productIds = expectOrderPages.stream()
                .map(OrderQueryDto::getProductId)
                .distinct()
                .toList();

        Map<Long, ProductImageDto> productImages = productClient.getProductImages(tenantId, productIds);

        List<OrderDto.Response> finalResponse = expectOrderPages.stream()
                .map(queryDto -> {
                    ProductImageDto imageDto = productImages.get(queryDto.getProductId());
                    String imagePath = (imageDto != null) ? imageDto.getImagePath() : null;

                    return OrderDto.Response.from(queryDto, imagePath);
                })
                .toList();

        return new CustomPage<>(finalResponse, pageable, expectOrderPages.getTotalElements());
    }


    @Transactional(readOnly = true)
    public List<String> getFilterFactories(String startAt, String endAt, String factoryName, String storeName, String setTypeName) {
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, factoryName, storeName, setTypeName);
        return customOrderRepository.findByFilterFactories(condition);
    }

    @Transactional(readOnly = true)
    public List<String> getFilterStores(String startAt, String endAt, String factoryName, String storeName, String setTypeName) {
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, factoryName, storeName, setTypeName);
        return customOrderRepository.findByFilterStores(condition);
    }

    @Transactional(readOnly = true)
    public List<String> getFilterSetType(String startAt, String endAt, String factoryName, String storeName, String setTypeName) {
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, factoryName, storeName, setTypeName);
        return customOrderRepository.findByFilterSetType(condition);
    }

}
