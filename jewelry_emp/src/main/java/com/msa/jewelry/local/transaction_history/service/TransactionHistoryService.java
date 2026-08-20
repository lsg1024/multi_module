package com.msa.jewelry.local.transaction_history.service;

import com.msa.jewelry.local.factory.repository.FactoryRepository;
import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.store.repository.StoreRepository;
import com.msa.jewelry.local.transaction_history.dto.TransactionDto;
import com.msa.jewelry.local.transaction_history.dto.TransactionPage;
import com.msa.jewelry.local.transaction_history.dto.PurchaseDto;
import com.msa.jewelry.local.transaction_history.repository.TransactionHistoryRepository;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.util.CustomPage;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TransactionHistoryService {

    private final StoreRepository storeRepository;
    private final FactoryRepository factoryRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final FactoryService factoryService;

    public TransactionHistoryService(StoreRepository storeRepository, FactoryRepository factoryRepository, TransactionHistoryRepository transactionHistoryRepository, FactoryService factoryService) {
        this.storeRepository = storeRepository;
        this.factoryRepository = factoryRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
        this.factoryService = factoryService;
    }

    @Transactional(readOnly = true)
    public TransactionDto getCurrentBalance(String type, String id, String name) {
        Long targetId = Long.valueOf(id);
        TransactionDto currentBalance;
        if (type.equals("store")) {
            currentBalance = storeRepository.findByStoreIdAndStoreName(targetId, name);
        } else {
            currentBalance = factoryRepository.findByFactoryIdAndFactoryName(targetId, name);
        }
        return currentBalance;
    }

    @Transactional(readOnly = true)
    public CustomPage<TransactionPage> findAccountPurchase(String start, String end, String accountType, String accountName, Pageable pageable) {
        return transactionHistoryRepository.findTransactionHistory(start, end, accountType, accountName, pageable);
    }

    @Transactional(readOnly = true)
    public CustomPage<TransactionPage> findFactoryPurchase(String start, String end, String accountType, String accountName, Pageable pageable) {
        return transactionHistoryRepository.findTransactionHistoryFactory(start, end, accountType, accountName, pageable);
    }

    /**
     * 매입 생성(여러 줄) - 매입 등록 모달에서 입력한 라인들을 한 트랜잭션으로 저장한다.
     */
    public void savePurchases(List<PurchaseDto> purchaseList) {
        if (purchaseList == null || purchaseList.isEmpty()) {
            throw new IllegalArgumentException("등록할 매입 항목이 없습니다.");
        }
        for (PurchaseDto dto : purchaseList) {
            savePurchase(dto);
        }
    }

    /**
     * 매입(거래) 단건 생성.
     * 제조사 잔액(currentGoldBalance/currentMoneyBalance) 갱신 + TransactionHistory + BalanceHistory 기록을
     * FactoryService.applyDelta 로 일괄 처리한다(판매 정산과 동일 경로).
     *
     * 부호 규칙: 매입(PURCHASE) 은 (+), 결제/반품/DC/통장 은 (-) 로 잔액에 반영.
     * goldAmount 는 "순금 환산 중량(g)" 으로 전달받는다(제조사 잔액은 순금 기준 누적).
     */
    public void savePurchase(PurchaseDto purchaseDto) {
        if (purchaseDto.getAccountId() == null || purchaseDto.getAccountId().isBlank()) {
            throw new IllegalArgumentException("매입처(accountId)는 필수입니다.");
        }
        Long factoryId = Long.parseLong(purchaseDto.getAccountId().trim());

        SaleStatus type = parseType(purchaseDto.getTransactionType());

        int sign = switch (type) {
            case PAYMENT, RETURN, DISCOUNT, PAYMENT_TO_BANK -> -1;
            default -> 1; // PURCHASE, SALE, WG
        };

        BigDecimal gold = purchaseDto.getGoldAmount() != null ? purchaseDto.getGoldAmount() : BigDecimal.ZERO;
        long money = purchaseDto.getMoneyAmount() != null ? purchaseDto.getMoneyAmount() : 0L;

        BigDecimal signedGold = sign < 0 ? gold.negate() : gold;
        long signedMoney = sign < 0 ? -money : money;

        String eventId = UUID.randomUUID().toString();
        Long accountSaleCode = (purchaseDto.getSaleCode() != null && !purchaseDto.getSaleCode().isBlank())
                ? Long.parseLong(purchaseDto.getSaleCode().trim())
                : null;

        factoryService.applyDelta(
                factoryId,
                signedGold,
                signedMoney,
                eventId,
                type.name(),
                purchaseDto.getMaterial(),
                accountSaleCode,
                purchaseDto.getTransactionNote(),
                purchaseDto.getTransactionDate()
        );
    }

    /**
     * 거래 유형 파싱 - enum name("PURCHASE") 과 표시명("매입") 모두 허용.
     */
    private SaleStatus parseType(String transactionType) {
        if (transactionType == null || transactionType.isBlank()) {
            return SaleStatus.PURCHASE;
        }
        try {
            return SaleStatus.valueOf(transactionType.trim());
        } catch (IllegalArgumentException e) {
            SaleStatus byDisplay = SaleStatus.fromDisplayName(transactionType.trim());
            if (byDisplay == null) {
                throw new IllegalArgumentException("알 수 없는 거래 유형: " + transactionType);
            }
            return byDisplay;
        }
    }
}
