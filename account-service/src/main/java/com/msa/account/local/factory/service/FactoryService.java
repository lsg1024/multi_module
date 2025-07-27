package com.msa.account.local.factory.service;

import com.msa.account.local.factory.dto.FactoryDto;
import com.msa.account.local.factory.entity.Factory;
import com.msa.account.local.factory.repository.FactoryRepository;
import com.msa.account.global.domain.dto.util.AuthorityUserRoleUtil;
import com.msa.account.global.domain.entity.GoldHarry;
import com.msa.account.global.domain.repository.GoldHarryRepository;
import com.msa.account.global.exception.NotAuthorityException;
import com.msa.account.global.exception.NotFoundException;
import com.msacommon.global.util.CustomPage;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public FactoryDto.FactorySingleResponse getFactoryInfo(String factoryId) {
        return factoryRepository.findByFactoryId(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_STORE));
    }

    @Transactional(readOnly = true)
    public CustomPage<FactoryDto.FactoryResponse> getFactoryList(Pageable pageable) {
        return factoryRepository.findAllFactory(pageable);
    }

    public void createFactory(FactoryDto.FactoryRequest factoryInfo) throws NotFoundException {
        if (factoryRepository.existsByFactoryName(factoryInfo.getFactoryInfo().getFactoryName())) {
            throw new NotFoundException(ALREADY_EXIST_FACTORY);
        }

        GoldHarry goldHarry = goldHarryRepository.findById(Long.valueOf(factoryInfo.getCommonOptionInfo().getGoldHarryId()))
                .orElseThrow(() -> new NotFoundException(WRONG_HARRY));

        Factory newfactory = factoryInfo.toEntity(goldHarry);

        factoryRepository.save(newfactory);
    }

    public void updateFactory(String token, String factoryId, FactoryDto.FactoryUpdate updateInfo) {

        Factory factory = factoryRepository.findWithAllOptionById(Long.valueOf(factoryId))
                .orElseThrow(() -> new NotFoundException(NOT_FOUND_FACTORY));

        if (authorityUserRoleUtil.factoryVerification(token, factory)) {
            FactoryDto.FactoryInfo factoryInfo = updateInfo.getFactoryInfo();

            if (factory.isNameChanged(factoryInfo.getFactoryName())) {
                if (factoryRepository.existsByFactoryName(factoryInfo.getFactoryName())) {
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

        if (authorityUserRoleUtil.factoryVerification(token, factory)) {
            factoryRepository.delete(factory);
            return;
        }

        throw new NotAuthorityException(NO_ROLE);
    }

}
