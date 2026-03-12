package com.msa.account.local.ledger.service;

import com.msa.account.global.exception.NotFoundException;
import com.msa.account.local.ledger.domain.dto.LedgerDto;
import com.msa.account.local.ledger.domain.entity.AssetType;
import com.msa.account.local.ledger.domain.entity.Ledger;
import com.msa.account.local.ledger.repository.LedgerRepository;
import com.msa.common.global.util.CustomPage;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LedgerService {

    private final LedgerRepository ledgerRepository;

    @Transactional
    public void createLedger(LedgerDto.CreateRequest request, String userId) {
        Ledger ledger = Ledger.builder()
                .ledgerDate(request.getLedgerDate())
                .assetType(request.getAssetType())
                .transactionType(request.getTransactionType())
                .goldAmount(request.getGoldAmount())
                .moneyAmount(request.getMoneyAmount())
                .description(request.getDescription())
                .createdBy(userId)
                .build();

        ledgerRepository.save(ledger);
    }

    @Transactional
    public void updateLedger(Long ledgerId, LedgerDto.UpdateRequest request) {
        Ledger ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new NotFoundException("가계부 내역을 찾을 수 없습니다. ID: " + ledgerId));

        ledger.update(
                request.getLedgerDate(),
                request.getTransactionType(),
                request.getGoldAmount(),
                request.getMoneyAmount(),
                request.getDescription()
        );
    }

    @Transactional
    public void deleteLedger(Long ledgerId) {
        Ledger ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new NotFoundException("가계부 내역을 찾을 수 없습니다. ID: " + ledgerId));

        ledgerRepository.delete(ledger);
    }

    public CustomPage<LedgerDto.LedgerResponse> getLedgerList(
            AssetType assetType, LocalDate startDate, LocalDate endDate, Pageable pageable) {

        Page<Ledger> page;

        if (assetType != null) {
            page = ledgerRepository.findByAssetTypeAndLedgerDateBetweenOrderByLedgerDateDesc(
                    assetType, startDate, endDate, pageable);
        } else {
            page = ledgerRepository.findByLedgerDateBetweenOrderByLedgerDateDesc(
                    startDate, endDate, pageable);
        }

        Page<LedgerDto.LedgerResponse> responsePage = page.map(LedgerDto.LedgerResponse::from);
        return new CustomPage<>(responsePage);
    }

    public LedgerDto.LedgerResponse getLedger(Long ledgerId) {
        Ledger ledger = ledgerRepository.findById(ledgerId)
                .orElseThrow(() -> new NotFoundException("가계부 내역을 찾을 수 없습니다. ID: " + ledgerId));

        return LedgerDto.LedgerResponse.from(ledger);
    }

    public LedgerDto.BalanceResponse getBalance() {
        return LedgerDto.BalanceResponse.builder()
                .totalGold(ledgerRepository.calculateGoldBalance())
                .totalMoney(ledgerRepository.calculateMoneyBalance())
                .build();
    }
}
