package com.msa.order.local.sale.entity;

import com.msa.order.local.stock.entity.Stock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "SALE_ITEM",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_SALE_ITEM_STOCK", columnNames = {"STOCK_ID"}) // 같은 재고 중복 출고 방지
        }
)

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_ITEM_ID")
    private Long saleItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="SALE_ID")
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="STOCK_ID")
    private Stock stock;

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    @Builder
    public SaleItem(Sale sale, Stock stock) {
        this.sale = sale;
        this.stock = stock;
    }
}
