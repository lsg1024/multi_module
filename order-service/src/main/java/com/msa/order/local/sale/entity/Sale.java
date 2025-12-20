package com.msa.order.local.sale.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.msa.common.global.domain.BaseEntity;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import io.hypersistence.utils.hibernate.id.Tsid;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(
        name = "SALE",
        uniqueConstraints = {
                @UniqueConstraint(name = "UK_SALE_CODE", columnNames = {"SALE_CODE"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sale extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_ID")
    private Long saleId;
    @Tsid
    @Column(name = "SALE_CODE")
    private Long saleCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "SALE_STATUS")
    private SaleStatus saleStatus;
    @Column(name = "ACCOUNT_ID", nullable = false)
    private Long accountId;
    @Column(name = "ACCOUNT_NAME", nullable = false)
    private String accountName;
    @Column(name = "ACCOUNT_HARRY", precision = 10, scale = 2)
    private BigDecimal accountHarry;
    @Column(name = "ACCOUNT_GRADE")
    private String accountGrade;
    @Column(name = "ACCOUNT_GOLD_PRICE")
    private Integer accountGoldPrice;

    @OneToMany(mappedBy="sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SaleItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SalePayment> salePayments = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (saleCode == null) {
            this.saleCode = TsidCreator.getTsid().toLong();
        }
        accountGoldPrice = 0;
    }

    @Builder
    public Sale(SaleStatus saleStatus, Long accountId, String accountName, BigDecimal accountHarry, String accountGrade, List<SaleItem> items) {
        this.saleStatus = saleStatus;
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountHarry = accountHarry;
        this.accountGrade = accountGrade;
        this.items = items;
    }

    public void updateAccountGoldPrice(Integer newPrice) {
        if (this.accountGoldPrice != null && this.accountGoldPrice > 0) {
            throw new IllegalArgumentException("금 시세가 이미 설정되어 있어 변경할 수 없습니다.");
        }
        this.accountGoldPrice = newPrice;
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
