package com.msa.account.domain.factory.repository;

import com.msa.account.domain.factory.dto.FactoryDto;
import com.msacommon.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CustomFactoryRepository {
    Optional<FactoryDto.FactorySingleResponse> findByFactoryId(Long factoryId);
    CustomPage<FactoryDto.FactoryResponse> findAllFactory(Pageable pageable);
}
