package com.msa.jewelry.local.goldharry.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.goldharry.dto.GoldHarryDto;
import com.msa.jewelry.local.goldharry.entity.GoldHarry;
import com.msa.jewelry.local.goldharry.repository.GoldHarryRepository;
import jakarta.ws.rs.ForbiddenException;
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
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * GoldHarryService 단위 테스트.
 *
 * <p>금시세(해리) 마스터의 CRUD + Spring Batch 연동을 검증한다.
 * 외부 의존성 (JwtUtil, JobLauncher, Job, AuthorityUserRoleUtil, GoldHarryRepository)
 * 을 모두 Mockito 로 대체한다.
 *
 * <p>특기 사항:
 * <ul>
 *   <li>updateLoss: 기존 loss 와 새 값이 같으면 save/배치 모두 호출하지 않는다.</li>
 *   <li>delete: AuthorityUserRoleUtil.verification == false 면 조용히 종료 (예외 없음).</li>
 *   <li>JobLauncher.run 실패는 IllegalStateException(BATCH_FAIL) 로 래핑된다.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GoldHarryService 단위 테스트")
class GoldHarryServiceTest {

    private static final String TOKEN = "Bearer test-token";
    private static final String TENANT = "jewelry-main";
    private static final Long HARRY_ID = 7L;

    @Mock JwtUtil jwtUtil;
    @Mock JobLauncher jobLauncher;
    @Mock Job updateStoreGoldHarryLossJob;
    @Mock Job deleteGoldHarryJob;
    @Mock AuthorityUserRoleUtil authorityUserRoleUtil;
    @Mock GoldHarryRepository goldHarryRepository;

    @InjectMocks
    GoldHarryService service;

    // ============================================================
    // updateLoss
    // ============================================================
    @Nested
    @DisplayName("updateLoss")
    class UpdateLoss {

        @Test
        @DisplayName("정상 — 변경 시 save + batch 호출")
        void 정상() throws Exception {
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT);

            GoldHarry harry = mock(GoldHarry.class);
            given(harry.getGoldHarryLoss()).willReturn(new BigDecimal("1.05"));
            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.of(harry));

            GoldHarryDto.Update req = new GoldHarryDto.Update("1.10");

            service.updateLoss(TOKEN, HARRY_ID, req);

