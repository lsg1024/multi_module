package com.msa.jewelry.local.product.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.tenant.TenantContext;
import com.msa.jewelry.local.product.dto.ProductImageDto;
import com.msa.jewelry.local.product.service.ProductImageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
public class ProductImageController {

    @Value("${FILE_UPLOAD_PATH2:/tmp/jewelry/uploads}")
    private String baseUploadPath;
    private final ProductImageService productImageService;
    private final JobLauncher jobLauncher;

    private final Job imageMigrationJob;

    public ProductImageController(ProductImageService productImageService, JobLauncher jobLauncher, Job imageMigrationJob) {
        this.productImageService = productImageService;
        this.jobLauncher = jobLauncher;
        this.imageMigrationJob = imageMigrationJob;
    }

    // 상품 다건 대표 이미지 일괄 조회
    @GetMapping("/api/products/images")
    public ResponseEntity<ApiResponse<java.util.Map<Long, ProductImageDto.ProductImageResponse>>> getProductImagesByIds(
            @RequestParam("ids") List<Long> productIds) {
        return ResponseEntity.ok(ApiResponse.success(
                productImageService.getImagesByProductIds(productIds)
        ));
    }

    @GetMapping("/products/images/**")
    public ResponseEntity<Resource> getImages(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        int idx = requestUri.indexOf("/images/");
        if (idx < 0) {
            return ResponseEntity.notFound().build();
        }
        String relativePath = requestUri.substring(idx + "/images/".length());

        if (!StringUtils.hasText(relativePath) || relativePath.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        String tenant = TenantContext.getTenant();
        if (!StringUtils.hasText(tenant)) {
            log.warn("이미지 요청에 tenant 없음: {}", requestUri);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Path baseDir = Paths.get(baseUploadPath, tenant).toAbsolutePath().normalize();
        Path filePath = baseDir.resolve(relativePath).normalize();

        // resolve 후에도 baseDir 밖으로 빠지지 않는지 재확인 (symlink/traversal 이중 방어)
        if (!filePath.startsWith(baseDir)) {
            log.warn("디렉토리 이탈 시도 차단: {}", filePath);
            return ResponseEntity.badRequest().build();
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
            if (contentType == null) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        } catch (Exception e) {
            log.error("이미지 서빙 실패: {}", filePath, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/products/images/migration")
    public ResponseEntity<ApiResponse<String>> runImageMigration() {

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("tenantId", TenantContext.getTenant())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(imageMigrationJob, params);

        } catch (Exception e) {
            log.error("이미지 마이그레이션 실패", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("마이그레이션 실패: " + e.getMessage()));
        }

        return ResponseEntity.ok(ApiResponse.success("이미지 정리 작업이 시작되었습니다."));
    }

    @PostMapping("/products/{id}/image")
    public ResponseEntity<ApiResponse<String>> saveProductImage(
            @PathVariable(name = "id") Long productId,
            @RequestParam("image") MultipartFile image) {

        productImageService.uploadProductImage(productId, image);

        return ResponseEntity.ok(ApiResponse.success("이미지 저장 완료"));
    }

    @PostMapping("/products/{id}/images")
    public ResponseEntity<ApiResponse<String>> addProductImages(
            @PathVariable(name = "id") Long productId,
            @RequestParam("images") List<MultipartFile> images) {

        productImageService.addProductImages(productId, images);

        return ResponseEntity.ok(ApiResponse.success(images.size() + "개 이미지 추가 완료"));
    }

    /**
     * 특정 상품의 모든 이미지 조회
     */
    @GetMapping("/products/{id}/images")
    public ResponseEntity<ApiResponse<List<ProductImageDto.Response>>> getProductImages(
            @PathVariable(name = "id") Long productId) {

        List<ProductImageDto.Response> images = productImageService.getProductImageList(productId);

        return ResponseEntity.ok(ApiResponse.success(images));
    }

    @PostMapping("/products/images/bulk-name")
    public ResponseEntity<ApiResponse<String>> bulkUploadByFileName(
            @RequestParam("images") List<MultipartFile> images) {

        productImageService.bulkUploadByFileName(images);

        return ResponseEntity.ok(ApiResponse.success("상품 이름 기반 이미지 일괄 업로드 완료"));
    }

    @DeleteMapping("/products/images/{imageId}")
    public ResponseEntity<ApiResponse<String>> deleteProductImage(
            @PathVariable(name = "imageId") Long imageId) {

        productImageService.deleteImage(imageId);

        return ResponseEntity.ok(ApiResponse.success("이미지 삭제 완료"));
    }

}
