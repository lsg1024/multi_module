package com.msa.order.local.sale.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.msa.common.global.domain.BaseEntity;
import com.msa.order.local.sale.sale_enum.SaleStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

@Getter
@Entity
@Table(
        name = "SALE_PAYMENT",
        indexes = { @Index(name = "IX_PAYMENT_SALE", columnList = "SALE_ID, create_date") },
        uniqueConstraints = { @UniqueConstraint(name = "UK_PAYMENT_IDEVT", columnNames = {"EVENT_ID"})}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE SALE_PAYMENT SET PAYMENT_DELETED = TRUE, DELETED_AT = CURRENT_TIMESTAMP WHERE SALE_PAYMENT_ID = ?")
@SQLRestriction("PAYMENT_DELETED = FALSE")
public class SalePayment extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_PAYMENT_ID")
    private Long salePaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SALE_ID", nullable = false)
    private Sale sale;

    @Column(name = "FLOW_CODE", nullable = false)
    private Long flowCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "SALE_STATUS")
    private SaleStatus saleStatus;

    @Column(name="CASH_AMOUNT")
    private Integer cashAmount = 0;

    @Column(name = "GOLD_MATERIAL")
    private String material;

    @Column(name="PURE_GOLD_WEIGHT", precision=18, scale=3)
    private BigDecimal pureGoldWeight = BigDecimal.ZERO;

    @Column(name="GOLD_WEIGHT", precision=18, scale=3)
    private BigDecimal goldWeight = BigDecimal.ZERO;

    @Column(name = "PAYMENT_NOTE")
    private String paymentNote;

    @Column(name="EVENT_ID", nullable=false)
    private String eventId;

    @Column(name = "PAYMENT_DELETED", nullable = false)
    private boolean deleted = false;

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    @PrePersist
    void onCreate() {
        if (cashAmount == null)  cashAmount  = 0;
        if (pureGoldWeight == null) pureGoldWeight = BigDecimal.ZERO;
        if (goldWeight == null) goldWeight  = BigDecimal.ZERO;
        if (this.sale != null) {
            this.flowCode = TsidCreator.getTsid().toLong();
        }
    }

    @Builder
    public SalePayment(SaleStatus saleStatus, Integer cashAmount, String material, BigDecimal pureGoldWeight, BigDecimal goldWeight, String paymentNote, String eventId) {
        this.saleStatus = saleStatus;
        this.cashAmount = cashAmount;
        this.material = material;
        this.pureGoldWeight = pureGoldWeight;
        this.goldWeight = goldWeight;
        this.paymentNote = paymentNote;
        this.eventId = eventId;
    }

    public void updateEventId(String eventId) {
        this.eventId = eventId;
    }
}
