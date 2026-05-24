package com.msa.jewelry.local.store.service;

import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.dto.AccountDto;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.global.excel.dto.AccountExcelDto;
import com.msa.jewelry.local.store.dto.StoreDto;
import com.msa.jewelry.local.store.dto.StorePhoneView;
import com.msa.jewelry.local.store.dto.StoreReceivableLogView;
import com.msa.jewelry.local.store.dto.StoreView;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

public interface StoreService {

    AccountDto.RecentActivityResponse getStoreRecentActivity(Long storeId, int limit);

    AccountDto.AccountSingleResponse getStoreInfo(String storeId);

    CustomPage<StoreDto.StoreResponse> getStoreList(String name, String searchField, String sortField, String sortOrder, Pageable pageable);

    CustomPage<AccountDto.AccountResponse> getStoreReceivable(String name, String field, String sort, Pageable pageable);

    AccountDto.AccountResponse getStoreReceivableDetail(String storeId);

    AccountDto.AccountSaleLogResponse getStoreReceivableLogDetail(String storeId, String saleCode);

    void createStore(StoreDto.StoreRequest storeInfo);

    void updateStore(String token, String storeId, AccountDto.AccountUpdate updateInfo);

    void deleteStore(String token, String storeId);

    StoreDto.ApiStoreInfo getStoreInfo(Long id);

    String getStoreGrade(String storeId);

    void updateStoreHarry(String accessToken, String storeId, String harryId);

    void updateStoreGrade(String accessToken, String storeId, String grade);

    List<AccountExcelDto> getExcel(String accessToken);

    List<StoreDto.StorePhoneInfo> getStorePhones(List<Long> storeIds);

    StoreDto.ApiStoreInfo getStoreInfoByName(String storeName);

    byte[] getReceivableExcel(String accessToken, String name) throws IOException;

    StoreView getStoreInfoView(Long storeId);

    StoreView findStoreByNameView(String storeName);

    List<StorePhoneView> getStorePhonesView(List<Long> storeIds);

    StoreReceivableLogView getReceivableLog(Long storeId, String saleCode);

    void applyDelta(Long storeId, BigDecimal goldDelta, Long moneyDelta, String eventId,
                    String transactionType, String material, Long accountSaleCode, String note);
}
