package com.msa.product.global.batch.product.legacy;

import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 상품 마이그레이션 실패 행 수집기.
 * 스레드 안전하게 실패 행과 사유를 보관하며, 배치 종료 후 CSV로 내보낼 수 있다.
 */
@Component
public class ProductMigrationFailureCollector {

    private final List<FailedProductRow> failures = Collections.synchronizedList(new ArrayList<>());

    // 스킵 통계 (단종/중복은 실패가 아닌 스킵이므로 별도 카운트)
    private final java.util.concurrent.atomic.AtomicInteger discontinuedCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger duplicateCount = new java.util.concurrent.atomic.AtomicInteger(0);
    private final java.util.concurrent.atomic.AtomicInteger totalProcessedCount = new java.util.concurrent.atomic.AtomicInteger(0);

    public void add(ProductLegacyCsvRow row, String reason) {
        failures.add(new FailedProductRow(row, reason));
    }

    /** 단종 스킵 기록 — 실패 CSV에도 포함 */
    public void addDiscontinued(ProductLegacyCsvRow row) {
        discontinuedCount.incrementAndGet();
        failures.add(new FailedProductRow(row, "[스킵] 단종 상품"));
    }

    /** 중복 스킵 기록 — 실패 CSV에도 포함 */
    public void addDuplicate(ProductLegacyCsvRow row, String productName) {
        duplicateCount.incrementAndGet();
        failures.add(new FailedProductRow(row, "[스킵] 이미 존재하는 상품: " + productName));
    }

    public void incrementProcessed() {
        totalProcessedCount.incrementAndGet();
    }

    public List<FailedProductRow> getFailures() {
        return Collections.unmodifiableList(failures);
    }

    public void clear() {
        failures.clear();
        discontinuedCount.set(0);
        duplicateCount.set(0);
        totalProcessedCount.set(0);
    }

    public boolean hasFailures() {
        return !failures.isEmpty();
    }

    public int getDiscontinuedCount() { return discontinuedCount.get(); }
    public int getDuplicateCount() { return duplicateCount.get(); }
    public int getTotalProcessedCount() { return totalProcessedCount.get(); }

    /** 상세 통계 문자열 */
    public String getSummary() {
        int total = totalProcessedCount.get();
        int success = total - failures.size();
        int disc = discontinuedCount.get();
        int dup = duplicateCount.get();
        int errors = failures.size() - disc - dup;
        return String.format("전체 %d건 → 성공 %d건, 단종 스킵 %d건, 중복 스킵 %d건, 실패 %d건",
                total, success, disc, dup, errors);
    }

    @Getter
    public static class FailedProductRow {
        private final ProductLegacyCsvRow row;
        private final String reason;

        public FailedProductRow(ProductLegacyCsvRow row, String reason) {
            this.row = row;
            this.reason = reason;
        }
    }
}
