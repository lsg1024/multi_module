package com.msa.jewelry.local.priority.service;

import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.jewelry.local.priority.dto.PriorityDto;
import com.msa.jewelry.local.priority.entity.Priority;
import com.msa.jewelry.local.priority.repository.PriorityRepository;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * PriorityService 단위 테스트.
 *
 * <p>출고 우선순위(일반/급/초급) CRUD — AuthorityUserRoleUtil.verification 으로 권한 확인 후 처리.
 *
 * <p>주의: 현 구현은 verification 통과 후에도 메서드 끝에서 무조건 ForbiddenException 을 던지는 구조.
 * (createPriority/updatePriority/delete 의 if 블록이 throw 를 가드하지 않음)
 * 본 테스트는 그 동작을 그대로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PriorityService 단위 테스트")
class PriorityServiceTest {

    private static final String TOKEN = "Bearer test-token";
    private static final String PRIORITY_ID_STR = "10";
    private static final Long PRIORITY_ID = 10L;

    @Mock AuthorityUserRoleUtil authorityUserRoleUtil;
    @Mock PriorityRepository priorityRepository;

    @InjectMocks
    PriorityService priorityService;

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("findAllPriority")
    class FindAllPriority {

        @Test
        @DisplayName("빈 결과 → 빈 리스트")
        void 빈결과() {
            given(priorityRepository.findAll()).willReturn(Collections.emptyList());

            List<PriorityDto> result = priorityService.findAllPriority();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다건 매핑 — id 는 String 으로 변환")
        void 다건_매핑() {
            Priority p1 = mock(Priority.class);
            given(p1.getPriorityId()).willReturn(1L);
            given(p1.getPriorityName()).willReturn("일반");
            given(p1.getPriorityDate()).willReturn(7);

            Priority p2 = mock(Priority.class);
            given(p2.getPriorityId()).willReturn(2L);
            given(p2.getPriorityName()).willReturn("급");
            given(p2.getPriorityDate()).willReturn(3);

            given(priorityRepository.findAll()).willReturn(List.of(p1, p2));

            List<PriorityDto> result = priorityService.findAllPriority();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getPriorityId()).isEqualTo("1");
            assertThat(result.get(0).getPriorityName()).isEqualTo("일반");
            assertThat(result.get(0).getPriorityDate()).isEqualTo(7);
            assertThat(result.get(1).getPriorityName()).isEqualTo("급");
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createPriority")
    class CreatePriority {

        @Test
        @DisplayName("권한 통과 — save 호출되지만 그 뒤 ForbiddenException — 알려진 버그 패턴")
        void 권한통과_그래도_예외() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            PriorityDto.Request req = new PriorityDto.Request("긴급", 3);

            // verification=true 일 때 save 까지 호출하고 메서드 끝에서 throw 함
            assertThatThrownBy(() -> priorityService.createPriority(TOKEN, req))
                    .isInstanceOf(ForbiddenException.class);

            // save 는 throw 직전에 호출된다
            ArgumentCaptor<Priority> captor = ArgumentCaptor.forClass(Priority.class);
            verify(priorityRepository).save(captor.capture());
            assertThat(captor.getValue().getPriorityName()).isEqualTo("긴급");
            assertThat(captor.getValue().getPriorityDate()).isEqualTo(3);
        }

        @Test
        @DisplayName("권한 없음 → ForbiddenException, save 호출 안 함")
        void 권한없음() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            PriorityDto.Request req = new PriorityDto.Request("긴급", 3);

            assertThatThrownBy(() -> priorityService.createPriority(TOKEN, req))
                    .isInstanceOf(ForbiddenException.class);

            verify(priorityRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updatePriority")
    class UpdatePriority {

        @Test
        @DisplayName("권한 통과 + 존재 — entity.updatePriority 호출 후 ForbiddenException")
        void 권한통과_정상수정() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            Priority priority = mock(Priority.class);
            given(priorityRepository.findById(PRIORITY_ID)).willReturn(Optional.of(priority));

            PriorityDto.Update dto = new PriorityDto.Update("초급", 1);

            assertThatThrownBy(() -> priorityService.updatePriority(TOKEN, PRIORITY_ID_STR, dto))
                    .isInstanceOf(ForbiddenException.class);

            verify(priority).updatePriority("초급", 1);
        }

        @Test
        @DisplayName("권한 통과 + 대상 없음 → NotFoundException")
        void 대상없음() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            given(priorityRepository.findById(PRIORITY_ID)).willReturn(Optional.empty());

            PriorityDto.Update dto = new PriorityDto.Update("초급", 1);

            assertThatThrownBy(() -> priorityService.updatePriority(TOKEN, PRIORITY_ID_STR, dto))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("권한 없음 → ForbiddenException, findById 호출 안 함")
        void 권한없음() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            PriorityDto.Update dto = new PriorityDto.Update("초급", 1);

            assertThatThrownBy(() -> priorityService.updatePriority(TOKEN, PRIORITY_ID_STR, dto))
                    .isInstanceOf(ForbiddenException.class);

            verify(priorityRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("priorityId 가 숫자 아님 → NumberFormatException")
        void 잘못된_priorityId() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            PriorityDto.Update dto = new PriorityDto.Update("초급", 1);

            assertThatThrownBy(() ->
                    priorityService.updatePriority(TOKEN, "abc", dto))
                    .isInstanceOf(NumberFormatException.class);
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("권한 통과 + 존재 — Repository.delete 후 ForbiddenException")
        void 권한통과_정상삭제() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            Priority priority = mock(Priority.class);
            given(priorityRepository.findById(PRIORITY_ID)).willReturn(Optional.of(priority));

            assertThatThrownBy(() -> priorityService.delete(TOKEN, PRIORITY_ID_STR))
                    .isInstanceOf(ForbiddenException.class);

            verify(priorityRepository).delete(priority);
        }

        @Test
        @DisplayName("권한 통과 + 대상 없음 → NotFoundException")
        void 대상없음() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            given(priorityRepository.findById(PRIORITY_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> priorityService.delete(TOKEN, PRIORITY_ID_STR))
                    .isInstanceOf(NotFoundException.class);

            verify(priorityRepository, never()).delete(any());
        }

        @Test
        @DisplayName("권한 없음 → ForbiddenException, delete 호출 안 함")
        void 권한없음() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> priorityService.delete(TOKEN, PRIORITY_ID_STR))
                    .isInstanceOf(ForbiddenException.class);

            verify(priorityRepository, never()).delete(any());
            verify(priorityRepository, never()).findById(anyLong());
        }
    }
}
