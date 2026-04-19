package com.msa.product.local.catalog.service;

import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.product.global.excel.dto.CatalogExcelDto;
import com.msa.product.global.excel.util.CatalogExcelUtil;
import com.msa.product.global.feign_client.client.StockClient;
import com.msa.product.local.catalog.dto.CatalogProductDto;
import com.msa.product.local.catalog.repository.CatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 카탈로그 서비스 (판매처 전용)
 * 가격 정보가 제외된 상품 정보를 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogService {

    private final CatalogRepository catalogRepository;
    private final AuthorityUserRoleUtil authorityUserRoleUtil;
    private final StockClient stockClient;

    /**
     * 판매처 권한 검증
     */
    private void validateStoreAccess(String accessToken) {
        if (!authorityUserRoleUtil.isStore(accessToken)) {
            throw new IllegalArgumentException("판매처 계정만 접근 가능합니다.");
        }
    }

    /**
     * 카탈로그 상품 목록 조회
     */
    public CustomPage<CatalogProductDto.Page> getCatalogProducts(
            String accessToken,
            String productName,
            String classificationId,
            String setTypeId,
            String sortField,
            String sort,
            Pageable pageable) {

        validateStoreAccess(accessToken);

        CustomPage<CatalogProductDto.Page> page = catalogRepository.findCatalogProducts(
                productName, classificationId, setTypeId,
                sortField, sort, pageable
        );

        // 재고 수량 조회 후 설정
        List<String> productNames = page.getContent().stream()
                .map(CatalogProductDto.Page::getProductName)
                .toList();
        if (!productNames.isEmpty()) {
            Map<String, Integer> stockCounts = stockClient.getStockCountByProductNames(accessToken, productNames);
            page.getContent().forEach(p ->
                    p.setStockCount(stockCounts.getOrDefault(p.getProductName(), 0))
            );
        }

        return page;
    }

    /**
     * 카탈로그 상품 상세 조회
     */
    public CatalogProductDto.Detail getCatalogProductDetail(String accessToken, Long productId) {
        validateStoreAccess(accessToken);

        CatalogProductDto.Detail detail = catalogRepository.findCatalogProductDetail(productId);

        if (detail == null) {
            throw new IllegalArgumentException("상품을 찾을 수 없습니다.");
        }

        return detail;
    }

    /**
     * 관련 상품 조회
     */
    public List<CatalogProductDto.RelatedProduct> getRelatedProducts(
            String accessToken, Long productId, String relatedNumber) {

        validateStoreAccess(accessToken);

        return catalogRepository.findRelatedProducts(productId, relatedNumber);
    }

    /**
     * 카탈로그 상품 엑셀 다운로드
     */
    public byte[] getCatalogProductsExcel(String accessToken, String productName,
                                          String classificationId, String setTypeId) throws IOException {
        validateStoreAccess(accessToken);

        List<CatalogExcelDto> catalogExcelDtos = catalogRepository.findCatalogProductsForExcel(
                productName, classificationId, setTypeId
        );

        return CatalogExcelUtil.createCatalogWorkSheet(catalogExcelDtos);
    }
}
