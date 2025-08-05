package com.msa.order.local.domain.order.entity;

import com.msa.order.local.domain.priority.entitiy.Priority;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    @Column(name = "STORE_NAME") //account - store
    private String storeName;
    @Column(name = "PRODUCT_NAME") //product - product
    private String productName;
    @Column(name = "PRODUCT_SIZE")
    private String productSize;
    @Column(name = "PRODUCT_LABOR_COST")
    private String productLaborCost;
    @Column(name = "ORDER_NOTE")
    private String orderNote;
    @Column(name = "FACTORY_NAME") //account - factory
    private String factoryName;
    @Column(name = "MATERIAL_NAME") //product - option
    private String materialName;
    @Column(name = "COLOR_NAME") //product - option
    private String colorName;
    @Column(name = "QUANTITY")
    private Integer quantity;
    @Column(name = "ORDER_MAIN_STONE_QUANTITY")
    private Integer orderMainStoneQuantity;
    @Column(name = "ORDER_AUXILIARY_STONE_QUANTITY")
    private Integer orderAuxiliaryStoneQuantity;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRIORITY_ID")
    private Priority priority;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<StatusHistory> statusHistory = new ArrayList<>();
    @Column(name = "ORDER_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Builder
    public Orders(Long orderId, String orderCode, String storeName, String productName, String productSize, String productLaborCost, String orderNote, String factoryName, String materialName, String colorName, Integer quantity, Integer orderMainStoneQuantity, Integer orderAuxiliaryStoneQuantity, List<StatusHistory> statusHistory, OrderStatus orderStatus) {
        this.orderId = orderId;
        this.orderCode = orderCode;
        this.storeName = storeName;
        this.productName = productName;
        this.productSize = productSize;
        this.productLaborCost = productLaborCost;
        this.orderNote = orderNote;
        this.factoryName = factoryName;
        this.materialName = materialName;
        this.colorName = colorName;
        this.quantity = quantity;
        this.orderMainStoneQuantity = orderMainStoneQuantity;
        this.orderAuxiliaryStoneQuantity = orderAuxiliaryStoneQuantity;
        this.statusHistory = statusHistory;
        this.orderStatus = orderStatus;
    }

    public void addStatusHistory(StatusHistory statusHistory) {
        this.statusHistory.add(statusHistory);
        statusHistory.setOrder(this);
    }

    public void addOrderCode(String orderCode) {
        this.orderCode = orderCode;
    }

    public void addPriority(Priority priority) {
        this.priority = priority;
    }
}

