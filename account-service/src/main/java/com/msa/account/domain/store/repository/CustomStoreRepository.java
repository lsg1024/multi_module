package com.msa.account.domain.store.repository;

import com.msa.account.domain.store.dto.StoreDto;
import com.msa.account.global.domain.dto.AccountDto;
import com.msacommon.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface CustomStoreRepository {

    Optional<StoreDto.StoreSingleResponse> findByStoreId(Long storeId);
    CustomPage<StoreDto.StoreResponse> findAllStore(Pageable pageable);
}
