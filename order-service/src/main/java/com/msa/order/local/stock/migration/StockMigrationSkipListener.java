package com.msa.order.local.stock.migration;

import com.msa.order.local.stock.entity.Stock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.batch.item.file.FlatFileParseException;

/**
 * Reader/Processor/Writer 단계에서 skip된 항목을 FailureCollector에 기록하는 리스너.
 * 주로 Reader의 CSV 파싱 오류(FlatFileParseException)를 캡처한다.
 */
@Slf4j
public class StockMigrationSkipListener implements SkipListener<StockCsvRow, Stock> {

    private final StockMigrationFailureCollector failureCollector;

    public StockMigrationSkipListener(StockMigrationFailureCollector failureCollector) {
        this.failureCollector = failureCollector;
    }

    /**
     * Reader에서 CSV 행 파싱 실패 시 호출.
     * FlatFileParseException에는 실패한 라인 번호와 원본 텍스트가 포함된다.
     */
    @Override
    public void onSkipInRead(Throwable t) {
        if (t instanceof FlatFileParseException parseEx) {
            String input = parseEx.getInput();
            int lineNumber = parseEx.getLineNumber();
            log.error("CSV 파싱 실패 [라인 {}]: {} — 원인: {}", lineNumber, input, parseEx.getMessage());

            // 원본 CSV 텍스트에서 기본 정보 추출하여 실패 기록
            StockCsvRow failRow = new StockCsvRow();
            failRow.setNo("라인 " + lineNumber);
            failRow.setModelName(extractField(input, 9));   // 모델명 (10번째 필드)
            failRow.setStoreName(extractField(input, 1));    // 매장명 (2번째 필드)
            failRow.setCurrentStockType(extractField(input, 4)); // 현재고구분 (5번째 필드)
            failureCollector.add(failRow, "CSV 파싱 실패 (라인 " + lineNumber + "): " + parseEx.getMessage());
        } else {
            log.error("Reader skip 발생: {}", t.getMessage());
            StockCsvRow failRow = new StockCsvRow();
            failRow.setNo("UNKNOWN");
            failureCollector.add(failRow, "Reader 오류: " + t.getMessage());
        }
    }

    /**
     * Processor에서 skip 시 호출 (이미 Processor 내부에서 failureCollector에 기록하므로 로그만)
     */
    @Override
    public void onSkipInProcess(StockCsvRow item, Throwable t) {
        log.error("Processor skip [No={}, 모델={}]: {}", item.getNo(), item.getModelName(), t.getMessage());
    }

    /**
     * Writer에서 skip 시 호출 (이미 Writer 내부에서 failureCollector에 기록하므로 로그만)
     */
    @Override
    public void onSkipInWrite(Stock item, Throwable t) {
        log.error("Writer skip [모델={}, 매장={}]: {}",
                item.getProduct() != null ? item.getProduct().getProductFactoryName() : "N/A",
                item.getStoreName(),
                t.getMessage());
    }

    /**
     * CSV 라인에서 특정 인덱스의 필드를 추출 (간단한 comma split)
     */
    private String extractField(String csvLine, int index) {
        if (csvLine == null) return "";
        try {
            String[] fields = csvLine.split(",", -1);
            if (index < fields.length) {
                return fields[index].replace("\"", "").trim();
            }
        } catch (Exception e) {
            // ignore
        }
        return "";
    }
}
