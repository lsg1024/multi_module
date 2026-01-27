package com.msa.order.local.order.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.CustomPage;
import com.msa.order.global.dto.StatusHistoryDto;
import com.msa.order.global.dto.StoneDto;
import com.msa.order.global.excel.dto.OrderExcelQueryDto;
import com.msa.order.global.exception.InvalidOrderStatusException;
import com.msa.order.global.exception.OrderNotFoundException;
import com.msa.order.global.feign_client.client.FactoryClient;
import com.msa.order.global.feign_client.client.ProductClient;
import com.msa.order.global.feign_client.client.StoreClient;
import com.msa.order.global.feign_client.dto.ProductImageDto;
import com.msa.order.local.order.dto.*;
import com.msa.order.local.order.entity.OrderProduct;
import com.msa.order.local.order.entity.OrderStone;
import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.ProductStatus;
import com.msa.order.local.order.entity.order_enum.SourceType;
import com.msa.order.local.order.repository.CustomOrderRepository;
import com.msa.order.local.order.repository.OrdersRepository;
import com.msa.order.local.order.repository.StatusHistoryRepository;
import com.msa.order.local.order.util.ChangeTracker;
import com.msa.order.local.order.util.StatusHistoryHelper;
import com.msa.order.local.outbox.repository.OutboxEventRepository;
import com.msa.order.local.priority.repository.PriorityRepository;
import com.msa.order.local.stock.dto.StockDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.msa.order.local.order.util.StoneUtil.toStoneDtoList;

@Slf4j
@Service
@Transactional
public class OrdersService {
    private final JwtUtil jwtUtil;
    private final FactoryClient factoryClient;
    private final StoreClient storeClient;
    private final ProductClient productClient;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final OutboxEventRepository outboxEventRepository;
    private final OrdersRepository ordersRepository;
    private final CustomOrderRepository customOrderRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final PriorityRepository priorityRepository;
    private final StatusHistoryHelper statusHistoryHelper;
    private final OrderCommandService orderCommandService;

    public OrdersService(JwtUtil jwtUtil, FactoryClient factoryClient, StoreClient storeClient, ProductClient productClient, ObjectMapper objectMapper, ApplicationEventPublisher eventPublisher, OutboxEventRepository outboxEventRepository, OrdersRepository ordersRepository, CustomOrderRepository customOrderRepository, StatusHistoryRepository statusHistoryRepository, PriorityRepository priorityRepository, StatusHistoryHelper statusHistoryHelper, OrderCommandService orderCommandService) {
        this.jwtUtil = jwtUtil;
        this.factoryClient = factoryClient;
        this.storeClient = storeClient;
        this.productClient = productClient;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.outboxEventRepository = outboxEventRepository;
        this.ordersRepository = ordersRepository;
        this.customOrderRepository = customOrderRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.priorityRepository = priorityRepository;
        this.statusHistoryHelper = statusHistoryHelper;
        this.orderCommandService = orderCommandService;
    }

    // 주문 단건 조회
    @Transactional(readOnly = true)
    public OrderDto.ResponseDetail getOrder(Long flowCode) {
        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new OrderNotFoundException(flowCode));

        List<OrderStone> orderStones = order.getOrderStones();
        List<StoneDto.StoneInfo> stonesDtos = toStoneDtoList(orderStones);

        OrderProduct orderProduct = order.getOrderProduct();

