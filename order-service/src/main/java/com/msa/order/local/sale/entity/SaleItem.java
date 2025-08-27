package com.msa.order.local.sale.entity;

import com.msa.order.local.stock.entity.Stock;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneId;

@Getter
@Entity
@Table(
        name = "SALE_ITEM",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_SALE_ITEM_STOCK", columnNames = {"STOCK_ID"}) // 같은 재고 중복 출고 방지
        }
)

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SaleItem {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_ITEM_ID")
    private Long saleItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="SALE_ID")
    private Sale sale;

    @Column(name = "SALE_CODE", nullable = false)
    private Long saleCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="STOCK_ID")
    private Stock stock;

    @Column(name = "FLOW_CODE", nullable = false)
    private Long flowCode;

    @Column(name="CREATED_BY")
    private String createdBy;

    @Column(name = "CREATE_AT", nullable = false, updatable = false)
    private OffsetDateTime createAt;

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public void setStock(Stock stock) {
        this.stock = stock;
    }

    @PrePersist
    void onCreate() {
        if (createAt == null) {
            createAt = OffsetDateTime.now(KST);
        }
    }

    @Builder
    public SaleItem(String createdBy, Long saleCode, Long flowCode) {
        this.createdBy = createdBy;
        this.saleCode = saleCode;
        this.flowCode = flowCode;
    }

}
