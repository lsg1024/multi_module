package com.msa.account.local.factory.controller;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.global.excel.dto.AccountExcelDto;
import com.msa.account.local.factory.domain.dto.FactoryDto;
import com.msa.account.local.factory.service.ExcelService;
import com.msa.account.local.factory.service.FactoryService;
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

@Slf4j
@RestController
public class FactoryController {

    private final FactoryService factoryService;
    private final ExcelService excelService;
    private final JobLauncher jobLauncher;
    private final Job factoryImportJob;

    public FactoryController(FactoryService factoryService, ExcelService excelService, JobLauncher jobLauncher, Job factoryImportJob) {
        this.factoryService = factoryService;
        this.excelService = excelService;
        this.jobLauncher = jobLauncher;
        this.factoryImportJob = factoryImportJob;
    }

    //단일 조회
    @GetMapping("/factory/{id}")
    public ResponseEntity<ApiResponse<AccountDto.AccountSingleResponse>> getFactoryInfo(
            @PathVariable("id") String factoryId) {

        AccountDto.AccountSingleResponse factoryInfo = factoryService.getFactoryInfo(factoryId);

        return ResponseEntity.ok(ApiResponse.success(factoryInfo));
    }

    //목록 조회
    @GetMapping("/factories")
    public ResponseEntity<ApiResponse<CustomPage<FactoryDto.FactoryResponse>>> getFactoryList(
            @RequestParam(name = "search", required = false) String name,
            @PageableDefault(size = 12) Pageable pageable) {

        CustomPage<FactoryDto.FactoryResponse> factoryList = factoryService.getFactoryList(name, pageable);

        return ResponseEntity.ok(ApiResponse.success(factoryList));
    }

    //생성
    @PostMapping("/factory")
    public ResponseEntity<ApiResponse<String>> createFactory(
            @Valid @RequestBody FactoryDto.FactoryRequest accountInfo) {

        factoryService.createFactory(accountInfo);

        return ResponseEntity.ok(ApiResponse.success());
    }

    //생성 - batch
    @PostMapping("/factories/batch")
    public ResponseEntity<ApiResponse<String>> createFactoriesForBatch(
            @RequestParam("file") MultipartFile file) {

        try {
            Path tempPath = Files.createTempFile("factory-upload-", ".json");

            file.transferTo(tempPath.toFile());

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("filePath", tempPath.toAbsolutePath().toString())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(factoryImportJob, jobParameters);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("저장 실패: " + e.getMessage()));
        }

        return ResponseEntity.ok(ApiResponse.success("저장 중..."));
    }

    //수정
    @PatchMapping("/factories/{id}")
    public ResponseEntity<ApiResponse<String>> updateFactory(
            @AccessToken String accessToken,
            @PathVariable("id") String factoryId,
            @Valid @RequestBody AccountDto.AccountUpdate factoryInfo) {

        factoryService.updateFactory(accessToken, factoryId, factoryInfo);

        return ResponseEntity.ok(ApiResponse.success());
    }

    //엑셀 다운로드
    @GetMapping("/factories/excel")
    public ResponseEntity<byte[]> getFactoryExcel(
            @AccessToken String accessToken) throws IOException {

        List<AccountExcelDto> excel = factoryService.getExcel(accessToken);

        byte[] formatDtoToExcel = excelService.getFormatDtoToExcel(excel, "매입처");

        HttpHeaders headers = new HttpHeaders();

        String fileName = "매입처_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''");
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");


        return new ResponseEntity<>(formatDtoToExcel, headers, HttpStatus.OK);
    }

    @PatchMapping("/factories/harry/{id}/{harry}")
    public ResponseEntity<ApiResponse<String>> updateHarry(
            @AccessToken String accessToken,
            @PathVariable("id") String storeId,
            @PathVariable("harry") String harryId) {
        factoryService.updateFactoryHarry(accessToken, storeId, harryId);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @PatchMapping("/factories/grade/{id}/{grade}")
    public ResponseEntity<ApiResponse<String>> updateGrade(
            @AccessToken String accessToken,
            @PathVariable("id") String storeId,
            @PathVariable("grade") String grade) {
        factoryService.updateFactoryGrade(accessToken, storeId, grade);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    //삭제
    @DeleteMapping("/factories/{id}")
    public ResponseEntity<ApiResponse<String>> deleteFactory(
            @AccessToken String accessToken,
            @PathVariable("id") String factoryId) {

        factoryService.deleteFactory(accessToken, factoryId);

        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/factories/grade")
    public ResponseEntity<ApiResponse<String>> getFactoryGrade(
            @RequestParam(name = "id") String storeId) {
        String grade = factoryService.getFactoryGrade(storeId);
        return ResponseEntity.ok(ApiResponse.success(grade));
    }

    //공장 검증
    @GetMapping("/api/factory/{id}")
    public ResponseEntity<ApiResponse<FactoryDto.ApiFactoryInfo>> getFactoryInfo(@PathVariable Long id) {
        FactoryDto.ApiFactoryInfo factoryIdAndName = factoryService.getFactoryIdAndName(id);
        return ResponseEntity.ok(ApiResponse.success(factoryIdAndName));
    }

    @GetMapping("/api/factories")
    public ResponseEntity<ApiResponse<List<FactoryDto.ApiFactoryInfo>>> getFactoryAll() {
        List<FactoryDto.ApiFactoryInfo> factoryInfos = factoryService.findAllFactory();
        return ResponseEntity.ok(ApiResponse.success(factoryInfos));
    }
}
