package com.msa.product.local.product.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.tenant.TenantContext;
import com.msa.product.local.product.dto.ProductImageDto;
import com.msa.product.local.product.service.ProductImageService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class ProductImageController {

    @Value("${FILE_UPLOAD_PATH}")
    private String baseUploadPath;
    private final ProductImageService productImageService;
    private final JobLauncher jobLauncher;

    private final Job imageMigrationJob;


    public ProductImageController(ProductImageService productImageService, JobLauncher jobLauncher, Job imageMigrationJob) {
        this.productImageService = productImageService;
        this.jobLauncher = jobLauncher;
        this.imageMigrationJob = imageMigrationJob;
    }


    @PostMapping("/products/images/migration")
    public ResponseEntity<ApiResponse<String>> runImageMigration() {

        try {
            JobParameters params = new JobParametersBuilder()
                    .addString("tenant", TenantContext.getTenant())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(imageMigrationJob, params);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("마이그레이션 실패: " + e.getMessage()));
        }

        return ResponseEntity.ok(ApiResponse.success("이미지 정리 작업이 시작되었습니다."));
    }

    @GetMapping("/products/images/**")
    public ResponseEntity<Resource> getImages(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String relativePath = requestUri.substring(requestUri.indexOf("/images/") + 8);

        String tenant = TenantContext.getTenant();

        Path filePath = Paths.get(baseUploadPath, tenant, relativePath);

        try {
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/products/images")
    public ResponseEntity<ApiResponse<Map<Long, ProductImageDto.ApiResponse>>> getProductImages(
            @RequestParam("ids") List<Long> productIds) {
        Map<Long, ProductImageDto.ApiResponse> imagesByProductIds = productImageService.getImagesByProductIds(productIds);
        return ResponseEntity.ok(ApiResponse.success(imagesByProductIds));
    }

    @PostMapping("/products/{id}/image")
    public ResponseEntity<ApiResponse<String>> saveProductImage(
            @PathVariable(name = "id") Long productId,
            @RequestParam("image") MultipartFile image) {

        productImageService.uploadProductImage(productId, image);

        return ResponseEntity.ok(ApiResponse.success("이미지 저장 완료"));
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
