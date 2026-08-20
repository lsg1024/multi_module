package com.msa.jewelry.local.transaction_history.service;

import com.msa.common.global.common_enum.sale_enum.SaleStatus;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.local.factory.repository.FactoryRepository;
import com.msa.jewelry.local.factory.service.FactoryService;
import com.msa.jewelry.local.store.repository.StoreRepository;
import com.msa.jewelry.local.transaction_history.dto.PurchaseDto;
import com.msa.jewelry.local.transaction_history.dto.TransactionDto;
import com.msa.jewelry.local.transaction_history.dto.TransactionPage;
import com.msa.jewelry.local.transaction_history.repository.TransactionHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TransactionHistoryService 단위 테스트")
class TransactionHistoryServiceTest {

    private static final Long   STORE_ID   = 100L;
    private static final Long   FACTORY_ID = 200L;
    private static final String STORE_NAME   = "강남금은방";
    private static final String FACTORY_NAME = "한국제조";

    @Mock StoreRepository storeRepository;
    @Mock FactoryRepository factoryRepository;
    @Mock TransactionHistoryRepository transactionHistoryRepository;
    @Mock FactoryService factoryService;

    @InjectMocks
    TransactionHistoryService service;

    @Nested
    @DisplayName("getCurrentBalance")
    class GetCurrentBalance {

        @Test
        @DisplayName("type=store - storeRepository 위임, factoryRepository 호출 없음")
        void store_분기() {
            TransactionDto dto = new TransactionDto("12.345", "1500000");
            given(storeRepository.findByStoreIdAndStoreName(STORE_ID, STORE_NAME)).willReturn(dto);

            TransactionDto result = service.getCurrentBalance("store", STORE_ID.toString(), STORE_NAME);

            assertThat(result).isEqualTo(dto);
            assertThat(result.currentGoldBalance()).isEqualTo("12.345");
            verify(storeRepository).findByStoreIdAndStoreName(STORE_ID, STORE_NAME);
            verifyNoInteractions(factoryRepository);
        }

        @Test
        @DisplayName("type=factory - factoryRepository 위임")
        void factory_분기() {
            TransactionDto dto = new TransactionDto("3.000", "200000");
            given(factoryRepository.findByFactoryIdAndFactoryName(FACTORY_ID, FACTORY_NAME)).willReturn(dto);

            TransactionDto result = service.getCurrentBalance("factory", FACTORY_ID.toString(), FACTORY_NAME);

            assertThat(result).isEqualTo(dto);
            verify(factoryRepository).findByFactoryIdAndFactoryName(FACTORY_ID, FACTORY_NAME);
            verifyNoInteractions(storeRepository);
        }

        @Test
        @DisplayName("type 이 store 가 아닌 임의 문자열(unknown) -> factory 분기로 폴백 (else 처리)")
        void unknown_type_factory_폴백() {
            TransactionDto dto = new TransactionDto("0", "0");
            given(factoryRepository.findByFactoryIdAndFactoryName(eq(FACTORY_ID), any())).willReturn(dto);

            TransactionDto result = service.getCurrentBalance("anything", FACTORY_ID.toString(), FACTORY_NAME);

            assertThat(result).isEqualTo(dto);
            verify(factoryRepository).findByFactoryIdAndFactoryName(FACTORY_ID, FACTORY_NAME);
        }

        @Test
        @DisplayName("ID 가 숫자로 파싱 불가 -> NumberFormatException")
        void 잘못된_ID() {
            assertThatThrownBy(() -> service.getCurrentBalance("store", "not-a-number", STORE_NAME))
                    .isInstanceOf(NumberFormatException.class);

            verifyNoInteractions(storeRepository, factoryRepository);
        }

        @Test
        @DisplayName("결과가 null 이어도 그대로 반환 (불변 조회)")
        void null_허용() {
            given(storeRepository.findByStoreIdAndStoreName(STORE_ID, STORE_NAME)).willReturn(null);

            assertThat(service.getCurrentBalance("store", STORE_ID.toString(), STORE_NAME)).isNull();
        }
    }

