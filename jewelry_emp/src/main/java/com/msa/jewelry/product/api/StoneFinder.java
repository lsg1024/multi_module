package com.msa.jewelry.product.api;

/**
 * 다른 모듈이 스톤(Stone) 정보를 조회할 때 사용하는 공개 API.
 */
public interface StoneFinder {

    /**
     * 주어진 stoneId 가 존재하는지 검사.
     */
    boolean existsStoneId(Long stoneId);
}
