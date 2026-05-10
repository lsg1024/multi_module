package com.msa.jewelry.account.internal.factory.repository;

import com.msa.jewelry.account.internal.global.domain.dto.AccountDto;
import com.msa.jewelry.account.internal.global.excel.dto.AccountExcelDto;
import com.msa.jewelry.account.internal.global.excel.dto.PurchaseExcelDto;
import com.msa.jewelry.account.internal.factory.domain.dto.FactoryDto;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface CustomFactoryRepository {
    Optional<AccountDto.AccountSingleResponse> findByFactoryId(Long factoryId);
    CustomPage<FactoryDto.FactoryResponse> findAllFactory(String name, String searchField, String sortField, String sortOrder, Pageable pageable);
    List<AccountExcelDto>  findAllFactoryExcel();
    List<FactoryDto.ApiFactoryInfo> findAllFactory();
    CustomPage<AccountDto.AccountResponse> findAllFactoryAndPurchase(String endAt, Pageable pageable);
    List<PurchaseExcelDto> findAllPurchaseExcel(String endAt);
//    AccountDto.AccountResponse findByFactoryIdAndPurchase(Long factoryId);
}
