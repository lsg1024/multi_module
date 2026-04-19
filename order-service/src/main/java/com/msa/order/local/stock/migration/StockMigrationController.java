package com.msa.order.local.stock.migration;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
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
    private final StockMigrationService migrationService;

    @PostMapping("/api/migration/stocks")
    public ResponseEntity<?> migrateStocks(
            @AccessToken String accessToken,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "encoding", defaultValue = "CP949") String encoding) {

        try {
            // 1. CSV → UTF-8 변환 후 임시 파일 저장 (기본: CP949, UTF-8 지정 가능)
            Path tempPath = convertToUtf8(file, Charset.forName(encoding));

            // 2. 실패 수집기 초기화
            failureCollector.clear();

            // 3. Batch Job 실행 (동기) — Reader는 항상 UTF-8
            JobParameters params = new JobParametersBuilder()
                    .addString("filePath", tempPath.toAbsolutePath().toString())
                    .addString("accessToken", accessToken)
                    .addString("encoding", "UTF-8")
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

            // 5. 실패 건 확인
            List<FailedStockRow> failures = failureCollector.getFailures();

            if (failures.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(
                        String.format("재고 마이그레이션 완료 (읽기: %d건, 저장: %d건)", readCount, writeCount)));
            }

            // 6. 실패 건이 있으면 CSV 파일로 저장
            byte[] failureCsv = migrationService.generateFailureCsv(failures);

            String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "migration_stock_failures_" + timestamp + ".csv";

            Path failurePath = Path.of(System.getProperty("java.io.tmpdir"), fileName);
            Files.write(failurePath, failureCsv);

            log.info("재고 마이그레이션 실패 {}건 - 파일 저장: {}", failures.size(), failurePath.toAbsolutePath());

            return ResponseEntity.ok(ApiResponse.error(
                    String.format("재고 마이그레이션 완료 (읽기: %d건, 저장: %d건, 실패: %d건) - 파일: %s",
                            readCount, writeCount, failures.size(), failurePath.toAbsolutePath())));

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