            verify(harry).updateLoss("1.10");
            verify(goldHarryRepository).save(harry);
            verify(jobLauncher).run(eq(updateStoreGoldHarryLossJob), any(JobParameters.class));
        }

        @Test
        @DisplayName("대상 없음 → NotFoundException(WRONG_HARRY)")
        void 없음() {
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT);
            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateLoss(TOKEN, HARRY_ID, new GoldHarryDto.Update("1.10")))
                    .isInstanceOf(NotFoundException.class);
            verify(goldHarryRepository, never()).save(any());
        }

        @Test
        @DisplayName("값 동일 — save/batch 모두 호출되지 않음")
        void 값_동일() throws Exception {
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT);

            GoldHarry harry = mock(GoldHarry.class);
            given(harry.getGoldHarryLoss()).willReturn(new BigDecimal("1.05"));
            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.of(harry));

            service.updateLoss(TOKEN, HARRY_ID, new GoldHarryDto.Update("1.05"));

            verify(harry, never()).updateLoss(any());
            verify(goldHarryRepository, never()).save(any());
            verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
        }

        @Test
        @DisplayName("배치 실행 실패 → IllegalStateException(BATCH_FAIL)")
        void 배치_실패() throws Exception {
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT);

            GoldHarry harry = mock(GoldHarry.class);
            given(harry.getGoldHarryLoss()).willReturn(new BigDecimal("1.05"));
            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.of(harry));
            willThrow(new RuntimeException("batch boom"))
                    .given(jobLauncher).run(eq(updateStoreGoldHarryLossJob), any(JobParameters.class));

            assertThatThrownBy(() -> service.updateLoss(TOKEN, HARRY_ID, new GoldHarryDto.Update("1.20")))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    // delete
    // ============================================================
    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("정상 — 권한 OK → 삭제 + 배치 호출")
        void 정상() throws Exception {
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT);
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            GoldHarry harry = mock(GoldHarry.class);
            given(harry.getDefaultOption()).willReturn(false);
            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.of(harry));

            service.delete(TOKEN, String.valueOf(HARRY_ID));

            verify(goldHarryRepository).delete(harry);
            verify(jobLauncher).run(eq(deleteGoldHarryJob), any(JobParameters.class));
        }

        @Test
        @DisplayName("권한 없음 — 조용히 종료, 어떤 작업도 호출되지 않음")
        void 권한_없음_조용히_종료() throws Exception {
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT);
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            service.delete(TOKEN, String.valueOf(HARRY_ID));

            verify(goldHarryRepository, never()).findById(any());
            verify(goldHarryRepository, never()).delete(any());
            verify(jobLauncher, never()).run(any(Job.class), any(JobParameters.class));
        }

        @Test
        @DisplayName("대상 없음 → NotFoundException")
        void 대상_없음() {
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT);
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.delete(TOKEN, String.valueOf(HARRY_ID)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("기본 옵션 — IllegalArgumentException(DEFAULT_HARRY)")
        void 기본옵션_삭제불가() {
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT);
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            GoldHarry harry = mock(GoldHarry.class);
            given(harry.getDefaultOption()).willReturn(true);
            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.of(harry));

            assertThatThrownBy(() -> service.delete(TOKEN, String.valueOf(HARRY_ID)))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(goldHarryRepository, never()).delete(any());
        }

        @Test
        @DisplayName("ID 문자열이 숫자가 아니면 → NumberFormatException")
        void ID_숫자아님() {
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT);
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            assertThatThrownBy(() -> service.delete(TOKEN, "abc"))
                    .isInstanceOf(NumberFormatException.class);
        }

        @Test
        @DisplayName("배치 실행 실패 → IllegalStateException(BATCH_FAIL)")
        void 배치_실패() throws Exception {
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT);
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            GoldHarry harry = mock(GoldHarry.class);
            given(harry.getDefaultOption()).willReturn(false);
            given(goldHarryRepository.findById(HARRY_ID)).willReturn(Optional.of(harry));
            willThrow(new RuntimeException("batch boom"))
                    .given(jobLauncher).run(eq(deleteGoldHarryJob), any(JobParameters.class));

            assertThatThrownBy(() -> service.delete(TOKEN, String.valueOf(HARRY_ID)))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================
    // getGoldHarries
    // ============================================================
    @Nested
    @DisplayName("getGoldHarries")
    class GetGoldHarries {

        @Test
        @DisplayName("정상 — 정렬 옵션으로 findAll, Response 변환")
        void 정상() {
            GoldHarry h1 = mock(GoldHarry.class);
            given(h1.getGoldHarryId()).willReturn(1L);
            given(h1.getGoldHarryLoss()).willReturn(new BigDecimal("1.05"));
            GoldHarry h2 = mock(GoldHarry.class);
            given(h2.getGoldHarryId()).willReturn(2L);
            given(h2.getGoldHarryLoss()).willReturn(new BigDecimal("1.10"));

            given(goldHarryRepository.findAll(Sort.by(Sort.Direction.ASC, "goldHarryLoss")))
                    .willReturn(List.of(h1, h2));

            List<GoldHarryDto.Response> result = service.getGoldHarries();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getGoldHarryId()).isEqualTo("1");
            assertThat(result.get(0).getGoldHarry()).isEqualTo("1.05");
            assertThat(result.get(1).getGoldHarry()).isEqualTo("1.10");
        }

        @Test
        @DisplayName("빈 결과 → 빈 리스트")
        void 빈결과() {
            given(goldHarryRepository.findAll(any(Sort.class))).willReturn(Collections.emptyList());

            assertThat(service.getGoldHarries()).isEmpty();
        }
    }

    // ============================================================
    // createHarry
    // ============================================================
    @Nested
    @DisplayName("createHarry")
    class CreateHarry {

        @Test
        @DisplayName("정상 — 권한 OK → save 호출, 빌더 필드 매핑")
        void 정상() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            GoldHarryDto.Request req = new GoldHarryDto.Request(new BigDecimal("1.07"));
            service.createHarry(TOKEN, req);

            ArgumentCaptor<GoldHarry> captor = ArgumentCaptor.forClass(GoldHarry.class);
            verify(goldHarryRepository).save(captor.capture());
            assertThat(captor.getValue().getGoldHarryLoss()).isEqualByComparingTo("1.07");
        }

        @Test
        @DisplayName("권한 없음 → ForbiddenException, save 호출 안 됨")
        void 권한_없음() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> service.createHarry(TOKEN, new GoldHarryDto.Request(BigDecimal.ONE)))
                    .isInstanceOf(ForbiddenException.class);
            verify(goldHarryRepository, never()).save(any());
        }
    }
}
