package com.msa.jewelry.product.api;

import java.util.List;
import java.util.Map;

/**
 * 다른 모듈이 상품 정보를 조회/수정할 때 사용하는 공개 API.
 *
 * <p>2026-05 모놀로식 통합 시 신설. 기존 MSA 의
 * {@code ProductFeignClient} / {@code ProductClient}(Feign wrapper) 호출들의
 * 동기 등가물을 모두 본 인터페이스로 흡수한다.
 */
public interface ProductFinder {

    /**
     * 마스터 데이터 기반 단순 view (이름, 분류, 세트, 기준무게).
     * @throws com.msa.jewelry.shared.exception.NotFoundException 미존재 시
     */
    ProductView getProduct(Long productId);

    /**
     * 상품명 기준 단순 view 조회 (없으면 예외).
     */
    ProductView findProductByName(String productName);

    /**
     * 등급별 가격(purchaseCost/laborCost) 포함 상세 조회.
     *
     * <p>기존 ProductClient.getProductInfo(token, productId, grade) 의 동기 등가물.
     *
     * @param productId 상품 ID
     * @param grade     등급 (WorkGrade.level 문자열, 예: "A", "B")
     */
    ProductDetailView getProductDetail(Long productId, String grade);

    /**
     * 이름 기반 상세 조회 (가격 정보는 null 가능).
     *
     * <p>기존 ProductClient.getProductInfoByName 의 동기 등가물.
     * 없으면 null 반환 (예외 미발생) — 기존 Feign wrapper 동작과 일관.
     */
    ProductDetailView findProductDetailByName(String productName);

    /**
     * 상품명 기준 스톤 매핑 목록 조회 (마이그레이션·조회용).
     *
     * <p>기존 ProductClient.getProductStonesByName 의 동기 등가물.
     */
    List<ProductStoneView> getProductStonesByName(String productName);

    /**
     * 다건 productId 에 대한 메인 이미지 일괄 조회.
     *
     * <p>기존 ProductClient.getProductImages 의 동기 등가물.
     * 반환 Map 의 key 는 productId, value 는 {@link ProductImageView}.
     * productId 에 해당하는 이미지가 없으면 Map 에 키 자체가 존재하지 않을 수 있다.
     */
    Map<Long, ProductImageView> getProductImages(List<Long> productIds);

    /**
     * 상품의 제조사 이름 필드를 갱신한다.
     *
     * <p>기존 ProductClient.updateProductFactoryName 의 동기 등가물.
     */
    void updateProductFactoryName(Long productId, String productFactoryName);
}
