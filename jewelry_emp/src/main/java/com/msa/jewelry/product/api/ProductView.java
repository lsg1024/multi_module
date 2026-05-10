package com.msa.jewelry.product.api;

import java.math.BigDecimal;

/**
 * 상품 view DTO.
 *
 * <p>실제 Product 엔티티에는 color / goldWeight / productLaborCost 필드가 없으며
 * (Product 는 카탈로그 마스터, 가격·무게는 ProductWorkGradePolicy 와 ProductStone 에 분산),
 * 따라서 view 도 핵심 식별자와 마스터 데이터만 노출한다.
 *
 * @param productId           상품 ID
 * @param productName         상품명
 * @param materialName        재질명 (Product.material → Material.materialName)
 * @param classificationName  분류명
 * @param setTypeName         세트 타입
 * @param factoryId           제조사 ID
 * @param factoryName         제조사명
 * @param standardWeight      기준 무게 (Product.standardWeight)
 */
public record ProductView(
        Long productId,
        String productName,
        String materialName,
        String classificationName,
        String setTypeName,
        Long factoryId,
        String factoryName,
        BigDecimal standardWeight
) {
}
