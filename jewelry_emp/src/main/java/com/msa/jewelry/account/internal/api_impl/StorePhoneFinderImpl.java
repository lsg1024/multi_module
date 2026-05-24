package com.msa.jewelry.account.internal.api_impl;

import com.msa.jewelry.account.api.StorePhoneFinder;
import com.msa.jewelry.account.api.StorePhoneView;
import com.msa.jewelry.account.internal.store.domain.dto.StoreDto;
import com.msa.jewelry.account.internal.store.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@link StorePhoneFinder} 의 같은 JVM 동기 구현체.
 *
 * <p>2026-05 P2-4 단계 신설. 기존 user 모듈의 {@code MessageService} 가
 * {@code AccountClient(Feign).getStorePhones(...)} 로 호출하던 경로를 본 빈으로 대체.
 * 내부적으로는 {@link StoreService#getStorePhones} 를 위임하고 결과를
 * 외부 노출용 {@link StorePhoneView} 로 매핑한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorePhoneFinderImpl implements StorePhoneFinder {

    private final StoreService storeService;

    @Override
    public List<StorePhoneView> getStorePhones(List<Long> storeIds) {
        List<StoreDto.StorePhoneInfo> phones = storeService.getStorePhones(storeIds);
        return phones.stream()
                .map(p -> new StorePhoneView(
                        p.getStoreId(),
                        p.getStoreName(),
                        p.getStorePhoneNumber()
                ))
                .toList();
    }
}
