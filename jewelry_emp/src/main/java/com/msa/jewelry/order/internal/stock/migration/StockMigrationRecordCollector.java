package com.msa.jewelry.order.internal.stock.migration;

import com.msa.jewelry.order.internal.stock.entity.Stock;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 재고 마이그레이션의 모든 row 처리 결과를 row 단위로 수집한다.
 *
 * <p>기존 {@link StockMigrationFailureCollector}는 실패만 모았으나, 본 collector 는
 * SUCCESS / SKIP / ERROR 모두를 기록하여 분석/감사용 records.csv 를 만든다.
 *
 * <p>thread-safe (synchronizedList + AtomicInteger). Bean 으로 등록하여
 * Processor/Writer가 같은 인스턴스를 공유하도록 한다.
 */
@Component
public class StockMigrationRecordCollector {

    public enum Outcome {
        SUCCESS,           // Writer에서 persist + flush 성공
        SKIP_NULL_STATUS,  // Processor: 현재고구분 매핑 실패 → null 반환
        SKIP_EXCEPTION,    // Processor: 그 외 예외 → null 반환
        ERROR_PERSIST      // Writer: persist/flush 또는 후속 단계에서 예외
    }

    public record Record(
            int sequenceNo,
            Outcome outcome,
            String csvNo,
            String modelName,
            String storeName,
            String currentStockType,
            String orderStatus,    // mapOrderStatus 결과 (Outcome.SUCCESS / ERROR_PERSIST에서 채워짐)
            Long stockId,           // SUCCESS 시 채워짐
            String flowCode,        // SUCCESS 시 채워짐
            String reason,          // SKIP / ERROR 시 사유
            Instant recordedAt
    ) {}

    private final List<Record> records = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger seq = new AtomicInteger(0);

    // ============================================================
    public void clear() {
        records.clear();
        seq.set(0);
    }

    public List<Record> getAll() {
        synchronized (records) {
            return List.copyOf(records);
        }
    }

    public int size() {
        return records.size();
    }

    public long countByOutcome(Outcome outcome) {
        synchronized (records) {
            return records.stream().filter(r -> r.outcome() == outcome).count();
        }
    }

    // ============================================================
    /** Writer에서 persist 성공 직후 호출 */
    public void recordSuccess(Stock stock) {
        records.add(new Record(
                seq.incrementAndGet(),
                Outcome.SUCCESS,
                null,                                                       // csvNo: Writer에는 row 없음
                stock.getProduct() != null ? stock.getProduct().getProductFactoryName() : null,
                // 2026-05 P4: Stock.storeName 컬럼 제거. 마이그레이션 로그용으로 storeId 만 출력.
                stock.getStoreId() != null ? "store#" + stock.getStoreId() : null,
                null,                                                       // CSV 원본 currentStockType은 Writer에서 모름
                stock.getOrderStatus() != null ? stock.getOrderStatus().name() : null,
                stock.getStockId(),
                stock.getFlowCode().toString(),
                null,
                Instant.now()
        ));
    }

    /** Processor에서 mapOrderStatus 실패 시 호출 */
    public void recordSkipNullStatus(StockCsvRow row) {
        records.add(new Record(
                seq.incrementAndGet(),
                Outcome.SKIP_NULL_STATUS,
                row.getNo(),
                row.getModelName(),
                row.getStoreName(),
                row.getCurrentStockType(),
                null,
                null,
                null,
                "알 수 없는 현재고구분 값: " + row.getCurrentStockType(),
                Instant.now()
        ));
    }

    /** Processor의 catch에서 호출 */
    public void recordSkipException(StockCsvRow row, String reason) {
        records.add(new Record(
                seq.incrementAndGet(),
                Outcome.SKIP_EXCEPTION,
                row.getNo(),
                row.getModelName(),
                row.getStoreName(),
                row.getCurrentStockType(),
                null,
                null,
                null,
                reason,
                Instant.now()
        ));
    }

    /** Writer의 catch에서 호출 */
    public void recordErrorPersist(Stock stock, String reason) {
        records.add(new Record(
                seq.incrementAndGet(),
                Outcome.ERROR_PERSIST,
                null,
                stock.getProduct() != null ? stock.getProduct().getProductFactoryName() : null,
                // 2026-05 P4: Stock.storeName 컬럼 제거. 마이그레이션 로그용으로 storeId 만 출력.
                stock.getStoreId() != null ? "store#" + stock.getStoreId() : null,
                null,
                stock.getOrderStatus() != null ? stock.getOrderStatus().name() : null,
                stock.getStockId(),
                stock.getFlowCode().toString(),
                reason,
                Instant.now()
        ));
    }
}
