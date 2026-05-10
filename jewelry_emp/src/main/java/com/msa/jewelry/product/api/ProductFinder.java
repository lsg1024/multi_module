package com.msa.jewelry.product.api;

/**
 * 다른 모듈이 상품 정보를 조회할 때 사용하는 공개 API.
 */
public interface ProductFinder {

    ProductView getProduct(Long productId);

    ProductView findProductByName(String productName);
}
