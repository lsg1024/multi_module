package com.msa.jewelry.account.internal.store.repository;

import com.msa.jewelry.account.internal.global.domain.dto.AccountDto;
import com.msa.jewelry.account.internal.global.excel.dto.AccountExcelDto;
import com.msa.jewelry.account.internal.global.excel.dto.ReceivableExcelDto;
import com.msa.jewelry.account.internal.store.domain.dto.StoreDto;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CustomStoreRepository {

    Optional<AccountDto.AccountSingleResponse> findByStoreId(Long storeId);
    CustomPage<StoreDto.StoreResponse> findAllStore(String name, String searchField, String sortField, String sortOrder, Pageable pageable);

    CustomPage<AccountDto.AccountResponse> findAllStoreAndReceivable(String name, String field, String sort, Pageable pageable);
    AccountDto.AccountResponse findByStoreIdAndReceivable(Long storeId);
    AccountDto.AccountSaleLogResponse findByStoreIdAndReceivableByLog(Long storeId);
    List<AccountExcelDto> findAllStoreExcel();
    List<ReceivableExcelDto> findAllReceivableExcel(String name);
}
