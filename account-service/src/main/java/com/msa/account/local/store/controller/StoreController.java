package com.msa.account.local.store.controller;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.global.excel.dto.AccountExcelDto;
import com.msa.account.local.factory.service.ExcelService;
import com.msa.account.local.store.domain.dto.StoreDto;
import com.msa.account.local.store.service.StoreService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 매장 관리 REST 컨트롤러.
 *
 * *매장 생성·조회·수정·삭제, 미수금 조회, 배치 등록, 엑셀 다운로드 API를 제공한다.
 *
 * *제공 엔드포인트 요약:
 *
 *   - {@code GET /store/{id}} — 매장 단건 상세 조회
 *   - {@code GET /stores} — 매장 목록 페이징 조회
 *   - {@code GET /stores/receivable} — 미수금 있는 매장 목록 조회
 *   - {@code GET /stores/receivable/{id}} — 특정 매장 미수금 상세 조회
 *   - {@code GET /stores/receivable/sale-log/{id}} — 판매 로그 기반 미수금 상세 조회
 *   - {@code POST /store} — 매장 생성
 *   - {@code POST /stores/batch} — Spring Batch를 이용한 JSON 파일 기반 매장 대량 등록
 *   - {@code PATCH /stores/{id}} — 매장 정보 수정
 *   - {@code DELETE /stores/{id}} — 매장 삭제
 *   - {@code GET /stores/excel} — 매장 목록 엑셀 다운로드
 *   - {@code GET /stores/receivable/excel} — 미수금 목록 엑셀 다운로드
 *   - {@code PATCH /stores/harry/{id}/{harry}} — 매장 금 해리 설정 변경
 *   - {@code PATCH /stores/grade/{id}/{grade}} — 매장 등급 변경
 *   - {@code GET /stores/grade} — 매장 등급 조회
 *   - {@code GET /api/store/{id}} — 내부 API 매장 정보 조회
 *   - {@code GET /api/stores/phones} — 내부 API 매장 전화번호 일괄 조회
 * 
 *
 * *의존성: {@link com.msa.account.local.store.service.StoreService},
 * {@link com.msa.account.local.factory.service.ExcelService},
 * {@link org.springframework.batch.core.launch.JobLauncher} (대량 등록용)
 */
@Slf4j
@RestController
public class StoreController {

    private final StoreService storeService;
    private final ExcelService excelService;
    private final JobLauncher jobLauncher;
    private final Job storeImportJob;

    public StoreController(StoreService storeService, ExcelService excelService, JobLauncher jobLauncher, Job storeImportJob) {
        this.storeService = storeService;
        this.excelService = excelService;
        this.jobLauncher = jobLauncher;
        this.storeImportJob = storeImportJob;
    }

    //상점 단일 조회
    @GetMapping("/store/{id}")
    public ResponseEntity<ApiResponse<AccountDto.AccountSingleResponse>> getStoreInfo(
            @PathVariable("id") String storeId) {

        AccountDto.AccountSingleResponse storeInfo = storeService.getStoreInfo(storeId);

        return ResponseEntity.ok(ApiResponse.success(storeInfo));
    }

    //상점 목록 조회
    @GetMapping("/stores")
    public ResponseEntity<ApiResponse<CustomPage<StoreDto.StoreResponse>>> getStoreList(
            @RequestParam(name = "search", required = false) String name,
            @PageableDefault(size = 12) Pageable pageable) {

        CustomPage<StoreDto.StoreResponse> storeList = storeService.getStoreList(name, pageable);

        return ResponseEntity.ok(ApiResponse.success(storeList));
    }

    //상점 미수 금액 조회
    @GetMapping("/stores/receivable")
    public ResponseEntity<ApiResponse<CustomPage<AccountDto.AccountResponse>>> getStoreReceivable(
            @RequestParam(name = "search", required = false) String name,
            @RequestParam(name = "sortField", required = false) String field,
            @RequestParam(name = "sortOrder", required = false) String sort,
            @PageableDefault(size = 12) Pageable pageable) {
        CustomPage<AccountDto.AccountResponse> storeAttemptList = storeService.getStoreReceivable(name, field, sort, pageable);
        return ResponseEntity.ok(ApiResponse.success(storeAttemptList));
    }

    //상점 미수 금액 상세조회 - 현재 미수 값 조회
    @GetMapping("/stores/receivable/{id}")
    public ResponseEntity<ApiResponse<AccountDto.AccountResponse>> getStoreReceivableDetail(
            @PathVariable(name = "id") String storeId) {

        AccountDto.AccountResponse storeAttemptDetail = storeService.getStoreReceivableDetail(storeId);
        return ResponseEntity.ok(ApiResponse.success(storeAttemptDetail));
    }

