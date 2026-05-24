package com.msa.jewelry.product.api;

/**
 * 다른 모듈이 분류(Classification) 정보를 조회할 때 사용하는 공개 API.
 */
public interface ClassificationFinder {

    /**
     * 분류 ID 로 이름 조회.
     * @throws com.msa.jewelry.shared.exception.NotFoundException 미존재 시
     */
    String getClassificationName(Long classificationId);
}
