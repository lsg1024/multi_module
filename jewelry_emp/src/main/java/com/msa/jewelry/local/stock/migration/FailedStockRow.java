package com.msa.jewelry.local.stock.migration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "재고 마이그레이션 실패 행 — 마이그레이션 처리 실패한 한 행과 실패 사유.")
public class FailedStockRow {
    @Schema(description = "실패한 CSV 행 원본")
    private final StockCsvRow row;
    @Schema(description = "실패 사유 메시지", example = "재질 매핑 실패")
    private final String reason;
}
