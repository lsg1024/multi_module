package com.msa.order.local.order.migration;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MigrationResult {
    private final int successCount;
    private final List<FailedOrderRow> failures;

    public int getFailureCount() {
        return failures.size();
    }

    public int getTotalCount() {
        return successCount + failures.size();
    }
}
