package com.msa.product.local.product.controller;

import com.msa.product.local.product.service.ProductImageService;
import com.msacommon.global.api.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
public class ProductImageController {

    private final ProductImageService productImageService;

    public ProductImageController(ProductImageService productImageService) {
        this.productImageService = productImageService;
    }

    @PostMapping("/products/{productId}/images")
    public ResponseEntity<ApiResponse<String>> uploadProductImages(
            @PathVariable(name = "productId") Long productId,
            @RequestParam("images") List<MultipartFile> images) {
        productImageService.uploadProductImages(productId, images);
        return ResponseEntity.ok(ApiResponse.success("이미지 업로드 완료"));
    }
}
