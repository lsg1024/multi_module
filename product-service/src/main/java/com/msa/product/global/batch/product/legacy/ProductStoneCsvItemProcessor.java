package com.msa.product.global.batch.product.legacy;

import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.entity.ProductStone;
import com.msa.product.local.product.repository.ProductRepository;
import com.msa.product.local.product.repository.stone.ProductStoneRepository;
import com.msa.product.local.stone.stone.entity.Stone;
import com.msa.product.local.stone.stone.repository.StoneRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 레거시 모델별 스톤정보 CSV → ProductStone 엔티티 변환 프로세서.
 *
 * 처리 로직:
 * 1. "소계" 행은 skip
 * 2. 모델번호(B)로 Product 조회 → 없으면 실패
 * 3. 스톤종류(D)로 Stone 조회 → 없으면 실패
 * 4. ProductStone 생성 후 Product에 연결
 */
@Slf4j
public class ProductStoneCsvItemProcessor implements ItemProcessor<ProductStoneCsvRow, ProductStone> {

    private final ProductRepository productRepository;
    private final StoneRepository stoneRepository;
    private final ProductStoneRepository productStoneRepository;
    private final StoneMigrationFailureCollector failureCollector;

    // 캐시
    private final Map<String, Product> productCache = new HashMap<>();
    private final Map<String, Optional<Stone>> stoneCache = new HashMap<>();

    public ProductStoneCsvItemProcessor(
            ProductRepository productRepository,
            StoneRepository stoneRepository,
            ProductStoneRepository productStoneRepository,
            StoneMigrationFailureCollector failureCollector) {
        this.productRepository = productRepository;
        this.stoneRepository = stoneRepository;
        this.productStoneRepository = productStoneRepository;
        this.failureCollector = failureCollector;
    }

    @Override
    public ProductStone process(ProductStoneCsvRow row) {
        try {
            String modelNumber = trim(row.getModelNumber());

            // 1. "소계" 행 skip (모델번호에 "소계"가 포함되거나 순서가 비어있고 모델번호에 "소계" 포함)
            if (!StringUtils.hasText(modelNumber) || modelNumber.contains("소계")) {
                return null;
            }

            // 순서 값이 비어있으면 소계 행일 수 있음
            if (!StringUtils.hasText(trim(row.getNo()))) {
                return null;
            }

            // 2. Product 조회 (모델번호 = productName)
            Product product = lookupProduct(modelNumber);
            if (product == null) {
                failureCollector.add(row, "DB에 존재하지 않는 상품(모델번호): " + modelNumber);
                return null;
            }

            // 3. Stone 조회 (스톤종류 = stoneName)
            String stoneName = trim(row.getStoneName());
            if (!StringUtils.hasText(stoneName)) {
                // 스톤종류가 비어있으면 스톤비고 값을 상품 비고(productNote)에 추가
                String stoneNote = trim(row.getStoneNote());
                if (StringUtils.hasText(stoneNote)) {
                    product.appendProductNote(stoneNote);
                    productRepository.save(product);
                    log.info("스톤종류 없음 - 상품 [{}] 비고에 추가: {}", modelNumber, stoneNote);
                }
                return null;
            }

            Optional<Stone> stoneOpt = lookupStone(stoneName);
            if (stoneOpt.isEmpty()) {
                failureCollector.add(row, "DB에 존재하지 않는 스톤: " + stoneName);
                return null;
            }

            // 4. 필드 매핑
            boolean mainStone = "메인".equals(trim(row.getMainStone()));
            boolean includeQuantity = !"N".equalsIgnoreCase(trim(row.getIncludeQuantity()));
            boolean includeStone = "Y".equalsIgnoreCase(trim(row.getIncludeStone()));
            boolean includePrice = "Y".equalsIgnoreCase(trim(row.getIncludePrice()));
            Integer stoneQuantity = parseInteger(row.getStoneQuantity());
            String stoneNote = trim(row.getStoneNote());

            Stone stone = stoneOpt.get();

            // 5. 기존 ProductStone이 있으면 includeQuantity 갱신, 없으면 신규 생성
            Optional<ProductStone> existingOpt = productStoneRepository
                    .findByProductProductIdAndStoneStoneId(product.getProductId(), stone.getStoneId());

            if (existingOpt.isPresent()) {
                ProductStone existing = existingOpt.get();
                existing.updateIncludeQuantity(includeQuantity);
                return existing;
            }

            ProductStone productStone = ProductStone.builder()
                    .product(product)
                    .stone(stone)
                    .mainStone(mainStone)
                    .includeQuantity(includeQuantity)
                    .includeStone(includeStone)
                    .includePrice(includePrice)
                    .stoneQuantity(stoneQuantity)
                    .productStoneNote(stoneNote)
                    .build();

            return productStone;

        } catch (Exception e) {
            log.error("스톤 마이그레이션 처리 실패 - 모델번호: {}", row.getModelNumber(), e);
            failureCollector.add(row, "처리 오류: " + e.getMessage());
            return null;
        }
    }

    // ── 캐시 조회 ──

    private Product lookupProduct(String productName) {
        return productCache.computeIfAbsent(productName,
                k -> productRepository.findByProductName(k).orElse(null));
    }

    private Optional<Stone> lookupStone(String stoneName) {
        return stoneCache.computeIfAbsent(stoneName.toLowerCase(),
                k -> stoneRepository.findByStoneNameIgnoreCase(stoneName));
    }

    // ── 유틸리티 ──

    private String trim(String value) {
        return value != null ? value.trim() : null;
    }

    private Integer parseInteger(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            String cleaned = value.replaceAll("[,\"' ]", "").trim();
            if (cleaned.isEmpty()) return null;
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
