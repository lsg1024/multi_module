package com.msa.account.global.kafka.service;

import com.msa.account.global.kafka.dto.KafkaEventDto;
import com.msa.account.local.factory.domain.entity.Factory;
import com.msa.account.local.factory.repository.FactoryRepository;
import com.msa.account.local.store.domain.entity.Store;
import com.msa.account.local.store.repository.StoreRepository;
import com.msa.account.local.transaction_history.domain.entity.SaleLog;
import com.msa.account.local.transaction_history.domain.entity.TransactionHistory;
import com.msa.account.local.transaction_history.repository.SaleLogRepository;
import com.msa.account.local.transaction_history.repository.TransactionHistoryRepository;
import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

/**
 * Kafka 메시지 기반 잔액 갱신 서비스.
 *
 * *order-service가 발행한 OutboxEvent를 소비하여 Store(매장) 또는 Factory(공장)의
 * 금(gold) 및 돈(money) 잔액을 업데이트한다. 처리 흐름은 다음과 같다:
 *
 *   - 이벤트 중복 여부를 {@code eventId}로 검사한다 (멱등성 보장).
 *   - 가장 최근 {@link SaleLog}에서 이전 잔액(previousBalance)을 확보한다.
 *   - 새로운 {@link SaleLog}를 생성하고 Store/Factory 잔액을 갱신한다.
 *   - {@link com.msa.account.local.transaction_history.domain.entity.TransactionHistory}를 기록한다.
 * 
 *
 * *의존 컴포넌트: {@link StoreRepository}, {@link FactoryRepository},
 * {@link SaleLogRepository}, {@link TransactionHistoryRepository}
 */
@Slf4j
@Service
@Transactional
public class KafkaService {

    private final StoreRepository storeRepository;
    private final FactoryRepository factoryRepository;
    private final SaleLogRepository saleLogRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    public KafkaService(StoreRepository storeRepository, FactoryRepository factoryRepository, SaleLogRepository saleLogRepository, TransactionHistoryRepository transactionHistoryRepository) {
        this.storeRepository = storeRepository;
        this.factoryRepository = factoryRepository;
        this.saleLogRepository = saleLogRepository;
        this.transactionHistoryRepository = transactionHistoryRepository;
    }


