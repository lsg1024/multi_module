package com.msa.jewelry.product.internal.api_impl;

import com.msa.jewelry.product.api.StoneFinder;
import com.msa.jewelry.product.internal.stone.stone.repository.StoneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoneFinderImpl implements StoneFinder {

    private final StoneRepository stoneRepository;

    @Override
    public boolean existsStoneId(Long stoneId) {
        return stoneRepository.existsByStoneId(stoneId);
    }
}
