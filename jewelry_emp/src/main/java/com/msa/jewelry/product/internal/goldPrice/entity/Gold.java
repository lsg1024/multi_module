package com.msa.jewelry.product.internal.goldPrice.entity;

import com.msa.common.global.domain.BaseTimeEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "GOLD")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "금시세 엔티티 — 날짜별 금 시세(돈/그램 등 단가) 기록")
public class Gold extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "GOLD_ID")
    @Schema(description = "금시세 PK", example = "100")
    private Long goldId;

    @Column(name = "GOLD_PRICE")
    @Schema(description = "금 단가 (원)", example = "350000")
    private Integer goldPrice;

    @Builder
    public Gold(Long goldId, Integer goldPrice) {
        this.goldId = goldId;
        this.goldPrice = goldPrice;
    }
}
