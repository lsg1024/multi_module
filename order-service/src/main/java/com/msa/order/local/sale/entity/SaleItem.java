package com.msa.order.local.sale.entity;

import com.msa.common.global.domain.BaseEntity;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.order.local.stock.entity.Stock;
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
public class SaleItem extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_ITEM_ID")
    private Long saleItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="SALE_ID")
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="STOCK_ID")
    private Stock stock;

    @Column(name = "FLOW_CODE", nullable = false)
    private Long flowCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "ITEM_STATUS")
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
