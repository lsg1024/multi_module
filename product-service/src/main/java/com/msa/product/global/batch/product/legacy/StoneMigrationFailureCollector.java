package com.msa.product.global.batch.product.legacy;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 스톤 마이그레이션 실패 행 수집기.
 */
@Component
public class StoneMigrationFailureCollector {

    private final List<FailedStoneRow> failures = Collections.synchronizedList(new ArrayList<>());

    public void add(ProductStoneCsvRow row, String reason) {
        failures.add(new FailedStoneRow(row, reason));
    }

    public List<FailedStoneRow> getFailures() {
        return Collections.unmodifiableList(failures);
    }

    public void clear() {
        failures.clear();
    }

    public boolean hasFailures() {
        return !failures.isEmpty();
    }

    @Getter
    public static class FailedStoneRow {
        private final ProductStoneCsvRow row;
        private final String reason;

        public FailedStoneRow(ProductStoneCsvRow row, String reason) {
            this.row = row;
            this.reason = reason;
        }
    }
}
