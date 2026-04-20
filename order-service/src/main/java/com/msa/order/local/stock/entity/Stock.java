package com.msa.order.local.stock.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.msa.common.global.domain.BaseTimeEntity;
import com.msa.order.local.order.dto.FactoryDto;
import com.msa.order.local.order.dto.StoreDto;
import com.msa.order.local.order.entity.OrderStone;
import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.stock.dto.StockDto;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.*;

/**
 * 재고 엔티티.
 *
 * *공장에서 출고된 제품의 재고 정보를 관리하며, 주문({@link Orders})과 연결되거나
 * 독립 재고로 존재할 수 있다. {@code stockCode}와 {@code flowCode}는 TSID 기반 식별자이며,
 * 주문 연결 시 {@code flowCode}는 주문의 값을 그대로 사용한다.
 *
 * *상태 전이 흐름:
 * {@code WAITING} → {@code STOCK} → {@code RENTAL} → {@code RETURN} → {@code SALE} 또는 {@code DELETED}
 *
 * *삭제는 소프트 딜리트({@code STOCK_DELETED = TRUE})로 처리된다.
 */
@Slf4j
@Getter
@Table(name = "STOCK")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE STOCK SET STOCK_DELETED = TRUE WHERE STOCK_ID = ?")
public class Stock extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STOCK_ID")
    private Long stockId;
    /** TSID 기반 재고 고유 코드. 독립 재고 시 flowCode와 동일하게 초기화된다. */
    @Tsid @Column(name = "STOCK_CODE")
    private Long stockCode;
    /** 주문·재고·판매·이력 테이블의 공통 추적 키. 주문 연결 시 Orders.flowCode를 사용한다. */
    @Column(name = "FLOW_CODE")
    private Long flowCode;
    @Column(name = "STORE_ID") //account - store
    private Long storeId;
    @Column(name = "STORE_NAME") //account - store
    private String storeName;
    @Column(name = "STORE_HARRY", precision = 10, scale = 2)
    private BigDecimal storeHarry;
    @Column(name = "STORE_GRADE") //account - store
    private String storeGrade;
    @Column(name = "FACTORY_ID") //account - factory
    private Long factoryId;
    @Column(name = "FACTORY_NAME")
    private String factoryName;
    @Column(name = "FACTORY_HARRY", precision = 10, scale = 2)
    private BigDecimal factoryHarry;
    @Column(name = "STOCK_NOTE")
    private String stockNote;
    @Column(name = "STOCK_MAIN_STONE_NOTE")
    private String stockMainStoneNote;
    @Column(name = "STOCK_ASSISTANCE_STONE_NOTE")
    private String stockAssistanceStoneNote;
    @Column(name = "STONE_MAIN_LABOR_COST") // 스톤 메인 매출 비용
    private Integer stoneMainLaborCost;
    @Column(name = "STONE_ASSISTANCE_LABOR_COST") // 스톤 보조 매출 비용
    private Integer stoneAssistanceLaborCost;
    @Column(name = "STONE_ADD_LABOR_COST") // 추가 스톤 매출 비용
    private Integer stoneAddLaborCost;
    @Column(name = "TOTAL_STONE_PURCHASE_COST") // 총 스톤 매입 비용
    private Integer totalStonePurchaseCost;
    @Column(name = "TOTAL_STONE_LABOR_COST")
    private Integer totalStoneLaborCost;
    @Column(name = "STOCK_DELETED", nullable = false)
    private boolean stockDeleted = false;

    @Column(name = "STOCK_CHECKED")
    private Boolean stockChecked = false;

    @Column(name = "STOCK_CHECKED_AT")
    private LocalDateTime stockCheckedAt;

    @Embedded
    private ProductSnapshot product;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID")
    private Orders order;

    @OneToMany(mappedBy = "stock", cascade = {PERSIST, MERGE, REMOVE}, orphanRemoval = true)
    private List<OrderStone> orderStones = new ArrayList<>();

    @Column(name = "ORDER_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    /** 마이그레이션 시 원재고구분 값을 임시로 전달하기 위한 필드 (DB 저장 안 됨) */
    @Transient
    private String migrationSourceType;

    /** 마이그레이션 시 CSV 등록일을 임시로 전달하기 위한 필드 (DB 저장 안 됨) */
    @Transient
    private LocalDateTime migrationCreatedDate;

    /** 마이그레이션 시 CSV 변경일을 임시로 전달하기 위한 필드 (DB 저장 안 됨) */
    @Transient
    private LocalDateTime migrationModifiedDate;

    public void setMigrationSourceType(String migrationSourceType) {
        this.migrationSourceType = migrationSourceType;
    }

    public void setMigrationCreatedDate(LocalDateTime migrationCreatedDate) {
        this.migrationCreatedDate = migrationCreatedDate;
    }

    public void setMigrationModifiedDate(LocalDateTime migrationModifiedDate) {
        this.migrationModifiedDate = migrationModifiedDate;
    }

    @Builder
    public Stock(Long stockCode, Long flowCode, Long storeId, String storeName, BigDecimal storeHarry, String storeGrade, Long factoryId, String factoryName, BigDecimal factoryHarry, String stockNote, String stockMainStoneNote, String stockAssistanceStoneNote, Integer stoneMainLaborCost, Integer stoneAssistanceLaborCost, Integer stoneAddLaborCost, Integer totalStonePurchaseCost, Integer totalStoneLaborCost, boolean stockDeleted, ProductSnapshot product, Orders orders, List<OrderStone> orderStones, OrderStatus orderStatus) {
        this.stockCode = stockCode;
        this.flowCode = flowCode;
        this.storeId = storeId;
        this.storeName = storeName;
        this.storeHarry = storeHarry;
        this.storeGrade = storeGrade;
        this.factoryId = factoryId;
        this.factoryName = factoryName;
        this.factoryHarry = factoryHarry;
        this.stockNote = stockNote;
        this.stockMainStoneNote = stockMainStoneNote;
        this.stockAssistanceStoneNote = stockAssistanceStoneNote;
        this.stoneMainLaborCost = stoneMainLaborCost;
        this.stoneAssistanceLaborCost = stoneAssistanceLaborCost;
        this.stoneAddLaborCost = stoneAddLaborCost;
        this.totalStonePurchaseCost = totalStonePurchaseCost;
        this.totalStoneLaborCost = totalStoneLaborCost;
        this.stockDeleted = stockDeleted;
        this.product = product;
        this.order = orders;
        this.orderStones = orderStones != null ? orderStones : new ArrayList<>();
        this.orderStatus = orderStatus;
    }

    public void setOrder(Orders orders) {
        this.order = orders;
        this.flowCode = orders.getFlowCode();
    }

    public void removeOrder() {
        this.flowCode = TsidCreator.getTsid().toLong();
        this.stockCode = flowCode;
        this.order = null;
    }

    public void unlinkOrder() {
        this.order = null;
    }

    public void addStockStone(OrderStone orderStone) {
        this.orderStones.add(orderStone);
        orderStone.setStock(this);
    }

    public void updateStore(StoreDto.Response storeDto) {
        this.storeId = storeDto.getStoreId();
        this.storeName = storeDto.getStoreName();
    }

    public void updateStore(Long storeId, String storeName, String storeGrade, BigDecimal storeHarry) {
        this.storeId = storeId;
        this.storeName = storeName;
        this.storeGrade = storeGrade;
        this.storeHarry = storeHarry;
    }

    public void updateFactory(FactoryDto.Response factoryDto) {
        this.factoryId = factoryDto.getFactoryId();
        this.factoryName = factoryDto.getFactoryName();
    }

    public void updateFactory(Long factoryId, String factoryName, BigDecimal factoryHarry) {
        this.factoryId = factoryId;
        this.factoryName = factoryName;
        this.factoryHarry = factoryHarry;
    }

    public void moveToRental(StockDto.StockRentalRequest rentalRequest) {
        this.stoneAddLaborCost = rentalRequest.getStoneAddLaborCost();
        this.stockNote = rentalRequest.getStockNote();
        this.stockMainStoneNote = rentalRequest.getMainStoneNote();
        this.stockAssistanceStoneNote = rentalRequest.getAssistanceStoneNote();
        this.orderStatus = OrderStatus.RENTAL;
        // 대여 시 거래처 변경
        if (rentalRequest.getStoreId() != null && !rentalRequest.getStoreId().isBlank()) {
            this.storeId = Long.parseLong(rentalRequest.getStoreId());
            this.storeName = rentalRequest.getStoreName();
            this.storeGrade = rentalRequest.getStoreGrade();
            if (rentalRequest.getStoreHarry() != null && !rentalRequest.getStoreHarry().isBlank()) {
                this.storeHarry = new BigDecimal(rentalRequest.getStoreHarry());
            }
        }
    }

    public void updateStoneCost(int totalStonePurchaseCost, int totalStoneLaborCost, int mainLaborCost, int assistanceLaborCost, int stoneAddLaborCost) {
        this.totalStonePurchaseCost = totalStonePurchaseCost;
        this.totalStoneLaborCost = totalStoneLaborCost;
        this.stoneMainLaborCost = mainLaborCost;
        this.stoneAssistanceLaborCost = assistanceLaborCost;
        this.stoneAddLaborCost = stoneAddLaborCost;
    }

    public void updateOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public void updateStockNote(String stockMainStoneNote, String stockAssistanceStoneNote, String stockNote) {
        this.stockMainStoneNote = stockMainStoneNote;
        this.stockAssistanceStoneNote = stockAssistanceStoneNote;
        this.stockNote = stockNote;
    }

    @PrePersist
    private void onCreate() {
        if (this.flowCode == null) {
            this.stockCode = TsidCreator.getTsid().toLong();
            this.flowCode = this.stockCode;   // 독립 재고 기본값
        }
        if (this.stockCode == null) {
            this.stockCode = this.flowCode;
        }
    }

    public void returnToStock() {
        this.orderStatus = OrderStatus.STOCK;
    }

    public void updateFlowCode() {
        long newFlowCode = TsidCreator.getTsid().toLong();
        this.flowCode = newFlowCode;
        this.stockCode = newFlowCode;
    }

    /**
     * 재고 조사 처리
     */
    public void markAsChecked() {
        this.stockChecked = true;
        this.stockCheckedAt = LocalDateTime.now();
    }

    /**
     * 재고 조사 초기화
     */
    public void resetStockCheck() {
        this.stockChecked = false;
        this.stockCheckedAt = null;
    }
}
