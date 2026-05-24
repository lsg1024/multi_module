package com.msa.jewelry.account.api;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 다른 모듈(주로 order 의 SaleService 영수증 출력)이 판매 시점 미수금
 * 잔액 변화를 조회할 때 사용하는 공개 API.
 *
 * <p>기존 MSA 의 {@code AccountFeignClient.getStoreReceivableDetailLog} 의 동기 등가물.
 */
@Schema(description = "거래처 판매 시점 미수금 잔액 변화 조회 공개 API — order 모듈 영수증 출력 시 사용")
public interface StoreReceivableFinder {

    /**
     * 특정 거래처의 특정 판매 시점 잔액 변화 로그를 조회한다.
     *
     * @param storeId  거래처 ID
     * @param saleCode 판매 세션 코드
     * @return 잔액 변화 view (없을 수도 있으나 일반적으로 미수금 거래라면 존재)
     * @throws com.msa.jewelry.shared.exception.NotFoundException 거래처가 없을 때
     */
    StoreReceivableLogView getReceivableLog(Long storeId, String saleCode);
}
