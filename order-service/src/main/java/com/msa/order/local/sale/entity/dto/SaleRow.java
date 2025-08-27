package com.msa.order.local.sale.entity.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SaleRow(
    OffsetDateTime createAt,
    String  saleType,
    String  name,
    Long    saleCode,
    Long    flowCode,
    String  productName,
    String  materialName,
    String  colorName,
    String  note,
    Integer mainStoneQuantity,
    Integer assistanceQuantity,
    BigDecimal productWeight,
    BigDecimal stoneWeight,
    Integer mainProductCost,
    Integer addProductCost,
    Integer mainStoneCost,
    Integer assistanceStoneCost,
    Integer totalPurchaseCost
) {}