package com.msa.order.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * 금 순중량 계산 유틸리티.
 *
 * *소재별 순도(PURITY_MAP: 14K=0.585, 18K=0.75, 24K=1.0)와 해리(harry) 계수를 사용하여
 * 총 중량에서 순금 중량을 산출하거나, WG(금 수거) 거래에서 현금 금액과 시세로 금 중량을 역산한다.
 *
 * *인스턴스화 불가 유틸리티 클래스이며 모든 메서드는 정적(static)이다.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoldUtils {
    private static final int DEFAULT_SCALE = 3;
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;
    /** 소재 코드 → 순도 매핑 (대문자 기준). 등록되지 않은 소재는 순도 0으로 처리된다. */
    private static final Map<String, BigDecimal> PURITY_MAP = Map.of(
            "14K", new BigDecimal("0.585"),
            "18K", new BigDecimal("0.750"),
            "24K", new BigDecimal("1")
    );

    private static BigDecimal parseBigDecimal(String weightStr) {
        if (weightStr == null || weightStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(weightStr);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    public static BigDecimal getGoldPurity(String material) {
        if (material == null) {
            return BigDecimal.ZERO;
        }
        return PURITY_MAP.getOrDefault(material.toUpperCase(), BigDecimal.ZERO);
    }

    public static BigDecimal calculatePureGoldWeightAndHarry(BigDecimal totalWeight, String material, BigDecimal harry) {
        if (totalWeight == null || totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        if (harry == null) {
            harry = BigDecimal.ONE;
        }

        if ("24K".equalsIgnoreCase(material)) {
            harry = BigDecimal.ONE;
        }

        BigDecimal purity = getGoldPurity(material);
        if (purity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(1.1);
        }

        BigDecimal result = totalWeight.multiply(purity).multiply(harry);

        return result.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * 문자열 중량을 받아 순금 중량을 계산한다.
     *
     * *계산식: {@code 순금 중량 = weight × purity × harry}
     *
     *   - 24K 소재는 해리(harry) 값을 무시하고 1.0으로 고정한다.
     *   - 등록되지 않은 소재(순도 0)는 오류 식별을 위해 {@code 1.1}을 반환한다.
     *   - 중량이 {@code null}이거나 0이면 {@link BigDecimal#ZERO}를 반환한다.
     *   - harry가 {@code null}이면 1.0으로 처리한다.
     * 
     *
     * @param weight   총 중량 문자열 (파싱 실패 시 0 처리)
     * @param material 소재 코드 (예: "14K", "18K", "24K", 대소문자 무관)
     * @param harry    해리 계수 (매장/공장별 적용 배율)
     * @return 소수점 3자리 반올림된 순금 중량
     */
    public static BigDecimal calculatePureGoldWeightWithHarry(String weight, String material, BigDecimal harry) {
        BigDecimal totalWeight = parseBigDecimal(weight);
        if (totalWeight == null || totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (harry == null) {
            harry = BigDecimal.ONE;
        }

        if ("24K".equalsIgnoreCase(material)) {
            harry = BigDecimal.ONE;
        }

        BigDecimal purity = getGoldPurity(material);
        if (purity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(1.1);
        }

        BigDecimal result = totalWeight.multiply(purity).multiply(harry);

        return result.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    /**
     * WG(금 수거) 거래용 — 현금 금액과 시세로 금 중량을 역산한다.
     *
     * *계산식: {@code 금 중량 = cashAmount ÷ marketPrice}
     * *{@code cashAmount} 또는 {@code marketPrice}가 {@code null}이거나
     * {@code marketPrice}가 0이면 {@link BigDecimal#ZERO}를 반환한다.
     *
     * @param cashAmount  현금 결제 금액 (원 단위)
     * @param marketPrice 금 시세 (원/g 단위)
     * @return 소수점 3자리 반올림된 금 중량(g)
     */
    public static BigDecimal calculateWeightFromPrice(Integer cashAmount, Integer marketPrice) {
        if (cashAmount == null || marketPrice == null || marketPrice == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal amount = new BigDecimal(cashAmount);
        BigDecimal price = new BigDecimal(marketPrice);

        return amount.divide(price, DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

}
