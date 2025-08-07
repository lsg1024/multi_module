package com.msa.order.local.domain.order.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.order.local.domain.order.dto.OrderDto;
import com.msa.order.local.domain.order.entity.*;
import com.msa.order.local.domain.order.external_client.*;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@Transactional
public class OrdersService {
    private final JwtUtil jwtUtil;
    private final StoreClient storeClient;
    private final FactoryClient factoryClient;
    private final ColorClient colorClient;
    private final ProductClient productClient;
    private final MaterialClient materialClient;
    private final ClassificationClient classificationClient;
    private final OrdersRepository ordersRepository;
    private final CustomOrderRepository customOrderRepository;
    private final PriorityRepository priorityRepository;

    public OrdersService(JwtUtil jwtUtil, StoreClient storeClient, FactoryClient factoryClient, ColorClient colorClient, ProductClient productClient, MaterialClient materialClient, ClassificationClient classificationClient, OrdersRepository ordersRepository, CustomOrderRepository customOrderRepository, PriorityRepository priorityRepository) {
        this.jwtUtil = jwtUtil;
        this.storeClient = storeClient;
        this.factoryClient = factoryClient;
        this.colorClient = colorClient;
        this.productClient = productClient;
        this.materialClient = materialClient;
        this.classificationClient = classificationClient;
        this.ordersRepository = ordersRepository;
        this.customOrderRepository = customOrderRepository;
        this.priorityRepository = priorityRepository;
    }

    //주문
    public void saveOrder(String accessToken, HttpServletRequest request, OrderDto.Request orderDto) {

        String nickname = jwtUtil.getNickname(accessToken);

        Long storeId = Long.valueOf(orderDto.getStoreId());
        Long factoryId = Long.valueOf(orderDto.getFactoryId());
        Long productId = Long.valueOf(orderDto.getProductId());
        Long materialId = Long.valueOf(orderDto.getMaterialId());
        Long classificationId = Long.valueOf(orderDto.getClassificationId());
        Long colorId = Long.valueOf(orderDto.getColorId());

        StoreClient.StoreInfo storeInfo = storeClient.getStoreInfo(request, storeId);
        ProductDetailDto productInfo = productClient.getProductInfo(request, productId, storeInfo.getGrade());
        String factoryName = factoryClient.getFactoryInfo(request,factoryId);
        String materialName = materialClient.getMaterialInfo(request, materialId);
        String classificationName = classificationClient.getClassificationInfo(request, classificationId);
        String colorName = colorClient.getColorInfo(request, colorId);

        // priority 추가
        Priority priority = priorityRepository.findByPriorityName(orderDto.getPriorityName());
        // orderStatus 추가
        OrderStatus orderStatus = OrderStatus.valueOf(orderDto.getOrderStatus());

        // orderProduct 추가
        OrderProduct orderProduct = OrderProduct.builder()
                .factoryId(factoryId)
                .factoryName(factoryName)
                .productId(productId)
                .productName(productInfo.getProductName())
                .productSize(orderDto.getProductSize())
                .productLaborCost(productInfo.getLaborCost())
                .productAddLaborCost(orderDto.getProductAddLaborCost())
                .materialName(materialName)
                .classificationName(classificationName)
                .colorName(colorName)
                .build();

        // productInfo 값에 있는 Stone 값을 스냅샷해 저장한다.
        Orders order = Orders.builder()
                .storeId(storeId)
                .storeName(storeInfo.getStoreName())
                .orderNote(orderDto.getOrderNote())
                .statusHistory(new ArrayList<>())
                .orderStatus(orderStatus)
                .build();

        order.addOrderProduct(orderProduct);
        order.addPriority(priority);

        // statusHistory 추가
        StatusHistory statusHistory = StatusHistory.builder()
                .orderStatus(orderStatus)
                .createAt(OffsetDateTime.parse(orderDto.getCreateAt()))
                .userName(nickname)
                .build();

        order.addStatusHistory(statusHistory);

        //orderStone 추가
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
    }

    //주문 변경 (미출고 재고만 변경 가능 + 실제로는 당일만 가능하지만 후처리도 가능해야됨)

    //주문 상태 변경

    //주문 거래처 변경

    //기성 대체 -> ?

    // 주문 단건 조회
    public OrderDto.ResponseDetail getOrder(Long orderId) {
        return customOrderRepository.findByOrderId(orderId);
    }

    // 주문 전체 리스트 조회
    public CustomPage<OrderDto.Response> getOrderProducts(OrderDto.Condition condition, Pageable pageable) {
        return customOrderRepository.findByOrders(condition, pageable);

    }
}
