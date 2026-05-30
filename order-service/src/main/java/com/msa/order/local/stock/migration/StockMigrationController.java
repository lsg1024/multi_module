package com.msa.order.local.stock.migration;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;


import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 레거시 재고 데이터 마이그레이션 컨트롤러.
 *
 * POST /api/migration/stocks — CSV 파일 업로드 → Spring Batch Job 실행
 * 실패 건이 있으면 실패 목록 CSV 파일을 응답으로 반환.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class StockMigrationController {

    private final JobLauncher jobLauncher;
    private final Job stockImportJob;
    private final StockMigrationFailureCollector failureCollector;
    private final StockMigrationRecordCollector recordCollector;
    private final StockMigrationService migrationService;

    @Value("${migration.failure-log-dir:src/main/resources/logs}")
    private String failureLogDir;

    @PostMapping("/api/migration/stocks")
    public ResponseEntity<?> migrateStocks(
            @AccessToken String accessToken,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "encoding", defaultValue = "UTF-8") String encoding,
            @RequestParam(value = "userName", defaultValue = "LEGACY_MIGRATION") String userName) {

        try {
            // 1. CSV → UTF-8 변환 후 임시 파일 저장 (기본: UTF-8, CP949 지정 가능)
            Path tempPath = convertToUtf8(file, Charset.forName(encoding));

            // 2. 실패/레코드 수집기 초기화 (이전 실행 결과 잔존 방지)
            failureCollector.clear();
            recordCollector.clear();

            // 3. Batch Job 실행 (동기) — Reader는 항상 UTF-8
            //    userName 은 StatusHistory.userName 에 그대로 사용된다.
            //    레거시: "LEGACY_MIGRATION" (기본값) / 크롤러: "CRAWL_MIGRATION" 등 호출 측이 식별 가능한 값을 지정.
            JobParameters params = new JobParametersBuilder()
                    .addString("filePath", tempPath.toAbsolutePath().toString())
                    .addString("accessToken", accessToken)
                    .addString("encoding", "UTF-8")
                    .addString("userName", userName)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            log.info("재고 마이그레이션 배치 Job 시작");
            var execution = jobLauncher.run(stockImportJob, params);
            long readCount = execution.getStepExecutions().stream()
                    .mapToLong(s -> s.getReadCount()).sum();
            long writeCount = execution.getStepExecutions().stream()
                    .mapToLong(s -> s.getWriteCount()).sum();
            long skipCount = execution.getStepExecutions().stream()
                    .mapToLong(s -> s.getReadSkipCount() + s.getProcessSkipCount() + s.getWriteSkipCount()).sum();
            log.info("재고 마이그레이션 배치 Job 완료 - 읽기: {}건, 저장: {}건, skip: {}건, 실패기록: {}건",
                    readCount, writeCount, skipCount, failureCollector.getFailureCount());

            // 4. 임시 파일 삭제
            Files.deleteIfExists(tempPath);

            // 5. logs 디렉터리 준비 (records / summary / failures 모두 여기에 저장)
            Path logsDir = Path.of(failureLogDir);
            Files.createDirectories(logsDir);
            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

            // 6. records.csv 항상 저장 — 모든 row 처리 결과(SUCCESS/SKIP/ERROR)
            List<StockMigrationRecordCollector.Record> records = recordCollector.getAll();
            Path recordsPath = logsDir.resolve("migration_stock_records_" + timestamp + ".csv");
            Files.write(recordsPath, migrationService.generateRecordsCsv(records));
            log.info("재고 마이그레이션 records.csv 저장: {} ({}건)", recordsPath.toAbsolutePath(), records.size());

            long successByRecord = recordCollector.countByOutcome(StockMigrationRecordCollector.Outcome.SUCCESS);
            long skipNullStatus = recordCollector.countByOutcome(StockMigrationRecordCollector.Outcome.SKIP_NULL_STATUS);
            long skipException = recordCollector.countByOutcome(StockMigrationRecordCollector.Outcome.SKIP_EXCEPTION);
            long errorPersist = recordCollector.countByOutcome(StockMigrationRecordCollector.Outcome.ERROR_PERSIST);

            // 7. summary.txt 항상 저장
            Path summaryPath = logsDir.resolve("migration_stock_summary_" + timestamp + ".txt");
            String summary = String.format(
                    "===== 재고 마이그레이션 요약 =====%n" +
                    "시각: %s%n" +
                    "userName: %s%n" +
                    "encoding(원본): %s%n" +
                    "원본 파일: %s%n" +
                    "%n" +
                    "[Spring Batch 통계]%n" +
                    "  읽기(readCount):  %d%n" +
                    "  저장(writeCount): %d%n" +
                    "  스킵(skipCount):  %d%n" +
                    "%n" +
                    "[RecordCollector 통계 — row 단위 결과]%n" +
                    "  SUCCESS:           %d%n" +
                    "  SKIP_NULL_STATUS:  %d  (현재고구분 매핑 실패)%n" +
                    "  SKIP_EXCEPTION:    %d  (Processor catch)%n" +
                    "  ERROR_PERSIST:     %d  (Writer catch)%n" +
                    "  총 records:         %d%n" +
                    "%n" +
                    "[FailureCollector 통계]%n" +
                    "  실패 row: %d%n" +
                    "%n" +
                    "[주의]%n" +
                    "  Spring Batch의 writeCount 는 Writer가 받은 아이템 수이며,%n" +
                    "  실제 commit 건수와 다를 수 있습니다.%n" +
                    "  진짜 commit 건수는 RecordCollector 의 SUCCESS 값을 참고하되,%n" +
                    "  chunk 트랜잭션이 silent rollback 된 경우 SUCCESS 도 부정확할 수 있습니다.%n" +
                    "  최종적으로 DB의 SELECT COUNT(*) FROM stock 으로 검증해야 합니다.%n",
                    java.time.LocalDateTime.now(),
                    userName, encoding,
                    file.getOriginalFilename(),
                    readCount, writeCount, skipCount,
                    successByRecord, skipNullStatus, skipException, errorPersist, records.size(),
                    failureCollector.getFailureCount()
            );
            Files.writeString(summaryPath, summary);
            log.info("재고 마이그레이션 summary.txt 저장: {}", summaryPath.toAbsolutePath());

            // 8. 실패 건 확인
            List<FailedStockRow> failures = failureCollector.getFailures();

            if (failures.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(String.format(
                        "재고 마이그레이션 완료 (읽기: %d건, writeCount: %d건, SUCCESS: %d건) | records: %s | summary: %s",
                        readCount, writeCount, successByRecord,
                        recordsPath.toAbsolutePath(), summaryPath.toAbsolutePath())));
            }

            // 9. 실패 건이 있으면 failures.csv도 저장
            byte[] failureCsv = migrationService.generateFailureCsv(failures);
            Path failurePath = logsDir.resolve("migration_stock_failures_" + timestamp + ".csv");
            Files.write(failurePath, failureCsv);
            log.info("재고 마이그레이션 실패 {}건 - 파일 저장: {}", failures.size(), failurePath.toAbsolutePath());

            return ResponseEntity.ok(ApiResponse.error(String.format(
                    "재고 마이그레이션 완료 (읽기: %d건, writeCount: %d건, SUCCESS: %d건, 실패: %d건) | records: %s | summary: %s | failures: %s",
                    readCount, writeCount, successByRecord, failures.size(),
                    recordsPath.toAbsolutePath(), summaryPath.toAbsolutePath(), failurePath.toAbsolutePath())));

        } catch (Exception e) {
            log.error("재고 마이그레이션 처리 중 오류", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("재고 마이그레이션 실패: " + e.getMessage()));
        }
    }

    /**
     * 마지막 재고 마이그레이션 실패 건 CSV 다운로드.
     */
    @GetMapping("/api/migration/stocks/failures")
    public ResponseEntity<byte[]> downloadStockFailures() {
        List<FailedStockRow> failures = failureCollector.getFailures();

        if (failures.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        try {
            byte[] csv = migrationService.generateFailureCsv(failures);

            // UTF-8 BOM 추가 (엑셀 한글 호환)
            byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
            byte[] result = new byte[bom.length + csv.length];
            System.arraycopy(bom, 0, result, 0, bom.length);
            System.arraycopy(csv, 0, result, bom.length, csv.length);

            String filename = java.net.URLEncoder.encode("재고마이그레이션_실패목록.csv", StandardCharsets.UTF_8);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(result);
        } catch (Exception e) {
            log.error("실패 CSV 생성 오류", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 업로드된 CSV 파일을 sourceCharset으로 읽어 UTF-8 임시 파일로 변환한다.
     * BOM이 있으면 자동으로 제거한다.
     */
    private Path convertToUtf8(MultipartFile file, Charset sourceCharset) throws IOException {
        Path tempPath = Files.createTempFile("stock-migration-", ".csv");

        try (InputStream is = file.getInputStream();
             Reader reader = new InputStreamReader(is, sourceCharset);
             BufferedReader br = new BufferedReader(reader);
             BufferedWriter bw = Files.newBufferedWriter(tempPath, StandardCharsets.UTF_8)) {

            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    // UTF-8 BOM 제거 (\uFEFF)
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    firstLine = false;
                }
                bw.write(line);
                bw.newLine();
            }
        }

        log.info("CSV 인코딩 변환 완료: {} → UTF-8 ({})", sourceCharset.name(), tempPath.toAbsolutePath());
        return tempPath;
    }
}
