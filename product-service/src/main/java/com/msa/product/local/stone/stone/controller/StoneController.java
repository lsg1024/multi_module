package com.msa.product.local.stone.stone.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import com.msa.product.local.stone.stone.dto.StoneDto;
import com.msa.product.local.stone.stone.service.StoneService;
import jakarta.validation.Valid;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
public class StoneController {
    private final StoneService stoneService;
    private final JobLauncher jobLauncher;
    private final Job stoneInsertJob;

    public StoneController(StoneService stoneService, JobLauncher jobLauncher, Job stoneInsertJob) {
        this.stoneService = stoneService;
        this.jobLauncher = jobLauncher;
        this.stoneInsertJob = stoneInsertJob;
    }

    // 생성
    @PostMapping("/stone")
    public ResponseEntity<ApiResponse<String>> createStone(
            @Valid @RequestBody StoneDto stoneDto) {
        stoneService.saveStone(stoneDto);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @PostMapping("/stones")
    public ResponseEntity<ApiResponse<String>> createStones(
            @Valid @RequestBody List<StoneDto> stoneDto) {
        stoneDto.forEach(stoneService::saveStone);
        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    @PostMapping("/stones/batch")
    public ResponseEntity<ApiResponse<String>> uploadStonesBatch(
            @RequestParam("file") MultipartFile file) {

        try {
            Path tempPath = Files.createTempFile("stone-upload-", ".json");

            file.transferTo(tempPath.toFile());

            JobParameters params = new JobParametersBuilder()
                    .addString("filePath", tempPath.toAbsolutePath().toString())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(stoneInsertJob, params);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("저장 실패: " + e.getMessage()));
        }

        return ResponseEntity.ok(ApiResponse.success("저장 중..."));
    }

    // 단건 조회
    @GetMapping("/stones/{id}")
    public ResponseEntity<ApiResponse<StoneDto.ResponseSingle>> getStone(
            @PathVariable(name = "id") Long stoneId) {
        StoneDto.ResponseSingle stone = stoneService.getStone(stoneId);
        return ResponseEntity.ok(ApiResponse.success(stone));
    }

    // 복수 조회 + 검색 + 페이징
    @GetMapping("/stones")
    public ResponseEntity<ApiResponse<CustomPage<StoneDto.PageDto>>> getStones(
            @RequestParam(name = "search", required = false) String stoneName,
            @PageableDefault(size = 12) Pageable pageable) {
        CustomPage<StoneDto.PageDto> result = stoneService.getStones(stoneName, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // 검증
    @GetMapping("/stones/exists")
    public ResponseEntity<ApiResponse<Boolean>> existStone(
            @RequestParam(name = "stone-type") String stoneTypeName,
            @RequestParam(name = "stone-shape") String stoneShapeName,
            @RequestParam(name = "stone-size") String stoneSize) {
        Boolean existStoneName = stoneService.getExistStoneName(stoneTypeName, stoneShapeName, stoneSize);
        return ResponseEntity.ok(ApiResponse.success(existStoneName));
    }

    // 수정
    @PatchMapping("/stones/{id}")
    public ResponseEntity<ApiResponse<String>> updateStone(
            @PathVariable(name = "id") Long stoneId,
            @Valid @RequestBody StoneDto stoneDto) {
        stoneService.updateStone(stoneId, stoneDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    // 삭제
    @DeleteMapping("/stones/{id}")
    public ResponseEntity<ApiResponse<String>> deleteStone(
            @AccessToken String accessToken,
            @PathVariable(name = "id") Long stoneId) {
        stoneService.deletedStone(accessToken, stoneId);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }

    @GetMapping("/api/stone/{id}")
    public ResponseEntity<ApiResponse<Boolean>> existStoneId(
            @PathVariable Long id) {
        Boolean existStoneId = stoneService.getExistStoneId(id);
        return ResponseEntity.ok(ApiResponse.success(existStoneId));
    }
}
