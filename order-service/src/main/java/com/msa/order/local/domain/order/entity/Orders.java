package com.msa.order.local.domain.order.entity;

import com.msa.order.local.domain.order.dto.FactoryDto;
import com.msa.order.local.domain.order.dto.StoreDto;
import com.msa.order.local.domain.priority.entitiy.Priority;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Table(name = "ORDERS")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Orders {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_ID")
    private Long orderId;
    @Column(name = "ORDER_CODE")
    private String orderCode;
    @Column(name = "STORE_ID") //account - store
    private Long storeId;
    @Column(name = "STORE_NAME") //account - store
    private String storeName;
    @Column(name = "FACTORY_ID")
    private Long factoryId;
    @Column(name = "FACTORY_NAME")
    private String factoryName;
    @Column(name = "ORDER_NOTE")
    private String orderNote;
    @Column(name = "ORDER_DATE")
    private OffsetDateTime orderDate;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private OrderProduct orderProduct;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderStone> orderStones = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<StatusHistory> statusHistory = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRIORITY_ID")
    private Priority priority; // 출고 등급


    @Column(name = "ORDER_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus; // 주문 상태

    @Builder
    public Orders(Long orderId, String orderCode, Long storeId, String storeName, Long factoryId, String factoryName, String orderNote, OffsetDateTime orderDate, List<StatusHistory> statusHistory, OrderStatus orderStatus) {
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.storeId = storeId;
        this.storeName = storeName;
        this.factoryId = factoryId;
        this.factoryName = factoryName;
        this.orderNote = orderNote;
        this.orderDate = orderDate;
        this.statusHistory = statusHistory;
        this.orderStatus = orderStatus;
    }

    public void addOrderStone(OrderStone orderStone) {
        this.orderStones.add(orderStone);
        orderStone.setOrder(this);
    }
    public void addStatusHistory(StatusHistory statusHistory) {
        this.statusHistory.add(statusHistory);
        statusHistory.setOrder(this);
    }

    public void addOrderProduct(OrderProduct orderProduct) {
        this.orderProduct = orderProduct;
        orderProduct.setOrder(this);
    }
    public void addOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public void addPriority(Priority priority) {
        this.priority = priority;
    }

    public void updateStatus(OrderStatus newStatus) {
        this.orderStatus = newStatus;
    }

    public void updateStoreName(StoreDto.Request storeDto) {
        this.storeId = storeDto.getStoreId();
        this.storeName = storeDto.getStoreName();
    }

    public void updateFactoryName(FactoryDto.Request factoryDto) {
        this.factoryId = factoryDto.getFactoryId();
        this.factoryName = factoryDto.getFactoryName();
    }
}

