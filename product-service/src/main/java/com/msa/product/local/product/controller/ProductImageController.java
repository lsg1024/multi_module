package com.msa.product.local.product.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.api.ApiResponse;
import com.msa.product.local.product.dto.ProductImageDto;
import com.msa.product.local.product.service.ProductImageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class ProductImageController {

    private final ObjectMapper objectMapper;
    private final ProductImageService productImageService;

    public ProductImageController(ObjectMapper objectMapper, ProductImageService productImageService) {
        this.objectMapper = objectMapper;
        this.productImageService = productImageService;
    }

    @PostMapping("/products/{id}/images")
    public ResponseEntity<ApiResponse<String>> uploadProductImages(
            @PathVariable(name = "id") Long productId,
            @RequestParam("images") List<MultipartFile> images) {
        productImageService.uploadProductImages(productId, images);
        return ResponseEntity.ok(ApiResponse.success("이미지 업로드 완료"));
    }

    @PatchMapping(value = "/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> updateProductImages(
            @PathVariable(name = "id") Long productId,
            @RequestParam(value = "metaData") String metaDataJson,
            @RequestParam(value = "images", required = false) List<MultipartFile> images) {

        ProductImageDto.Request metaData;
        try {
            metaData = objectMapper.readValue(metaDataJson, ProductImageDto.Request.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("metaData 파싱 실패", e);
        }

        productImageService.updateImages(productId, images, metaData);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    @GetMapping("/api/products/images")
    public ResponseEntity<ApiResponse<Map<Long, ProductImageDto.ApiResponse>>> getProductImages(
            @RequestParam("ids") List<Long> productIds) {
        Map<Long, ProductImageDto.ApiResponse> imagesByProductIds = productImageService.getImagesByProductIds(productIds);
        return ResponseEntity.ok(ApiResponse.success(imagesByProductIds));
    }

}
