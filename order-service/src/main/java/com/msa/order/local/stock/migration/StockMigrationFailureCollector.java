package com.msa.order.local.stock.migration;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 재고 마이그레이션 실패 건을 수집하는 공유 컴포넌트.
 * Job 실행 전 clear(), 실행 중 add(), 완료 후 getFailures()로 사용.
 */
@Component
public class StockMigrationFailureCollector {

    private final List<FailedStockRow> failures = Collections.synchronizedList(new ArrayList<>());

    public void add(StockCsvRow row, String reason) {
        failures.add(new FailedStockRow(row, reason));
    }

    public List<FailedStockRow> getFailures() {
        return Collections.unmodifiableList(new ArrayList<>(failures));
    }

    public int getFailureCount() {
        return failures.size();
    }

    public void clear() {
        failures.clear();
    }
}