    @Nested
    @DisplayName("findAccountPurchase")
    class FindAccountPurchase {

        @Test
        @DisplayName("정상 - 레포에 위임")
        void 정상() {
            Pageable pageable = PageRequest.of(0, 20);
            CustomPage<TransactionPage> empty = new CustomPage<>(Collections.emptyList(), pageable, 0L);
            given(transactionHistoryRepository.findTransactionHistory(
                    "2026-05-01", "2026-05-31", "store", "강남금은방", pageable))
                    .willReturn(empty);

            CustomPage<TransactionPage> result = service.findAccountPurchase(
                    "2026-05-01", "2026-05-31", "store", "강남금은방", pageable);

            assertThat(result.getContent()).isEmpty();
            verify(transactionHistoryRepository).findTransactionHistory(
                    "2026-05-01", "2026-05-31", "store", "강남금은방", pageable);
        }

        @Test
        @DisplayName("페이징 경계 - 큰 페이지 번호 그대로 전달")
        void 큰_페이지() {
            Pageable pageable = PageRequest.of(999, 5);
            CustomPage<TransactionPage> empty = new CustomPage<>(Collections.emptyList(), pageable, 0L);
            given(transactionHistoryRepository.findTransactionHistory(any(), any(), any(), any(), eq(pageable)))
                    .willReturn(empty);

            service.findAccountPurchase("2026-05-01", "2026-05-31", "store", "x", pageable);

            verify(transactionHistoryRepository).findTransactionHistory(
                    "2026-05-01", "2026-05-31", "store", "x", pageable);
        }
    }

    @Nested
    @DisplayName("findFactoryPurchase")
    class FindFactoryPurchase {

        @Test
        @DisplayName("정상 - factory 전용 레포 메서드 위임")
        void 정상() {
            Pageable pageable = PageRequest.of(0, 20);
            CustomPage<TransactionPage> empty = new CustomPage<>(Collections.emptyList(), pageable, 0L);
            given(transactionHistoryRepository.findTransactionHistoryFactory(
                    "2026-05-01", "2026-05-31", "factory", "한국제조", pageable))
                    .willReturn(empty);

            CustomPage<TransactionPage> result = service.findFactoryPurchase(
                    "2026-05-01", "2026-05-31", "factory", "한국제조", pageable);

            assertThat(result.getContent()).isEmpty();
            verify(transactionHistoryRepository).findTransactionHistoryFactory(
                    "2026-05-01", "2026-05-31", "factory", "한국제조", pageable);
        }

