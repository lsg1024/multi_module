package com.msa.jewelry.local.factory.service;

import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.dto.AccountDto;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.global.excel.dto.AccountExcelDto;
import com.msa.jewelry.local.factory.dto.FactoryDto;
import com.msa.jewelry.local.factory.dto.FactoryView;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public interface FactoryService {

    AccountDto.RecentActivityResponse getFactoryRecentActivity(Long factoryId, int limit);

    AccountDto.AccountSingleResponse getFactoryInfo(String factoryId);

    CustomPage<FactoryDto.FactoryResponse> getFactoryList(String name, String searchField, String sortField, String sortOrder, Pageable pageable);

    void createFactory(FactoryDto.FactoryRequest factoryInfo) throws NotFoundException;

    void updateFactory(String token, String factoryId, AccountDto.AccountUpdate updateInfo);

    void deleteFactory(String token, String factoryId);

    FactoryDto.ApiFactoryInfo getFactoryIdAndName(Long id);

    String getFactoryGrade(String storeId);

    void updateFactoryHarry(String accessToken, String factoryId, String harryId);

    void updateFactoryGrade(String accessToken, String factoryId, String grade);

    List<AccountExcelDto> getExcel(String accessToken);

    FactoryDto.ApiFactoryInfo getFactoryInfoByName(String factoryName);

    List<FactoryDto.ApiFactoryInfo> findAllFactory();

    CustomPage<AccountDto.AccountResponse> getFactoryPurchase(String endAt, Pageable pageable);

    byte[] getPurchaseExcel(String accessToken, String endAt) throws IOException;

    FactoryView getFactoryInfo(Long factoryId);

    FactoryView findFactoryByName(String factoryName);

    List<FactoryView> findAllFactoryView();

    void applyDelta(Long factoryId, BigDecimal goldDelta, Long moneyDelta, String eventId,
                    String transactionType, String material, Long accountSaleCode, String note);
}
