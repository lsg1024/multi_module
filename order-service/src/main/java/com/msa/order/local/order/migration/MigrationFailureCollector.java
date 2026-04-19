package com.msa.order.local.order.migration;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 마이그레이션 실패 건을 수집하는 공유 컴포넌트.
 * Job 실행 전 clear(), 실행 중 add(), 완료 후 getFailures()로 사용.
 */
@Component
public class MigrationFailureCollector {

    private final List<FailedOrderRow> failures = Collections.synchronizedList(new ArrayList<>());

    public void add(OrderCsvRow row, String reason) {
        failures.add(new FailedOrderRow(row, reason));
    }

    public List<FailedOrderRow> getFailures() {
        return Collections.unmodifiableList(new ArrayList<>(failures));
    }

    public int getFailureCount() {
        return failures.size();
    }

    public void clear() {
        failures.clear();
    }
}
