package com.msa.product.local.catalog.repository;

import com.msa.common.global.util.CustomPage;
import com.msa.product.global.excel.dto.CatalogExcelDto;
import com.msa.product.local.catalog.dto.CatalogProductDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 카탈로그 전용 Repository (가격 정보 제외)
 */
public interface CatalogRepository {

    /**
     * 상품 목록 조회 (가격/제조사 정보 제외)
     */
    CustomPage<CatalogProductDto.Page> findCatalogProducts(
            String productName,
            String classificationId,
            String setTypeId,
            String sortField,
            String sort,
            Pageable pageable
    );

    /**
     * 상품 상세 조회 (가격 정보 제외)
     */
    CatalogProductDto.Detail findCatalogProductDetail(Long productId);

    /**
     * 관련 상품 조회
     */
    List<CatalogProductDto.RelatedProduct> findRelatedProducts(Long productId, String relatedNumber);

    /**
     * 엑셀 다운로드용 상품 목록 조회
     */
    List<CatalogExcelDto> findCatalogProductsForExcel(String productName, String classificationId, String setTypeId);
}
