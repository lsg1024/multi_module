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

/**
 * 판매 집합 엔티티.
 *
 * *하나의 판매 세션(주문장)을 나타내며, 여러 {@link SaleItem}과 {@link SalePayment}를 포함한다.
 * {@code saleCode}는 TSID 기반 전역 고유 식별자이며 유니크 제약이 적용된다.
 *
 * *{@code accountGoldPrice}는 해당 판매 세션의 금 시세로,
 * 한 번 설정된 후에는 변경이 불가능하다({@link #updateAccountGoldPrice} 참조).
 *
 * *{@code displayCode}는 사용자에게 표시되는 날짜+순번 형식의 주문장 코드이다
 * (예: {@code YYMMDDNN}).
 */
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
    /** TSID 기반 판매 세션 고유 코드. OutboxEvent의 메시지 키로도 활용된다. */
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
    /** 이 판매 세션에 적용된 금 시세(원/g). 최초 설정 후 변경 불가. */
    @Column(name = "ACCOUNT_GOLD_PRICE")
    private Integer accountGoldPrice;

    /** 사용자 표시용 주문장 코드 (형식: YYMMDDNN, 예: 2604010001). */
    @Column(name = "DISPLAY_CODE", length = 10)
    private String displayCode;

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
    public Sale(SaleStatus saleStatus, Long accountId, String accountName, BigDecimal accountHarry, String accountGrade, String displayCode, List<SaleItem> items) {
        this.saleStatus = saleStatus;
        this.accountId = accountId;
        this.accountName = accountName;
        this.accountHarry = accountHarry;
        this.accountGrade = accountGrade;
        this.displayCode = displayCode;
        this.items = items;
    }

    public void updateAccountGoldPrice(Integer newPrice) {
        if (!this.accountGoldPrice.equals(newPrice) && this.accountGoldPrice > 0) {
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

    public boolean isAccountGoldPrice() {
        return this.accountGoldPrice != null;
    }
}
