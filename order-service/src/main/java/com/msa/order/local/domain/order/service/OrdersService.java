package com.msa.order.local.domain.order.service;

import com.msa.order.local.domain.order.dto.OrderDto;
import com.msa.order.local.domain.order.entity.Orders;
import com.msa.order.local.domain.order.external_client.*;
import com.msa.order.local.domain.order.repository.OrdersRepository;
import com.msa.order.local.domain.priority.entitiy.Priority;
import com.msa.order.local.domain.priority.repository.PriorityRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrdersService {
    private final StoreClient storeClient;
    private final FactoryClient factoryClient;
    private final ColorClient colorClient;
    private final ProductClient productClient;
    private final MaterialClient materialClient;

    private final OrdersRepository ordersRepository;
    private final PriorityRepository priorityRepository;

    public OrdersService(StoreClient storeClient, FactoryClient factoryClient, ColorClient colorClient, ProductClient productClient, MaterialClient materialClient, OrdersRepository ordersRepository, PriorityRepository priorityRepository) {
        this.storeClient = storeClient;
        this.factoryClient = factoryClient;
        this.colorClient = colorClient;
        this.productClient = productClient;
        this.materialClient = materialClient;
        this.ordersRepository = ordersRepository;
        this.priorityRepository = priorityRepository;
    }

    //주문
    public void saveOrder(HttpServletRequest request, OrderDto.Request orderDto) {

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
                .build();

        order.setPriority(priority);
        ordersRepository.save(order);

        //order update 통해 주문 번호 생성 j + fk
        order.setOrderCode(String.format("J%05d", order.getOrderId()));
    }

    //주문 변경 (미출고 재고만 변경 가능 + 실제로는 당일만 가능하지만 후처리도 가능해야됨)

    //주문 상태 변경

    //주문 거래처 변경

    //기성 대체

}
