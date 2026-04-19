package com.msa.order.local.order.migration;

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
 * 레거시 주문 데이터 마이그레이션 컨트롤러.
 *
 * POST /api/migration/orders — CSV 파일 업로드 → Spring Batch Job 실행
 * 실패 건이 있으면 실패 목록 CSV 파일을 응답으로 반환.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OrderMigrationController {

    private final JobLauncher jobLauncher;
    private final Job orderImportJob;
    private final Job deletedOrderImportJob;
    private final MigrationFailureCollector failureCollector;
    private final OrderMigrationService migrationService;

    /**
     * 일반 주문 마이그레이션 (주문리스트.csv, 수리관리.csv)
     * 인코딩: CP949
     */
    @PostMapping("/api/migration/orders")
    public ResponseEntity<?> migrateOrders(
            @AccessToken String accessToken,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "encoding", defaultValue = "CP949") String encoding) {
        return executeMigration(accessToken, file, orderImportJob, encoding, false, false, "주문");
    }

    /**
     * 수리 주문 마이그레이션 (수리관리.csv)
     * 인코딩: CP949 (기본값, UTF-8 지정 가능)
     * orderStatus = FIX 로 저장
     */
    @PostMapping("/api/migration/orders/fix")
    public ResponseEntity<?> migrateFixOrders(
            @AccessToken String accessToken,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "encoding", defaultValue = "CP949") String encoding) {
        return executeMigration(accessToken, file, orderImportJob, encoding, false, true, "수리");
    }

    /**
     * 삭재 주문 마이그레이션 (삭재리스트.csv)
     * 인코딩: UTF-8 (삭재리스트는 UTF-8)
     * orderDeleted = true, orderStatus = DELETED, productStatus = DELETED 로 저장
     */
    @PostMapping("/api/migration/orders/deleted")
    public ResponseEntity<?> migrateDeletedOrders(
            @AccessToken String accessToken,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "encoding", defaultValue = "UTF-8") String encoding) {
        return executeMigration(accessToken, file, deletedOrderImportJob, encoding, true, false, "삭재 주문");
    }

    private ResponseEntity<?> executeMigration(
            String accessToken, MultipartFile file, Job job,
            String encoding, boolean deleted, boolean fixOrder, String label) {
        try {
            // 1. CSV → UTF-8 변환 후 임시 파일 저장
            Path tempPath = convertToUtf8(file, Charset.forName(encoding));

            // 2. 실패 수집기 초기화
            failureCollector.clear();

            // 3. Batch Job 실행 (동기) — Reader는 항상 UTF-8
            JobParameters params = new JobParametersBuilder()
                    .addString("filePath", tempPath.toAbsolutePath().toString())
                    .addString("accessToken", accessToken)
                    .addString("encoding", "UTF-8")
                    .addString("deleted", String.valueOf(deleted))
                    .addString("fixOrder", String.valueOf(fixOrder))
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            log.info("{} 마이그레이션 배치 Job 시작 (encoding={}, deleted={})", label, encoding, deleted);
            jobLauncher.run(job, params);
            log.info("{} 마이그레이션 배치 Job 완료 - 실패: {}건", label, failureCollector.getFailureCount());

            // 4. 임시 파일 삭제
            Files.deleteIfExists(tempPath);

            // 5. 실패 건 확인
            List<FailedOrderRow> failures = failureCollector.getFailures();

            if (failures.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(label + " 마이그레이션 완료"));
            }

            // 6. 실패 건이 있으면 CSV 파일로 저장
            byte[] failureCsv = migrationService.generateFailureCsv(failures);

            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "migration_" + label + "_failures_" + timestamp + ".csv";

            Path failurePath = Path.of(System.getProperty("java.io.tmpdir"), fileName);
            Files.write(failurePath, failureCsv);

            log.info("{} 마이그레이션 실패 {}건 - 파일 저장: {}", label, failures.size(), failurePath.toAbsolutePath());

            return ResponseEntity.ok(ApiResponse.error(
                    String.format("%s 마이그레이션 완료 (실패 %d건) - 파일: %s",
                            label, failures.size(), failurePath.toAbsolutePath())));

        } catch (Exception e) {
            log.error("{} 마이그레이션 처리 중 오류", label, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(label + " 마이그레이션 실패: " + e.getMessage()));
        }
    }

    /**
     * 마지막 주문 마이그레이션 실패 건 CSV 다운로드.
     */
    @GetMapping("/api/migration/orders/failures")
    public ResponseEntity<byte[]> downloadOrderFailures() {
        List<FailedOrderRow> failures = failureCollector.getFailures();

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

            String filename = java.net.URLEncoder.encode("주문마이그레이션_실패목록.csv", StandardCharsets.UTF_8);

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
        Path tempPath = Files.createTempFile("order-migration-", ".csv");

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
