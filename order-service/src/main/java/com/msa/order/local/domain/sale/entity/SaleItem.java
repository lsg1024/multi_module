package com.msa.order.local.domain.sale.entity;

import com.msa.order.local.domain.stock.entity.domain.Stock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "SALE_ITEM")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="SALE_ID")
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="STOCK_ID")
    private Stock stock;

    public void setSale(Sale sale) {
        this.sale = sale;
    }
}
