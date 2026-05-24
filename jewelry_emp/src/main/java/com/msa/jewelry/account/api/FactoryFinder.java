package com.msa.jewelry.account.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 다른 모듈이 제조사 정보를 조회할 때 사용하는 공개 API.
 * (기존 ProductFeignClient.getFactoryInfo / FactoryClient.getFactories 등의 동기 등가물)
 */
@Schema(description = "제조사 정보 조회 공개 API — 다른 모듈에서 제조사 단건/이름/전체 조회 시 사용")
public interface FactoryFinder {

    /**
     * 제조사 ID 로 단건 조회.
     * @throws com.msa.jewelry.shared.exception.NotFoundException 미존재 시
     */
    FactoryView getFactoryInfo(Long factoryId);

    /**
     * 제조사 이름으로 정확히 일치하는 항목 조회 (없으면 null).
     */
    FactoryView findFactoryByName(String factoryName);

    /**
     * 모든 제조사 목록 조회 (배치 캐싱 등에서 사용).
     *
     * <p>기존 FactoryClient.getFactories(token) 의 동기 등가물.
     * 운영 데이터 규모가 수천 건을 넘어가면 페이지네이션 신설 고려.
     */
    List<FactoryView> findAll();
}