    /**
     * Store 또는 Factory의 현재 잔액을 Kafka 이벤트 정보를 기반으로 갱신한다.
     *
     * *처리 규칙:
     *
     *   - <b>RETURN 타입</b>: 원본 {@link SaleLog}를 조회하여 해당 거래 시각에
     *       +1 마이크로초를 더한 타임스탬프를 사용한다. 동일 시각 충돌 방지 목적.
     *   - <b>WG 타입</b>: 금 거래만 인정하므로 {@code moneyAmount}를 강제로 0으로 설정한다.
     *   - <b>멱등성</b>: {@code eventId}로 {@link TransactionHistoryRepository}를 조회하여
     *       이미 처리된 이벤트인 경우 {@link IllegalArgumentException}을 발생시켜 중복 처리를 차단한다.
     *       DB 유니크 제약 위반({@link org.springframework.dao.DataIntegrityViolationException})도
     *       컨슈머 레이어에서 동일하게 처리된다.
     *   - <b>previousBalance 확보</b>: 마지막 {@link SaleLog}가 존재하면 그 {@code afterBalance}를
     *       이번 거래의 {@code previousBalance}로 사용하고, 없으면 엔티티의 현재 잔액을 사용한다.
     *   - <b>afterBalance 계산</b>: {@code prevBalance + delta}
     * 
     *
     * @param dto Kafka 이벤트로부터 역직렬화된 잔액 갱신 요청 DTO
     * @return 새로 저장된 {@link SaleLog} 엔티티
     * @throws IllegalArgumentException 이미 처리된 이벤트이거나 알 수 없는 {@code type}인 경우
     */
    // 상점 or 공장 잔액 업데이트
    public SaleLog updateCurrentBalance(KafkaEventDto.updateCurrentBalance dto) {

        String type = dto.getType();
        Long saleCode = Long.parseLong(dto.getSaleCode());
        Long entityId = dto.getId();
        String eventId = dto.getEventId();
        String saleType = dto.getSaleType();
        String material = dto.getMaterial();
        BigDecimal pureGoldAmount = new BigDecimal(dto.getPureGoldBalance());
        Long moneyAmount = Long.valueOf(dto.getMoneyBalance());
        LocalDateTime transactionDate;

        log.info("updateCurrentBalance = {}", dto.toString());

        if (SaleStatus.RETURN.name().equals(saleType)) {
            Optional<SaleLog> originalLog = Optional.empty();

            if ("STORE".equals(type)) {
                originalLog = saleLogRepository.findTopByAccountSaleCodeAndStore_StoreIdOrderBySaleDateDesc(saleCode, entityId);
            } else if ("FACTORY".equals(type)) {
                originalLog = saleLogRepository.findTopByAccountSaleCodeAndFactory_FactoryIdOrderBySaleDateDesc(saleCode, entityId);
            }

            transactionDate = originalLog.map(saleLog -> saleLog.getSaleDate().plus(1, ChronoUnit.MICROS)).orElseGet(() -> dto.getSaleDate() != null ? dto.getSaleDate() : LocalDateTime.now());
        } else {
            transactionDate = dto.getSaleDate() != null ? dto.getSaleDate() : LocalDateTime.now();
        }

        if (SaleStatus.WG.name().equals(saleType)) {
            moneyAmount = 0L;
        }

        transactionDate = transactionDate.truncatedTo(ChronoUnit.MICROS);

        Store store = null;
        Factory factory = null;

        SaleLog lastLog = null;
        BigDecimal prevGold = BigDecimal.ZERO;
        Long prevMoney = 0L;

        if ("STORE".equals(type)) {

            if (transactionHistoryRepository.existsByEventIdAndStore_StoreId(eventId, entityId)) {
                log.info("이미 처리된 상점 잔액 업데이트입니다. eventId={}, storeId={}", eventId, entityId);
                throw new IllegalArgumentException("이미 처리된 상점 잔액 업데이트입니다.");
            }

            store = storeRepository.findByIdWithLock(entityId)
                    .orElseThrow(() -> new IllegalArgumentException("TENANT ID: " + dto.getTenantId() + " STORE: NOT FOUND"));

            lastLog = saleLogRepository.findTopByStoreAndOwnerTypeOrderBySaleDateDesc(store, "STORE").orElse(null);

            if (lastLog == null) {
                prevGold = store.getCurrentGoldBalance() != null ? store.getCurrentGoldBalance() : BigDecimal.ZERO;
                prevMoney = store.getCurrentMoneyBalance() != null ? store.getCurrentMoneyBalance() : 0L;
            }

        } else if ("FACTORY".equals(type)) {

            if (transactionHistoryRepository.existsByEventIdAndFactory_FactoryId(eventId, entityId)) {
                log.info("이미 처리된 공장 잔액 업데이트입니다. eventId={}, factoryId={}", eventId, entityId);
                throw new IllegalArgumentException("이미 처리된 공장 잔액 업데이트입니다");
            }

            factory = factoryRepository.findByIdWithLock(entityId)
                    .orElseThrow(() -> new IllegalArgumentException("TENANT ID: " + dto.getTenantId() + " FACTORY: NOT FOUND"));

            lastLog = saleLogRepository.findTopByFactoryAndOwnerTypeOrderBySaleDateDesc(factory, "FACTORY").orElse(null);

            if (lastLog == null) {
                prevGold = factory.getCurrentGoldBalance() != null ? factory.getCurrentGoldBalance() : BigDecimal.ZERO;
                prevMoney = factory.getCurrentMoneyBalance() != null ? factory.getCurrentMoneyBalance() : 0L;
            }

        } else {
            throw new IllegalArgumentException("Unknown balance type: " + type);
        }

        if (lastLog != null) {
            prevGold = lastLog.getAfterGoldBalance();
            prevMoney = lastLog.getAfterMoneyBalance();
        }

        BigDecimal afterGold = prevGold.add(pureGoldAmount);
        Long afterMoney = prevMoney + moneyAmount;

        SaleLog newLog = SaleLog.builder()
                .accountSaleCode(saleCode)
                .ownerType(type)
                .previousGoldBalance(prevGold)
                .previousMoneyBalance(prevMoney)
                .afterGoldBalance(afterGold)
                .afterMoneyBalance(afterMoney)
                .saleDate(transactionDate)
                .store(store)
                .factory(factory)
                .build();

        SaleLog saleLog = saleLogRepository.save(newLog);

        if (store != null) {
            store.updateBalance(pureGoldAmount, moneyAmount);
        } else {
            factory.updateBalance(pureGoldAmount, moneyAmount);
        }

        TransactionHistory history = TransactionHistory.builder()
                .eventId(eventId)
                .accountSaleCode(saleCode)
                .transactionType(SaleStatus.valueOf(saleType))
                .material(material)
                .goldAmount(pureGoldAmount)
                .moneyAmount(moneyAmount)
                .store(store)
                .factory(factory)
                .transactionHistoryNote("")
                .build();

        transactionHistoryRepository.save(history);

        return saleLog;
    }
}
