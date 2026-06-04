package com.msa.jewelry.local.factory.service;

import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.dto.AccountDto;
import com.msa.jewelry.global.excel.dto.AccountExcelDto;
import com.msa.jewelry.global.excel.dto.PurchaseExcelDto;
import com.msa.jewelry.global.excel.util.PurchaseExcelUtil;
import com.msa.jewelry.global.exception.NotAuthorityException;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.common_option.entity.OptionLevel;
import com.msa.jewelry.local.factory.dto.FactoryDto;
import com.msa.jewelry.local.factory.dto.FactoryView;
import com.msa.jewelry.local.factory.entity.Factory;
import com.msa.jewelry.local.factory.repository.FactoryRepository;
import com.msa.jewelry.local.goldharry.entity.GoldHarry;
import com.msa.jewelry.local.goldharry.repository.GoldHarryRepository;
import com.msa.jewelry.local.transaction_history.entity.BalanceHistory;
import com.msa.jewelry.local.transaction_history.entity.TransactionHistory;
import com.msa.jewelry.local.transaction_history.repository.BalanceHistoryRepository;
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
public class FactoryServiceImpl implements FactoryService {

    private final AuthorityUserRoleUtil authorityUserRoleUtil;
    private final FactoryRepository factoryRepository;
    private final GoldHarryRepository goldHarryRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final BalanceHistoryRepository balanceHistoryRepository;

