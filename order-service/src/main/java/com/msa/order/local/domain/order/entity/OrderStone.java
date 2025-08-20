package com.msa.order.local.domain.order.entity;

import com.msa.order.local.domain.stock.dto.StockDto;
import com.msa.order.local.domain.stock.entity.domain.Stock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "ORDER_STONE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderStone {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_STONE_ID")
    private Long orderStoneId;

    @Column(name = "ORIGIN_STONE_ID")
    private Long originStoneId;

    @Column(name = "ORIGIN_STONE_NAME")
    private String originStoneName;

    @Column(name = "ORIGIN_STONE_WEIGHT", precision = 8, scale = 2)
    private BigDecimal originStoneWeight;

    @Column(name = "STONE_PURCHASE_COST")
    private Integer stonePurchaseCost; // 매입 금액

    @Column(name = "STONE_LABOR_COST")
    private Integer stoneLaborCost; // 판매 금액

    @Column(name = "STONE_QUANTITY")
    private Integer stoneQuantity; // 스톤 개수

    @Column(name = "PRODUCT_STONE_MAIN")
    private Boolean productStoneMain; // 메인 여부

    @Column(name = "INCLUDE_QUANTITY")
    private Boolean includeQuantity; // 수량 포함 여부

    @Column(name = "INCLUDE_WEIGHT")
    private Boolean includeWeight; // 중량 포함 여부

    @Column(name = "INCLUDE_LABOR")
    private Boolean includeLabor; // 공임 포함 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID")
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STOCK_ID")
    private Stock stock;

    @Builder
    public OrderStone(Long originStoneId, String originStoneName, BigDecimal originStoneWeight, Integer stonePurchaseCost, Integer stoneLaborCost, Integer stoneQuantity, Boolean productStoneMain, Boolean includeQuantity, Boolean includeWeight, Boolean includeLabor) {
        this.originStoneId = originStoneId;
        this.originStoneName = originStoneName;
        this.originStoneWeight = originStoneWeight;
        this.stonePurchaseCost = stonePurchaseCost;
        this.stoneLaborCost = stoneLaborCost;
        this.stoneQuantity = stoneQuantity;
        this.productStoneMain = productStoneMain;
        this.includeQuantity = includeQuantity;
        this.includeWeight = includeWeight;
        this.includeLabor = includeLabor;
    }

    public void setOrder(Orders order) {
        this.order = order;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public void updateFrom(StockDto.StoneInfo s) {
        this.originStoneId = Long.valueOf(s.getStoneId());
        this.originStoneName = s.getStoneName();
        this.originStoneWeight = new BigDecimal(s.getStoneWeight());
        this.stonePurchaseCost = s.getPurchaseCost();
        this.stoneLaborCost = s.getLaborCost();
        this.stoneQuantity = s.getQuantity();
        this.productStoneMain = s.isProductStoneMain();
        this.includeQuantity = s.isIncludeQuantity();
        this.includeWeight = s.isIncludeWeight();
        this.includeLabor = s.isIncludeLabor();
    }
}
