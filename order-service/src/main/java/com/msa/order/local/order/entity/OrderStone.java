package com.msa.order.local.order.entity;

import com.msa.order.global.dto.StoneDto;
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

    @Column(name = "ORIGIN_STONE_WEIGHT", precision = 10, scale = 3)
    private BigDecimal originStoneWeight;

    @Column(name = "STONE_PURCHASE_COST")
    private Integer stonePurchaseCost; // 매입 금액

    @Column(name = "STONE_LABOR_COST")
    private Integer stoneLaborCost; // 판매 금액

    @Column(name = "STONE_ADD_LABOR_COST")
    private Integer stoneAddLaborCost; // 추가 판매 금액

    @Column(name = "STONE_QUANTITY")
    private Integer stoneQuantity; // 스톤 개수

    @Column(name = "IS_MAIN_STONE")
    private Boolean mainStone; // 메인 여부

    @Column(name = "IS_INCLUDE_STONE")
    private Boolean includeStone; // 포함 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID")
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STOCK_ID")
    private Stock stock;

    @Builder
    public OrderStone(Long originStoneId, String originStoneName, BigDecimal originStoneWeight, Integer stonePurchaseCost, Integer stoneLaborCost, Integer stoneAddLaborCost, Integer stoneQuantity, Boolean mainStone, Boolean includeStone) {
        this.originStoneId = originStoneId;
        this.originStoneName = originStoneName;
        this.originStoneWeight = originStoneWeight;
        this.stonePurchaseCost = stonePurchaseCost;
        this.stoneLaborCost = stoneLaborCost;
        this.stoneAddLaborCost = stoneAddLaborCost;
        this.stoneQuantity = stoneQuantity;
        this.mainStone = mainStone;
        this.includeStone = includeStone;
    }

    public void setOrder(Orders order) {
        this.order = order;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    /**
     * StoneInfo 로부터 값을 갱신한다. null / 빈 문자열 필드는 기존 값을 유지.
     * ({@code Long.valueOf(null)}, {@code new BigDecimal(null)} 로 인한 NPE 및
     *  payload 누락 시 DB 값이 지워지는 현상을 동시에 방지한다)
     */
    public void updateFrom(StoneDto.StoneInfo s) {
        if (s == null) {
            return;
        }
        if (s.getStoneId() != null && !s.getStoneId().isEmpty()) {
            this.originStoneId = Long.valueOf(s.getStoneId());
        }
        if (s.getStoneName() != null && !s.getStoneName().isEmpty()) {
            this.originStoneName = s.getStoneName();
        }
        if (s.getStoneWeight() != null && !s.getStoneWeight().isEmpty()) {
            this.originStoneWeight = new BigDecimal(s.getStoneWeight());
        }
        if (s.getPurchaseCost() != null) {
            this.stonePurchaseCost = s.getPurchaseCost();
        }
        if (s.getLaborCost() != null) {
            this.stoneLaborCost = s.getLaborCost();
        }
        if (s.getQuantity() != null) {
            this.stoneQuantity = s.getQuantity();
        }
        this.mainStone = s.isMainStone();
        this.includeStone = s.isIncludeStone();
    }

}