    public FactoryServiceImpl(AuthorityUserRoleUtil authorityUserRoleUtil,
                              FactoryRepository factoryRepository,
                              GoldHarryRepository goldHarryRepository,
                              TransactionHistoryRepository transactionHistoryRepository,
                              BalanceHistoryRepository balanceHistoryRepository) {
        this.authorityUserRoleUtil = authorityUserRoleUtil;
        this.factoryRepository = factoryRepository;
        this.goldHarryRepository = goldHarryRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.balanceHistoryRepository = balanceHistoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto.RecentActivityResponse getFactoryRecentActivity(Long factoryId, int limit) {
        return new AccountDto.RecentActivityResponse(
                transactionHistoryRepository.findRecentSalesByFactory(factoryId, limit),
                transactionHistoryRepository.findPaymentSummaryByFactory(factoryId)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDto.AccountSingleResponse getFactoryInfo(String factoryId) {
        return factoryRepository.findByFactoryId(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomPage<FactoryDto.FactoryResponse> getFactoryList(String name, String searchField, String sortField, String sortOrder, Pageable pageable) {
        return factoryRepository.findAllFactory(name, searchField, sortField, sortOrder, pageable);
    }

    @Override
    public void createFactory(FactoryDto.FactoryRequest factoryInfo) throws NotFoundException {
        if (factoryRepository.existsByFactoryName(factoryInfo.getAccountInfo().getAccountName())) {
            throw new NotFoundException(ALREADY_EXIST_FACTORY);
        }
        GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(factoryInfo.getCommonOptionInfo().getGoldHarryId()))
                .orElseThrow(() -> new NotFoundException(WRONG_HARRY));
        Factory newfactory = factoryInfo.toEntity(goldHarry);
        factoryRepository.save(newfactory);
    }

    @Override
    public void updateFactory(String token, String factoryId, AccountDto.AccountUpdate updateInfo) {
        Factory factory = factoryRepository.findWithAllOptionById(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FACTORY));
        if (!authorityUserRoleUtil.verification(token)) {
            throw new NotAuthorityException(NO_ROLE);
        }
        AccountDto.AccountInfo factoryInfo = updateInfo.getAccountInfo();
        if (factory.isNameChanged(factoryInfo.getAccountName())
                && factoryRepository.existsByFactoryName(factoryInfo.getAccountName())) {
            throw new NotFoundException(ALREADY_EXIST_FACTORY);
        }
        GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(updateInfo.getCommonOptionInfo().getGoldHarryId()))
                .orElseThrow(() -> new NotFoundException(WRONG_HARRY));
        factory.updateFactoryInfo(factoryInfo);
        factory.updateCommonOption(updateInfo.getCommonOptionInfo(), goldHarry);
        if (updateInfo.getAddressInfo() != null) {
            factory.updateAddressInfo(updateInfo.getAddressInfo());
        }
    }

    @Override
    public void deleteFactory(String token, String factoryId) {
        Factory factory = factoryRepository.findById(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FACTORY));
        if (!authorityUserRoleUtil.verification(token)) {
            throw new NotAuthorityException(NO_ROLE);
        }
        factoryRepository.delete(factory);
    }

    @Override
    public FactoryDto.ApiFactoryInfo getFactoryIdAndName(Long id) {
        Factory factory = factoryRepository.findWithAllOptionById(id)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FACTORY));
        return new FactoryDto.ApiFactoryInfo(factory.getFactoryId(), factory.getFactoryName(), factory.getCommonOption().getGoldHarryLoss());
    }

    @Override
    public String getFactoryGrade(String storeId) {
        OptionLevel grade = factoryRepository.findByCommonOptionOptionLevel(Long.valueOf(storeId));
        return grade.getGrade();
    }

    @Override
    public void updateFactoryHarry(String accessToken, String factoryId, String harryId) {
        Factory factory = factoryRepository.findById(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FACTORY));
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new NotAuthorityException(NO_ROLE);
        }
        GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(harryId))
                .orElseThrow(() -> new NotFoundException(WRONG_HARRY));
        factory.getCommonOption().updateGoldHarry(goldHarry);
    }

    @Override
    public void updateFactoryGrade(String accessToken, String factoryId, String grade) {
        Factory factory = factoryRepository.findById(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FACTORY));
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new NotAuthorityException(NO_ROLE);
        }
        factory.getCommonOption().updateOptionLevel(grade);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountExcelDto> getExcel(String accessToken) {
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new NotAuthorityException(NO_ROLE);
        }
        return factoryRepository.findAllFactoryExcel();
    }

    @Override
    @Transactional(readOnly = true)
    public FactoryDto.ApiFactoryInfo getFactoryInfoByName(String factoryName) {
        List<Factory> factories = factoryRepository.findByFactoryNameIgnoreCase(factoryName);
        if (factories.isEmpty()) {
            throw new NotFoundException(NOT_FOUND_FACTORY);
        }
        Factory factory = factories.get(0);
        return new FactoryDto.ApiFactoryInfo(factory.getFactoryId(), factory.getFactoryName(), factory.getCommonOption().getGoldHarryLoss());
    }

    @Override
    public List<FactoryDto.ApiFactoryInfo> findAllFactory() {
        return factoryRepository.findAllFactory();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomPage<AccountDto.AccountResponse> getFactoryPurchase(String startAt, String endAt, Pageable pageable) {
        return factoryRepository.findAllFactoryAndPurchase(startAt, endAt, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPurchaseExcel(String accessToken, String endAt) throws IOException {
        if (!authorityUserRoleUtil.verification(accessToken)) {
            throw new NotAuthorityException(NO_ROLE);
        }
        List<PurchaseExcelDto> purchaseList = factoryRepository.findAllPurchaseExcel(endAt);
        return PurchaseExcelUtil.createPurchaseWorkSheet(purchaseList, "매입잔액");
    }

    @Override
    @Transactional(readOnly = true)
    public FactoryView getFactoryInfo(Long factoryId) {
        Factory factory = factoryRepository.findWithAllOptionById(factoryId)
                .orElseThrow(() -> new NotFoundException("제조사 미존재: factoryId=" + factoryId));
        return toView(factory);
    }

    @Override
    @Transactional(readOnly = true)
    public FactoryView findFactoryByName(String factoryName) {
        return factoryRepository.findByFactoryNameIgnoreCase(factoryName)
                .stream()
                .findFirst()
                .map(FactoryServiceImpl::toView)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FactoryView> findAllFactoryView() {
        return factoryRepository.findAll().stream()
                .map(FactoryServiceImpl::toView)
                .toList();
    }

    @Override
    public void applyDelta(Long factoryId, BigDecimal goldDelta, Long moneyDelta, String eventId,
                           String transactionType, String material, Long accountSaleCode, String note) {
        Factory factory = factoryRepository.findByIdWithLock(factoryId)
                .orElseThrow(() -> new NotFoundException("Factory not found: factoryId=" + factoryId));
        BigDecimal gold = goldDelta != null ? goldDelta : BigDecimal.ZERO;
        Long money = moneyDelta != null ? moneyDelta : 0L;

        BigDecimal beforeGold = factory.getCurrentGoldBalance();
        Long beforeMoney = factory.getCurrentMoneyBalance();

        factory.updateBalance(gold, money);
        TransactionHistory history = TransactionHistory.builder()
                .transactionType(parseSaleStatus(transactionType))
                .material(material)
                .goldAmount(gold)
                .moneyAmount(money)
                .eventId(eventId)
                .accountSaleCode(accountSaleCode)
                .factory(factory)
                .transactionHistoryNote(note)
                .build();
        transactionHistoryRepository.save(history);

        balanceHistoryRepository.save(BalanceHistory.builder()
                .ownerType("FACTORY")
                .factory(factory)
                .beforeGoldBalance(beforeGold)
                .afterGoldBalance(factory.getCurrentGoldBalance())
                .beforeMoneyBalance(beforeMoney)
                .afterMoneyBalance(factory.getCurrentMoneyBalance())
                .deltaGold(gold)
                .deltaMoney(money)
                .reason(transactionType != null && !transactionType.isBlank() ? transactionType : "UNKNOWN")
                .eventId(eventId)
                .accountSaleCode(accountSaleCode)
                .note(note)
                .build());
        log.info("FactoryService.applyDelta: factoryId={} goldDelta={} moneyDelta={} eventId={} type={}", factoryId, gold, money, eventId, transactionType);
    }

    private static FactoryView toView(Factory entity) {
        String harry = entity.getCommonOption() != null
                ? entity.getCommonOption().getGoldHarryLoss()
                : null;
        String grade = entity.getCommonOption() != null
                && entity.getCommonOption().getOptionLevel() != null
                ? entity.getCommonOption().getOptionLevel().name()
                : null;
        return new FactoryView(entity.getFactoryId(), entity.getFactoryName(), grade, harry);
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
