package com.msa.product.global.batch.product.legacy;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 레거시 마이그레이션 REST 컨트롤러.
 *
 * POST /api/migration/products              — 상품 CSV 업로드 → 배치 실행
 * GET  /api/migration/products/failures      — 상품 실패 CSV 다운로드
 * POST /api/migration/products/stones        — 스톤 CSV 업로드 → 배치 실행
 * GET  /api/migration/products/stones/failures — 스톤 실패 CSV 다운로드
 */
@Slf4j
@RestController
@RequestMapping("/api/migration/products")
public class ProductLegacyMigrationController {

    private final JobLauncher jobLauncher;
    private final Job productLegacyImportJob;
    private final Job productStoneLegacyImportJob;
    private final ProductMigrationFailureCollector failureCollector;
    private final StoneMigrationFailureCollector stoneFailureCollector;

    public ProductLegacyMigrationController(
            JobLauncher jobLauncher,
            Job productLegacyImportJob,
            Job productStoneLegacyImportJob,
            ProductMigrationFailureCollector failureCollector,
            StoneMigrationFailureCollector stoneFailureCollector) {
        this.jobLauncher = jobLauncher;
        this.productLegacyImportJob = productLegacyImportJob;
        this.productStoneLegacyImportJob = productStoneLegacyImportJob;
        this.failureCollector = failureCollector;
        this.stoneFailureCollector = stoneFailureCollector;
    }

    private static final Charset CP949 = Charset.forName("CP949");

