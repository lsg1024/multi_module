package com.msa.order.local.sale.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.msa.order.local.sale.sale_enum.SaleStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Getter
@Entity
@Table(
        name = "SALE_PAYMENT",
        indexes = { @Index(name = "IX_PAYMENT_SALE", columnList = "SALE_ID, CREATED_AT") },
        uniqueConstraints = { @UniqueConstraint(name = "UK_PAYMENT_IDEMP", columnNames = {"IDEMP_KEY"})}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE SALE_PAYMENT SET PAYMENT_DELETED = TRUE, DELETED_AT = CURRENT_TIMESTAMP WHERE SALE_PAYMENT_ID = ?")
@SQLRestriction("PAYMENT_DELETED = FALSE")
public class SalePayment {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_PAYMENT_ID")
    private Long salePaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SALE_ID", nullable = false)
    private Sale sale;

    @Column(name = "SALE_CODE", nullable = false)
    private Long saleCode;

    @Column(name = "FLOW_CODE", nullable = false)
    private Long flowCode;

    @Column(name = "STORE_ID", nullable = false, updatable = false)
    private Long storeId;

    @Column(name = "STORE_NAME", nullable = false, updatable = false)
    private String storeName;

    @Enumerated(EnumType.STRING)
    @Column(name = "SALE_STATUS")
    private SaleStatus saleStatus;

    @Column(name="CASH_AMOUNT")
    private Long cashAmount = 0L;

    @Column(name = "GOLD_MATERIAL")
    private String material;

    @Column(name="GOLD_WEIGHT", precision=18, scale=3)
    private BigDecimal goldWeight = BigDecimal.ZERO;

    @Column(name = "PAYMENT_NOTE")
    private String paymentNote;

    @Column(name="CREATED_AT", nullable=false, updatable=false)
    private OffsetDateTime createdAt;

    @Column(name="CREATED_BY")
    private String createdBy;

    @Column(name="IDEMP_KEY", nullable=false)
    private String idempotencyKey;

    @Column(name = "PAYMENT_DELETED", nullable = false)
    private boolean deleted = false;

    @Column(name = "DELETED_AT")
    private OffsetDateTime deletedAt;
    public void setSale(Sale sale) {
        this.sale = sale;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now(KST);
        if (cashAmount == null)  cashAmount  = 0L;
        if (goldWeight == null)  goldWeight  = BigDecimal.ZERO;
        if (this.sale != null) {
            this.saleCode  = sale.getSaleCode();
            this.storeId   = sale.getStoreId();
            this.storeName = sale.getStoreName();
            this.flowCode = TsidCreator.getTsid().toLong();
        }
    }

    public static SalePayment payment(String material, String by, String idemp, String note, Long cash, BigDecimal gold) {
        SalePayment s = base(material, SaleStatus.PAYMENT, by, idemp, note);
        s.cashAmount = nz(cash); s.goldWeight = nz(gold); return s;
    }

    public static SalePayment paymentBank(String material, String by, String idemp, String note, Long cash) {
        SalePayment s = base(material, SaleStatus.PAYMENT_TO_BANK, by, idemp, note);
        s.cashAmount = nz(cash); return s;
    }
    public static SalePayment discount(String material, String by, String idemp, String note, Long cashDisc, BigDecimal goldDisc) {
        SalePayment s = base(material, SaleStatus.DISCOUNT, by, idemp, note);
        s.cashAmount = nz(cashDisc);
        s.goldWeight = nz(goldDisc); return s;
    }

    public static SalePayment wg(String material, String by, String idemp, String note, Long cash, BigDecimal gold) {
        SalePayment s = base(material, SaleStatus.PAYMENT, by, idemp, note);
        s.cashAmount = nz(cash);
        s.goldWeight = nz(gold);
        return s;
    }

    private static SalePayment base(String material, SaleStatus type, String by, String idemp, String note) {
        SalePayment s = new SalePayment();
        s.material = material;
        s.saleStatus = type;
        s.createdBy = by;
        s.idempotencyKey = idemp;
        s.paymentNote = note;
        return s;
    }
    private static Long nz(Long v){ return v==null? 0L : v; }
    private static BigDecimal nz(BigDecimal v){ return v==null? BigDecimal.ZERO : v; }
}
