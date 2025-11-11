package com.msa.order.local.sale.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.msa.common.global.domain.BaseEntity;
import com.msa.order.local.sale.sale_enum.SaleStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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

    @Column(name = "DELETED_AT")
    private OffsetDateTime deletedAt;
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

    public static SalePayment payment(String material, String eventId, String note, Integer cash, BigDecimal pureGold, BigDecimal gold) {
        SalePayment s = base(material, SaleStatus.PAYMENT, eventId, note);
        s.cashAmount = nz(cash);
        s.pureGoldWeight = nz(pureGold);
        s.goldWeight = nz(gold);
        return s;
    }

    public static SalePayment paymentBank(String material, String eventId, String note, Integer cash) {
        SalePayment s = base(material, SaleStatus.PAYMENT_TO_BANK, eventId, note);
        s.cashAmount = nz(cash); return s;
    }
    public static SalePayment discount(String material, String eventId, String note, Integer cashDisc, BigDecimal pureGold, BigDecimal goldDisc) {
        SalePayment s = base(material, SaleStatus.DISCOUNT, eventId, note);
        s.cashAmount = nz(cashDisc);
        s.pureGoldWeight = nz(pureGold);
        s.goldWeight = nz(goldDisc);
        return s;
    }

    public static SalePayment wg(String material, String idemp, String note, Integer cash, BigDecimal pureGold, BigDecimal gold) {
        SalePayment s = base(material, SaleStatus.PAYMENT, idemp, note);
        s.cashAmount = nz(cash);
        s.pureGoldWeight = nz(pureGold);
        s.goldWeight = nz(gold);
        return s;
    }

    private static SalePayment base(String material, SaleStatus type, String eventId, String note) {
        SalePayment s = new SalePayment();
        s.material = material;
        s.saleStatus = type;
        s.eventId = eventId;
        s.paymentNote = note;
        return s;
    }
    private static Integer nz(Integer v){ return v==null? 0 : v; }
    private static BigDecimal nz(BigDecimal v){ return v==null? BigDecimal.ZERO : v; }
}
