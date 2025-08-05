package com.msa.order.local.domain.order.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.order.local.domain.order.dto.OrderDto;
import com.msa.order.local.domain.order.entity.OrderStatus;
import com.msa.order.local.domain.order.entity.Orders;
import com.msa.order.local.domain.order.entity.StatusHistory;
import com.msa.order.local.domain.order.external_client.*;
import com.msa.order.local.domain.order.repository.OrdersRepository;
import com.msa.order.local.domain.order.repository.StatusHistoryRepository;
import com.msa.order.local.domain.priority.entitiy.Priority;
import com.msa.order.local.domain.priority.repository.PriorityRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

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
    private final OrdersRepository ordersRepository;
    private final PriorityRepository priorityRepository;

    public OrdersService(JwtUtil jwtUtil, StoreClient storeClient, FactoryClient factoryClient, ColorClient colorClient, ProductClient productClient, MaterialClient materialClient, OrdersRepository ordersRepository, PriorityRepository priorityRepository) {
        this.jwtUtil = jwtUtil;
        this.storeClient = storeClient;
        this.factoryClient = factoryClient;
        this.colorClient = colorClient;
        this.productClient = productClient;
        this.materialClient = materialClient;
        this.ordersRepository = ordersRepository;
        this.priorityRepository = priorityRepository;
    }

    //주문
    public void saveOrder(String accessToken, HttpServletRequest request, OrderDto.Request orderDto) {

        String nickname = jwtUtil.getNickname(accessToken);

        Long storeId = Long.valueOf(orderDto.getStoreId());
        Long factoryId = Long.valueOf(orderDto.getFactoryId());
        Long productId = Long.valueOf(orderDto.getProductId());
        Long materialId = Long.valueOf(orderDto.getMaterialId());
        Long colorId = Long.valueOf(orderDto.getColorId());

        String storeName = storeClient.getStoreInfo(request, storeId);
        String productName = productClient.getProductInfo(request, productId);
        String factoryName = factoryClient.getFactoryInfo(request,factoryId);
        String materialName = materialClient.getMaterialInfo(request, materialId);
        String colorName = colorClient.getColorInfo(request, colorId);

        // priority 추가
        Priority priority = priorityRepository.findByPriorityName(orderDto.getPriorityName());
        // orderStatus 추가
        OrderStatus orderStatus = OrderStatus.valueOf(orderDto.getOrderStatus());
        // statusHistory 추가
        StatusHistory statusHistory = StatusHistory.builder()
                .orderStatus(orderStatus)
                .createAt(orderDto.getCreateAt())
                .userName(nickname)
                .build();

        Orders order = Orders.builder()
                .storeName(storeName)
                .productName(productName)
                .productSize(orderDto.getProductSize())
                .productLaborCost(orderDto.getProductLaborCost())
                .orderNote(orderDto.getOrderNote())
                .factoryName(factoryName)
                .materialName(materialName)
                .colorName(colorName)
                .quantity(orderDto.getQuantity())
                .orderMainStoneQuantity(orderDto.getOrderMainStoneQuantity())
                .orderAuxiliaryStoneQuantity(orderDto.getOrderAuxiliaryStoneQuantity())
                .orderStatus(orderStatus)
                .statusHistory(new ArrayList<>())
                .build();

        order.addPriority(priority);
        order.addStatusHistory(statusHistory);

        ordersRepository.save(order);

        order.addOrderCode(String.format("J%05d", order.getOrderId()));
        ordersRepository.save(order);
    }

    //주문 변경 (미출고 재고만 변경 가능 + 실제로는 당일만 가능하지만 후처리도 가능해야됨)

    //주문 상태 변경

    //주문 거래처 변경

    //기성 대체

}
