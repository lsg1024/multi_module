package com.msa.account.local.factory.service;

import com.msa.account.global.domain.dto.AccountDto;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.entity.OptionLevel;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.excel.dto.AccountExcelDto;
import com.msa.account.global.exception.NotAuthorityException;
import com.msa.account.global.exception.NotFoundException;
import com.msa.account.local.factory.domain.dto.FactoryDto;
import com.msa.account.local.factory.domain.entity.Factory;
import com.msa.account.local.factory.repository.FactoryRepository;
import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.msa.account.global.exception.ExceptionMessage.*;

@Service
@Transactional
public class FactoryService {
    private final AuthorityUserRoleUtil authorityUserRoleUtil;
    private final FactoryRepository factoryRepository;
    private final GoldHarryRepository goldHarryRepository;

    public FactoryService(AuthorityUserRoleUtil authorityUserRoleUtil, FactoryRepository factoryRepository, GoldHarryRepository goldHarryRepository) {
        this.authorityUserRoleUtil = authorityUserRoleUtil;
        this.factoryRepository = factoryRepository;
        this.goldHarryRepository = goldHarryRepository;
    }

    @Transactional(readOnly = true)
    public AccountDto.AccountSingleResponse getFactoryInfo(String factoryId) {
        return factoryRepository.findByFactoryId(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));
    }

    @Transactional(readOnly = true)
    public CustomPage<FactoryDto.FactoryResponse> getFactoryList(String name, Pageable pageable) {
        return factoryRepository.findAllFactory(name, pageable);
    }

    public void createFactory(FactoryDto.FactoryRequest factoryInfo) throws NotFoundException {
        if (factoryRepository.existsByFactoryName(factoryInfo.getAccountInfo().getAccountName())) {
            throw new NotFoundException(ALREADY_EXIST_FACTORY);
        }

        GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(factoryInfo.getCommonOptionInfo().getGoldHarryId()))
                .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

        Factory newfactory = factoryInfo.toEntity(goldHarry);

        factoryRepository.save(newfactory);
    }

    public void updateFactory(String token, String factoryId, AccountDto.AccountUpdate updateInfo) {

        Factory factory = factoryRepository.findWithAllOptionById(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FACTORY));

        if (authorityUserRoleUtil.verification(token)) {
            AccountDto.AccountInfo factoryInfo = updateInfo.getAccountInfo();

            if (factory.isNameChanged(factoryInfo.getAccountName())) {
                if (factoryRepository.existsByFactoryName(factoryInfo.getAccountName())) {
                    throw new NotFoundException(ALREADY_EXIST_FACTORY);
                }
            }

            GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(updateInfo.getCommonOptionInfo().getGoldHarryId()))
                    .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

            factory.updateFactoryInfo(factoryInfo);
            factory.updateAddressInfo(updateInfo.getAddressInfo());
            factory.updateCommonOption(updateInfo.getCommonOptionInfo(), goldHarry);
            return;
        }

        throw new NotAuthorityException(NO_ROLE);
    }

    public void deleteFactory(String token, String factoryId) {
        Factory factory = factoryRepository.findById(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FACTORY));

        if (authorityUserRoleUtil.verification(token)) {
            factoryRepository.delete(factory);
            return;
        }

        throw new NotAuthorityException(NO_ROLE);
    }

    public FactoryDto.ApiFactoryInfo getFactoryIdAndName(Long id) {
        Factory factory = factoryRepository.findWithAllOptionById(id)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FACTORY));

        return new FactoryDto.ApiFactoryInfo(factory.getFactoryId(), factory.getFactoryName(), factory.getCommonOption().getGoldHarryLoss());
    }

    public String getFactoryGrade(String storeId) {
        OptionLevel grade = factoryRepository.findByCommonOptionOptionLevel(Long.valueOf(storeId));
        return grade.getLevel();
    }

    public void updateFactoryHarry(String accessToken, String factoryId, String harryId) {
        Factory factory = factoryRepository.findById(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FACTORY));

        if (authorityUserRoleUtil.verification(accessToken)) {
            GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(harryId))
                    .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

            factory.getCommonOption().updateGoldHarry(goldHarry);
            return;
        }

        throw new NotAuthorityException(NO_ROLE);
    }

    public void updateFactoryGrade(String accessToken, String factoryId, String grade) {
        Factory factory = factoryRepository.findById(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FACTORY));

        if (authorityUserRoleUtil.verification(accessToken)) {
            factory.getCommonOption().updateOptionLevel(grade);
            return;
        }

        throw new NotAuthorityException(NO_ROLE);
    }

    @Transactional(readOnly = true)
    public List<AccountExcelDto> getExcel(String accessToken) {
        if (authorityUserRoleUtil.verification(accessToken)) {
            return factoryRepository.findAllFactoryExcel();
        }
        throw new NotAuthorityException(NO_ROLE);
    }

    public List<FactoryDto.ApiFactoryInfo> findAllFactory() {
        return factoryRepository.findAllFactory();
    }
}
