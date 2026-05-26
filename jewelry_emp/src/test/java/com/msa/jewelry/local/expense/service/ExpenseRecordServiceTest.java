package com.msa.jewelry.local.expense.service;

import com.msa.common.global.common_enum.expense_enum.ExpenseType;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.local.expense.dto.ExpenseRecordDto;
import com.msa.jewelry.local.expense.entity.ExpenseBankType;
import com.msa.jewelry.local.expense.entity.ExpenseExpenseAccount;
import com.msa.jewelry.local.expense.entity.ExpenseIncomeAccount;
import com.msa.jewelry.local.expense.entity.ExpenseRecord;
import com.msa.jewelry.local.expense.repository.ExpenseBankTypeRepository;
import com.msa.jewelry.local.expense.repository.ExpenseExpenseAccountRepository;
import com.msa.jewelry.local.expense.repository.ExpenseIncomeAccountRepository;
import com.msa.jewelry.local.expense.repository.ExpenseRecordRepository;
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
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * ExpenseRecordService 단위 테스트.
 *
 * <p>외부 의존성 (Repository 4종) 을 Mockito 로 격리하여 서비스 로직만 검증한다.
 *
 * <p>커버리지:
 * <ul>
 *   <li>날짜 기반 조회 5종 (전체/타입/은행/거래처/복합) — 위임 파라미터 캡쳐 검증</li>
 *   <li>CRUD (단건/배치) — NOT_FOUND, 잘못된 ExpenseType, 거래처 미존재 등</li>
 *   <li>합계(getSummary) — null 안전 처리 및 음수(netAmount) 계산</li>
 *   <li>날짜 파싱 분기 (빈 문자열, ISO, 잘못된 형식)</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ExpenseRecordService 단위 테스트")
class ExpenseRecordServiceTest {

    private static final Long   RECORD_ID  = 1_001L;
    private static final Long   BANK_ID    = 10L;
    private static final Long   INCOME_ID  = 20L;
    private static final Long   EXPENSE_ID = 30L;
    private static final String START      = "2026-05-01";
    private static final String END        = "2026-05-31";

    @Mock ExpenseRecordRepository recordRepository;
    @Mock ExpenseBankTypeRepository bankTypeRepository;
    @Mock ExpenseIncomeAccountRepository incomeAccountRepository;
    @Mock ExpenseExpenseAccountRepository expenseAccountRepository;

    @InjectMocks
    ExpenseRecordService service;

    // -----------------------------------------------------------------------
    // getExpenseRecordsByDateRange
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getExpenseRecordsByDateRange")
    class GetByDateRange {

        @Test
        @DisplayName("정상 — 시작/종료 LocalDateTime 으로 변환되어 위임")
        void 정상조회() {
            Pageable pageable = PageRequest.of(0, 20);
            given(recordRepository.findByDateRange(any(), any(), eq(pageable)))
                    .willReturn(emptyPage(pageable));

            CustomPage<ExpenseRecordDto.ListResponse> result =
                    service.getExpenseRecordsByDateRange(START, END, pageable);

            assertThat(result.getContent()).isEmpty();

            ArgumentCaptor<LocalDateTime> startCap = ArgumentCaptor.forClass(LocalDateTime.class);
            ArgumentCaptor<LocalDateTime> endCap   = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(recordRepository).findByDateRange(startCap.capture(), endCap.capture(), eq(pageable));

            assertThat(startCap.getValue().getHour()).isZero();
            assertThat(endCap.getValue().getHour()).isEqualTo(23);
        }

