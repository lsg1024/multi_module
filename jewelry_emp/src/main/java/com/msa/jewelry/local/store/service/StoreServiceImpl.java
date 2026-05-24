package com.msa.jewelry.local.store.service;

import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.dto.AccountDto;
import com.msa.jewelry.global.excel.dto.AccountExcelDto;
import com.msa.jewelry.global.excel.dto.ReceivableExcelDto;
import com.msa.jewelry.global.excel.util.ReceivableExcelUtil;
import com.msa.jewelry.global.exception.ExceptionMessage;
import com.msa.jewelry.global.exception.NotAuthorityException;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.common_option.entity.OptionLevel;
import com.msa.jewelry.local.goldharry.entity.GoldHarry;
import com.msa.jewelry.local.goldharry.repository.GoldHarryRepository;
import com.msa.jewelry.local.store.dto.StoreDto;
import com.msa.jewelry.local.store.dto.StorePhoneView;
import com.msa.jewelry.local.store.dto.StoreReceivableLogView;
import com.msa.jewelry.local.store.dto.StoreView;
import com.msa.jewelry.local.store.entity.Store;
import com.msa.jewelry.local.store.repository.StoreRepository;
import com.msa.jewelry.local.transaction_history.entity.SaleLog;
import com.msa.jewelry.local.transaction_history.entity.TransactionHistory;
import com.msa.jewelry.local.transaction_history.repository.SaleLogRepository;
import com.msa.jewelry.local.transaction_history.repository.TransactionHistoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import static com.msa.jewelry.global.exception.ExceptionMessage.*;

@Slf4j
@Service
@Transactional
public class StoreServiceImpl implements StoreService {

