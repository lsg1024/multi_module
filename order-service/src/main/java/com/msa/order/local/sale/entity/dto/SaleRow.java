package com.msa.order.local.sale.entity.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SaleRow(
    LocalDateTime createAt,
    String createBy,
    String  saleType,
    String storeName,
    String saleCode,
    String flowCode,
    String productName,
    String materialName,
    String colorName,
    String note, // (비고 + 메인 + 보조 메모)
    Boolean assistantStone,
    String assistantName,
    BigDecimal totalWeight, // (상품 + 스톤 중량 합)
    BigDecimal goldWeight, // (재질 기반 순금 무게)
    Integer totalProductLaborCost, // (기본 + 추가)
    Integer mainStoneLaborCost,
    Integer assistanceStoneLaborCost,
    Integer stoneAddLaborCost,
    Integer mainStoneQuantity,
    Integer assistanceStoneQuantity
) {}