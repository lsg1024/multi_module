package com.msa.jewelry.local.factory.service;

import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.global.dto.AccountDto;
import com.msa.jewelry.global.excel.dto.AccountExcelDto;
import com.msa.jewelry.global.excel.dto.PurchaseExcelDto;
import com.msa.jewelry.global.exception.ExceptionMessage;
import com.msa.jewelry.global.exception.NotAuthorityException;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.common_option.dto.CommonOptionDto;
import com.msa.jewelry.local.common_option.entity.CommonOption;
import com.msa.jewelry.local.common_option.entity.OptionLevel;
import com.msa.jewelry.local.factory.dto.FactoryDto;
import com.msa.jewelry.local.factory.dto.FactoryView;
import com.msa.jewelry.local.factory.entity.Factory;
import com.msa.jewelry.local.factory.repository.FactoryRepository;
import com.msa.jewelry.local.goldharry.entity.GoldHarry;
import com.msa.jewelry.local.goldharry.repository.GoldHarryRepository;
import com.msa.jewelry.local.transaction_history.entity.TransactionHistory;
import com.msa.jewelry.local.transaction_history.repository.TransactionHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * FactoryServiceImpl 단위 테스트.
 *
 * <p>외부 의존성(AuthorityUserRoleUtil, FactoryRepository, GoldHarryRepository,
 * TransactionHistoryRepository)을 Mockito 로 격리하여 FactoryServiceImpl 의
 * 로직만 검증한다.
 *
 * <p>커버리지 — 20 public 메서드 × (정상 / NOT_FOUND / 권한 거부 / 빈 결과 / 잘못된 상태) 시나리오.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FactoryServiceImpl 단위 테스트")
class FactoryServiceImplTest {

    private static final String TOKEN      = "Bearer test-token";
    private static final Long   FACTORY_ID = 200L;
    private static final String FACTORY_ID_STR = "200";
    private static final String FACTORY_NAME = "한빛제조사";
    private static final String HARRY_ID   = "1";
    private static final String EVENT_ID   = "evt-factory-unit-001";

    @Mock AuthorityUserRoleUtil authorityUserRoleUtil;
    @Mock FactoryRepository factoryRepository;
    @Mock GoldHarryRepository goldHarryRepository;
    @Mock TransactionHistoryRepository transactionHistoryRepository;

    @InjectMocks
    FactoryServiceImpl factoryService;

    // -----------------------------------------------------------------------
    // getFactoryRecentActivity
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFactoryRecentActivity")
    class GetFactoryRecentActivity {

        @Test
        @DisplayName("정상 — 최근 거래 + 결제 집계 합본 반환")
        void 정상() {
            given(transactionHistoryRepository.findRecentSalesByFactory(FACTORY_ID, 20))
                    .willReturn(Collections.emptyList());
            given(transactionHistoryRepository.findPaymentSummaryByFactory(FACTORY_ID))
                    .willReturn(null);

            AccountDto.RecentActivityResponse result =
                    factoryService.getFactoryRecentActivity(FACTORY_ID, 20);

            assertThat(result).isNotNull();
            assertThat(result.getRecentTransactions()).isEmpty();
            verify(transactionHistoryRepository).findRecentSalesByFactory(FACTORY_ID, 20);
            verify(transactionHistoryRepository).findPaymentSummaryByFactory(FACTORY_ID);
        }
    }

    // -----------------------------------------------------------------------
    // getFactoryInfo(String)
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFactoryInfo(String)")
    class GetFactoryInfoString {

        @Test
        @DisplayName("Factory 없음 → NotFoundException(NOT_FOUND_STORE)")
        void 없음() {
            given(factoryRepository.findByFactoryId(FACTORY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> factoryService.getFactoryInfo(FACTORY_ID_STR))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND_STORE);
        }

        @Test
        @DisplayName("정상 — repository 결과 그대로 반환")
        void 정상() {
            AccountDto.AccountSingleResponse expected = mock(AccountDto.AccountSingleResponse.class);
            given(factoryRepository.findByFactoryId(FACTORY_ID)).willReturn(Optional.of(expected));

            AccountDto.AccountSingleResponse result = factoryService.getFactoryInfo(FACTORY_ID_STR);

            assertThat(result).isSameAs(expected);
        }
    }

