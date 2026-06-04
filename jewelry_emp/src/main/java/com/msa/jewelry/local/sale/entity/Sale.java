package com.msa.jewelry.local.sale.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.msa.common.global.domain.BaseEntity;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import io.hypersistence.utils.hibernate.id.Tsid;
import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "판매 세션(주문장) 엔티티 — 한 거래처와의 한 차례 판매를 묶는 단위. 여러 SaleItem 과 SalePayment 를 포함.")
public class Sale extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_ID")
    @Schema(description = "판매 세션 PK", example = "7001")
    private Long saleId;
    @Tsid
    @Column(name = "SALE_CODE")
    @Schema(description = "TSID 기반 판매 세션 고유 코드", example = "445823472384938240")
    private Long saleCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "SALE_STATUS")
    @Schema(description = "판매 세션 상태 (SALE/PURCHASE/RETURN/PAYMENT/DISCOUNT/WG/PAYMENT_TO_BANK 등)", example = "SALE")
    private SaleStatus saleStatus;
    @Column(name = "ACCOUNT_ID", nullable = false)
    @Schema(description = "거래처 ID (account 모듈 Account FK)", example = "10")
    private Long accountId;
    @Column(name = "ACCOUNT_NAME", nullable = false)
    @Schema(description = "거래처 이름 (스냅샷)", example = "홍길동 보석상")
    private String accountName;
    @Column(name = "ACCOUNT_HARRY", precision = 10, scale = 2)
    @Schema(description = "거래 당시 거래처 수수료(허리) 스냅샷", example = "1.50")
    private BigDecimal accountHarry;
    @Column(name = "ACCOUNT_GRADE")
    @Schema(description = "거래 당시 거래처 등급 스냅샷", example = "A")
    private String accountGrade;
    /** 이 판매 세션에 적용된 금 시세(원/g). 최초 설정 후 변경 불가. */
    @Column(name = "ACCOUNT_GOLD_PRICE")
    @Schema(description = "이 판매 세션에 적용된 금 시세(원/g). 최초 설정 후 변경 불가.", example = "85000")
    private Integer accountGoldPrice;

    /** 사용자 표시용 주문장 코드 (형식: YYMMDDNN, 예: 2604010001). */
    @Column(name = "DISPLAY_CODE", length = 10)
    @Schema(description = "사용자 표시용 주문장 코드 (YYMMDDNN)", example = "2605160001")
    private String displayCode;

    @OneToMany(mappedBy="sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "판매 라인 목록 (장바구니 안의 각 재고)")
    private List<SaleItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "판매 결제 내역 목록")
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
