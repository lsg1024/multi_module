package com.msa.order.local.domain.stock.entity.domain;

import com.msa.order.local.domain.order.entity.OrderStone;
import com.msa.order.local.domain.order.entity.StatusHistory;
import com.msa.order.local.domain.order.entity.order_enum.OrderStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.SQLDelete;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.MERGE;
import static jakarta.persistence.CascadeType.PERSIST;

@Slf4j
@Getter
@Table(name = "STOCK")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE STOCK SET STOCK_DELETED = TRUE WHERE STOCK_ID = ?")
public class Stock {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STOCK_ID")
    private Long stockId;
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
    @Column(name = "STOCK_NOTE")
    private String stockNote;
    @Column(name = "STOCK_MAIN_STONE_NOTE")
    private String stockMainStoneNote;
    @Column(name = "STOCK_ASSISTANCE_STONE_NOTE")
    private String stockAssistanceStoneNote;
    @Column(name = "STOCK_DATE")
    private OffsetDateTime stockDate;
    @Column(name = "STOCK_EXPECT_DATE")
    private OffsetDateTime stockExpectDate;
    @Column(name = "STOCK_DELETED", nullable = false)
    private boolean stockDeleted = false;

    @Embedded
    private ProductSnapshot product;

    @OneToMany(mappedBy = "stock", cascade = {PERSIST, MERGE})
    private List<OrderStone> orderStones = new ArrayList<>();

    @OneToMany(mappedBy = "stock", cascade = {PERSIST, MERGE})
    private List<StatusHistory> statusHistory = new ArrayList<>();

    @Column(name = "ORDER_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

}