    private final AuthorityUserRoleUtil authorityUserRoleUtil;
    private final StoreRepository storeRepository;
    private final SaleLogRepository saleLogRepository;
    private final GoldHarryRepository goldHarryRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    public StoreServiceImpl(AuthorityUserRoleUtil authorityUserRoleUtil,
                            StoreRepository storeRepository,
                            SaleLogRepository saleLogRepository,
                            GoldHarryRepository goldHarryRepository,
                            TransactionHistoryRepository transactionHistoryRepository) {
        this.authorityUserRoleUtil = authorityUserRoleUtil;
        this.storeRepository = storeRepository;
        this.saleLogRepository = saleLogRepository;
        this.goldHarryRepository = goldHarryRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto.RecentActivityResponse getStoreRecentActivity(Long storeId, int limit) {
        return new AccountDto.RecentActivityResponse(
                transactionHistoryRepository.findRecentSalesByStore(storeId, limit),
                transactionHistoryRepository.findPaymentSummaryByStore(storeId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto.AccountSingleResponse getStoreInfo(String storeId) {
        return storeRepository.findByStoreId(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.NOT_FOUND_STORE));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomPage<StoreDto.StoreResponse> getStoreList(String name, String searchField, String sortField, String sortOrder, Pageable pageable) {
        return storeRepository.findAllStore(name, searchField, sortField, sortOrder, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomPage<AccountDto.AccountResponse> getStoreReceivable(String name, String field, String sort, Pageable pageable) {
        return storeRepository.findAllStoreAndReceivable(name, field, sort, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto.AccountResponse getStoreReceivableDetail(String storeId) {
        return storeRepository.findByStoreIdAndReceivable(Long.valueOf(storeId));
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto.AccountSaleLogResponse getStoreReceivableLogDetail(String storeId, String saleCode) {
        Long transformStoreId = Long.valueOf(storeId);
        Long transformSaleCode = Long.valueOf(saleCode);
        AccountDto.AccountSaleLogResponse storeAttempt = storeRepository.findByStoreIdAndReceivableByLog(transformStoreId);

        SaleLog firstLog = saleLogRepository.findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateAsc(transformSaleCode, transformStoreId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND + " Start Log " + saleCode));
        SaleLog lastLog = saleLogRepository.findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateDesc(transformSaleCode, transformStoreId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND + " " + saleCode));

        storeAttempt.updateBalance(
                firstLog.getPreviousGoldBalance(),
                firstLog.getPreviousMoneyBalance(),
                lastLog.getAfterGoldBalance(),
                lastLog.getAfterMoneyBalance());
        return storeAttempt;
    }

    @Override
    public void createStore(StoreDto.StoreRequest storeInfo) {
        if (storeRepository.existsByStoreName(storeInfo.getAccountInfo().getAccountName())) {
            throw new NotFoundException(ALREADY_EXIST_STORE);
        }
        GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(storeInfo.getCommonOptionInfo().getGoldHarryId()))
                .orElseThrow(() -> new NotFoundException(WRONG_HARRY));
        Store newstore = storeInfo.toEntity(goldHarry);
        storeRepository.save(newstore);
    }

    @Override
    public void updateStore(String token, String storeId, AccountDto.AccountUpdate updateInfo) {
        Store store = storeRepository.findWithAllOptionsById(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));
        if (!authorityUserRoleUtil.verification(token)) {
            throw new NotAuthorityException(NO_ROLE);
        }
        AccountDto.AccountInfo storeInfo = updateInfo.getAccountInfo();
        if (store.isNameChanged(storeInfo.getAccountName())
                && storeRepository.existsByStoreName(storeInfo.getAccountName())) {
            throw new NotFoundException(ALREADY_EXIST_STORE);
        }
        GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(updateInfo.getCommonOptionInfo().getGoldHarryId()))
                .orElseThrow(() -> new NotFoundException(WRONG_HARRY));
        store.updateStoreInfo(updateInfo.getAccountInfo());
        store.updateCommonOption(updateInfo.getCommonOptionInfo(), goldHarry);
        if (updateInfo.getAdditionalOptionInfo() != null) {
            store.updateAdditionalOption(updateInfo.getAdditionalOptionInfo());
        }
        if (updateInfo.getAddressInfo() != null) {
            store.updateAddressInfo(updateInfo.getAddressInfo());
        }
    }

    @Override
    public void deleteStore(String token, String storeId) {
        Store store = storeRepository.findById(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));
        if (store.isStoreDefault()) {
            throw new IllegalArgumentException("기본 값은 삭제가 불가능 합니다.");
        }
        if (!authorityUserRoleUtil.verification(token)) {
            throw new NotAuthorityException(NO_ROLE);
        }
        storeRepository.delete(store);
    }

    @Override
    public StoreDto.ApiStoreInfo getStoreInfo(Long id) {
        Store store = storeRepository.findByStoreInfo(id)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        boolean applyPastSales = store.getAdditionalOption() != null && store.getAdditionalOption().isOptionApplyPastSales();
        return new StoreDto.ApiStoreInfo(store.getStoreId(), store.getStoreName(), store.getCommonOption().getOptionLevel().getGrade(), store.getCommonOption().getGoldHarryLoss(), applyPastSales);
    }

    @Override
    public String getStoreGrade(String storeId) {
        OptionLevel grade = storeRepository.findByCommonOptionOptionLevel(Long.valueOf(storeId));
        return grade.getGrade();
    }

    @Override
    public void updateStoreHarry(String accessToken, String storeId, String harryId) {
        Store store = storeRepository.findById(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new NotAuthorityException(NO_ROLE);
        }
        GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(harryId))
                .orElseThrow(() -> new NotFoundException(WRONG_HARRY));
        store.getCommonOption().updateGoldHarry(goldHarry);
    }

    @Override
    public void updateStoreGrade(String accessToken, String storeId, String grade) {
        Store store = storeRepository.findById(Long.valueOf(storeId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new NotAuthorityException(NO_ROLE);
        }
        store.getCommonOption().updateOptionLevel(grade);
    }

    @Override
    public List<AccountExcelDto> getExcel(String accessToken) {
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new NotAuthorityException(NO_ROLE);
        }
        return storeRepository.findAllStoreExcel();
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreDto.StorePhoneInfo> getStorePhones(List<Long> storeIds) {
        return storeIds.stream()
                .map(id -> storeRepository.findById(id)
                        .map(store -> new StoreDto.StorePhoneInfo(
                                store.getStoreId(),
                                store.getStoreName(),
                                store.getStorePhoneNumber()))
                        .orElse(null))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StoreDto.ApiStoreInfo getStoreInfoByName(String storeName) {
        List<Store> stores = storeRepository.findByStoreNameIgnoreCase(storeName);
        if (stores.isEmpty()) {
            throw new NotFoundException(NOT_FOUND_STORE);
        }
        Store store = stores.get(0);
        boolean applyPastSales = store.getAdditionalOption() != null && store.getAdditionalOption().isOptionApplyPastSales();
        return new StoreDto.ApiStoreInfo(store.getStoreId(), store.getStoreName(), store.getCommonOption().getOptionLevel().getGrade(), store.getCommonOption().getGoldHarryLoss(), applyPastSales);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getReceivableExcel(String accessToken, String name) throws IOException {
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new NotAuthorityException(NO_ROLE);
        }
        List<ReceivableExcelDto> receivableList = storeRepository.findAllReceivableExcel(name);
        return ReceivableExcelUtil.createReceivableWorkSheet(receivableList, "미수금");
    }

    @Override
    @Transactional(readOnly = true)
    public StoreView getStoreInfoView(Long storeId) {
        Store store = storeRepository.findWithAllOptionsById(storeId)
                .orElseThrow(() -> new NotFoundException("거래처 미존재: storeId=" + storeId));
        return toStoreView(store);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreView findStoreByNameView(String storeName) {
        return storeRepository.findByStoreNameIgnoreCase(storeName)
                .stream()
                .findFirst()
                .map(StoreServiceImpl::toStoreView)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StorePhoneView> getStorePhonesView(List<Long> storeIds) {
        List<StoreDto.StorePhoneInfo> phones = getStorePhones(storeIds);
        return phones.stream()
                .map(p -> new StorePhoneView(p.getStoreId(), p.getStoreName(), p.getStorePhoneNumber()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public StoreReceivableLogView getReceivableLog(Long storeId, String saleCode) {
        AccountDto.AccountSaleLogResponse logResp = getStoreReceivableLogDetail(String.valueOf(storeId), saleCode);
        return new StoreReceivableLogView(
                logResp.getPreviousGoldBalance(),
                logResp.getPreviousMoneyBalance(),
                logResp.getAfterGoldBalance(),
                logResp.getAfterMoneyBalance(),
                logResp.getLastSaleDate()
        );
    }

    @Override
    public void applyDelta(Long storeId, BigDecimal goldDelta, Long moneyDelta, String eventId,
                           String transactionType, String material, Long accountSaleCode, String note) {
        if (transactionHistoryRepository.existsByEventIdAndStore_StoreId(eventId, storeId)) {
            log.debug("StoreService.applyDelta: eventId={} storeId={} already processed (idempotent skip)", eventId, storeId);
            return;
        }
        Store store = storeRepository.findByIdWithLock(storeId)
                .orElseThrow(() -> new NotFoundException("Store not found: storeId=" + storeId));
        BigDecimal gold = goldDelta != null ? goldDelta : BigDecimal.ZERO;
        Long money = moneyDelta != null ? moneyDelta : 0L;
        store.updateBalance(gold, money);
        TransactionHistory history = TransactionHistory.builder()
                .transactionType(parseSaleStatus(transactionType))
                .material(material)
                .goldAmount(gold)
                .moneyAmount(money)
                .eventId(eventId)
                .accountSaleCode(accountSaleCode)
                .store(store)
                .transactionHistoryNote(note)
                .build();
        transactionHistoryRepository.save(history);
        log.info("StoreService.applyDelta: storeId={} goldDelta={} moneyDelta={} eventId={} type={}", storeId, gold, money, eventId, transactionType);
    }

    private static StoreView toStoreView(Store entity) {
        String harry = entity.getCommonOption() != null ? entity.getCommonOption().getGoldHarryLoss() : null;
        String tradeType = entity.getCommonOption() != null && entity.getCommonOption().getOptionTradeType() != null
                ? entity.getCommonOption().getOptionTradeType().name() : null;
        String grade = entity.getCommonOption() != null && entity.getCommonOption().getOptionLevel() != null
                ? entity.getCommonOption().getOptionLevel().name() : null;
        boolean applyPast = entity.getAdditionalOption() != null && entity.getAdditionalOption().isOptionApplyPastSales();
        return new StoreView(entity.getStoreId(), entity.getStoreName(), grade, harry, tradeType, applyPast);
    }

    private SaleStatus parseSaleStatus(String transactionType) {
        if (transactionType == null || transactionType.isBlank()) return null;
        try {
            return SaleStatus.valueOf(transactionType);
        } catch (IllegalArgumentException e) {
            log.warn("parseSaleStatus: unknown transactionType '{}' — stored as null", transactionType);
            return null;
        }
    }
}
