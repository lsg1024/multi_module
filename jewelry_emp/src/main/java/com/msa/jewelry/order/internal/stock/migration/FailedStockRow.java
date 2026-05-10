package com.msa.jewelry.order.internal.stock.migration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FailedStockRow {
    private final StockCsvRow row;
    private final String reason;
}
