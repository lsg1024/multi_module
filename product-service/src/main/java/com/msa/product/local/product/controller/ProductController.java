package com.msa.product.local.product.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import com.msa.product.local.product.dto.ProductDetailDto;
import com.msa.product.local.product.dto.ProductDto;
import com.msa.product.local.product.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<String>> createProduct(
            HttpServletRequest request,
            @Valid @RequestBody ProductDto productDto) {
        productService.saveProduct(request, productDto);
        return ResponseEntity.ok(ApiResponse.success("카테고리 생성완료"));
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
            @RequestParam(name = "level", required = false) String level,
            @PageableDefault(size = 12) Pageable pageable) { // 검색 옵션으로 제조사, 제조번호, 등급
        CustomPage<ProductDto.Page> products = productService.getProducts(productName, factoryName, classificationId, setTypeId, pageable, level);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PatchMapping("/products/{id}")
    public ResponseEntity<ApiResponse<String>> updateProduct(
            HttpServletRequest request,
            @PathVariable(name = "id") Long productId,
            @RequestBody ProductDto.Update productDto) {
        productService.updateProduct(request, productId, productDto);

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

}