        @Test
        @DisplayName("findAccountPurchase 와 별개의 메서드 - 위임 분리 확인")
        void 메서드_분리() {
            Pageable pageable = PageRequest.of(0, 20);
            CustomPage<TransactionPage> empty = new CustomPage<>(Collections.emptyList(), pageable, 0L);
            given(transactionHistoryRepository.findTransactionHistoryFactory(any(), any(), any(), any(), any()))
                    .willReturn(empty);

            service.findFactoryPurchase("2026-05-01", "2026-05-31", "factory", "한국제조", pageable);

            verify(transactionHistoryRepository).findTransactionHistoryFactory(any(), any(), any(), any(), any());
            verify(transactionHistoryRepository, never()).findTransactionHistory(any(), any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("savePurchase / savePurchases")
    class SavePurchase {

        @Test
        @DisplayName("매입(PURCHASE) - applyDelta 에 양수 금/금액 위임, 재질/등록일 전달")
        void 매입_양수() {
            PurchaseDto dto = purchaseDto("PURCHASE", "18K", new BigDecimal("3.333"), 500_000L,
                    null, LocalDateTime.of(2026, 5, 16, 0, 0));
            dto.setAccountId("10");

            service.savePurchase(dto);

            verify(factoryService).applyDelta(
                    eq(10L),
                    eq(new BigDecimal("3.333")),
                    eq(500_000L),
                    any(String.class),
                    eq("PURCHASE"),
                    eq("18K"),
                    eq(null),
                    eq("단위테스트"),
                    eq(LocalDateTime.of(2026, 5, 16, 0, 0)));
            verifyNoInteractions(transactionHistoryRepository);
        }

        @Test
        @DisplayName("결제(PAYMENT) - 금/금액 부호 음수로 잔액 반영")
        void 결제_음수() {
            PurchaseDto dto = purchaseDto("PAYMENT", "24K", new BigDecimal("2.000"), 1_000L, null, null);
            dto.setAccountId("10");

            service.savePurchase(dto);

            verify(factoryService).applyDelta(
                    eq(10L),
                    eq(new BigDecimal("2.000").negate()),
                    eq(-1_000L),
                    any(String.class),
                    eq("PAYMENT"),
                    eq("24K"),
                    eq(null),
                    any(),
                    eq(null));
        }

        @Test
        @DisplayName("표시명(매입) 도 허용 - PURCHASE 로 매핑")
        void 표시명_허용() {
            PurchaseDto dto = purchaseDto("매입", "14K", new BigDecimal("1.000"), 0L, null, null);
            dto.setAccountId("7");

            service.savePurchase(dto);

            verify(factoryService).applyDelta(
                    eq(7L), eq(new BigDecimal("1.000")), eq(0L), any(String.class),
                    eq("PURCHASE"), eq("14K"), eq(null), any(), eq(null));
        }

        @Test
        @DisplayName("잘못된 transactionType -> IllegalArgumentException, applyDelta 미호출")
        void 잘못된_type() {
            PurchaseDto dto = purchaseDto("NOT_A_TYPE", "18K", BigDecimal.ZERO, 0L, null, null);
            dto.setAccountId("1");

            assertThatThrownBy(() -> service.savePurchase(dto))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(factoryService);
        }

        @Test
        @DisplayName("accountId 누락 -> IllegalArgumentException")
        void accountId_누락() {
            PurchaseDto dto = purchaseDto("PURCHASE", "18K", BigDecimal.ZERO, 0L, null, null);
            dto.setAccountId(null);

            assertThatThrownBy(() -> service.savePurchase(dto))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(factoryService);
        }

        @Test
        @DisplayName("savePurchases - 빈 목록이면 IllegalArgumentException")
        void 빈_목록() {
            assertThatThrownBy(() -> service.savePurchases(Collections.emptyList()))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(factoryService);
        }

        @Test
        @DisplayName("savePurchases - 여러 줄을 각각 applyDelta 로 위임")
        void 여러줄() {
            PurchaseDto a = purchaseDto("PURCHASE", "18K", new BigDecimal("1.0"), 100L, null, null);
            a.setAccountId("10");
            PurchaseDto b = purchaseDto("PURCHASE", "24K", new BigDecimal("2.0"), 200L, null, null);
            b.setAccountId("11");

            service.savePurchases(List.of(a, b));

            verify(factoryService).applyDelta(eq(10L), eq(new BigDecimal("1.0")), eq(100L), any(String.class),
                    eq("PURCHASE"), eq("18K"), eq(null), any(), eq(null));
            verify(factoryService).applyDelta(eq(11L), eq(new BigDecimal("2.0")), eq(200L), any(String.class),
                    eq("PURCHASE"), eq("24K"), eq(null), any(), eq(null));
        }
    }

    private static PurchaseDto purchaseDto(String type, String material, BigDecimal gold, Long money,
                                           String saleCode, LocalDateTime txDate) {
        return PurchaseDto.builder()
                .transactionType(type)
                .material(material)
                .goldAmount(gold)
                .moneyAmount(money)
                .saleCode(saleCode)
                .accountId("10")
                .transactionNote("단위테스트")
                .transactionDate(txDate)
                .build();
    }
}
