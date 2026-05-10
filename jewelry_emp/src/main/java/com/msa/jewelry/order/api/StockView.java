package com.msa.jewelry.order.api;

import java.math.BigDecimal;

public record StockView(
        Long flowCode,
        String orderStatus,
        String productName,
        String materialName,
        String colorName,
        BigDecimal goldWeight,
        BigDecimal stoneWeight
) {
}