        @Test
        @DisplayName("빈 날짜 문자열 → 오늘 자정/23:59:59 로 대체")
        void 빈_날짜_기본값() {
            Pageable pageable = PageRequest.of(0, 10);
            given(recordRepository.findByDateRange(any(), any(), any()))
                    .willReturn(emptyPage(pageable));

            service.getExpenseRecordsByDateRange("", "", pageable);

            verify(recordRepository).findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class), eq(pageable));
        }

        @Test
        @DisplayName("결과가 있을 때 ListResponse 변환 — 수입은 incomeAccount 명 사용")
        void 변환_INCOME() {
            Pageable pageable = PageRequest.of(0, 20);
            ExpenseRecord rec = mockIncomeRecord();
            given(recordRepository.findByDateRange(any(), any(), eq(pageable)))
                    .willReturn(new PageImpl<>(List.of(rec), pageable, 1));

            CustomPage<ExpenseRecordDto.ListResponse> result =
                    service.getExpenseRecordsByDateRange(START, END, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getExpenseType()).isEqualTo("INCOME");
            assertThat(result.getContent().get(0).getAccountName()).isEqualTo("판매수입");
        }

        @Test
        @DisplayName("잘못된 날짜 포맷 → DateTimeParseException")
        void 잘못된_날짜_포맷() {
            Pageable pageable = PageRequest.of(0, 10);

            assertThatThrownBy(() -> service.getExpenseRecordsByDateRange("not-a-date", END, pageable))
                    .isInstanceOf(java.time.format.DateTimeParseException.class);

            verifyNoInteractions(recordRepository);
        }
    }

    // -----------------------------------------------------------------------
    // getExpenseRecordsByDateRangeAndType
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getExpenseRecordsByDateRangeAndType")
    class GetByDateRangeAndType {

        @Test
        @DisplayName("정상 — 소문자도 toUpperCase 후 enum 변환")
        void 소문자_타입() {
            Pageable pageable = PageRequest.of(0, 20);
            given(recordRepository.findByDateRangeAndExpenseType(any(), any(), eq(ExpenseType.EXPENSE), eq(pageable)))
                    .willReturn(emptyPage(pageable));

            CustomPage<ExpenseRecordDto.ListResponse> result =
                    service.getExpenseRecordsByDateRangeAndType(START, END, "expense", pageable);

            assertThat(result.getContent()).isEmpty();
            verify(recordRepository).findByDateRangeAndExpenseType(any(), any(), eq(ExpenseType.EXPENSE), eq(pageable));
        }

        @Test
        @DisplayName("존재하지 않는 ExpenseType → IllegalArgumentException")
        void 잘못된_타입() {
            Pageable pageable = PageRequest.of(0, 20);

            assertThatThrownBy(() ->
                    service.getExpenseRecordsByDateRangeAndType(START, END, "NOT_EXIST", pageable))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(recordRepository, never()).findByDateRangeAndExpenseType(any(), any(), any(), any());
        }

        @Test
        @DisplayName("INCOME 타입으로 정상 위임")
        void INCOME_타입() {
            Pageable pageable = PageRequest.of(0, 5);
            given(recordRepository.findByDateRangeAndExpenseType(any(), any(), eq(ExpenseType.INCOME), eq(pageable)))
                    .willReturn(emptyPage(pageable));

            service.getExpenseRecordsByDateRangeAndType(START, END, "INCOME", pageable);

            verify(recordRepository).findByDateRangeAndExpenseType(any(), any(), eq(ExpenseType.INCOME), eq(pageable));
        }
    }

    // -----------------------------------------------------------------------
    // getExpenseRecordsByDateRangeAndBankType
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getExpenseRecordsByDateRangeAndBankType")
    class GetByDateRangeAndBankType {

        @Test
        @DisplayName("정상 — bankTypeId 그대로 위임")
        void 정상() {
            Pageable pageable = PageRequest.of(0, 20);
            given(recordRepository.findByDateRangeAndBankType(any(), any(), eq(BANK_ID), eq(pageable)))
                    .willReturn(emptyPage(pageable));

            CustomPage<ExpenseRecordDto.ListResponse> result =
                    service.getExpenseRecordsByDateRangeAndBankType(START, END, BANK_ID, pageable);

            assertThat(result.getContent()).isEmpty();
            verify(recordRepository).findByDateRangeAndBankType(any(), any(), eq(BANK_ID), eq(pageable));
        }

        @Test
        @DisplayName("페이징 경계 — 마지막 페이지 size 검증")
        void 페이징_경계() {
            Pageable pageable = PageRequest.of(99, 10);
            given(recordRepository.findByDateRangeAndBankType(any(), any(), eq(BANK_ID), eq(pageable)))
                    .willReturn(emptyPage(pageable));

            CustomPage<ExpenseRecordDto.ListResponse> result =
                    service.getExpenseRecordsByDateRangeAndBankType(START, END, BANK_ID, pageable);

            assertThat(result.getNumber()).isEqualTo(99);
        }
    }

    // -----------------------------------------------------------------------
    // getExpenseRecordsByDateRangeAndCounterparty
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getExpenseRecordsByDateRangeAndCounterparty")
    class GetByDateRangeAndCounterparty {

        @Test
        @DisplayName("거래처 검색 — 위임만 수행")
        void 정상() {
            Pageable pageable = PageRequest.of(0, 20);
            given(recordRepository.findByDateRangeAndCounterparty(any(), any(), eq("ABC상사"), eq(pageable)))
                    .willReturn(emptyPage(pageable));

            CustomPage<ExpenseRecordDto.ListResponse> result =
                    service.getExpenseRecordsByDateRangeAndCounterparty(START, END, "ABC상사", pageable);

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("빈 검색어 — 그대로 위임 (필터 로직은 레포에 위임)")
        void 빈_검색어() {
            Pageable pageable = PageRequest.of(0, 20);
            given(recordRepository.findByDateRangeAndCounterparty(any(), any(), eq(""), eq(pageable)))
                    .willReturn(emptyPage(pageable));

            service.getExpenseRecordsByDateRangeAndCounterparty(START, END, "", pageable);

            verify(recordRepository).findByDateRangeAndCounterparty(any(), any(), eq(""), eq(pageable));
        }
    }

    // -----------------------------------------------------------------------
    // getExpenseRecordsByDateRangeAndFilters
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getExpenseRecordsByDateRangeAndFilters")
    class GetByDateRangeAndFilters {

        @Test
        @DisplayName("type + bankTypeId 복합 조건 — 정상 위임")
        void 정상() {
            Pageable pageable = PageRequest.of(0, 20);
            given(recordRepository.findByDateRangeAndExpenseTypeAndBankType(
                    any(), any(), eq(ExpenseType.INCOME), eq(BANK_ID), eq(pageable)))
                    .willReturn(emptyPage(pageable));

            service.getExpenseRecordsByDateRangeAndFilters(START, END, "income", BANK_ID, pageable);

            verify(recordRepository).findByDateRangeAndExpenseTypeAndBankType(
                    any(), any(), eq(ExpenseType.INCOME), eq(BANK_ID), eq(pageable));
        }

        @Test
        @DisplayName("잘못된 type → IllegalArgumentException, 레포 호출 안 함")
        void 잘못된_타입() {
            Pageable pageable = PageRequest.of(0, 20);

            assertThatThrownBy(() ->
                    service.getExpenseRecordsByDateRangeAndFilters(START, END, "INVALID", BANK_ID, pageable))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(recordRepository);
        }
    }

    // -----------------------------------------------------------------------
    // getExpenseRecordById
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getExpenseRecordById")
    class GetById {

        @Test
        @DisplayName("정상 — DetailResponse 변환")
        void 정상() {
            ExpenseRecord rec = mockExpenseRecord();
            given(recordRepository.findById(RECORD_ID)).willReturn(Optional.of(rec));

            ExpenseRecordDto.DetailResponse resp = service.getExpenseRecordById(RECORD_ID);

            assertThat(resp.getId()).isEqualTo(RECORD_ID);
            assertThat(resp.getExpenseType()).isEqualTo("EXPENSE");
            assertThat(resp.getCounterparty()).isEqualTo("XYZ제작소");
        }

        @Test
        @DisplayName("미존재 — IllegalArgumentException(not found 메시지 포함)")
        void 없음() {
            given(recordRepository.findById(RECORD_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getExpenseRecordById(RECORD_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");
        }
    }

    // -----------------------------------------------------------------------
    // createExpenseRecord
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createExpenseRecord")
    class CreateExpenseRecord {

        @Test
        @DisplayName("EXPENSE — 정상 저장, expenseAccount 만 set")
        void 지출_정상() {
            ExpenseRecordDto.CreateRequest req = mockCreateRequest("EXPENSE", null, EXPENSE_ID);
            ExpenseBankType bankType = mockBankType();
            ExpenseExpenseAccount expAcc = mockExpenseAccount();

            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.of(bankType));
            given(expenseAccountRepository.findById(EXPENSE_ID)).willReturn(Optional.of(expAcc));
            given(recordRepository.save(any(ExpenseRecord.class))).willAnswer(inv -> {
                ExpenseRecord saved = inv.getArgument(0);
                // simulate id assigned
                return saved;
            });

            ExpenseRecordDto.DetailResponse resp = service.createExpenseRecord(req);

            assertThat(resp).isNotNull();
            verify(incomeAccountRepository, never()).findById(any());
            verify(recordRepository).save(any(ExpenseRecord.class));
        }

        @Test
        @DisplayName("INCOME — incomeAccount 만 set, 지출 계정 lookup 안 함")
        void 수입_정상() {
            ExpenseRecordDto.CreateRequest req = mockCreateRequest("INCOME", INCOME_ID, null);
            ExpenseBankType bankType = mockBankType();
            ExpenseIncomeAccount incAcc = mockIncomeAccount();

            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.of(bankType));
            given(incomeAccountRepository.findById(INCOME_ID)).willReturn(Optional.of(incAcc));
            given(recordRepository.save(any(ExpenseRecord.class))).willAnswer(inv -> inv.getArgument(0));

            service.createExpenseRecord(req);

            verify(expenseAccountRepository, never()).findById(any());
            verify(incomeAccountRepository).findById(INCOME_ID);
            verify(recordRepository).save(any(ExpenseRecord.class));
        }

        @Test
        @DisplayName("BankType 미존재 → IllegalArgumentException")
        void 은행유형_없음() {
            ExpenseRecordDto.CreateRequest req = mockCreateRequest("EXPENSE", null, EXPENSE_ID);
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.createExpenseRecord(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bank type not found");

            verify(recordRepository, never()).save(any());
        }

        @Test
        @DisplayName("INCOME 타입인데 incomeAccount 미존재 → 예외")
        void 수입계정_없음() {
            ExpenseRecordDto.CreateRequest req = mockCreateRequest("INCOME", INCOME_ID, null);
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.of(mockBankType()));
            given(incomeAccountRepository.findById(INCOME_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.createExpenseRecord(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Income account not found");
        }

        @Test
        @DisplayName("EXPENSE 타입인데 expenseAccount 미존재 → 예외")
        void 지출계정_없음() {
            ExpenseRecordDto.CreateRequest req = mockCreateRequest("EXPENSE", null, EXPENSE_ID);
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.of(mockBankType()));
            given(expenseAccountRepository.findById(EXPENSE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.createExpenseRecord(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Expense account not found");
        }

        @Test
        @DisplayName("expenseType 잘못된 값 → IllegalArgumentException(enum)")
        void 잘못된_expenseType() {
            ExpenseRecordDto.CreateRequest req = mock(ExpenseRecordDto.CreateRequest.class);
            given(req.getBankTypeId()).willReturn(BANK_ID);
            given(req.getExpenseType()).willReturn("WRONG");
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.of(mockBankType()));

            assertThatThrownBy(() -> service.createExpenseRecord(req))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("INCOME 인데 incomeAccountId 가 null → 계정 조회 스킵, 저장은 진행")
        void 수입_계정ID_null() {
            ExpenseRecordDto.CreateRequest req = mockCreateRequest("INCOME", null, null);
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.of(mockBankType()));
            given(recordRepository.save(any(ExpenseRecord.class))).willAnswer(inv -> inv.getArgument(0));

            service.createExpenseRecord(req);

            verify(incomeAccountRepository, never()).findById(any());
            verify(recordRepository).save(any(ExpenseRecord.class));
        }
    }

    // -----------------------------------------------------------------------
    // updateExpenseRecord
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateExpenseRecord")
    class UpdateExpenseRecord {

        @Test
        @DisplayName("정상 — record.update() 호출 및 save")
        void 정상() {
            ExpenseRecordDto.UpdateRequest req = mockUpdateRequest("EXPENSE", null, EXPENSE_ID);
            ExpenseRecord existing = mock(ExpenseRecord.class);
            given(existing.getExpenseRecordId()).willReturn(RECORD_ID);
            given(existing.getExpenseType()).willReturn(ExpenseType.EXPENSE);

            given(recordRepository.findById(RECORD_ID)).willReturn(Optional.of(existing));
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.of(mockBankType()));
            given(expenseAccountRepository.findById(EXPENSE_ID)).willReturn(Optional.of(mockExpenseAccount()));
            given(recordRepository.save(any(ExpenseRecord.class))).willReturn(existing);

            service.updateExpenseRecord(RECORD_ID, req);

            verify(existing).update(any(), eq(ExpenseType.EXPENSE), any(), any(), any(),
                    any(), any(), any(), any(), any(), any(), any(), any());
            verify(recordRepository).save(existing);
        }

        @Test
        @DisplayName("record 미존재 → IllegalArgumentException")
        void 미존재() {
            ExpenseRecordDto.UpdateRequest req = mockUpdateRequest("EXPENSE", null, EXPENSE_ID);
            given(recordRepository.findById(RECORD_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateExpenseRecord(RECORD_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");

            verify(bankTypeRepository, never()).findById(any());
        }

        @Test
        @DisplayName("BankType 미존재 → IllegalArgumentException")
        void 은행유형_없음() {
            ExpenseRecordDto.UpdateRequest req = mockUpdateRequest("EXPENSE", null, EXPENSE_ID);
            given(recordRepository.findById(RECORD_ID)).willReturn(Optional.of(mock(ExpenseRecord.class)));
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateExpenseRecord(RECORD_ID, req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bank type not found");
        }

        @Test
        @DisplayName("동시성 충돌 — OptimisticLockingFailureException 전파")
        void 낙관락_충돌() {
            ExpenseRecordDto.UpdateRequest req = mockUpdateRequest("EXPENSE", null, EXPENSE_ID);
            ExpenseRecord existing = mock(ExpenseRecord.class);
            given(existing.getExpenseType()).willReturn(ExpenseType.EXPENSE);

            given(recordRepository.findById(RECORD_ID)).willReturn(Optional.of(existing));
            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.of(mockBankType()));
            given(expenseAccountRepository.findById(EXPENSE_ID)).willReturn(Optional.of(mockExpenseAccount()));
            willThrow(new OptimisticLockingFailureException("lock"))
                    .given(recordRepository).save(any(ExpenseRecord.class));

            assertThatThrownBy(() -> service.updateExpenseRecord(RECORD_ID, req))
                    .isInstanceOf(OptimisticLockingFailureException.class);
        }
    }

    // -----------------------------------------------------------------------
    // deleteExpenseRecord
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("deleteExpenseRecord")
    class DeleteExpenseRecord {

        @Test
        @DisplayName("정상 — existsById=true 인 경우 deleteById 호출")
        void 정상() {
            given(recordRepository.existsById(RECORD_ID)).willReturn(true);

            service.deleteExpenseRecord(RECORD_ID);

            verify(recordRepository).deleteById(RECORD_ID);
        }

        @Test
        @DisplayName("미존재 → IllegalArgumentException, deleteById 호출 안 함")
        void 미존재() {
            given(recordRepository.existsById(RECORD_ID)).willReturn(false);

            assertThatThrownBy(() -> service.deleteExpenseRecord(RECORD_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("not found");

            verify(recordRepository, never()).deleteById(any());
        }
    }

    // -----------------------------------------------------------------------
    // createRecordsBatch
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createRecordsBatch")
    class CreateRecordsBatch {

        @Test
        @DisplayName("배치 — 요청 개수만큼 createExpenseRecord 호출")
        void 정상_3건() {
            ExpenseRecordDto.CreateRequest r1 = mockCreateRequest("EXPENSE", null, EXPENSE_ID);
            ExpenseRecordDto.CreateRequest r2 = mockCreateRequest("INCOME", INCOME_ID, null);
            ExpenseRecordDto.CreateRequest r3 = mockCreateRequest("EXPENSE", null, EXPENSE_ID);

            given(bankTypeRepository.findById(BANK_ID)).willReturn(Optional.of(mockBankType()));
            given(expenseAccountRepository.findById(EXPENSE_ID)).willReturn(Optional.of(mockExpenseAccount()));
            given(incomeAccountRepository.findById(INCOME_ID)).willReturn(Optional.of(mockIncomeAccount()));
            given(recordRepository.save(any(ExpenseRecord.class))).willAnswer(inv -> inv.getArgument(0));

            service.createRecordsBatch(Arrays.asList(r1, r2, r3));

            verify(recordRepository, times(3)).save(any(ExpenseRecord.class));
        }

        @Test
        @DisplayName("빈 리스트 → 아무 동작 안 함")
        void 빈_리스트() {
            service.createRecordsBatch(Collections.emptyList());

            verifyNoInteractions(recordRepository, bankTypeRepository,
                    incomeAccountRepository, expenseAccountRepository);
        }

        @Test
        @DisplayName("두 번째 요청에서 예외 발생 → 전체 롤백 (전파) — 첫 번째는 save 됨")
        void 중간_실패_전파() {
            ExpenseRecordDto.CreateRequest r1 = mockCreateRequest("EXPENSE", null, EXPENSE_ID);
            ExpenseRecordDto.CreateRequest r2 = mockCreateRequest("EXPENSE", null, EXPENSE_ID);

            given(bankTypeRepository.findById(BANK_ID))
                    .willReturn(Optional.of(mockBankType()))
                    .willReturn(Optional.empty()); // 두 번째 호출에서 실패
            given(expenseAccountRepository.findById(EXPENSE_ID)).willReturn(Optional.of(mockExpenseAccount()));
            given(recordRepository.save(any(ExpenseRecord.class))).willAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() -> service.createRecordsBatch(Arrays.asList(r1, r2)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Bank type not found");

            verify(recordRepository, times(1)).save(any(ExpenseRecord.class));
        }
    }

    // -----------------------------------------------------------------------
    // getSummary
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getSummary")
    class GetSummary {

        @Test
        @DisplayName("정상 — netWeight/netAmount 계산")
        void 정상_합계() {
            given(recordRepository.sumIncomeWeight(any(), any())).willReturn(new BigDecimal("30.500"));
            given(recordRepository.sumIncomeAmount(any(), any())).willReturn(5_000_000L);
            given(recordRepository.sumExpenseWeight(any(), any())).willReturn(new BigDecimal("15.250"));
            given(recordRepository.sumExpenseAmount(any(), any())).willReturn(2_500_000L);

            ExpenseRecordDto.SummaryResponse resp = service.getSummary(START, END);

            assertThat(resp.getTotalIncomeWeight()).isEqualByComparingTo("30.500");
            assertThat(resp.getTotalExpenseAmount()).isEqualTo(2_500_000L);
            assertThat(resp.getNetWeight()).isEqualByComparingTo("15.250");
            assertThat(resp.getNetAmount()).isEqualTo(2_500_000L);
        }

        @Test
        @DisplayName("모든 합계가 null 인 경우 — 0/ZERO 로 대체")
        void 모두_null() {
            given(recordRepository.sumIncomeWeight(any(), any())).willReturn(null);
            given(recordRepository.sumIncomeAmount(any(), any())).willReturn(null);
            given(recordRepository.sumExpenseWeight(any(), any())).willReturn(null);
            given(recordRepository.sumExpenseAmount(any(), any())).willReturn(null);

            ExpenseRecordDto.SummaryResponse resp = service.getSummary(START, END);

            assertThat(resp.getTotalIncomeWeight()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(resp.getTotalIncomeAmount()).isZero();
            assertThat(resp.getTotalExpenseWeight()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(resp.getTotalExpenseAmount()).isZero();
            assertThat(resp.getNetWeight()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(resp.getNetAmount()).isZero();
        }

        @Test
        @DisplayName("지출이 수입보다 큰 경우 — netAmount 음수")
        void 음수_순익() {
            given(recordRepository.sumIncomeWeight(any(), any())).willReturn(new BigDecimal("1.000"));
            given(recordRepository.sumIncomeAmount(any(), any())).willReturn(100_000L);
            given(recordRepository.sumExpenseWeight(any(), any())).willReturn(new BigDecimal("3.000"));
            given(recordRepository.sumExpenseAmount(any(), any())).willReturn(500_000L);

            ExpenseRecordDto.SummaryResponse resp = service.getSummary(START, END);

            assertThat(resp.getNetAmount()).isEqualTo(-400_000L);
            assertThat(resp.getNetWeight()).isEqualByComparingTo("-2.000");
        }
    }

    // -----------------------------------------------------------------------
    // 헬퍼
    // -----------------------------------------------------------------------
    private static Page<ExpenseRecord> emptyPage(Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    private static ExpenseRecord mockExpenseRecord() {
        ExpenseRecord rec = mock(ExpenseRecord.class);
        given(rec.getExpenseRecordId()).willReturn(RECORD_ID);
        given(rec.getRecordDate()).willReturn(LocalDateTime.now());
        given(rec.getExpenseType()).willReturn(ExpenseType.EXPENSE);
        given(rec.getBankType()).willReturn(mockBankType());
        given(rec.getExpenseAccount()).willReturn(mockExpenseAccount());
        given(rec.getCounterparty()).willReturn("XYZ제작소");
        given(rec.getDescription()).willReturn("월세 정산");
        given(rec.getMaterial()).willReturn("18K");
        given(rec.getWeight()).willReturn(new BigDecimal("12.345"));
        given(rec.getQuantity()).willReturn(1);
        given(rec.getUnitPrice()).willReturn(100_000L);
        given(rec.getSupplyAmount()).willReturn(1_000_000L);
        given(rec.getTaxAmount()).willReturn(100_000L);
        return rec;
    }

    private static ExpenseRecord mockIncomeRecord() {
        ExpenseRecord rec = mock(ExpenseRecord.class);
        given(rec.getExpenseRecordId()).willReturn(2_002L);
        given(rec.getRecordDate()).willReturn(LocalDateTime.now());
        given(rec.getExpenseType()).willReturn(ExpenseType.INCOME);
        given(rec.getBankType()).willReturn(mockBankType());
        given(rec.getIncomeAccount()).willReturn(mockIncomeAccount());
        given(rec.getCounterparty()).willReturn("ABC상사");
        given(rec.getDescription()).willReturn("입금");
        given(rec.getMaterial()).willReturn("24K");
        given(rec.getWeight()).willReturn(new BigDecimal("5.000"));
        given(rec.getQuantity()).willReturn(1);
        given(rec.getUnitPrice()).willReturn(50_000L);
        given(rec.getSupplyAmount()).willReturn(500_000L);
        given(rec.getTaxAmount()).willReturn(50_000L);
        return rec;
    }

    private static ExpenseBankType mockBankType() {
        ExpenseBankType bt = mock(ExpenseBankType.class);
        given(bt.getExpenseBankTypeId()).willReturn(BANK_ID);
        given(bt.getBankTypeName()).willReturn("국민은행");
        return bt;
    }

    private static ExpenseIncomeAccount mockIncomeAccount() {
        ExpenseIncomeAccount acc = mock(ExpenseIncomeAccount.class);
        given(acc.getExpenseIncomeAccountId()).willReturn(INCOME_ID);
        given(acc.getIncomeAccountName()).willReturn("판매수입");
        return acc;
    }

    private static ExpenseExpenseAccount mockExpenseAccount() {
        ExpenseExpenseAccount acc = mock(ExpenseExpenseAccount.class);
        given(acc.getExpenseExpenseAccountId()).willReturn(EXPENSE_ID);
        given(acc.getExpenseAccountName()).willReturn("사무용품비");
        return acc;
    }

    private static ExpenseRecordDto.CreateRequest mockCreateRequest(String type, Long incId, Long expId) {
        ExpenseRecordDto.CreateRequest req = mock(ExpenseRecordDto.CreateRequest.class);
        given(req.getRecordDate()).willReturn("2026-05-16");
        given(req.getExpenseType()).willReturn(type);
        given(req.getBankTypeId()).willReturn(BANK_ID);
        given(req.getIncomeAccountId()).willReturn(incId);
        given(req.getExpenseAccountId()).willReturn(expId);
        given(req.getCounterparty()).willReturn("거래처");
        given(req.getDescription()).willReturn("적요");
        given(req.getMaterial()).willReturn("18K");
        given(req.getWeight()).willReturn(new BigDecimal("3.000"));
        given(req.getQuantity()).willReturn(1);
        given(req.getUnitPrice()).willReturn(100_000L);
        given(req.getSupplyAmount()).willReturn(300_000L);
        given(req.getTaxAmount()).willReturn(30_000L);
        return req;
    }

    private static ExpenseRecordDto.UpdateRequest mockUpdateRequest(String type, Long incId, Long expId) {
        ExpenseRecordDto.UpdateRequest req = mock(ExpenseRecordDto.UpdateRequest.class);
        given(req.getRecordDate()).willReturn("2026-05-16");
        given(req.getExpenseType()).willReturn(type);
        given(req.getBankTypeId()).willReturn(BANK_ID);
        given(req.getIncomeAccountId()).willReturn(incId);
        given(req.getExpenseAccountId()).willReturn(expId);
        given(req.getCounterparty()).willReturn("거래처-수정");
        given(req.getDescription()).willReturn("적요-수정");
        given(req.getMaterial()).willReturn("18K");
        given(req.getWeight()).willReturn(new BigDecimal("2.500"));
        given(req.getQuantity()).willReturn(2);
        given(req.getUnitPrice()).willReturn(120_000L);
        given(req.getSupplyAmount()).willReturn(240_000L);
        given(req.getTaxAmount()).willReturn(24_000L);
        return req;
    }
}
