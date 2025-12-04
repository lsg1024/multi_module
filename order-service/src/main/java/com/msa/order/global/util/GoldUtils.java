package com.msa.order.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GoldUtils {
    private static final int DEFAULT_SCALE = 3;
    private static final RoundingMode DEFAULT_ROUNDING = RoundingMode.HALF_UP;
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

    public static BigDecimal calculatePureGoldWeight(BigDecimal totalWeight, String material) {
        if (totalWeight == null || totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal purity = getGoldPurity(material);
        if (purity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // 계산: 순금무게 = 총무게 * 순도
        return totalWeight.multiply(purity).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal calculatePureGoldWeightAndHarry(BigDecimal totalWeight, String material, BigDecimal harry) {
        if (totalWeight == null || totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal purity = getGoldPurity(material);
        if (purity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalWeight.multiply(purity).multiply(harry).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal calculatePureGoldWeight(String weightStr, String material) {
        return calculatePureGoldWeight(parseBigDecimal(weightStr), material);
    }

    public static BigDecimal calculatePureGoldWeightWithHarry(String weight, String material, BigDecimal harry) {
        BigDecimal totalWeight = parseBigDecimal(weight);
        if (totalWeight == null || totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        if (harry == null) {
            harry = BigDecimal.ONE;
        }

        BigDecimal purity = getGoldPurity(material);
        if (purity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(1.1);
        }

        BigDecimal result = totalWeight.multiply(purity).multiply(harry);

        return result.setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal calculateAddHarryToPureGoldWeight(BigDecimal pureGoldWeight, BigDecimal harry) {
        if (pureGoldWeight == null || harry == null) return BigDecimal.ZERO;
        return pureGoldWeight.multiply(harry).setScale(DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static BigDecimal calculateMinusHarryToPureGoldWeight(BigDecimal pureGoldWeight, BigDecimal harry) {
        if (pureGoldWeight == null || harry == null || harry.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return pureGoldWeight.divide(harry, DEFAULT_SCALE, DEFAULT_ROUNDING);
    }

    public static Map<String, BigDecimal> getPurityMap() {
        return Map.copyOf(PURITY_MAP);
    }

}
