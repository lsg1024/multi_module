package com.msa.account.local.factory.repository;

import com.msa.account.local.factory.domain.dto.FactoryDto;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CustomFactoryRepository {
    Optional<FactoryDto.FactorySingleResponse> findByFactoryId(Long factoryId);
    CustomPage<FactoryDto.FactoryResponse> findAllFactory(String name, Pageable pageable);
}
