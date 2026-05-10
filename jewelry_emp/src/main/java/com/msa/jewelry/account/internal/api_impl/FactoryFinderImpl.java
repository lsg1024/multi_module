package com.msa.jewelry.account.internal.api_impl;

import com.msa.jewelry.account.api.FactoryFinder;
import com.msa.jewelry.account.api.FactoryView;
import com.msa.jewelry.account.internal.factory.domain.entity.Factory;
import com.msa.jewelry.account.internal.factory.repository.FactoryRepository;
import com.msa.jewelry.shared.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link FactoryFinder} 의 같은 JVM 동기 구현체.
 *
 * <p>기존 ProductFeignClient.getFactoryInfo / OrderFeignClient.getFactoryInfo 를 대체.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FactoryFinderImpl implements FactoryFinder {

    private final FactoryRepository factoryRepository;

    @Override
    public FactoryView getFactoryInfo(Long factoryId) {
        Factory factory = factoryRepository.findWithAllOptionById(factoryId)
                .orElseThrow(() -> new NotFoundException("제조사 미존재: factoryId=" + factoryId));
        return toView(factory);
    }

    @Override
    public FactoryView findFactoryByName(String factoryName) {
        return factoryRepository.findByFactoryNameIgnoreCase(factoryName)
                .stream()
                .findFirst()
                .map(FactoryFinderImpl::toView)
                .orElse(null);
    }

    private static FactoryView toView(Factory entity) {
        // CommonOption.goldHarryLoss 는 String 으로 저장됨.
        String harry = entity.getCommonOption() != null
                ? entity.getCommonOption().getGoldHarryLoss()
                : null;
        String grade = entity.getCommonOption() != null
                && entity.getCommonOption().getOptionLevel() != null
                ? entity.getCommonOption().getOptionLevel().name()
                : null;
        return new FactoryView(
                entity.getFactoryId(),
                entity.getFactoryName(),
                grade,
                harry
        );
    }
}
