package com.msa.jewelry.product.internal.imagesearch.controller;

import com.msa.jewelry.product.internal.imagesearch.dto.ImageSearchDtos.SearchResponse;
import com.msa.jewelry.product.internal.imagesearch.service.ProductImageSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 사용자(직원) 노출 이미지 검색 API.
 * - 인증/테넌시는 기존 인터셉터 체인이 처리 (X-Tenant-ID + Authorization)
 */
@Slf4j
@RestController
@RequestMapping("/products/search")
@RequiredArgsConstructor
public class ProductImageSearchController {

    private final ProductImageSearchService service;

    @PostMapping(value = "/by-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SearchResponse searchByImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "classification", required = false) String classification,
            @RequestParam(value = "materialIds",    required = false) List<Long> materialIds,
            @RequestParam(value = "colorIds",       required = false) List<Long> colorIds,
            @RequestParam(value = "topK",           required = false) Integer topK
    ) throws IOException {
        return service.searchByImage(file, classification, materialIds, colorIds, topK);
    }

    @PostMapping("/by-text")
    public SearchResponse searchByText(@RequestBody SearchByTextRequest request) {
        return service.searchByText(
                request.text(),
                request.classification(),
                request.materialIds(),
                request.colorIds(),
                request.topK()
        );
    }

    public record SearchByTextRequest(
            String text,
            String classification,
            List<Long> materialIds,
            List<Long> colorIds,
            Integer topK
    ) {}
}
