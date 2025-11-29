package com.msa.product.local.product.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.tenant.TenantContext;
import com.msa.product.local.product.dto.ProductImageDto;
import com.msa.product.local.product.service.ProductImageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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

    private final ObjectMapper objectMapper;
    private final ProductImageService productImageService;

    public ProductImageController(ObjectMapper objectMapper, ProductImageService productImageService) {
        this.objectMapper = objectMapper;
        this.productImageService = productImageService;
    }

    @GetMapping("/products/images/**")
    public ResponseEntity<Resource> serveFile(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String relativePath = requestUri.substring(requestUri.indexOf("/images/") + 7);

        String tenant = TenantContext.getTenant();

        Path filePath = Paths.get(baseUploadPath, tenant, relativePath);

        log.info("serverFile filePath = {}", filePath);

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

    @PostMapping("/products/{id}/images")
    public ResponseEntity<ApiResponse<String>> uploadProductImages(
            @PathVariable(name = "id") Long productId,
            @RequestParam("images") List<MultipartFile> images) {
        productImageService.uploadProductImages(productId, images);
        return ResponseEntity.ok(ApiResponse.success("이미지 업로드 완료"));
    }

    @PostMapping("/products/images/bulk-name")
    public ResponseEntity<ApiResponse<String>> bulkUploadByFileName(
            @RequestParam("images") List<MultipartFile> images) {

        productImageService.bulkUploadByFileName(images);

        return ResponseEntity.ok(ApiResponse.success("상품 이름 기반 이미지 일괄 업로드 완료"));
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
