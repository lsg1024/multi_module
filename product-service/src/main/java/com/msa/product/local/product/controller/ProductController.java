package com.msa.product.local.product.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import com.msa.product.local.product.dto.ProductDetailDto;
import com.msa.product.local.product.dto.ProductDto;
import com.msa.product.local.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
public class ProductController {
    private final ProductService productService;
    private final JobLauncher jobLauncher;
    private final Job productInsertJob;

    public ProductController(ProductService productService, JobLauncher jobLauncher, Job productInsertJob) {
        this.productService = productService;
        this.jobLauncher = jobLauncher;
        this.productInsertJob = productInsertJob;
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<String>> createProduct(
            @AccessToken String accessToken,
            @Valid @RequestBody ProductDto productDto) {
        Long productId = productService.saveProduct(accessToken, productDto);
        return ResponseEntity.ok(ApiResponse.success("생성 완료", productId.toString()));
    }

    /**
     * 서버 관리자용 대량 등록 메서드
     * @param accessToken 배치 처리 과정에 tenant 추출
     * @param file 상품 json 코드
     * @return 저장 응답
     */
    @PostMapping("/products/batch")
    public ResponseEntity<ApiResponse<String>> createBatchProduct(
            @AccessToken String accessToken,
            @RequestParam("file") MultipartFile file) {

        try {
            Path tempPath = Files.createTempFile("product-upload-", ".json");

            file.transferTo(tempPath.toFile());

            JobParameters params = new JobParametersBuilder()
                    .addString("filePath", tempPath.toAbsolutePath().toString())
                    .addString("accessToken", accessToken)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(productInsertJob, params);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("저장 실패: " + e.getMessage()));
        }

        return ResponseEntity.ok(ApiResponse.success("저장 중..."));

    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto.Detail>> getProduct(
            @PathVariable(name = "id") String productId) {
        ProductDto.Detail product = productService.getProduct(Long.valueOf(productId));
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<CustomPage<ProductDto.Page>>> getProducts(
            @RequestParam(name = "name", required = false) String productName,
            @RequestParam(name = "factory", required = false) String factoryName,
            @RequestParam(name = "classification", required = false) String classificationId,
            @RequestParam(name = "setType", required = false) String setTypeId,
            @RequestParam(name = "sortField", required = false) String sortField,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "grade", required = false) String grade,
            @PageableDefault(size = 12) Pageable pageable) {
        CustomPage<ProductDto.Page> products = productService.getProducts(productName, factoryName, classificationId, setTypeId, pageable, sortField, sort, grade);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PatchMapping("/products/{id}")
    public ResponseEntity<ApiResponse<String>> updateProduct(
            @AccessToken String accessToken,
            @PathVariable(name = "id") Long productId,
            @Valid @RequestBody ProductDto.Update productDto) {
        productService.updateProduct(accessToken, productId, productDto);

        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<String>> deletedProduct(
            @AccessToken String accessToken,
            @PathVariable(name = "id") Long productId) {
        productService.deletedProduct(accessToken, productId);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }

    @GetMapping("/api/product/{id}/{grade}")
    public ResponseEntity<ApiResponse<ProductDetailDto>> getProductInfo(
            @PathVariable Long id,
            @PathVariable(name = "grade") String grade) {
        ProductDetailDto productInfo = productService.getProductInfo(id, grade);
        return ResponseEntity.ok(ApiResponse.success(productInfo));
    }

    /**
     * 관련 상품 조회 (상품 상세보기용)
     * 동일한 관련번호를 가진 상품들을 조회
     */
    @GetMapping("/products/{id}/related")
    public ResponseEntity<ApiResponse<java.util.List<ProductDto.RelatedProduct>>> getRelatedProducts(
            @PathVariable(name = "id") Long productId) {
        java.util.List<ProductDto.RelatedProduct> relatedProducts = productService.getRelatedProducts(productId);
        return ResponseEntity.ok(ApiResponse.success(relatedProducts));
    }

}
