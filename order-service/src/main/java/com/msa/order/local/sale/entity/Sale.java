package com.msa.order.local.sale.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.msa.common.global.domain.BaseEntity;
import com.msa.order.local.sale.sale_enum.SaleStatus;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "SALE",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_SALE_STORE_DATE", columnNames = {"STORE_ID","SALE_DATE"}),
                @UniqueConstraint(name = "UK_SALE_CODE",       columnNames = {"SALE_CODE"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sale extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_ID")
    private Long saleId;
    @Column(name = "SALE_CODE") @Tsid
    private Long saleCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "SALE_STATUS")
    private SaleStatus saleStatus;

    @Column(name = "STORE_ID", nullable = false)
    private Long storeId;

    @Column(name = "STORE_NAME", nullable = false)
    private String storeName;

    @OneToMany(mappedBy="sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalePayment> salePayments = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (saleCode == null) {
            this.saleCode = TsidCreator.getTsid().toLong();
        }
    }

    @Builder
    public Sale(SaleStatus saleStatus, Long storeId, String storeName, List<SaleItem> items) {
        this.saleStatus = saleStatus;
        this.storeId = storeId;
        this.storeName = storeName;
        this.items = items;
    }

    public void addItem(SaleItem item) {
        this.items.add(item);
        item.setSale(this);
    }

    public void addPayment(SalePayment salePayment) {
        this.salePayments.add(salePayment);
        salePayment.setSale(this);
    }
}
