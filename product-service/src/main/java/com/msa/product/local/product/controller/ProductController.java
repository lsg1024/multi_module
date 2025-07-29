package com.msa.product.local.product.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.product.local.product.dto.ProductDto;
import com.msa.product.local.product.service.ProductService;
import com.msacommon.global.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ProductController {

    private final ObjectMapper objectMapper;
    private final ProductService productService;

    public ProductController(ObjectMapper objectMapper, ProductService productService) {
        this.objectMapper = objectMapper;
        this.productService = productService;
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<String>> createProduct(
            @RequestParam("product") String productStr,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {

        try {
            ProductDto productDto = objectMapper.readValue(productStr, ProductDto.class);
            productService.saveProduct(productDto, images);
            return ResponseEntity.ok(ApiResponse.success("상품이 성공적으로 생성되었습니다."));
        } catch (JsonProcessingException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("JSON 파싱 오류: " + e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("서버 오류: " + e.getMessage()));
        }
    }

}
