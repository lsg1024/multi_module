package com.msa.product.local.classification.controller;

import com.msa.product.local.classification.dto.ClassificationDto;
import com.msa.product.local.classification.service.ClassificationService;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ClassificationController {

    private final ClassificationService classificationService;

    public ClassificationController(ClassificationService classificationService) {
        this.classificationService = classificationService;
    }

    @PostMapping("/classifications")
    public ResponseEntity<ApiResponse<String>> createClassification(
            @Valid @RequestBody ClassificationDto classificationDto) {

        classificationService.saveClassification(classificationDto);

        return ResponseEntity.ok(ApiResponse.success("생성 완료"));
    }

    /**
     * 테스트용 생성 코드 단일 save 분리호출
     * @param classificationDtos
     * @return
     */
    @PostMapping("/classificationss")
    public ResponseEntity<ApiResponse<String>> createClassifications(
            @Valid @RequestBody List<ClassificationDto> classificationDtos) {

        classificationDtos.forEach(classificationService::saveClassification);

        return ResponseEntity.ok(ApiResponse.success("일괄 생성 완료"));
    }

    @GetMapping("/classifications/{id}")
    public ResponseEntity<ApiResponse<ClassificationDto.ResponseSingle>> getClassification(
            @PathVariable(name = "id") Long classificationId) {

        ClassificationDto.ResponseSingle classification = classificationService.getClassification(classificationId);

        return ResponseEntity.ok(ApiResponse.success(classification));
    }

    @GetMapping("/classifications")
    public ResponseEntity<ApiResponse<List<ClassificationDto.ResponseSingle>>> getClassifications(
            @RequestParam(name = "name", required = false) String classificationName) {
        return ResponseEntity.ok(ApiResponse.success(classificationService.getClassifications(classificationName)));
    }

    @PatchMapping("/classifications/{id}")
    public ResponseEntity<ApiResponse<String>> updateClassification(
            @PathVariable(name = "id") Long classificationId,
            @Valid @RequestBody ClassificationDto classificationDto) {
        classificationService.updateClassification(classificationId, classificationDto);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @DeleteMapping("/classifications/{id}")
    public ResponseEntity<ApiResponse<String>> deletedClassification(
            @AccessToken String accessToken,
            @PathVariable(name = "id") Long classificationId) {

        classificationService.deletedClassification(accessToken, classificationId);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }

    @GetMapping("/api/classification/{id}")
    public ResponseEntity<ApiResponse<String>> getClassificationInfo(
            @PathVariable Long id) {
        String classificationName = classificationService.getClassificationName(id);
        return ResponseEntity.ok(ApiResponse.success(classificationName));
    }
}
