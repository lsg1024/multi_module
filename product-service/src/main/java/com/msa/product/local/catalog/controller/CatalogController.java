package com.msa.product.local.catalog.controller;

import com.msa.common.global.api.ApiResponse;
import com.msa.common.global.jwt.AccessToken;
import com.msa.common.global.util.CustomPage;
import com.msa.product.local.catalog.dto.CatalogProductDto;
import com.msa.product.local.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 카탈로그 컨트롤러 (판매처 전용)
 * 가격 정보가 제외된 상품 정보를 제공하는 API
 */
@Slf4j
@RestController
@RequestMapping("/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    /**
     * 카탈로그 상품 목록 조회
     * 가격/제조사 정보가 제외된 상품 목록을 페이징으로 제공
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<CustomPage<CatalogProductDto.Page>>> getCatalogProducts(
            @AccessToken String accessToken,
            @RequestParam(name = "name", required = false) String productName,
            @RequestParam(name = "classification", required = false) String classificationId,
            @RequestParam(name = "setType", required = false) String setTypeId,
            @RequestParam(name = "sortField", required = false) String sortField,
            @RequestParam(name = "sort", required = false) String sort,
            @PageableDefault(size = 12) Pageable pageable) {

        CustomPage<CatalogProductDto.Page> products = catalogService.getCatalogProducts(
                accessToken, productName, classificationId, setTypeId,
                sortField, sort, pageable
        );

        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * 카탈로그 상품 상세 조회
     * 가격 정보가 제외된 상품 상세 정보를 제공
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<CatalogProductDto.Detail>> getCatalogProductDetail(
            @AccessToken String accessToken,
            @PathVariable(name = "id") Long productId) {

        CatalogProductDto.Detail detail = catalogService.getCatalogProductDetail(accessToken, productId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    /**
     * 카탈로그 관련 상품 조회
     * 동일한 관련번호를 가진 상품들을 조회
     */
    @GetMapping("/products/{id}/related")
    public ResponseEntity<ApiResponse<List<CatalogProductDto.RelatedProduct>>> getRelatedProducts(
            @AccessToken String accessToken,
            @PathVariable(name = "id") Long productId,
            @RequestParam(name = "relatedNumber", required = false) String relatedNumber) {

        List<CatalogProductDto.RelatedProduct> relatedProducts =
                catalogService.getRelatedProducts(accessToken, productId, relatedNumber);

        return ResponseEntity.ok(ApiResponse.success(relatedProducts));
    }

    /**
     * 카탈로그 상품 엑셀 다운로드
     */
    @GetMapping("/products/excel")
    public ResponseEntity<byte[]> downloadCatalogProductsExcel(
            @AccessToken String accessToken,
            @RequestParam(name = "name", required = false) String productName,
            @RequestParam(name = "classification", required = false) String classificationId,
            @RequestParam(name = "setType", required = false) String setTypeId) throws IOException {

        byte[] excelBytes = catalogService.getCatalogProductsExcel(
                accessToken, productName, classificationId, setTypeId
        );

        String fileName = "상품카탈로그_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
        headers.setContentLength(excelBytes.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelBytes);
    }
}
