package com.msa.jewelry.account.api;

/**
 * 다른 모듈이 제조사 정보를 조회할 때 사용하는 공개 API.
 * (기존 ProductFeignClient.getFactoryInfo 등의 동기 등가물)
 */
public interface FactoryFinder {

    FactoryView getFactoryInfo(Long factoryId);

    FactoryView findFactoryByName(String factoryName);
}
