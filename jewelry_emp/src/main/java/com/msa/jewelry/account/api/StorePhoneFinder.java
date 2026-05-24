package com.msa.jewelry.account.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 다른 모듈(주로 user 의 SMS 발송 서비스)이 거래처 전화번호를 일괄 조회할 때
 * 사용하는 공개 API.
 *
 * <p>기존 MSA 의 {@code AccountClient.getStorePhones(token, storeIds)} 의 동기 등가물.
 */
@Schema(description = "거래처 전화번호 일괄 조회 공개 API — SMS 발송 시 user 모듈에서 사용")
public interface StorePhoneFinder {

    /**
     * 주어진 storeId 목록에 대한 전화번호 view 일괄 조회.
     */
    List<StorePhoneView> getStorePhones(List<Long> storeIds);
}
