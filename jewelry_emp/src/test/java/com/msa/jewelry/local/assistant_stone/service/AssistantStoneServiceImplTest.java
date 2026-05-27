package com.msa.jewelry.local.assistant_stone.service;

import com.msa.common.global.util.AuthorityUserRoleUtil;
import com.msa.jewelry.local.assistant_stone.dto.AssistantStoneDto;
import com.msa.jewelry.local.assistant_stone.dto.AssistantStoneView;
import com.msa.jewelry.local.assistant_stone.entity.AssistantStone;
import com.msa.jewelry.local.assistant_stone.repository.AssistantStoneRepository;
import com.msa.jewelry.global.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssistantStoneServiceImpl 단위 테스트")
class AssistantStoneServiceImplTest {

    private static final String TOKEN = "Bearer token";
    private static final Long   ID    = 201L;

    @Mock AuthorityUserRoleUtil authorityUserRoleUtil;
    @Mock AssistantStoneRepository assistantStoneRepository;

    @InjectMocks
    AssistantStoneServiceImpl service;

    @Nested
    @DisplayName("getAssistantStoneAll")
    class GetAll {

        @Test
        @DisplayName("정상 — 정렬 위임 + 변환")
        void 정상() {
            AssistantStone entity = mock(AssistantStone.class);
            given(entity.getAssistanceStoneId()).willReturn(ID);
            given(entity.getAssistanceStoneName()).willReturn("큐빅 0.05ct");
            given(entity.getAssistanceStoneNote()).willReturn("큐빅 지르코니아");
            given(assistantStoneRepository.findAll(any(Sort.class))).willReturn(List.of(entity));

            List<AssistantStoneDto.Response> result = service.getAssistantStoneAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAssistantStoneName()).isEqualTo("큐빅 0.05ct");
        }

        @Test
        @DisplayName("빈 결과")
        void 빈결과() {
            given(assistantStoneRepository.findAll(any(Sort.class))).willReturn(Collections.emptyList());
            assertThat(service.getAssistantStoneAll()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAssistantStone")
    class GetOne {

        @Test
        @DisplayName("정상 — Response 변환")
        void 정상() {
            AssistantStone entity = mock(AssistantStone.class);
            given(entity.getAssistanceStoneId()).willReturn(ID);
            given(entity.getAssistanceStoneName()).willReturn("큐빅");
            given(entity.getAssistanceStoneNote()).willReturn("note");
            given(assistantStoneRepository.findById(ID)).willReturn(Optional.of(entity));

            AssistantStoneDto.Response resp = service.getAssistantStone(ID);

            assertThat(resp.getAssistantStoneId()).isEqualTo(ID);
            assertThat(resp.getAssistantStoneName()).isEqualTo("큐빅");
        }

        @Test
        @DisplayName("없음 → IllegalArgumentException")
        void 없음() {
            given(assistantStoneRepository.findById(ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAssistantStone(ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("createAssistantStone")
    class Create {

        @Test
        @DisplayName("권한 없음 → 예외")
        void 권한없음() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            AssistantStoneDto.Request req = mock(AssistantStoneDto.Request.class);

            assertThatThrownBy(() -> service.createAssistantStone(TOKEN, req))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(assistantStoneRepository, never()).save(any());
        }

        @Test
        @DisplayName("정상 — save 호출")
        void 정상() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);

            AssistantStoneDto.Request req = mock(AssistantStoneDto.Request.class);
            given(req.getAssistantStoneName()).willReturn("큐빅");
            given(req.getAssistantStoneNote()).willReturn("note");

            service.createAssistantStone(TOKEN, req);

            verify(assistantStoneRepository).save(any(AssistantStone.class));
        }
    }

    @Nested
    @DisplayName("updateAssistantStone")
    class Update {

        @Test
        @DisplayName("권한 없음 → 예외")
        void 권한없음() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> service.updateAssistantStone(TOKEN, "1", mock(AssistantStoneDto.Request.class)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("대상 없음 → jakarta NotFoundException")
        void 없음() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            given(assistantStoneRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateAssistantStone(TOKEN, "1", mock(AssistantStoneDto.Request.class)))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("정상 — entity 메서드 호출")
        void 정상() {
            given(authorityUserRoleUtil.verification(TOKEN)).willReturn(true);
            AssistantStone entity = mock(AssistantStone.class);
            given(entity.getAssistanceStoneNote()).willReturn("oldNote");
            given(assistantStoneRepository.findById(1L)).willReturn(Optional.of(entity));

            AssistantStoneDto.Request req = mock(AssistantStoneDto.Request.class);
            given(req.getAssistantStoneName()).willReturn("새이름");

            service.updateAssistantStone(TOKEN, "1", req);

            verify(entity).updateAssistantStone("새이름", "oldNote");
        }
    }

    @Nested
    @DisplayName("deletedAssistantStone")
    class Delete {

        @Test
        @DisplayName("ADMIN 아님 → 예외")
        void 권한없음() {
            given(authorityUserRoleUtil.isAdmin(TOKEN)).willReturn(false);

            assertThatThrownBy(() -> service.deletedAssistantStone(TOKEN, "1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("대상 없음 → NotFoundException")
        void 없음() {
            given(authorityUserRoleUtil.isAdmin(TOKEN)).willReturn(true);
            given(assistantStoneRepository.findById(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deletedAssistantStone(TOKEN, "1"))
                    .isInstanceOf(NotFoundException.class);
        }

        @Test
        @DisplayName("정상 — delete 호출")
        void 정상() {
            given(authorityUserRoleUtil.isAdmin(TOKEN)).willReturn(true);
            AssistantStone entity = mock(AssistantStone.class);
            given(assistantStoneRepository.findById(1L)).willReturn(Optional.of(entity));

            service.deletedAssistantStone(TOKEN, "1");

            verify(assistantStoneRepository).delete(entity);
        }
    }

    @Nested
    @DisplayName("getAssistantStoneView")
    class View {

        @Test
        @DisplayName("정상 — record 반환")
        void 정상() {
            AssistantStone entity = mock(AssistantStone.class);
            given(entity.getAssistanceStoneId()).willReturn(ID);
            given(entity.getAssistanceStoneName()).willReturn("큐빅");
            given(entity.getAssistanceStoneNote()).willReturn("note");
            given(assistantStoneRepository.findById(ID)).willReturn(Optional.of(entity));

            AssistantStoneView view = service.getAssistantStoneView(ID);

            assertThat(view.assistantStoneId()).isEqualTo(ID);
            assertThat(view.assistantStoneName()).isEqualTo("큐빅");
        }

        @Test
        @DisplayName("없음 → com.msa.jewelry.global NotFoundException")
        void 없음() {
            given(assistantStoneRepository.findById(ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getAssistantStoneView(ID))
                    .isInstanceOf(com.msa.jewelry.global.exception.NotFoundException.class);
        }
    }
}
