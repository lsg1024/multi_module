package com.msa.jewelry.local.order.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.tenant.TenantContext;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.local.order.dto.StatusHistoryDto;
import com.msa.jewelry.local.order.dto.StoneDto;
import com.msa.jewelry.global.excel.dto.OrderExcelQueryDto;
import com.msa.jewelry.global.exception.InvalidOrderStatusException;
import com.msa.jewelry.global.exception.OrderNotFoundException;
import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.factory.dto.FactoryView;
import com.msa.jewelry.local.store.service.StoreService;
import com.msa.jewelry.local.store.dto.StoreView;
import com.msa.jewelry.local.product.service.ProductService;
import com.msa.jewelry.local.product.dto.ProductImageView;
import com.msa.jewelry.global.util.SafeParse;
import com.msa.jewelry.local.order.dto.*;
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
import com.msa.jewelry.local.order.util.ChangeTracker;
import com.msa.jewelry.local.order.util.StatusHistoryHelper;
import com.msa.jewelry.local.priority.repository.PriorityRepository;
import com.msa.jewelry.local.stock.dto.StockDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.msa.jewelry.local.order.util.StoneUtil.toStoneDtoList;

@Slf4j
@Service
@Transactional
public class OrdersService {
    private final JwtUtil jwtUtil;
    private final FactoryService factoryService;
    private final StoreService storeService;
    private final ProductService productService;
    private final OrdersRepository ordersRepository;
    private final CustomOrderRepository customOrderRepository;
    private final StatusHistoryRepository statusHistoryRepository;
    private final PriorityRepository priorityRepository;
    private final StatusHistoryHelper statusHistoryHelper;
    private final OrderCommandService orderCommandService;

    public OrdersService(JwtUtil jwtUtil, FactoryService factoryService, StoreService storeService, ProductService productService, OrdersRepository ordersRepository, CustomOrderRepository customOrderRepository, StatusHistoryRepository statusHistoryRepository, PriorityRepository priorityRepository, StatusHistoryHelper statusHistoryHelper, OrderCommandService orderCommandService) {
        this.jwtUtil = jwtUtil;
        this.factoryService = factoryService;
        this.storeService = storeService;
        this.productService = productService;
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

        String storeName = order.getStoreId() != null
                ? storeService.getStoreInfoView(order.getStoreId()).storeName()
                : null;
        String factoryName = order.getFactoryId() != null
                ? factoryService.getFactoryInfo(order.getFactoryId()).factoryName()
                : null;

        return OrderDto.ResponseDetail.builder()
                .createAt(order.getCreateAt() != null ? order.getCreateAt().toString() : null)
                .shippingAt(order.getShippingAt() != null ? order.getShippingAt().toString() : null)
                .flowCode(order.getFlowCode() != null ? order.getFlowCode().toString() : null)
                .storeId(order.getStoreId() != null ? order.getStoreId().toString() : null)
                .storeName(storeName)
                .storeHarry(order.getStoreHarry() != null ? order.getStoreHarry().toPlainString() : null)
                .storeGrade(order.getStoreGrade())
                .factoryId(order.getFactoryId() != null ? order.getFactoryId().toString() : null)
                .factoryName(factoryName)
                .productId(orderProduct.getProductId() != null ? orderProduct.getProductId().toString() : null)
                .productName(orderProduct.getProductName())
                .productFactoryName(orderProduct.getProductFactoryName())
                .productSize(orderProduct.getProductSize())
                .productLaborCost(orderProduct.getProductLaborCost())
                .productAddLaborCost(orderProduct.getProductAddLaborCost())
                .classificationId(orderProduct.getClassificationId() != null ? orderProduct.getClassificationId().toString() : null)
                .classificationName(orderProduct.getClassificationName())
                .materialId(orderProduct.getMaterialId() != null ? orderProduct.getMaterialId().toString() : null)
                .materialName(orderProduct.getMaterialName())
                .colorId(orderProduct.getColorId() != null ? orderProduct.getColorId().toString() : null)
                .colorName(orderProduct.getColorName())
                .setTypeId(orderProduct.getSetTypeId() != null ? orderProduct.getSetTypeId().toString() : null)
                .setTypeName(orderProduct.getSetTypeName())
                .orderNote(order.getOrderNote())
                .mainStoneNote(orderProduct.getOrderMainStoneNote())
                .assistanceStoneNote(orderProduct.getOrderAssistanceStoneNote())
                .priority(order.getPriority().getPriorityName())
                .productStatus(order.getProductStatus().getDisplayName())
                .orderStatus(order.getOrderStatus().getDisplayName())
                .stoneInfos(stonesDtos)
                .stoneAddLaborCost(orderProduct.getStoneAddLaborCost() != null ? orderProduct.getStoneAddLaborCost().toString() : null)
                .assistantStone(orderProduct.isAssistantStone())
                .assistantStoneId(orderProduct.getAssistantStoneId() != null ? orderProduct.getAssistantStoneId().toString() : null)
                .assistantStoneName(orderProduct.getAssistantStoneName())
                .assistantStoneCreateAt(orderProduct.getAssistantStoneCreateAt())
                .build();

    }

