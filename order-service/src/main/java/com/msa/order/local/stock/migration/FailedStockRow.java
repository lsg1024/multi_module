package com.msa.order.local.stock.migration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FailedStockRow {
    private final StockCsvRow row;
    private final String reason;
}
