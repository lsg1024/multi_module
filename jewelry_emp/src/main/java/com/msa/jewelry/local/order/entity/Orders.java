package com.msa.jewelry.local.order.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.msa.common.global.domain.BaseEntity;
import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.order.entity.order_enum.ProductStatus;
import com.msa.jewelry.local.priority.entity.Priority;
import io.hypersistence.utils.hibernate.id.Tsid;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static jakarta.persistence.CascadeType.*;

@Getter
@Table(name = "ORDERS")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE ORDERS SET ORDER_DELETED = TRUE WHERE ORDER_ID = ?")
@Schema(description = "주문 엔티티 — 매장(store)이 공장(factory)에 발주하는 제품 주문. flowCode 로 주문/재고/판매/이력을 묶는다.")
public class Orders extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_ID")
    @Schema(description = "주문 PK", example = "12345")
    private Long orderId;
    @Tsid @Column(name = "FLOW_CODE")
    @Schema(description = "TSID 기반 전역 고유 흐름 코드 — 주문·재고·판매·이력 공통 추적 키", example = "445823472384938240")
    private Long flowCode;
    @Column(name = "STORE_ID")
    @Schema(description = "매장(거래처) ID. account 모듈의 Store FK. 이름은 storeService 로 조회한다.", example = "10")
    private Long storeId;
    @Column(name = "STORE_GRADE")
    @Schema(description = "거래 당시 거래처 등급 (스냅샷, P4 유지)", example = "A")
    private String storeGrade;
    @Column(name = "STORE_HARRY", precision = 10, scale = 2)
    @Schema(description = "거래 당시 거래처 수수료(허리) (스냅샷, P4 유지)", example = "1.50")
    private BigDecimal storeHarry;
    @Column(name = "FACTORY_ID")
    @Schema(description = "제조사 ID. account 모듈의 Factory FK. 이름은 factoryService 로 조회한다.", example = "5")
    private Long factoryId;
    @Column(name = "FACTORY_HARRY", precision = 10, scale = 2)
    @Schema(description = "거래 당시 제조사 수수료(허리) (스냅샷, P4 유지)", example = "1.20")
    private BigDecimal factoryHarry;
    @Column(name = "ORDER_NOTE")
    @Schema(description = "주문 비고", example = "포장 정중하게 부탁드립니다")
    private String orderNote;
    @Column(name = "CREATE_AT")
    @Schema(description = "주문 접수일 (사용자 입력 도메인 컬럼). BaseEntity 의 createDate(audit) 와 별개.", example = "2026-05-16T14:30:00")
    private LocalDateTime createAt;
    @Column(name = "SHIPPING_AT")
    @Schema(description = "출고 예정일", example = "2026-05-20T10:00:00")
    private LocalDateTime shippingAt;
    @Column(name = "ORDER_DELETED", nullable = false)
    @Schema(description = "소프트 삭제 플래그 — TRUE 이면 사용자에게는 보이지 않음", example = "false")
    private boolean orderDeleted = false;

    @OneToOne(mappedBy = "order", cascade = {PERSIST, MERGE})
    @Schema(description = "주문 상품 정보 (1:1)")
    private OrderProduct orderProduct;

    @OneToMany(mappedBy = "order", cascade = {PERSIST, MERGE, REMOVE}, orphanRemoval = true)
    @Schema(description = "주문에 포함된 스톤 목록")
    private List<OrderStone> orderStones = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRIORITY_ID")
    @Schema(description = "출고 우선순위 (예: 일반/긴급)")
    private Priority priority;

    @Column(name = "PRODUCT_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "상품 진행 상태 (WAITING, RECEIPT 등)")
    private ProductStatus productStatus;

    @Column(name = "ORDER_STATUS", nullable = false)
    @Enumerated(EnumType.STRING)
    @Schema(description = "주문 비즈니스 처리 상태 (WAIT/STOCK/RENTAL/SALE/RETURN/DELETED 등)")
    private OrderStatus orderStatus;

    @Builder
    public Orders(Long orderId, Long flowCode, Long storeId, String storeGrade, BigDecimal storeHarry,
                  Long factoryId, BigDecimal factoryHarry, String orderNote,
                  LocalDateTime createAt, LocalDateTime shippingAt,
                  ProductStatus productStatus, OrderStatus orderStatus) {
        this.orderId = orderId;
        this.flowCode = flowCode;
        this.storeId = storeId;
        this.storeGrade = storeGrade;
        this.storeHarry = storeHarry;
        this.factoryId = factoryId;
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

    public void updateStore(Long storeId, String storeGrade, BigDecimal storeHarry) {
        if (storeId != null) {
            this.storeId = storeId;
        }
        if (storeGrade != null && !storeGrade.isEmpty()) {
            this.storeGrade = storeGrade;
        }
        if (storeHarry != null) {
            this.storeHarry = storeHarry;
        }
    }

    public void updateFactory(Long factoryId, BigDecimal factoryHarry) {
        if (factoryId != null) {
            this.factoryId = factoryId;
        }
        if (factoryHarry != null) {
            this.factoryHarry = factoryHarry;
        }
    }

    public void updateOrderNote(String orderNote) {
        if (orderNote != null) {
            this.orderNote = orderNote;
        }
    }

    public void updateCreateDate(LocalDateTime createAt) {
        if (createAt != null) {
            this.createAt = createAt;
        }
    }

    public void updateShippingDate(LocalDateTime shippingAt) {
        if (shippingAt != null) {
            this.shippingAt = shippingAt;
        }
    }

    public void deletedOrder(LocalDateTime orderExpectDate) {
        this.shippingAt = orderExpectDate;
        this.orderDeleted = true;
    }

    public void updateOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

}
