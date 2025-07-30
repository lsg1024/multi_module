package com.msa.product.local.product.controller;

import com.msa.product.local.product.dto.ProductDto;
import com.msa.product.local.product.dto.ProductStoneDto;
import com.msa.product.local.product.service.ProductService;
import com.msacommon.global.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<Long>> createProduct(
            @RequestBody ProductDto productDto) {

        Long productId = productService.saveProduct(productDto);
        return ResponseEntity.ok(ApiResponse.success("상품이 성공적으로 생성되었습니다.", productId));
    }

//    @GetMapping("/products/{id}")
//    public ResponseEntity<ApiResponse<ProductDto.Detail>> getProductV1(
//            @PathVariable(name = "id") String productId) {
//
//        ProductDto.Detail product = productService.getProductV1(Long.valueOf(productId));
//
//        return ResponseEntity.ok(ApiResponse.success(product));
//    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto.Detail>> getProductV2(
            @PathVariable(name = "id") String productId) {

        ProductDto.Detail product = productService.getProductV2(Long.valueOf(productId));

        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/productstone/{id}")
    public ResponseEntity<ApiResponse<List<ProductStoneDto.Response>>> getProductStone(
            @PathVariable(name = "id") String productId) {

        List<ProductStoneDto.Response> productStone = productService.getProductStone(Long.valueOf(productId));

        return ResponseEntity.ok(ApiResponse.success(productStone));
    }

}
