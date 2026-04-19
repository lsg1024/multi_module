package com.msa.order.local.order.migration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FailedOrderRow {
    private final OrderCsvRow row;
    private final String reason;
}