    /**
     * 레거시 기본정보 CSV 업로드 → 상품 마이그레이션 배치 실행.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> migrateProducts(
            @AccessToken String accessToken,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "encoding", defaultValue = "UTF-8") String encoding) {

        try {
            failureCollector.clear();

            // 지정된 인코딩 → UTF-8 변환 (기본: CP949, UTF-8 가능)
            Charset sourceCharset = "UTF-8".equalsIgnoreCase(encoding)
                    ? StandardCharsets.UTF_8
                    : Charset.forName(encoding);
            Path tempPath = convertToUtf8(file, sourceCharset);

            JobParameters params = new JobParametersBuilder()
                    .addString("filePath", tempPath.toAbsolutePath().toString())
                    .addString("accessToken", accessToken)
                    .addString("encoding", "UTF-8")
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(productLegacyImportJob, params);

            String message = "상품 마이그레이션 완료 — " + failureCollector.getSummary();
            if (failureCollector.hasFailures()) {
                message += " (GET /api/migration/products/failures 로 상세 내역 다운로드)";
            }

            return ResponseEntity.ok(ApiResponse.success(message));

        } catch (Exception e) {
            log.error("상품 마이그레이션 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("마이그레이션 실패: " + e.getMessage()));
        }
    }

    /**
     * 마지막 배치의 실패 행 CSV 다운로드.
     */
    @GetMapping("/failures")
    public ResponseEntity<byte[]> downloadFailures() {
        List<ProductMigrationFailureCollector.FailedProductRow> failures = failureCollector.getFailures();

        if (failures.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        StringBuilder csv = new StringBuilder();
        // 헤더
        csv.append("No,등록일,모델번호,제조사,제조번호,세트구분,모델분류,기본재질,기본중량,")
           .append("공개여부,단종여부,단가제,비고사항,기본색상,기본공임원가,")
           .append("기본공임1등급,기본공임2등급,기본공임3등급,기본공임4등급,공임설명,실패사유\n");

        for (ProductMigrationFailureCollector.FailedProductRow f : failures) {
            ProductLegacyCsvRow r = f.getRow();
            csv.append(esc(r.getNo())).append(",");
            csv.append(esc(r.getRegisterDate())).append(",");
            csv.append(esc(r.getModelNumber())).append(",");
            csv.append(esc(r.getManufacturer())).append(",");
            csv.append(esc(r.getManufacturingNo())).append(",");
            csv.append(esc(r.getSetType())).append(",");
            csv.append(esc(r.getClassification())).append(",");
            csv.append(esc(r.getMaterial())).append(",");
            csv.append(esc(r.getStandardWeight())).append(",");
            csv.append(esc(r.getIsPublic())).append(",");
            csv.append(esc(r.getDiscontinued())).append(",");
            csv.append(esc(r.getUnitPrice())).append(",");
            csv.append(esc(r.getNote())).append(",");
            csv.append(esc(r.getDefaultColor())).append(",");
            csv.append(esc(r.getPurchasePrice())).append(",");
            csv.append(esc(r.getGrade1LaborCost())).append(",");
            csv.append(esc(r.getGrade2LaborCost())).append(",");
            csv.append(esc(r.getGrade3LaborCost())).append(",");
            csv.append(esc(r.getGrade4LaborCost())).append(",");
            csv.append(esc(r.getLaborCostNote())).append(",");
            csv.append(esc(f.getReason())).append("\n");
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        // BOM 추가 (엑셀 한글 호환)
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + bytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(bytes, 0, result, bom.length, bytes.length);

        String filename;
        try {
            filename = URLEncoder.encode("상품마이그레이션_실패목록.csv", StandardCharsets.UTF_8);
        } catch (Exception e) {
            filename = "product_migration_failures.csv";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(result);
    }

    // ── 스톤 마이그레이션 ──

    /**
     * 레거시 모델별 스톤정보 CSV 업로드 → ProductStone 마이그레이션 배치 실행.
     * 상품 마이그레이션 완료 후 실행해야 함.
     */
    @PostMapping("/stones")
    public ResponseEntity<ApiResponse<String>> migrateProductStones(
            @AccessToken String accessToken,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "encoding", defaultValue = "UTF-8") String encoding) {

        try {
            stoneFailureCollector.clear();

            Charset sourceCharset = "UTF-8".equalsIgnoreCase(encoding)
                    ? StandardCharsets.UTF_8
                    : Charset.forName(encoding);
            Path tempPath = convertToUtf8(file, sourceCharset);

            JobParameters params = new JobParametersBuilder()
                    .addString("filePath", tempPath.toAbsolutePath().toString())
                    .addString("encoding", "UTF-8")
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(productStoneLegacyImportJob, params);

            String message = "스톤 마이그레이션 완료";
            if (stoneFailureCollector.hasFailures()) {
                message += " (실패 " + stoneFailureCollector.getFailures().size()
                        + "건 — GET /api/migration/products/stones/failures 로 다운로드)";
            }

            return ResponseEntity.ok(ApiResponse.success(message));

        } catch (Exception e) {
            log.error("스톤 마이그레이션 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("스톤 마이그레이션 실패: " + e.getMessage()));
        }
    }

    /**
     * 스톤 마이그레이션 실패 행 CSV 다운로드.
     */
    @GetMapping("/stones/failures")
    public ResponseEntity<byte[]> downloadStoneFailures() {
        List<StoneMigrationFailureCollector.FailedStoneRow> failures = stoneFailureCollector.getFailures();

        if (failures.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        StringBuilder csv = new StringBuilder();
        csv.append("순서,모델번호,메인,스톤종류,스톤비고,알개수적용,알개수,알차감적용,")
           .append("개당스톤중량,스톤원단가,공임적용여부,스톤공임단가1,스톤공임단가2,")
           .append("스톤공임단가3,스톤공임단가4,실패사유\n");

        for (StoneMigrationFailureCollector.FailedStoneRow f : failures) {
            ProductStoneCsvRow r = f.getRow();
            csv.append(esc(r.getNo())).append(",");
            csv.append(esc(r.getModelNumber())).append(",");
            csv.append(esc(r.getMainStone())).append(",");
            csv.append(esc(r.getStoneName())).append(",");
            csv.append(esc(r.getStoneNote())).append(",");
            csv.append(esc(r.getIncludeQuantity())).append(",");
            csv.append(esc(r.getStoneQuantity())).append(",");
            csv.append(esc(r.getIncludeStone())).append(",");
            csv.append(esc(r.getStoneWeight())).append(",");
            csv.append(esc(r.getStonePurchasePrice())).append(",");
            csv.append(esc(r.getIncludePrice())).append(",");
            csv.append(esc(r.getGradePrice1())).append(",");
            csv.append(esc(r.getGradePrice2())).append(",");
            csv.append(esc(r.getGradePrice3())).append(",");
            csv.append(esc(r.getGradePrice4())).append(",");
            csv.append(esc(f.getReason())).append("\n");
        }

        byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
        byte[] result = new byte[bom.length + bytes.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(bytes, 0, result, bom.length, bytes.length);

        String filename;
        try {
            filename = URLEncoder.encode("스톤마이그레이션_실패목록.csv", StandardCharsets.UTF_8);
        } catch (Exception e) {
            filename = "stone_migration_failures.csv";
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(result);
    }

    // ── 유틸리티 ──

    /**
     * CP949 CSV → UTF-8 임시파일 변환 (BOM 제거 포함).
     */
    private Path convertToUtf8(MultipartFile file, Charset sourceCharset) throws IOException {
        Path tempPath = Files.createTempFile("product-legacy-", ".csv");

        try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(file.getInputStream(), sourceCharset));
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(new FileOutputStream(tempPath.toFile()), StandardCharsets.UTF_8))) {

            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) {
                    // BOM 제거
                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }
                    first = false;
                }
                writer.write(line);
                writer.newLine();
            }
        }

        return tempPath;
    }

    /**
     * CSV 필드 이스케이프 (쉼표·큰따옴표 포함 시 감싸기).
     */
    private String esc(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
