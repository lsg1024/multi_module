package com.msa.jewelry.account.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 다른 모듈(order, product 등)이 거래처 정보를 조회할 때 사용하는 공개 API.
 *
 * <p>기존 MSA 시절 {@code AccountFeignClient} + {@code StoreClient} 의
 * 동기 등가물. 같은 JVM 안이므로 토큰 전달이나 Circuit Breaker 가 필요 없으며,
 * 호출 실패는 단순 예외로 전파된다.
 *
 * <p>구현체는 {@code com.msa.jewelry.account.internal} 패키지에 위치하며
 * 다른 모듈에서 직접 import 가 금지된다 (Spring Modulith 가 검증).
 */
@Schema(description = "거래처(매장) 정보 조회 공개 API — 다른 모듈에서 단건/이름으로 조회 시 사용")
public interface StoreFinder {

    /**
     * 거래처 ID 로 거래처 정보를 조회한다.
     *
     * @throws com.msa.jewelry.shared.exception.NotFoundException 해당 ID 의 거래처가 없을 때
     */
    StoreView getStoreInfo(Long storeId);

    /**
     * 거래처 이름으로 정확히 일치하는 거래처를 조회한다.
     *
     * <p>존재하지 않으면 {@code null} 반환 (예외 미발생).
     */
    StoreView findStoreByName(String storeName);
}
