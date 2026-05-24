package com.msa.jewelry.order.internal.sale.entity;

import com.msa.common.global.domain.BaseEntity;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.jewelry.order.internal.stock.entity.Stock;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "SALE_ITEM",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_SALE_ITEM_STOCK", columnNames = {"STOCK_ID"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "판매 라인 엔티티 — Sale 안의 한 라인. 재고(Stock) 한 건과 1:1 매핑.")
public class SaleItem extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_ITEM_ID")
    @Schema(description = "판매 라인 PK", example = "8001")
    private Long saleItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="SALE_ID")
    @Schema(description = "소속 판매 세션 (Sale)")
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="STOCK_ID")
    @Schema(description = "판매되는 재고 (Stock)")
    private Stock stock;

    @Column(name = "FLOW_CODE", nullable = false)
    @Schema(description = "전역 흐름 코드 — 재고/주문 추적과 연결", example = "445823472384938240")
    private Long flowCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "ITEM_STATUS")
    @Schema(description = "라인 상태 (SALE/RETURN 등)", example = "SALE")
    private SaleStatus itemStatus = SaleStatus.SALE;

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    public void markAsReturn() {
        if (this.itemStatus == SaleStatus.RETURN) {
            throw new IllegalArgumentException("이미 반품 처리된 상품입니다.");
        }
        this.itemStatus = SaleStatus.RETURN;
    }

    public boolean isReturned() {
        return this.itemStatus == SaleStatus.RETURN;
    }

    @Builder
    public SaleItem(Long flowCode) {
        this.flowCode = flowCode;
    }

}