    // -----------------------------------------------------------------------
    // getFactoryList
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFactoryList")
    class GetFactoryList {

        @Test
        @DisplayName("위임 — repository 호출 결과 그대로 반환")
        void 위임() {
            Pageable pageable = PageRequest.of(0, 20);
            @SuppressWarnings("unchecked")
            CustomPage<FactoryDto.FactoryResponse> page = mock(CustomPage.class);
            given(factoryRepository.findAllFactory("name", "field", "sort", "DESC", pageable))
                    .willReturn(page);

            CustomPage<FactoryDto.FactoryResponse> result =
                    factoryService.getFactoryList("name", "field", "sort", "DESC", pageable);

            assertThat(result).isSameAs(page);
        }
    }

    // -----------------------------------------------------------------------
    // createFactory
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createFactory")
    class CreateFactory {

        @Test
        @DisplayName("이미 존재하는 factoryName → NotFoundException(ALREADY_EXIST_FACTORY)")
        void 이미_존재() {
            AccountDto.AccountInfo accountInfo = mock(AccountDto.AccountInfo.class);
            given(accountInfo.getAccountName()).willReturn(FACTORY_NAME);

            FactoryDto.FactoryRequest req = mock(FactoryDto.FactoryRequest.class);
            given(req.getAccountInfo()).willReturn(accountInfo);
            given(factoryRepository.existsByFactoryName(FACTORY_NAME)).willReturn(true);

            assertThatThrownBy(() -> factoryService.createFactory(req))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.ALREADY_EXIST_FACTORY);

            verify(factoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("GoldHarry 없음 → NotFoundException(WRONG_HARRY)")
        void 해리_없음() {
            AccountDto.AccountInfo accountInfo = mock(AccountDto.AccountInfo.class);
            given(accountInfo.getAccountName()).willReturn(FACTORY_NAME);

            CommonOptionDto.CommonOptionInfo commonOpt = mock(CommonOptionDto.CommonOptionInfo.class);
            given(commonOpt.getGoldHarryId()).willReturn(HARRY_ID);

            FactoryDto.FactoryRequest req = mock(FactoryDto.FactoryRequest.class);
            given(req.getAccountInfo()).willReturn(accountInfo);
            given(req.getCommonOptionInfo()).willReturn(commonOpt);

            given(factoryRepository.existsByFactoryName(FACTORY_NAME)).willReturn(false);
            given(goldHarryRepository.findById(Long.valueOf(HARRY_ID))).willReturn(Optional.empty());

            assertThatThrownBy(() -> factoryService.createFactory(req))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.WRONG_HARRY);
        }
    }

    // -----------------------------------------------------------------------
    // updateFactory
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateFactory")
    class UpdateFactory {

        @Test
        @DisplayName("Factory 없음 → NotFoundException(NOT_FOUND_FACTORY)")
        void factory_없음() {
            given(factoryRepository.findWithAllOptionById(FACTORY_ID)).willReturn(Optional.empty());

            AccountDto.AccountUpdate updateInfo = mock(AccountDto.AccountUpdate.class);
            assertThatThrownBy(() -> factoryService.updateFactory(TOKEN, FACTORY_ID_STR, updateInfo))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND_FACTORY);
        }

        @Test
        @DisplayName("권한 없음 → NotAuthorityException(NO_ROLE)")
        void 권한_없음() {
            Factory factory = mock(Factory.class);
            given(factoryRepository.findWithAllOptionById(FACTORY_ID)).willReturn(Optional.of(factory));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            AccountDto.AccountUpdate updateInfo = mock(AccountDto.AccountUpdate.class);

            assertThatThrownBy(() -> factoryService.updateFactory(TOKEN, FACTORY_ID_STR, updateInfo))
                    .isInstanceOf(NotAuthorityException.class)
                    .hasMessage(ExceptionMessage.NO_ROLE);
        }

