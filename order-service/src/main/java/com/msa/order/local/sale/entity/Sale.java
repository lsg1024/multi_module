package com.msa.order.local.sale.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.msa.order.local.sale.sale_enum.SaleStatus;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
public class Sale {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_ID")
    private Long saleId;
    @Column(name = "SALE_CODE") @Tsid
    private Long saleCode;

    @Enumerated(EnumType.STRING)
    private SaleStatus saleStatus;
    @Column(name = "CREATE_AT", nullable = false, updatable = false)
    private OffsetDateTime createAt;

    @Column(name = "SALE_DATE", nullable = false, updatable = false)        // ★ KST 기준 일자
    private LocalDate saleDate;
    @Column(name = "STORE_ID", nullable = false)
    private Long storeId;

    @Column(name = "STORE_NAME", nullable = false)
    private String storeName;

    @OneToMany(mappedBy="sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createAt == null) {
            createAt = OffsetDateTime.now(KST);
        }
        if (saleDate == null) {
            saleDate = createAt.atZoneSameInstant(KST).toLocalDate();
        }
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
}