    // 주문 전체 리스트 조회
    @Transactional(readOnly = true)
    public CustomPage<OrderDto.Response> getOrderProducts(String input, String searchField, String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String classificationName, String materialName, String sortField, String sort, String orderStatus, Pageable pageable) {
        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input, searchField);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName, classificationName, materialName);
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

        Map<Long, ProductImageView> productImages = productService.getProductImages(productIds);

        List<StatusHistory> allHistories = statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(flowCodes);

        // flowCode별로 그룹핑
        Map<Long, List<StatusHistory>> historyMap = allHistories.stream()
                .collect(Collectors.groupingBy(StatusHistory::getFlowCode));

        List<OrderDto.Response> finalResponse = queryDtos.stream()
                .map(queryDto -> {
                    ProductImageView image = productImages.get(queryDto.getProductId());
                    String imagePath = (image != null) ? image.imagePath() : null;

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

        // OrderCommandService로 위임
        Orders order = orderCommandService.createOrder(orderStatus, orderDto);

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
        Orders order = orderCommandService.updateOrder(flowCode, orderStatus, orderDto);

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

        String beforeStoreName = order.getStoreId() != null
                ? storeService.getStoreInfoView(order.getStoreId()).storeName()
                : null;

        StoreView storeInfo = storeService.getStoreInfoView(storeDto.getStoreId());

        order.updateStore(
                storeInfo.storeId(),
                storeInfo.storeGrade(),
                SafeParse.toBigDecimalOrNull(storeInfo.storeHarry())
        );

        // statusHistory 추가 (ChangeTracker 사용)
        ChangeTracker tracker = new ChangeTracker("판매처 변경");
        tracker.track("판매처", beforeStoreName, storeInfo.storeName());

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

        String beforeFactoryName = order.getFactoryId() != null
                ? factoryService.getFactoryInfo(order.getFactoryId()).factoryName()
                : null;

        FactoryView factoryInfo = factoryService.getFactoryInfo(factoryDto.getFactoryId());

        order.updateFactory(
                factoryInfo.factoryId(),
                SafeParse.toBigDecimalOrNull(factoryInfo.goldHarryLoss())
        );

        // statusHistory 추가 (ChangeTracker 사용)
        ChangeTracker tracker = new ChangeTracker("제조사 변경");
        tracker.track("제조사", beforeFactoryName, factoryInfo.factoryName());

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

        LocalDateTime beforeShippingAt = order.getShippingAt();

        // 애플리케이션·DB 가 모두 KST 기준이므로 추가 offset 변환 없이 그대로 사용한다.
        LocalDateTime receivedKst = newDate.getDeliveryDate();

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
    public CustomPage<OrderDto.Response> getFixProducts(String accessToken, String input, String searchField, String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String classificationName, String materialName, String sortField, String sort, String orderStatus, Pageable pageable) {
        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input, searchField);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName, classificationName, materialName);
        OrderDto.SortCondition sortCondition = new OrderDto.SortCondition(sortField, sort);
        OrderDto.OrderCondition fixCondition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, sortCondition, orderStatus);

        CustomPage<OrderQueryDto> fixOrders = customOrderRepository.findByFixOrders(inputCondition, fixCondition, pageable);

        List<Long> productIds = fixOrders.stream()
                .map(OrderQueryDto::getProductId)
                .distinct()
                .toList();

        Map<Long, ProductImageView> productImages = productService.getProductImages(productIds);

        List<Long> flowCodes = fixOrders.stream()
                .map(dto -> Long.valueOf(dto.getFlowCode()))
                .toList();

        List<StatusHistory> allHistories = statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(flowCodes);

        // flowCode별로 그룹핑
        Map<Long, List<StatusHistory>> historyMap = allHistories.stream()
                .collect(Collectors.groupingBy(StatusHistory::getFlowCode));

        List<OrderDto.Response> finalResponse = fixOrders.stream()
                .map(queryDto -> {
                    ProductImageView imageDto = productImages.get(queryDto.getProductId());
                    String imagePath = (imageDto != null) ? imageDto.imagePath() : null;

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
    public CustomPage<OrderDto.Response> getDeliveryProducts(String accessToken, String input, String searchField,
                                                              String startAt, String endAt, String orderStatus,
                                                              String factoryName, String storeName, String setTypeName,
                                                              String colorName, String classificationName, String materialName,
                                                              String sortField, String sort, Pageable pageable) {
        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input, searchField);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName, classificationName, materialName);
        OrderDto.SortCondition sortCondition = new OrderDto.SortCondition(sortField, sort);
        OrderDto.ExpectCondition expectCondition = new OrderDto.ExpectCondition(startAt, endAt, orderStatus, optionCondition, sortCondition);

        CustomPage<OrderQueryDto> expectOrderPages = customOrderRepository.findByDeliveryOrders(inputCondition, expectCondition, pageable);

        List<Long> productIds = expectOrderPages.stream()
                .map(OrderQueryDto::getProductId)
                .distinct()
                .toList();

        Map<Long, ProductImageView> productImages = productService.getProductImages(productIds);

        List<Long> flowCodes = expectOrderPages.stream()
                .map(dto -> Long.valueOf(dto.getFlowCode()))
                .toList();

        List<StatusHistory> allHistories = statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(flowCodes);

        // flowCode별로 그룹핑
        Map<Long, List<StatusHistory>> historyMap = allHistories.stream()
                .collect(Collectors.groupingBy(StatusHistory::getFlowCode));

        List<OrderDto.Response> finalResponse = expectOrderPages.stream()
                .map(queryDto -> {
                    ProductImageView imageDto = productImages.get(queryDto.getProductId());
                    String imagePath = (imageDto != null) ? imageDto.imagePath() : null;

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
    public CustomPage<OrderDto.Response> getDeletedProducts(String accessToken, String input, String searchField, String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String classificationName, String materialName, String sortField, String sort, String orderStatus, Pageable pageable) {
        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(input, searchField);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName, classificationName, materialName);
        OrderDto.SortCondition sortCondition = new OrderDto.SortCondition(sortField, sort);
        OrderDto.OrderCondition orderCondition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, sortCondition, orderStatus);

        CustomPage<OrderQueryDto> expectOrderPages = customOrderRepository.findByDeletedOrders(inputCondition, orderCondition, pageable);

        List<Long> productIds = expectOrderPages.stream()
                .map(OrderQueryDto::getProductId)
                .distinct()
                .toList();

        Map<Long, ProductImageView> productImages = productService.getProductImages(productIds);

        List<Long> flowCodes = expectOrderPages.stream()
                .map(dto -> Long.valueOf(dto.getFlowCode()))
                .toList();

        List<StatusHistory> allHistories = statusHistoryRepository.findAllByFlowCodeInOrderByCreateAtAsc(flowCodes);

        // flowCode별로 그룹핑
        Map<Long, List<StatusHistory>> historyMap = allHistories.stream()
                .collect(Collectors.groupingBy(StatusHistory::getFlowCode));

        List<OrderDto.Response> finalResponse = expectOrderPages.stream()
                .map(queryDto -> {
                    ProductImageView imageDto = productImages.get(queryDto.getProductId());
                    String imagePath = (imageDto != null) ? imageDto.imagePath() : null;

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
    public List<OrderExcelQueryDto> getExcel(String startAt, String endAt, String search, String searchField, String factoryName, String storeName, String setTypeName, String colorName, String classificationName, String materialName, String orderStatus) {
        OrderDto.InputCondition inputCondition = new OrderDto.InputCondition(search, searchField);
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName, classificationName, materialName);
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        return customOrderRepository.findByExcelData(inputCondition, condition);

    }

    @Transactional(readOnly = true)
    public List<String> getFilterFactories(String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String classificationName, String materialName, String orderStatus) {
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName, classificationName, materialName);
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        return customOrderRepository.findByFilterFactories(condition);
    }

    @Transactional(readOnly = true)
    public List<String> getFilterStores(String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String classificationName, String materialName, String orderStatus) {
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName, classificationName, materialName);
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        return customOrderRepository.findByFilterStores(condition);
    }

    @Transactional(readOnly = true)
    public List<String> getFilterSetType(String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String classificationName, String materialName, String orderStatus) {
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName, classificationName, materialName);
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        return customOrderRepository.findByFilterSetType(condition);
    }

    @Transactional(readOnly = true)
    public List<String> getFilterColors(String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String classificationName, String materialName, String orderStatus) {
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName, classificationName, materialName);
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        return customOrderRepository.findByFilterColor(condition);
    }

    @Transactional(readOnly = true)
    public List<String> getFilterClassifications(String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String classificationName, String materialName, String orderStatus) {
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName, classificationName, materialName);
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        return customOrderRepository.findByFilterClassification(condition);
    }

    @Transactional(readOnly = true)
    public List<String> getFilterMaterials(String startAt, String endAt, String factoryName, String storeName, String setTypeName, String colorName, String classificationName, String materialName, String orderStatus) {
        OrderDto.OptionCondition optionCondition = new OrderDto.OptionCondition(factoryName, storeName, setTypeName, colorName, classificationName, materialName);
        OrderDto.OrderCondition condition = new OrderDto.OrderCondition(startAt, endAt, optionCondition, orderStatus);
        return customOrderRepository.findByFilterMaterial(condition);
    }

    @Transactional(readOnly = true)
    public List<StockDto.ResponseDetail> getOrderRegisterStock(List<Long> flowCodes) {
        List<Orders> orders = ordersRepository.findWithDetailsByFlowCodeIn(flowCodes);
        List<StatusHistory> statusHistories = statusHistoryRepository.findTopByFlowCodeOrderByIdDescIn(flowCodes);

        // findTopByFlowCodeOrderByIdDescIn 쿼리는 실제로 flowCode 당 최신 이력 하나가 아닌
        // 조건에 매칭되는 모든 이력을 반환한다. 따라서 flowCode 별 최신(id 최대) 이력만 선택하도록
        // 매핑을 구성해 orders 와의 인덱스 오정렬/IndexOutOfBoundsException 을 방지한다.
        Map<Long, StatusHistory> latestHistoryByFlowCode = new HashMap<>();
        for (StatusHistory history : statusHistories) {
            if (history == null || history.getFlowCode() == null) {
                continue;
            }
            StatusHistory current = latestHistoryByFlowCode.get(history.getFlowCode());
            if (current == null
                    || (history.getId() != null
                        && (current.getId() == null || history.getId() > current.getId()))) {
                latestHistoryByFlowCode.put(history.getFlowCode(), history);
            }
        }

        List<StockDto.ResponseDetail> responseDetails = new ArrayList<>();
        for (Orders order : orders) {
            StatusHistory statusHistory = latestHistoryByFlowCode.get(order.getFlowCode());

            OrderProduct orderProduct = order.getOrderProduct();
            List<OrderStone> orderStones = order.getOrderStones();
            List<StoneDto.StoneInfo> stonesDtos = toStoneDtoList(orderStones);

            String originalProductStatus = (statusHistory != null && statusHistory.getSourceType() != null)
                    ? statusHistory.getSourceType().getDisplayName()
                    : null;

            String orderStoreName = order.getStoreId() != null
                    ? storeService.getStoreInfoView(order.getStoreId()).storeName()
                    : null;
            String orderFactoryName = order.getFactoryId() != null
                    ? factoryService.getFactoryInfo(order.getFactoryId()).factoryName()
                    : null;

            StockDto.ResponseDetail orderDetail = StockDto.ResponseDetail.builder()
                    .createAt(order.getCreateAt() != null ? order.getCreateAt().toString() : null)
                    .flowCode(order.getFlowCode() != null ? order.getFlowCode().toString() : null)
                    .originalProductStatus(originalProductStatus)
                    .storeId(order.getStoreId() != null ? order.getStoreId().toString() : null)
                    .storeName(orderStoreName)
                    .storeHarry(order.getStoreHarry() != null ? order.getStoreHarry().toPlainString() : null)
                    .storeGrade(order.getStoreGrade())
                    .factoryId(order.getFactoryId() != null ? order.getFactoryId().toString() : null)
                    .factoryName(orderFactoryName)
                    .productId(orderProduct != null && orderProduct.getProductId() != null ? orderProduct.getProductId().toString() : null)
                    .productName(orderProduct != null ? orderProduct.getProductName() : null)
                    .productSize(orderProduct != null ? orderProduct.getProductSize() : null)
                    .colorId(orderProduct != null && orderProduct.getColorId() != null ? String.valueOf(orderProduct.getColorId()) : null)
                    .colorName(orderProduct != null ? orderProduct.getColorName() : null)
                    .materialId(orderProduct != null && orderProduct.getMaterialId() != null ? String.valueOf(orderProduct.getMaterialId()) : null)
                    .materialName(orderProduct != null ? orderProduct.getMaterialName() : null)
                    .note(order.getOrderNote())
                    .isProductWeightSale(orderProduct != null && orderProduct.isProductWeightSale())
                    .productPurchaseCost(orderProduct != null ? orderProduct.getProductPurchaseCost() : null)
                    .productLaborCost(orderProduct != null ? orderProduct.getProductLaborCost() : null)
                    .productAddLaborCost(orderProduct != null ? orderProduct.getProductAddLaborCost() : null)
                    .goldWeight(orderProduct != null && orderProduct.getGoldWeight() != null ? orderProduct.getGoldWeight().toPlainString() : null)
                    .stoneWeight(orderProduct != null && orderProduct.getStoneWeight() != null ? orderProduct.getStoneWeight().toPlainString() : null)
                    .mainStoneNote(orderProduct != null ? orderProduct.getOrderMainStoneNote() : null)
                    .assistanceStoneNote(orderProduct != null ? orderProduct.getOrderAssistanceStoneNote() : null)
                    .assistantStone(orderProduct != null && orderProduct.isAssistantStone())
                    .assistantStoneId(orderProduct != null && orderProduct.getAssistantStoneId() != null ? String.valueOf(orderProduct.getAssistantStoneId()) : null)
                    .assistantStoneName(orderProduct != null ? orderProduct.getAssistantStoneName() : null)
                    .assistantStoneCreateAt(orderProduct != null ? orderProduct.getAssistantStoneCreateAt() : null)
                    .stoneInfos(stonesDtos)
                    .stoneAddLaborCost(orderProduct != null ? orderProduct.getStoneAddLaborCost() : null)
                    .build();

            responseDetails.add(orderDetail);
        }

        return responseDetails;
    }
}