        return OrderDto.ResponseDetail.builder()
                .createAt(order.getCreateAt().toString())
                .shippingAt(order.getShippingAt().toString())
                .flowCode(order.getFlowCode().toString())
                .storeId(order.getStoreId().toString())
                .storeName(order.getStoreName())
                .storeHarry(order.getStoreHarry().toPlainString())
                .storeGrade(order.getStoreGrade())
                .factoryId(order.getFactoryId().toString())
                .factoryName(order.getFactoryName())
                .productId(orderProduct.getProductId().toString())
                .productName(orderProduct.getProductName())
                .productFactoryName(orderProduct.getProductFactoryName())
                .productSize(orderProduct.getProductSize())
                .productLaborCost(orderProduct.getProductLaborCost())
                .productAddLaborCost(orderProduct.getProductAddLaborCost())
                .classificationId(String.valueOf(orderProduct.getClassificationId()))
                .classificationName(orderProduct.getClassificationName())
                .materialId(String.valueOf(orderProduct.getMaterialId()))
                .materialName(orderProduct.getMaterialName())
                .colorId(String.valueOf(orderProduct.getColorId()))
                .colorName(orderProduct.getColorName())
                .setTypeId(String.valueOf(orderProduct.getSetTypeId()))
                .setTypeName(orderProduct.getSetTypeName())
                .orderNote(order.getOrderNote())
                .mainStoneNote(orderProduct.getOrderMainStoneNote())
                .assistanceStoneNote(orderProduct.getOrderAssistanceStoneNote())
                .priority(order.getPriority().getPriorityName())
                .productStatus(order.getProductStatus().getDisplayName())
                .orderStatus(order.getOrderStatus().getDisplayName())
                .stoneInfos(stonesDtos)
                .stoneAddLaborCost(String.valueOf(orderProduct.getStoneAddLaborCost()))
                .assistantStone(orderProduct.isAssistantStone())
                .assistantStoneId(String.valueOf(orderProduct.getAssistantStoneId()))
                .assistantStoneName(orderProduct.getAssistantStoneName())
                .assistantStoneCreateAt(orderProduct.getAssistantStoneCreateAt())
                .build();

    }

    // 주문 전체 리스트 조회
    @Transactional(readOnly = true)
    public CustomPage<OrderDto.Response> getOrderProducts(String accessToken, String input, String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String sortField, String sort, String orderStatus, Pageable pageable) {
        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName);
        OrderDto.SortCondition sortCondition = new OrderDto.SortCondition(sortField, sort);
        OrderDto.OrderCondition orderCondition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, sortCondition, orderStatus);

        CustomPage<OrderQueryDto> queryDtoPage = customOrderRepository.findByOrders(inputCondition, orderCondition, pageable);
        List<OrderQueryDto> queryDtos = queryDtoPage.getContent();

        List<Long> productIds = queryDtos.stream()
                .map(OrderQueryDto::getProductId)
                .distinct()
                .toList();

        List<Long> flowCodes = queryDtos.stream()
                .map(dto -> Long.valueOf(dto.getFlowCode()))
                .toList();

        Map<Long, ProductImageDto> productImages = productClient.getProductImages(accessToken, productIds);

        List<StatusHistory> allHistories = statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(flowCodes);

        // flowCode별로 그룹핑
        Map<Long, List<StatusHistory>> historyMap = allHistories.stream()
                .collect(Collectors.groupingBy(StatusHistory::getFlowCode));

        List<OrderDto.Response> finalResponse = queryDtos.stream()
                .map(queryDto -> {
                    ProductImageDto image = productImages.get(queryDto.getProductId());
                    String imagePath = (image != null) ? image.getImagePath() : null;

                    Long flowCode = Long.valueOf(queryDto.getFlowCode());
                    List<StatusHistory> statusHistories = historyMap.getOrDefault(flowCode, new ArrayList<>());

                    List<StatusHistoryDto> statusHistoryDtos = statusHistories.stream()
                            .map(StatusHistory::toDto)
                            .toList();

                    return OrderDto.Response.from(queryDto, imagePath, statusHistoryDtos);
                })
                .toList();

        return new CustomPage<>(finalResponse, pageable, queryDtoPage.getTotalElements());
    }

    //주문
    public void saveOrder(String accessToken, String orderStatus, OrderDto.Request orderDto) {
        log.info("saveOrder = {}", orderStatus);
        String nickname = jwtUtil.getNickname(accessToken);
        String tenantId = jwtUtil.getTenantId(accessToken);

        // OrderCommandService로 위임
        Orders order = orderCommandService.createOrder(tenantId, accessToken, orderStatus, orderDto, nickname);

        // statusHistory 추가
        statusHistoryHelper.saveCreate(
                order.getFlowCode(),
                SourceType.valueOf(orderStatus),
                BusinessPhase.WAITING,
                "주문 등록",
                nickname
        );
    }

    //주문 수정
    public void updateOrder(String accessToken, Long flowCode, String orderStatus, OrderDto.Request orderDto) {
        String nickname = jwtUtil.getNickname(accessToken);
        String tenantId = TenantContext.getTenant();

        // 변경 전 값 저장
        Orders beforeOrder = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new OrderNotFoundException(flowCode));
        OrderProduct beforeProduct = beforeOrder.getOrderProduct();

        ChangeTracker tracker = new ChangeTracker("주문 수정");
        tracker.track("주문일", beforeOrder.getCreateAt() != null ? beforeOrder.getCreateAt().toLocalDate().toString() : null,
                      orderDto.getCreateAt() != null ? orderDto.getCreateAt().substring(0, 10) : null);
        tracker.track("출고일", beforeOrder.getShippingAt() != null ? beforeOrder.getShippingAt().toLocalDate().toString() : null,
                      orderDto.getShippingAt() != null ? orderDto.getShippingAt().substring(0, 10) : null);

        tracker.track("사이즈", beforeProduct.getProductSize(), orderDto.getProductSize());
        tracker.track("추가공임", beforeProduct.getProductAddLaborCost(), orderDto.getProductAddLaborCost());
        tracker.track("스톤중량", beforeProduct.getStoneWeight(), orderDto.getStoneWeight());
        tracker.track("메인스톤메모", beforeProduct.getOrderMainStoneNote(), orderDto.getMainStoneNote());
        tracker.track("보조스톤메모", beforeProduct.getOrderAssistanceStoneNote(), orderDto.getAssistanceStoneNote());
        tracker.track("등급", beforeOrder.getPriority() != null ? beforeOrder.getPriority().getPriorityName() : null, orderDto.getPriorityName());
        tracker.track("주문메모", beforeOrder.getOrderNote(), orderDto.getOrderNote());
        tracker.track("색상", beforeProduct.getColorName(), orderDto.getColorName());
        tracker.track("재질", beforeProduct.getMaterialName(), orderDto.getMaterialName());

        // 보조석 변경 추적
        String beforeAssistantId = beforeProduct.getAssistantStoneId() != null ? beforeProduct.getAssistantStoneId().toString() : null;
        if (!Objects.equals(beforeAssistantId, orderDto.getAssistantStoneId())) {
            tracker.track("보조석", beforeProduct.getAssistantStoneName(), orderDto.getAssistantStoneName());
        }

        // OrderCommandService
        Orders order = orderCommandService.updateOrder(tenantId, accessToken, flowCode, orderStatus, orderDto, nickname);

        // statusHistory
        statusHistoryHelper.savePhaseChangeFromLast(
                order.getFlowCode(),
                BusinessPhase.UPDATE,
                tracker.buildContent(),
                nickname
        );
    }

    //주문 상태 조회 (주문, 취소)
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
        throw new OrderNotFoundException(flowCode);
    }

    //주문 상태 변경
    public void updateOrderStatus(String accessToken, String id, String status) {
        List<String> allowed = Arrays.asList(
                ProductStatus.RECEIPT.name(),
                ProductStatus.WAITING.name());

        if (!allowed.contains(status.toUpperCase())) {
            throw new InvalidOrderStatusException("주문 상태를 변경할 수 없습니다.");
        }

        long flowCode = Long.parseLong(id);
        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new OrderNotFoundException(flowCode));

        OrderStatus currentOrderStatus = order.getOrderStatus();
        List<OrderStatus> allowedCurrentStatuses = Arrays.asList(
                OrderStatus.ORDER,
                OrderStatus.FIX,
                OrderStatus.NORMAL
        );

        if (!allowedCurrentStatuses.contains(currentOrderStatus)) {
            throw new InvalidOrderStatusException("주문, 수리, 일반 주문만 변경할 수 있습니다.");
        }

        ProductStatus beforeStatus = order.getProductStatus();
        ProductStatus newStatus = ProductStatus.valueOf(status.toUpperCase());
        order.updateProductStatus(newStatus);

        ordersRepository.save(order);

        // statusHistory 추가 (ChangeTracker 사용)
        ChangeTracker tracker = new ChangeTracker("상태 변경");
        tracker.track("상태", beforeStatus.getDisplayName(), newStatus.getDisplayName());

        statusHistoryHelper.savePhaseChangeFromLast(
                order.getFlowCode(),
                BusinessPhase.UPDATE,
                tracker.buildContent(),
                jwtUtil.getNickname(accessToken)
        );
    }

    //판매처 변경 -> account -> store 리스트 호출 /store/list
    public void updateOrderStore(String accessToken, String id, StoreDto.Request storeDto) {
        long flowCode = Long.parseLong(id);
        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new OrderNotFoundException(flowCode));

        String beforeStoreName = order.getStoreName();

        StoreDto.Response storeInfo = storeClient.getStoreInfo(accessToken, storeDto.getStoreId());

        order.updateStore(storeInfo);

        // statusHistory 추가 (ChangeTracker 사용)
        ChangeTracker tracker = new ChangeTracker("판매처 변경");
        tracker.track("판매처", beforeStoreName, storeInfo.getStoreName());

        statusHistoryHelper.savePhaseChangeFromLast(
                order.getFlowCode(),
                BusinessPhase.UPDATE,
                tracker.buildContent(),
                jwtUtil.getNickname(accessToken)
        );
    }

    //제조사 변경 ->
    public void updateOrderFactory(String accessToken, String id, FactoryDto.Request factoryDto) {
        long flowCode = Long.parseLong(id);
        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new OrderNotFoundException(flowCode));

        String beforeFactoryName = order.getFactoryName();

        FactoryDto.Response factoryInfo = factoryClient.getFactoryInfo(accessToken, factoryDto.getFactoryId());

        order.updateFactory(factoryInfo.getFactoryId(), factoryInfo.getFactoryName());

        // statusHistory 추가 (ChangeTracker 사용)
        ChangeTracker tracker = new ChangeTracker("제조사 변경");
        tracker.track("제조사", beforeFactoryName, factoryInfo.getFactoryName());

        statusHistoryHelper.savePhaseChangeFromLast(
                order.getFlowCode(),
                BusinessPhase.UPDATE,
                tracker.buildContent(),
                jwtUtil.getNickname(accessToken)
        );
    }

    //출고일 변경
    public void updateOrderDeliveryDate(String accessToken, String id, DateDto newDate) {

        long flowCode = Long.parseLong(id);
        Orders order = ordersRepository.findByFlowCode(flowCode)
                .orElseThrow(() -> new OrderNotFoundException(flowCode));

        OffsetDateTime beforeShippingAt = order.getShippingAt();

        OffsetDateTime received = newDate.getDeliveryDate();
        OffsetDateTime receivedKst  = received.withOffsetSameInstant(ZoneOffset.ofHours(9));

        order.updateShippingDate(receivedKst);

        // statusHistory 추가 (ChangeTracker 사용)
        ChangeTracker tracker = new ChangeTracker("출고일 변경");
        tracker.track("출고일",
                beforeShippingAt != null ? beforeShippingAt.toLocalDate().toString() : null,
                receivedKst.toLocalDate().toString());

        statusHistoryHelper.savePhaseChangeFromLast(
                order.getFlowCode(),
                BusinessPhase.UPDATE,
                tracker.buildContent(),
                jwtUtil.getNickname(accessToken)
        );
    }

    //기성 대체 -> 재고에 있는 제품 (이름, 색상, 재질 동일)

    //주문 -> 삭제
    public void deletedOrders(String accessToken, String id) {
        String nickname = jwtUtil.getNickname(accessToken);
        String role = jwtUtil.getRole(accessToken);

        Long flowCode = Long.valueOf(id);

        // OrderCommandService로 위임 (권한 체크 포함)
        orderCommandService.deleteOrder(flowCode, role);

        // statusHistory 추가
        statusHistoryHelper.savePhaseChangeFromLast(
                flowCode,
                BusinessPhase.DELETED,
                "주문 삭제",
                nickname
        );
    }

    // 수리 예정 목록 출력
    @Transactional(readOnly = true)
    public CustomPage<OrderDto.Response> getFixProducts(String accessToken, String input, String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String sortField, String sort, String orderStatus, Pageable pageable) {
        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName);
        OrderDto.SortCondition sortCondition = new OrderDto.SortCondition(sortField, sort);
        OrderDto.OrderCondition fixCondition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, sortCondition, orderStatus);

        CustomPage<OrderQueryDto> fixOrders = customOrderRepository.findByFixOrders(inputCondition, fixCondition, pageable);

        List<Long> productIds = fixOrders.stream()
                .map(OrderQueryDto::getProductId)
                .distinct()
                .toList();

        Map<Long, ProductImageDto> productImages = productClient.getProductImages(accessToken, productIds);

        List<Long> flowCodes = fixOrders.stream()
                .map(dto -> Long.valueOf(dto.getFlowCode()))
                .toList();

        List<StatusHistory> allHistories = statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(flowCodes);

        // flowCode별로 그룹핑
        Map<Long, List<StatusHistory>> historyMap = allHistories.stream()
                .collect(Collectors.groupingBy(StatusHistory::getFlowCode));

        List<OrderDto.Response> finalResponse = fixOrders.stream()
                .map(queryDto -> {
                    ProductImageDto imageDto = productImages.get(queryDto.getProductId());
                    String imagePath = (imageDto != null) ? imageDto.getImagePath() : null;

                    Long flowCode = Long.valueOf(queryDto.getFlowCode());
                    List<StatusHistory> statusHistories = historyMap.getOrDefault(flowCode, new ArrayList<>());

                    List<StatusHistoryDto> statusHistoryDtos = statusHistories.stream()
                            .map(StatusHistory::toDto)
                            .toList();

                    return OrderDto.Response.from(queryDto, imagePath, statusHistoryDtos);
                })
                .toList();

        return new CustomPage<>(finalResponse, pageable, fixOrders.getTotalElements());
    }

    // 출고 예정 목록 출력
    @Transactional(readOnly = true)
    public CustomPage<OrderDto.Response> getDeliveryProducts(String accessToken, String input, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String sortField, String sort, Pageable pageable) {
        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName);
        OrderDto.SortCondition sortCondition = new OrderDto.SortCondition(sortField, sort);
        OrderDto.ExpectCondition expectCondition = new OrderDto.ExpectCondition(endAt, optionCondition, sortCondition);

        CustomPage<OrderQueryDto> expectOrderPages = customOrderRepository.findByDeliveryOrders(inputCondition, expectCondition, pageable);

        List<Long> productIds = expectOrderPages.stream()
                .map(OrderQueryDto::getProductId)
                .distinct()
                .toList();

        Map<Long, ProductImageDto> productImages = productClient.getProductImages(accessToken, productIds);

        List<Long> flowCodes = expectOrderPages.stream()
                .map(dto -> Long.valueOf(dto.getFlowCode()))
                .toList();

        List<StatusHistory> allHistories = statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(flowCodes);

        // flowCode별로 그룹핑
        Map<Long, List<StatusHistory>> historyMap = allHistories.stream()
                .collect(Collectors.groupingBy(StatusHistory::getFlowCode));

        List<OrderDto.Response> finalResponse = expectOrderPages.stream()
                .map(queryDto -> {
                    ProductImageDto imageDto = productImages.get(queryDto.getProductId());
                    String imagePath = (imageDto != null) ? imageDto.getImagePath() : null;

                    Long flowCode = Long.valueOf(queryDto.getFlowCode());
                    List<StatusHistory> statusHistories = historyMap.getOrDefault(flowCode, new ArrayList<>());

                    List<StatusHistoryDto> statusHistoryDtos = statusHistories.stream()
                            .map(StatusHistory::toDto)
                            .toList();

                    return OrderDto.Response.from(queryDto, imagePath, statusHistoryDtos);
                })
                .toList();

        return new CustomPage<>(finalResponse, pageable, expectOrderPages.getTotalElements());
    }

    // 주문 상태에서 삭제된 목록 출력
    @Transactional(readOnly = true)
    public CustomPage<OrderDto.Response> getDeletedProducts(String accessToken, String input, String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String sortField, String sort, String orderStatus, Pageable pageable) {
        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName);
        OrderDto.SortCondition sortCondition = new OrderDto.SortCondition(sortField, sort);
        OrderDto.OrderCondition orderCondition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, sortCondition, orderStatus);

        CustomPage<OrderQueryDto> expectOrderPages = customOrderRepository.findByDeletedOrders(inputCondition, orderCondition, pageable);

        List<Long> productIds = expectOrderPages.stream()
                .map(OrderQueryDto::getProductId)
                .distinct()
                .toList();

        Map<Long, ProductImageDto> productImages = productClient.getProductImages(accessToken, productIds);

        List<Long> flowCodes = expectOrderPages.stream()
                .map(dto -> Long.valueOf(dto.getFlowCode()))
                .toList();

        List<StatusHistory> allHistories = statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(flowCodes);

        // flowCode별로 그룹핑
        Map<Long, List<StatusHistory>> historyMap = allHistories.stream()
                .collect(Collectors.groupingBy(StatusHistory::getFlowCode));

        List<OrderDto.Response> finalResponse = expectOrderPages.stream()
                .map(queryDto -> {
                    ProductImageDto imageDto = productImages.get(queryDto.getProductId());
                    String imagePath = (imageDto != null) ? imageDto.getImagePath() : null;

                    Long flowCode = Long.valueOf(queryDto.getFlowCode());
                    List<StatusHistory> statusHistories = historyMap.getOrDefault(flowCode, new ArrayList<>());

                    List<StatusHistoryDto> statusHistoryDtos = statusHistories.stream()
                            .map(StatusHistory::toDto)
                            .toList();

                    return OrderDto.Response.from(queryDto, imagePath, statusHistoryDtos);
                })
                .toList();

        return new CustomPage<>(finalResponse, pageable, expectOrderPages.getTotalElements());
    }

    // 엑셀 주문장 호출 가변 형태와 당일 값을 호출 가능하게 설정 - 주문, 수리, 출고예정, 삭제 전체에서 사용
    @Transactional(readOnly = true)
    public List<OrderExcelQueryDto> getExcel(String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String orderStatus) {
        // 주문장에서 사용할 데이터 호출
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName);
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        return customOrderRepository.findByExcelData(condition);

    }

    @Transactional(readOnly = true)
    public List<String> getFilterFactories(String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String orderStatus) {
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName);
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        return customOrderRepository.findByFilterFactories(condition);
    }

    @Transactional(readOnly = true)
    public List<String> getFilterStores(String startAt, String endAt, String factoryName, String storeName,String setTypeName, String colorName, String orderStatus) {
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName);
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        return customOrderRepository.findByFilterStores(condition);
    }

    @Transactional(readOnly = true)
    public List<String> getFilterSetType(String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String orderStatus) {
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName);
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        return customOrderRepository.findByFilterSetType(condition);
    }

    @Transactional(readOnly = true)
    public List<String> getFilterColors(String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String orderStatus) {
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName);
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        return customOrderRepository.findByFilterColor(condition);
    }

    @Transactional(readOnly = true)
    public List<StockDto.ResponseDetail> getOrderRegisterStock(List<Long> flowCodes) {
        List<Orders> orders = ordersRepository.findWithDetailsByFlowCodeIn(flowCodes);
        List<StatusHistory> statusHistories = statusHistoryRepository.findTopByFlowCodeOrderByIdDescIn(flowCodes);

        List<StockDto.ResponseDetail> responseDetails = new ArrayList<>();
        for (int i = 0; i < orders.size(); i++) {
            Orders order = orders.get(i);
            StatusHistory statusHistory = statusHistories.get(i);

            OrderProduct orderProduct = order.getOrderProduct();
            List<OrderStone> orderStones = order.getOrderStones();
            List<StoneDto.StoneInfo> stonesDtos = toStoneDtoList(orderStones);

            StockDto.ResponseDetail orderDetail = StockDto.ResponseDetail.builder()
                    .createAt(order.getCreateAt().toString())
                    .flowCode(order.getFlowCode().toString())
                    .originalProductStatus(statusHistory.getSourceType().getDisplayName())
                    .storeId(order.getStoreId().toString())
                    .storeName(order.getStoreName())
                    .storeHarry(order.getStoreHarry().toPlainString())
                    .storeGrade(order.getStoreGrade())
                    .factoryId(order.getFactoryId().toString())
                    .factoryName(order.getFactoryName())
                    .productId(orderProduct.getProductId().toString())
                    .productName(orderProduct.getProductName())
                    .productSize(orderProduct.getProductSize())
                    .colorId(String.valueOf(orderProduct.getColorId()))
                    .colorName(orderProduct.getColorName())
                    .materialId(String.valueOf(orderProduct.getMaterialId()))
                    .materialName(orderProduct.getMaterialName())
                    .note(order.getOrderNote())
                    .isProductWeightSale(order.getOrderProduct().isProductWeightSale())
                    .productPurchaseCost(orderProduct.getProductPurchaseCost())
                    .productLaborCost(orderProduct.getProductLaborCost())
                    .productAddLaborCost(orderProduct.getProductAddLaborCost())
                    .goldWeight(String.valueOf(orderProduct.getGoldWeight()))
                    .stoneWeight(String.valueOf(orderProduct.getStoneWeight()))
                    .mainStoneNote(orderProduct.getOrderMainStoneNote())
                    .assistanceStoneNote(orderProduct.getOrderAssistanceStoneNote())
                    .assistantStone(orderProduct.isAssistantStone())
                    .assistantStoneId(String.valueOf(orderProduct.getAssistantStoneId()))
                    .assistantStoneName(orderProduct.getAssistantStoneName())
                    .assistantStoneCreateAt(orderProduct.getAssistantStoneCreateAt())
                    .stoneInfos(stonesDtos)
                    .stoneAddLaborCost(order.getOrderProduct().getStoneAddLaborCost())
                    .build();

            responseDetails.add(orderDetail);
        }

        return responseDetails;
    }
}
