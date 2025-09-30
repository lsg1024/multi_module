package com.msa.order.local.order.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.msa.order.local.order.dto.FactoryDto;
import com.msa.order.local.order.dto.StoreDto;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.ProductStatus;
import com.msa.order.local.priority.entitiy.Priority;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.*;

@Getter
@Table(name = "ORDERS")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE ORDERS SET ORDER_DELETED = TRUE WHERE ORDER_ID = ?")
public class Orders {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_ID")
    private Long orderId;
    @Tsid @Column(name = "FLOW_CODE")
    private Long flowCode;
    @Column(name = "STORE_ID") //account - store
    private Long storeId;
    @Column(name = "STORE_NAME") //account - store
    private String storeName;
    @Column(name = "STORE_HARRY", precision = 10, scale = 2)
    private BigDecimal storeHarry;
    @Column(name = "FACTORY_ID") //account - factory
    private Long factoryId;
    @Column(name = "FACTORY_NAME")
    private String factoryName;
    @Column(name = "FACTORY_HARRY", precision = 10, scale = 2)
    private BigDecimal factoryHarry;
    @Column(name = "ORDER_NOTE")
    private String orderNote;
    @Column(name = "CREATE_AT")
    private OffsetDateTime createAt;
    @Column(name = "SHIPPING_AT")
    private OffsetDateTime shippingAt;
    @Column(name = "ORDER_DELETED", nullable = false)
    private boolean orderDeleted = false;

    @OneToOne(mappedBy = "order", cascade = {PERSIST, MERGE})
    private OrderProduct orderProduct;

    @OneToMany(mappedBy = "order", cascade = {PERSIST, MERGE, REMOVE}, orphanRemoval = true)
    private List<OrderStone> orderStones = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRIORITY_ID")
    private Priority priority; // 출고 등급


    @Column(name = "PRODUCT_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private ProductStatus productStatus; // 주문 상태

    @Column(name = "ORDER_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Builder
    public Orders(Long orderId, Long flowCode, Long storeId, String storeName, Long factoryId, String factoryName, BigDecimal factoryHarry, String orderNote, OffsetDateTime createAt, OffsetDateTime shippingAt, ProductStatus productStatus, OrderStatus orderStatus) {
        this.orderId = orderId;
        this.flowCode = flowCode;
        this.storeId = storeId;
        this.storeName = storeName;
        this.factoryId = factoryId;
        this.factoryName = factoryName;
        this.factoryHarry = factoryHarry;
        this.orderNote = orderNote;
        this.createAt = createAt;
        this.shippingAt = shippingAt;
        this.productStatus = productStatus;
        this.orderStatus = orderStatus;
    }

    @PrePersist
    private void onCreate() {
        if (this.flowCode == null) {
            this.flowCode = TsidCreator.getTsid().toLong();
        }
    }

    public void addOrderStone(OrderStone orderStone) {
        this.orderStones.add(orderStone);
        orderStone.setOrder(this);
    }

    public void addOrderProduct(OrderProduct orderProduct) {
        this.orderProduct = orderProduct;
        orderProduct.setOrder(this);
    }

    public void addPriority(Priority priority) {
        this.priority = priority;
    }

    public void updateProductStatus(ProductStatus newStatus) {
        this.productStatus = newStatus;
    }

    public void updateStore(StoreDto.Response storeDto) {
        this.storeId = storeDto.getStoreId();
        this.storeName = storeDto.getStoreName();
        this.storeHarry = new BigDecimal(storeDto.getStoreHarry());
    }

    public void updateFactory(FactoryDto.Response factoryDto) {
        this.factoryId = factoryDto.getFactoryId();
        this.factoryName = factoryDto.getFactoryName();
        this.factoryHarry = new BigDecimal(factoryDto.getFactoryHarry());
    }

    public void updateOrderNote(String orderNote) {this.orderNote = orderNote;}
    public void updateCreateDate(OffsetDateTime createAt) {this.createAt = createAt;}
    public void updateShippingDate(OffsetDateTime shippingAt) {
        this.shippingAt = shippingAt;
    }

    public void deletedOrder(OffsetDateTime orderExpectDate) {
        this.shippingAt = orderExpectDate;
        this.orderDeleted = true;
    }

    public void updateOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

}

