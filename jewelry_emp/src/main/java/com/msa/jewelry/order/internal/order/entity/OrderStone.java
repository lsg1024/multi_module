package com.msa.jewelry.order.internal.order.entity;

import com.msa.jewelry.order.internal.global.dto.StoneDto;
import com.msa.jewelry.order.internal.stock.entity.Stock;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "주문 스톤 엔티티 — 주문(Orders) 한 건에 포함된 스톤 라인 (메인/보조 스톤 모두). 1:N.")
public class OrderStone {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_STONE_ID")
    @Schema(description = "주문 스톤 PK", example = "2001")
    private Long orderStoneId;

    @Column(name = "ORIGIN_STONE_ID")
    @Schema(description = "원본 스톤 ID (product 모듈 Stone FK)", example = "101")
    private Long originStoneId;

    @Column(name = "ORIGIN_STONE_NAME")
    @Schema(description = "원본 스톤 이름 (스냅샷)", example = "다이아몬드")
    private String originStoneName;

    @Column(name = "ORIGIN_STONE_WEIGHT", precision = 10, scale = 3)
    @Schema(description = "원본 스톤 무게 (g)", example = "0.250")
    private BigDecimal originStoneWeight;

    @Column(name = "STONE_PURCHASE_COST")
    @Schema(description = "스톤 매입 금액", example = "100000")
    private Integer stonePurchaseCost; // 매입 금액

    @Column(name = "STONE_LABOR_COST")
    @Schema(description = "스톤 판매(공임) 금액", example = "150000")
    private Integer stoneLaborCost; // 판매 금액

    @Column(name = "STONE_ADD_LABOR_COST")
    @Schema(description = "스톤 추가 판매(공임) 금액", example = "20000")
    private Integer stoneAddLaborCost; // 추가 판매 금액

    @Column(name = "STONE_QUANTITY")
    @Schema(description = "스톤 개수", example = "1")
    private Integer stoneQuantity; // 스톤 개수

    @Column(name = "IS_MAIN_STONE")
    @Schema(description = "메인 스톤 여부 (true=메인, false=보조)", example = "true")
    private Boolean mainStone; // 메인 여부

    @Column(name = "IS_INCLUDE_STONE")
    @Schema(description = "스톤 포함 거래 여부 (가격에 포함시킬지)", example = "true")
    private Boolean includeStone; // 포함 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID")
    @Schema(description = "소속 주문 (Orders)")
    private Orders order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STOCK_ID")
    @Schema(description = "연결된 재고 (Stock) — 재고 생성 후 연결")
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