        @Test
        @DisplayName("factoryName 변경 시 동일명 중복 검증 → 중복이면 ALREADY_EXIST_FACTORY")
        void 이름_변경_중복() {
            Factory factory = mock(Factory.class);
            given(factory.isNameChanged("새이름")).willReturn(true);
            given(factoryRepository.findWithAllOptionById(FACTORY_ID)).willReturn(Optional.of(factory));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            given(factoryRepository.existsByFactoryName("새이름")).willReturn(true);

            AccountDto.AccountInfo accountInfo = mock(AccountDto.AccountInfo.class);
            given(accountInfo.getAccountName()).willReturn("새이름");
            AccountDto.AccountUpdate updateInfo = mock(AccountDto.AccountUpdate.class);
            given(updateInfo.getAccountInfo()).willReturn(accountInfo);

            assertThatThrownBy(() -> factoryService.updateFactory(TOKEN, FACTORY_ID_STR, updateInfo))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.ALREADY_EXIST_FACTORY);
        }
    }

    // -----------------------------------------------------------------------
    // deleteFactory
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("deleteFactory")
    class DeleteFactory {

        @Test
        @DisplayName("Factory 없음 → NotFoundException(NOT_FOUND_FACTORY)")
        void 없음() {
            given(factoryRepository.findById(FACTORY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> factoryService.deleteFactory(TOKEN, FACTORY_ID_STR))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND_FACTORY);
        }

        @Test
        @DisplayName("권한 없음 → NotAuthorityException")
        void 권한_없음() {
            Factory factory = mock(Factory.class);
            given(factoryRepository.findById(FACTORY_ID)).willReturn(Optional.of(factory));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> factoryService.deleteFactory(TOKEN, FACTORY_ID_STR))
                    .isInstanceOf(NotAuthorityException.class);
            verify(factoryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("정상 — repository.delete 호출")
        void 정상() {
            Factory factory = mock(Factory.class);
            given(factoryRepository.findById(FACTORY_ID)).willReturn(Optional.of(factory));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            factoryService.deleteFactory(TOKEN, FACTORY_ID_STR);

            verify(factoryRepository).delete(factory);
        }
    }

    // -----------------------------------------------------------------------
    // getFactoryIdAndName
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFactoryIdAndName")
    class GetFactoryIdAndName {

        @Test
        @DisplayName("Factory 없음 → NotFoundException")
        void 없음() {
            given(factoryRepository.findWithAllOptionById(FACTORY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> factoryService.getFactoryIdAndName(FACTORY_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND_FACTORY);
        }

        @Test
        @DisplayName("정상 — id/name/goldHarryLoss 매핑")
        void 정상() {
            Factory factory = stubFactoryWithCommonOption();
            given(factoryRepository.findWithAllOptionById(FACTORY_ID)).willReturn(Optional.of(factory));

            FactoryDto.ApiFactoryInfo result = factoryService.getFactoryIdAndName(FACTORY_ID);

            assertThat(result.getFactoryId()).isEqualTo(FACTORY_ID);
            assertThat(result.getFactoryName()).isEqualTo(FACTORY_NAME);
            assertThat(result.getFactoryHarry()).isEqualTo("1.5");
        }
    }

    // -----------------------------------------------------------------------
    // getFactoryGrade
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFactoryGrade")
    class GetFactoryGrade {

        @Test
        @DisplayName("정상 — OptionLevel.getGrade() 반환")
        void 정상() {
            given(factoryRepository.findByCommonOptionOptionLevel(FACTORY_ID))
                    .willReturn(OptionLevel.A);

            assertThat(factoryService.getFactoryGrade(FACTORY_ID_STR))
                    .isEqualTo(OptionLevel.A.getGrade());
        }
    }

    // -----------------------------------------------------------------------
    // updateFactoryHarry
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateFactoryHarry")
    class UpdateFactoryHarry {

        @Test
        @DisplayName("Factory 없음 → NotFoundException(NOT_FOUND_FACTORY)")
        void 없음() {
            given(factoryRepository.findById(FACTORY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> factoryService.updateFactoryHarry(TOKEN, FACTORY_ID_STR, HARRY_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND_FACTORY);
        }

        @Test
        @DisplayName("권한 없음 → NotAuthorityException")
        void 권한_없음() {
            Factory factory = mock(Factory.class);
            given(factoryRepository.findById(FACTORY_ID)).willReturn(Optional.of(factory));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> factoryService.updateFactoryHarry(TOKEN, FACTORY_ID_STR, HARRY_ID))
                    .isInstanceOf(NotAuthorityException.class);
        }

        @Test
        @DisplayName("GoldHarry 없음 → NotFoundException(WRONG_HARRY)")
        void 해리_없음() {
            Factory factory = mock(Factory.class);
            given(factoryRepository.findById(FACTORY_ID)).willReturn(Optional.of(factory));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            given(goldHarryRepository.findById(Long.valueOf(HARRY_ID))).willReturn(Optional.empty());

            assertThatThrownBy(() -> factoryService.updateFactoryHarry(TOKEN, FACTORY_ID_STR, HARRY_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.WRONG_HARRY);
        }
    }

    // -----------------------------------------------------------------------
    // updateFactoryGrade
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateFactoryGrade")
    class UpdateFactoryGrade {

        @Test
        @DisplayName("Factory 없음 → NotFoundException")
        void 없음() {
            given(factoryRepository.findById(FACTORY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> factoryService.updateFactoryGrade(TOKEN, FACTORY_ID_STR, "A"))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("권한 없음 → NotAuthorityException")
        void 권한_없음() {
            Factory factory = mock(Factory.class);
            given(factoryRepository.findById(FACTORY_ID)).willReturn(Optional.of(factory));
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> factoryService.updateFactoryGrade(TOKEN, FACTORY_ID_STR, "A"))
                    .isInstanceOf(NotAuthorityException.class);
        }
    }

    // -----------------------------------------------------------------------
    // getExcel
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getExcel")
    class GetExcel {

        @Test
        @DisplayName("권한 없음 → NotAuthorityException, repo 호출 없음")
        void 권한_없음() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> factoryService.getExcel(TOKEN))
                    .isInstanceOf(NotAuthorityException.class)
                    .hasMessage(ExceptionMessage.NO_ROLE);

            verify(factoryRepository, never()).findAllFactoryExcel();
        }

        @Test
        @DisplayName("정상 — repository 결과 그대로 반환")
        void 정상() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            given(factoryRepository.findAllFactoryExcel()).willReturn(Collections.emptyList());

            List<AccountExcelDto> result = factoryService.getExcel(TOKEN);
            assertThat(result).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getFactoryInfoByName
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFactoryInfoByName")
    class GetFactoryInfoByName {

        @Test
        @DisplayName("빈 결과 → NotFoundException(NOT_FOUND_FACTORY)")
        void 없음() {
            given(factoryRepository.findByFactoryNameIgnoreCase(FACTORY_NAME))
                    .willReturn(Collections.emptyList());

            assertThatThrownBy(() -> factoryService.getFactoryInfoByName(FACTORY_NAME))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND_FACTORY);
        }

        @Test
        @DisplayName("정상 — 첫 번째 Factory 의 ApiFactoryInfo 반환")
        void 정상() {
            Factory factory = stubFactoryWithCommonOption();
            given(factoryRepository.findByFactoryNameIgnoreCase(FACTORY_NAME))
                    .willReturn(List.of(factory));

            FactoryDto.ApiFactoryInfo result = factoryService.getFactoryInfoByName(FACTORY_NAME);

            assertThat(result.getFactoryId()).isEqualTo(FACTORY_ID);
            assertThat(result.getFactoryName()).isEqualTo(FACTORY_NAME);
        }
    }

    // -----------------------------------------------------------------------
    // findAllFactory
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("findAllFactory")
    class FindAllFactory {

        @Test
        @DisplayName("위임")
        void 위임() {
            given(factoryRepository.findAllFactory()).willReturn(Collections.emptyList());
            assertThat(factoryService.findAllFactory()).isEmpty();
            verify(factoryRepository).findAllFactory();
        }
    }

    // -----------------------------------------------------------------------
    // getFactoryPurchase
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFactoryPurchase")
    class GetFactoryPurchase {

        @Test
        @DisplayName("위임 — endAt/pageable 전달")
        void 위임() {
            Pageable pageable = PageRequest.of(0, 20);
            @SuppressWarnings("unchecked")
            CustomPage<AccountDto.AccountResponse> page = mock(CustomPage.class);
            given(factoryRepository.findAllFactoryAndPurchase("2026-05-31", pageable)).willReturn(page);

            CustomPage<AccountDto.AccountResponse> result =
                    factoryService.getFactoryPurchase("2026-05-31", pageable);

            assertThat(result).isSameAs(page);
        }
    }

    // -----------------------------------------------------------------------
    // getPurchaseExcel
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getPurchaseExcel")
    class GetPurchaseExcel {

        @Test
        @DisplayName("권한 없음 → NotAuthorityException")
        void 권한_없음() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> factoryService.getPurchaseExcel(TOKEN, "2026-05-31"))
                    .isInstanceOf(NotAuthorityException.class);
        }

        @Test
        @DisplayName("정상 — 빈 리스트라도 byte[] 반환")
        void 정상_빈리스트() throws Exception {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            given(factoryRepository.findAllPurchaseExcel("2026-05-31"))
                    .willReturn(Collections.emptyList());

            byte[] bytes = factoryService.getPurchaseExcel(TOKEN, "2026-05-31");
            assertThat(bytes).isNotNull();
            assertThat(bytes).isNotEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // getFactoryInfo(Long)
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getFactoryInfo(Long)")
    class GetFactoryInfoLong {

        @Test
        @DisplayName("Factory 없음 → NotFoundException ('제조사 미존재:' 메시지)")
        void 없음() {
            given(factoryRepository.findWithAllOptionById(FACTORY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> factoryService.getFactoryInfo(FACTORY_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("제조사 미존재");
        }

        @Test
        @DisplayName("정상 — FactoryView 매핑")
        void 정상() {
            Factory factory = stubFactoryWithCommonOption();
            given(factoryRepository.findWithAllOptionById(FACTORY_ID)).willReturn(Optional.of(factory));

            FactoryView result = factoryService.getFactoryInfo(FACTORY_ID);

            assertThat(result.factoryId()).isEqualTo(FACTORY_ID);
            assertThat(result.factoryName()).isEqualTo(FACTORY_NAME);
            assertThat(result.goldHarryLoss()).isEqualTo("1.5");
            assertThat(result.grade()).isEqualTo(OptionLevel.A.name());
        }

        @Test
        @DisplayName("commonOption 이 null 인 경우 — grade/harry 가 null 로 매핑")
        void commonOption_null() {
            Factory factory = mock(Factory.class);
            given(factory.getFactoryId()).willReturn(FACTORY_ID);
            given(factory.getFactoryName()).willReturn(FACTORY_NAME);
            given(factory.getCommonOption()).willReturn(null);
            given(factoryRepository.findWithAllOptionById(FACTORY_ID)).willReturn(Optional.of(factory));

            FactoryView result = factoryService.getFactoryInfo(FACTORY_ID);

            assertThat(result.factoryId()).isEqualTo(FACTORY_ID);
            assertThat(result.grade()).isNull();
            assertThat(result.goldHarryLoss()).isNull();
        }
    }

    // -----------------------------------------------------------------------
    // findFactoryByName
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("findFactoryByName")
    class FindFactoryByName {

        @Test
        @DisplayName("빈 결과 — null 반환 (NotFoundException 아님)")
        void 없음() {
            given(factoryRepository.findByFactoryNameIgnoreCase(FACTORY_NAME))
                    .willReturn(Collections.emptyList());

            assertThat(factoryService.findFactoryByName(FACTORY_NAME)).isNull();
        }

        @Test
        @DisplayName("정상 — 첫 번째 결과를 view 로 매핑")
        void 정상() {
            Factory factory = stubFactoryWithCommonOption();
            given(factoryRepository.findByFactoryNameIgnoreCase(FACTORY_NAME))
                    .willReturn(List.of(factory));

            FactoryView result = factoryService.findFactoryByName(FACTORY_NAME);

            assertThat(result).isNotNull();
            assertThat(result.factoryId()).isEqualTo(FACTORY_ID);
        }
    }

    // -----------------------------------------------------------------------
    // findAllFactoryView
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("findAllFactoryView")
    class FindAllFactoryView {

        @Test
        @DisplayName("빈 결과 — 빈 리스트")
        void 빈() {
            given(factoryRepository.findAll()).willReturn(Collections.emptyList());

            assertThat(factoryService.findAllFactoryView()).isEmpty();
        }

        @Test
        @DisplayName("정상 — Factory → FactoryView 매핑 리스트")
        void 정상() {
            Factory factory = stubFactoryWithCommonOption();
            given(factoryRepository.findAll()).willReturn(List.of(factory));

            List<FactoryView> result = factoryService.findAllFactoryView();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).factoryId()).isEqualTo(FACTORY_ID);
        }
    }

    // -----------------------------------------------------------------------
    // applyDelta
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("applyDelta")
    class ApplyDelta {

        @Test
        @DisplayName("Factory 없음 → NotFoundException ('Factory not found:' 메시지)")
        void 없음() {
            given(factoryRepository.findByIdWithLock(FACTORY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> factoryService.applyDelta(FACTORY_ID,
                    new BigDecimal("1.000"), 50_000L, EVENT_ID, "PAYMENT", "18K", 5L, "테스트"))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("Factory not found");

            verify(transactionHistoryRepository, never()).save(any());
        }

        @Test
        @DisplayName("정상 — factory.updateBalance + transactionHistory 저장")
        void 정상() {
            Factory factory = mock(Factory.class);
            given(factoryRepository.findByIdWithLock(FACTORY_ID)).willReturn(Optional.of(factory));

            factoryService.applyDelta(FACTORY_ID,
                    new BigDecimal("1.000"), 50_000L, EVENT_ID, "PAYMENT", "18K", 5L, "정상 결제");

            verify(factory).updateBalance(new BigDecimal("1.000"), 50_000L);
            ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
            verify(transactionHistoryRepository).save(captor.capture());
        }

        @Test
        @DisplayName("goldDelta / moneyDelta 가 null 이면 0 으로 대체")
        void null_델타() {
            Factory factory = mock(Factory.class);
            given(factoryRepository.findByIdWithLock(FACTORY_ID)).willReturn(Optional.of(factory));

            factoryService.applyDelta(FACTORY_ID, null, null, EVENT_ID,
                    "SALE", "18K", 5L, "null 델타");

            verify(factory).updateBalance(BigDecimal.ZERO, 0L);
            verify(transactionHistoryRepository).save(any());
        }

        @Test
        @DisplayName("알 수 없는 transactionType — 예외 없이 null 로 저장")
        void 알수없는_타입() {
            Factory factory = mock(Factory.class);
            given(factoryRepository.findByIdWithLock(FACTORY_ID)).willReturn(Optional.of(factory));

            factoryService.applyDelta(FACTORY_ID, BigDecimal.ONE, 100L, EVENT_ID,
                    "NOT_A_REAL_TYPE", "18K", 5L, "비정상 타입");

            // 예외가 밖으로 새어 나가지 않고 정상 저장
            verify(transactionHistoryRepository).save(any());
        }

        @Test
        @DisplayName("transactionType 이 null/blank — null 로 저장")
        void null_타입() {
            Factory factory = mock(Factory.class);
            given(factoryRepository.findByIdWithLock(FACTORY_ID)).willReturn(Optional.of(factory));

            factoryService.applyDelta(FACTORY_ID, BigDecimal.ONE, 100L, EVENT_ID,
                    null, "18K", 5L, "타입 누락");

            verify(transactionHistoryRepository).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------
    private static Factory stubFactoryWithCommonOption() {
        Factory factory = mock(Factory.class);
        CommonOption commonOption = mock(CommonOption.class);
        given(factory.getFactoryId()).willReturn(FACTORY_ID);
        given(factory.getFactoryName()).willReturn(FACTORY_NAME);
        given(factory.getCommonOption()).willReturn(commonOption);
        given(commonOption.getGoldHarryLoss()).willReturn("1.5");
        given(commonOption.getOptionLevel()).willReturn(OptionLevel.A);
        return factory;
    }
}
