package com.msa.order.local.order.entity;

import com.msa.order.local.stock.dto.StockDto;
import com.msa.order.local.stock.entity.Stock;
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

    @Column(name = "IS_MAIN_STONE")
    private Boolean isMainStone; // 메인 여부

    @Column(name = "IS_INCLUDE_STONE")
    private Boolean isIncludeStone; // 포함 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID")
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STOCK_ID")
    private Stock stock;

    @Builder
    public OrderStone(Long originStoneId, String originStoneName, BigDecimal originStoneWeight, Integer stonePurchaseCost, Integer stoneLaborCost, Integer stoneQuantity, Boolean isMainStone, Boolean isIncludeStone) {
        this.originStoneId = originStoneId;
        this.originStoneName = originStoneName;
        this.originStoneWeight = originStoneWeight;
        this.stonePurchaseCost = stonePurchaseCost;
        this.stoneLaborCost = stoneLaborCost;
        this.stoneQuantity = stoneQuantity;
        this.isMainStone = isMainStone;
        this.isIncludeStone = isIncludeStone;
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
        this.isMainStone = s.getIsMainStone();
        this.isIncludeStone = s.getIsIncludeStone();
    }

}
