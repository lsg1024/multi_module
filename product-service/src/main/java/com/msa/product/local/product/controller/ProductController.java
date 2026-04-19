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

/**
 * 상품 카탈로그 REST 컨트롤러.
 *
 * *상품 생성·조회·수정·삭제 및 배치 등록 API를 제공한다.
 *
 * *제공 엔드포인트 요약:
 *
 *   - {@code POST /products} — 상품 생성
 *   - {@code POST /products/batch} — Spring Batch를 이용한 JSON 파일 기반 상품 대량 등록
 *   - {@code GET /products/{id}} — 상품 단건 상세 조회
 *   - {@code GET /products} — 상품 목록 페이징 조회
 *       (검색어·검색 필드·범위 검색·등급·세트유형·분류·공장 필터, 동적 정렬 지원)
 *   - {@code PATCH /products/{id}} — 상품 수정
 *   - {@code DELETE /products/{id}} — 상품 삭제
 *   - {@code GET /api/product/{id}/{grade}} — 내부 API 상품 정보 및 등급별 공임비 조회
 *   - {@code GET /products/{id}/related} — 동일 관련번호를 가진 관련 상품 목록 조회
 * 
 *
 * *의존성: {@link com.msa.product.local.product.service.ProductService},
 * {@link org.springframework.batch.core.launch.JobLauncher} (대량 등록용)
 */
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

        Path tempPath = null;
        try {
            tempPath = Files.createTempFile("product-upload-", ".json");

            file.transferTo(tempPath.toFile());

            JobParameters params = new JobParametersBuilder()
                    .addString("filePath", tempPath.toAbsolutePath().toString())
                    .addString("accessToken", accessToken)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(productInsertJob, params);

        } catch (Exception e) {
            log.error("Product batch upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("저장 실패: " + e.getMessage()));
        } finally {
            if (tempPath != null) {
                try { java.nio.file.Files.deleteIfExists(tempPath); } catch (java.io.IOException ignored) {}
            }
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
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "searchField", required = false) String searchField,
            @RequestParam(name = "searchMin", required = false) String searchMin,
            @RequestParam(name = "searchMax", required = false) String searchMax,
            @RequestParam(name = "sortField", required = false) String sortField,
            @RequestParam(name = "sortOrder", required = false) String sortOrder,
            @RequestParam(name = "grade", required = false) String grade,
            @RequestParam(name = "setType", required = false) String setTypeFilter,
            @RequestParam(name = "classification", required = false) String classificationFilter,
            @RequestParam(name = "factory", required = false) String factoryFilter,
            @PageableDefault(size = 12) Pageable pageable) {
        CustomPage<ProductDto.Page> products = productService.getProducts(
                search, searchField, searchMin, searchMax, sortField, sortOrder, grade,
                setTypeFilter, classificationFilter, factoryFilter, pageable);
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

    @GetMapping("/api/product/name")
    public ResponseEntity<ApiResponse<ProductDetailDto>> getProductInfoByName(
            @RequestParam("name") String productName) {
        ProductDetailDto productInfo = productService.getProductInfoByName(productName);
        return ResponseEntity.ok(ApiResponse.success(productInfo));
    }

    /**
     * 상품명 기반 스톤 목록 조회 (마이그레이션용).
     * 카탈로그에 등록된 상품의 스톤 매핑 정보를 반환한다.
     */
    @GetMapping("/api/product/stones")
    public ResponseEntity<ApiResponse<java.util.List<ProductDetailDto.StoneInfo>>> getProductStonesByName(
            @RequestParam("name") String productName) {
        java.util.List<ProductDetailDto.StoneInfo> stones = productService.getProductStonesByName(productName);
        return ResponseEntity.ok(ApiResponse.success(stones));
    }

    @PatchMapping("/api/product/{id}/factory-name")
    public ResponseEntity<ApiResponse<String>> updateProductFactoryName(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        productService.updateProductFactoryName(id, body.get("productFactoryName"));
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
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