    //판매 로그 기반 상점 미수 금액 상세조회 - 현재 미수 값 조회
    @GetMapping("/stores/receivable/sale-log/{id}")
    public ResponseEntity<ApiResponse<AccountDto.AccountSaleLogResponse>> getStoreReceivableLogDetail(
            @PathVariable(name = "id") String storeId,
            @RequestParam(name = "saleCode") String saleCode) {

        AccountDto.AccountSaleLogResponse storeAttemptDetail = storeService.getStoreReceivableLogDetail(storeId, saleCode);
        return ResponseEntity.ok(ApiResponse.success(storeAttemptDetail));
    }


    //상점 생성
    @PostMapping("/store")
    public ResponseEntity<ApiResponse<String>> createStore(
            @Valid @RequestBody StoreDto.StoreRequest accountInfo) {

        storeService.createStore(accountInfo);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @PostMapping("/stores/batch")
    public ResponseEntity<ApiResponse<String>> createStoreForBatch(
            @RequestParam("file") MultipartFile file) {

        Path tempPath = null;
        try {
            tempPath = Files.createTempFile("store-upload-", ".json");

            file.transferTo(tempPath.toFile());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", tempPath.toAbsolutePath().toString())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(storeImportJob, jobParameters);

        } catch (Exception e) {
            log.error("Store batch upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("저장 실패: " + e.getMessage()));
        } finally {
            if (tempPath != null) {
                try { Files.deleteIfExists(tempPath); } catch (IOException ignored) {}
            }
        }

        return ResponseEntity.ok(ApiResponse.success("저장 중..."));
    }

    //상점 수정
    @PatchMapping("/stores/{id}")
    public ResponseEntity<ApiResponse<String>> updateStore(
            @AccessToken String accessToken,
            @PathVariable("id") String storeId,
            @Valid @RequestBody AccountDto.AccountUpdate updateInfo) {

        storeService.updateStore(accessToken, storeId, updateInfo);

        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    //엑셀 다운로드
    @GetMapping("/stores/excel")
    public ResponseEntity<byte[]> getStoreExcel(
            @AccessToken String accessToken) throws IOException {

        List<AccountExcelDto> excel = storeService.getExcel(accessToken);

        byte[] formatDtoToExcel = excelService.getFormatDtoToExcel(excel, "판매처");

        HttpHeaders headers = new HttpHeaders();

        String fileName = "판매처_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return new ResponseEntity<>(formatDtoToExcel, headers, HttpStatus.OK);
    }

    @GetMapping("/stores/receivable/excel")
    public ResponseEntity<byte[]> getReceivableExcel(
            @AccessToken String accessToken,
            @RequestParam(name = "search", required = false) String name) throws IOException {

        byte[] excelBytes = storeService.getReceivableExcel(accessToken, name);

        HttpHeaders headers = new HttpHeaders();
        String fileName = "미수금_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }

    @PatchMapping("/stores/harry/{id}/{harry}")
    public ResponseEntity<ApiResponse<String>> updateHarry(
            @AccessToken String accessToken,
            @PathVariable("id") String storeId,
            @PathVariable("harry") String harryId) {
        storeService.updateStoreHarry(accessToken, storeId, harryId);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @PatchMapping("/stores/grade/{id}/{grade}")
    public ResponseEntity<ApiResponse<String>> updateGrade(
            @AccessToken String accessToken,
            @PathVariable("id") String storeId,
            @PathVariable("grade") String grade) {
        storeService.updateStoreGrade(accessToken, storeId, grade);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    //상점 삭제
    @DeleteMapping("/stores/{id}")
    public ResponseEntity<ApiResponse<String>> deleteStore(
            @AccessToken String accessToken,
            @PathVariable("id") String storeId) {

        storeService.deleteStore(accessToken, storeId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    //상점 등급 조회
    @GetMapping("/stores/grade")
    public ResponseEntity<ApiResponse<String>> getStoreGrade(
            @RequestParam(name = "id") String storeId) {
        String grade = storeService.getStoreGrade(storeId);
        return ResponseEntity.ok(ApiResponse.success(grade));
    }

    @GetMapping("/api/store/{id}")
    public ResponseEntity<ApiResponse<StoreDto.ApiStoreInfo>> getStoreInfo(
            @PathVariable Long id) {
        StoreDto.ApiStoreInfo storeInfo = storeService.getStoreInfo(id);
        return ResponseEntity.ok(ApiResponse.success(storeInfo));
    }

    @GetMapping("/api/store/name")
    public ResponseEntity<ApiResponse<StoreDto.ApiStoreInfo>> getStoreInfoByName(
            @RequestParam("name") String storeName) {
        StoreDto.ApiStoreInfo storeInfo = storeService.getStoreInfoByName(storeName);
        return ResponseEntity.ok(ApiResponse.success(storeInfo));
    }

    @GetMapping("/api/stores/phones")
    public ResponseEntity<ApiResponse<List<StoreDto.StorePhoneInfo>>> getStorePhones(
            @RequestParam("ids") List<Long> storeIds) {
        List<StoreDto.StorePhoneInfo> phones = storeService.getStorePhones(storeIds);
        return ResponseEntity.ok(ApiResponse.success(phones));
    }

}
