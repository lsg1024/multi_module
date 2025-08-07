package com.msa.account.local.store.repository;

import com.msa.account.local.store.dto.StoreDto;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CustomStoreRepository {

    Optional<StoreDto.StoreSingleResponse> findByStoreId(Long storeId);
    CustomPage<StoreDto.StoreResponse> findAllStore(Pageable pageable);
}
