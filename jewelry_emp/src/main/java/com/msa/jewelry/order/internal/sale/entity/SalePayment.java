package com.msa.jewelry.order.internal.sale.entity;

import com.github.f4b6a3.tsid.TsidCreator;
import com.msa.common.global.domain.BaseEntity;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;

/**
 * 판매 결제 상세 엔티티.
 *
 * *하나의 {@link Sale} 세션에 속하는 개별 결제(미수금 납부) 레코드를 나타낸다.
 * 결제 금액과 순금 중량은 미수금 차감을 의미하므로 음수(negate)로 저장된다.
 *
 * *{@code eventId}는 멱등성 키로 {@code UK_PAYMENT_IDEVT} 유니크 제약이 걸려 있어
 * 동일한 요청이 두 번 처리되는 것을 DB 레벨에서 방지한다.
 *
 * *삭제는 소프트 딜리트({@code PAYMENT_DELETED = TRUE})로 처리되며,
 * {@code @SQLRestriction}으로 삭제된 레코드는 자동 필터링된다.
 */
@Getter
@Entity
@Table(
        name = "SALE_PAYMENT",
        indexes = { @Index(name = "IX_PAYMENT_SALE", columnList = "SALE_ID, create_date") },
        uniqueConstraints = { @UniqueConstraint(name = "UK_PAYMENT_IDEVT", columnNames = {"EVENT_ID"})}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE SALE_PAYMENT SET PAYMENT_DELETED = TRUE WHERE SALE_PAYMENT_ID = ?")
@SQLRestriction("PAYMENT_DELETED = FALSE")
@Schema(description = "판매 결제 상세 엔티티 — Sale 한 건의 개별 결제(미수금 납부) 레코드. 결제 금액/순금은 음수로 저장(미수금 차감).")
public class SalePayment extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SALE_PAYMENT_ID")
    @Schema(description = "결제 PK", example = "9001")
    private Long salePaymentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SALE_ID", nullable = false)
    @Schema(description = "소속 판매 세션 (Sale)")
    private Sale sale;

    /** TSID 기반 결제 흐름 코드. 판매 이력 추적 및 취소 시 조회 키로 사용된다. */
    @Column(name = "FLOW_CODE", nullable = false)
    @Schema(description = "TSID 기반 결제 흐름 코드 — 결제 취소/이력 추적에 사용", example = "445823472384938240")
    private Long flowCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "SALE_STATUS")
    @Schema(description = "결제 유형 (PAYMENT/DISCOUNT/WG/PAYMENT_TO_BANK 등)", example = "PAYMENT")
    private SaleStatus saleStatus;

    @Column(name="CASH_AMOUNT")
    @Schema(description = "현금 결제 금액 (음수면 미수금 차감)", example = "100000")
    private Integer cashAmount = 0;

    @Column(name = "GOLD_MATERIAL")
    @Schema(description = "납입 금 재질 코드 (예: 18K, 14K)", example = "18K")
    private String material;

    /** 해리 계수가 적용된 순금 중량(g). 결제 시 음수로 저장되어 미수금에서 차감된다. */
    @Column(name="PURE_GOLD_WEIGHT", precision=18, scale=3)
    @Schema(description = "해리 계수가 적용된 순금 중량(g). 결제 시 음수.", example = "-3.250")
    private BigDecimal pureGoldWeight = BigDecimal.ZERO;

    @Column(name="GOLD_WEIGHT", precision=18, scale=3)
    @Schema(description = "납입 금 중량(g) — 해리 적용 전 원본 무게", example = "-3.500")
    private BigDecimal goldWeight = BigDecimal.ZERO;

    @Column(name = "PAYMENT_NOTE")
    @Schema(description = "결제 비고", example = "현금 영수증 발행")
    private String paymentNote;

    /** 멱등성 키 — DB 유니크 제약(UK_PAYMENT_IDEVT)으로 중복 결제를 방지한다. */
    @Column(name="EVENT_ID", nullable=false)
    @Schema(description = "멱등성 키 — 중복 결제 방지 (UK_PAYMENT_IDEVT)", example = "evt_abc123")
    private String eventId;

    @Column(name = "PAYMENT_DELETED", nullable = false)
    @Schema(description = "소프트 삭제 플래그", example = "false")
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
