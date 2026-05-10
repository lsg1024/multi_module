package com.msa.jewelry.account.internal.api_impl;

import com.msa.jewelry.account.api.StoreFinder;
import com.msa.jewelry.account.api.StoreView;
import com.msa.jewelry.account.internal.store.domain.entity.Store;
import com.msa.jewelry.account.internal.store.repository.StoreRepository;
import com.msa.jewelry.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link StoreFinder} 의 같은 JVM 동기 구현체.
 *
 * <p>기존 MSA 의 {@code com.msa.jewelry.order.internal.global.feign_client.client.StoreClient}
 * 를 대체. 다른 모듈(order, product 등)이 거래처 정보를 조회할 때 이 빈을 주입받아 사용.
 *
 * <p>차이점:
 * <ul>
 *   <li>Feign 호출 → 직접 Repository 조회</li>
 *   <li>토큰 헤더 / FeignException / fallback 모두 제거</li>
 *   <li>네트워크 latency 제거</li>
 *   <li>호출자 트랜잭션에 자연 합류 (REQUIRED 전파)</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreFinderImpl implements StoreFinder {

    private final StoreRepository storeRepository;

    @Override
    public StoreView getStoreInfo(Long storeId) {
        Store store = storeRepository.findWithAllOptionsById(storeId)
                .orElseThrow(() -> new NotFoundException("거래처 미존재: storeId=" + storeId));
        return toView(store);
    }

    @Override
    public StoreView findStoreByName(String storeName) {
        return storeRepository.findByStoreNameIgnoreCase(storeName)
                .stream()
                .findFirst()
                .map(StoreFinderImpl::toView)
                .orElse(null);
    }

    private static StoreView toView(Store entity) {
        // CommonOption.goldHarryLoss 는 String 으로 저장됨.
        String harry = entity.getCommonOption() != null
                ? entity.getCommonOption().getGoldHarryLoss()
                : null;
        String tradeType = entity.getCommonOption() != null
                && entity.getCommonOption().getOptionTradeType() != null
                ? entity.getCommonOption().getOptionTradeType().name()
                : null;
        String grade = entity.getCommonOption() != null
                && entity.getCommonOption().getOptionLevel() != null
                ? entity.getCommonOption().getOptionLevel().name()
                : null;
        // AdditionalOption.optionApplyPastSales 는 boolean → Lombok 이 isOptionApplyPastSales() 생성.
        boolean applyPast = entity.getAdditionalOption() != null
                && entity.getAdditionalOption().isOptionApplyPastSales();
        return new StoreView(
                entity.getStoreId(),
                entity.getStoreName(),
                grade,
                harry,
                tradeType,
                applyPast
        );
    }
}
