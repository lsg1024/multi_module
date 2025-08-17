package com.msa.order.local.domain.order.entity;

import com.msa.order.local.domain.order.entity.order_enum.OrderStatus;
import com.msa.order.local.domain.order.entity.order_enum.ProductStatus;
import com.msa.order.local.domain.stock.entity.domain.Stock;
import com.msa.order.local.domain.stock.entity.stock_enum.StockStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Table(name = "STATUS_HISTORY")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StatusHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;
    @Column(name = "PRODUCT_STATUS")
    @Enumerated(EnumType.STRING)
    private ProductStatus productStatus;
    @Column(name = "ORDER_STATUS")
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    @Column(name = "STOCK_STATUS")
    @Enumerated(EnumType.STRING)
    private StockStatus stockStatus;
    @Column(name = "CREATE_AT")
    private OffsetDateTime createAt;
    @Column(name = "USER_NAME")
    private String userName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID")
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STOCK_ID")
    private Stock stock;

    @Builder
    public StatusHistory(ProductStatus productStatus, OrderStatus orderStatus, StockStatus stockStatus, OffsetDateTime createAt, String userName, Orders order) {
        this.productStatus = productStatus;
        this.orderStatus = orderStatus;
        this.stockStatus = stockStatus;
        this.createAt = createAt;
        this.userName = userName;
        this.order = order;
    }

    public void setOrder(Orders orders) {
        this.order = orders;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }
}
