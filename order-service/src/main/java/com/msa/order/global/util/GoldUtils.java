package com.msa.order.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoldUtils {
    private static final int DEFAULT_SCALE = 3;
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal DON_WEIGHT_GRAMS = new BigDecimal("3.75");
    private static final Map<String, BigDecimal> PURITY_MAP = Map.of(
            "14K", new BigDecimal("0.585"),
            "18K", new BigDecimal("0.750")
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

    public static BigDecimal calculatePureGoldWeight(BigDecimal totalWeight, String material) {
        if (totalWeight == null || totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal purity = getGoldPurity(material);
        if (purity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO; // 14K, 18K가 아니면 0 반환
        }

        // 계산: 순금무게 = 총무게 * 순도
        BigDecimal pureGoldWeight = totalWeight.multiply(purity);

        // TS의 toFixed(2)와 동일하게 소수점 2자리로 반올림
        return pureGoldWeight.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal calculatePureGoldWeight(String weightStr, String material) {
        return calculatePureGoldWeight(parseBigDecimal(weightStr), material);
    }

    public static String calculateGoldContent(String weightStr, String material) {
        BigDecimal pureGoldWeight = calculatePureGoldWeight(weightStr, material);

        if (pureGoldWeight.compareTo(BigDecimal.ZERO) == 0) {
            return "";
        }

        return String.format("(순금: %.2fg)", pureGoldWeight);
    }

    public static BigDecimal getGoldTransferWeight(BigDecimal totalWeight) {
        if (totalWeight == null || totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalWeight.divide(DON_WEIGHT_GRAMS, DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal getGoldTransferWeight(String weightStr) {
        return getGoldTransferWeight(parseBigDecimal(weightStr));
    }

}
