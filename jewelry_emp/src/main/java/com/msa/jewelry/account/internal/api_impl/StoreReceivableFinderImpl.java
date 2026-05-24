package com.msa.jewelry.account.internal.api_impl;

import com.msa.jewelry.account.api.StoreReceivableFinder;
import com.msa.jewelry.account.api.StoreReceivableLogView;
import com.msa.jewelry.account.internal.global.domain.dto.AccountDto;
import com.msa.jewelry.account.internal.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link StoreReceivableFinder} 의 같은 JVM 동기 구현체.
 *
 * <p>2026-05 P2-1c 단계 신설. 기존 SaleService 가
 * {@code storeClient.getStoreReceivableDetailLog(token, storeId, saleCode)} 로 호출하던 경로를
 * 본 빈으로 대체. 내부적으로는 모놀로식 내 흡수된 {@link StoreService#getStoreReceivableLogDetail}
 * 을 위임하고 5개 필드만 추려서 {@link StoreReceivableLogView} 로 변환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreReceivableFinderImpl implements StoreReceivableFinder {

    private final StoreService storeService;

    @Override
    public StoreReceivableLogView getReceivableLog(Long storeId, String saleCode) {
        AccountDto.AccountSaleLogResponse log =
                storeService.getStoreReceivableLogDetail(String.valueOf(storeId), saleCode);

        return new StoreReceivableLogView(
                log.getPreviousGoldBalance(),
                log.getPreviousMoneyBalance(),
                log.getAfterGoldBalance(),
                log.getAfterMoneyBalance(),
                log.getLastSaleDate()
        );
    }
}
