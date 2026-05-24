package com.msa.jewelry.product.api;

/**
 * 다른 모듈이 세트 타입(SetType) 정보를 조회할 때 사용하는 공개 API.
 */
public interface SetTypeFinder {

    /**
     * 세트 타입 ID 로 이름 조회.
     * @throws com.msa.jewelry.shared.exception.NotFoundException 미존재 시
     */
    String getSetTypeName(Long setTypeId);
}
