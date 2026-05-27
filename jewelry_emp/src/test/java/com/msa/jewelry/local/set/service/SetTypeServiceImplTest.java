package com.msa.jewelry.local.set.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.set.dto.SetTypeDto;
import com.msa.jewelry.local.set.entity.SetType;
import com.msa.jewelry.local.set.repository.SetTypeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SetTypeServiceImpl 단위 테스트")
class SetTypeServiceImplTest {

    private static final String TOKEN     = "Bearer test-token";
    private static final String TENANT_ID = "tenant-001";
    private static final Long   ID        = 1L;

    @Mock JwtUtil jwtUtil;
    @Mock JobLauncher jobLauncher;
    @Mock Job updateSetTypeUpdateJob;
    @Mock SetTypeRepository setTypeRepository;

    @InjectMocks
    SetTypeServiceImpl service;

    @Nested
    @DisplayName("saveSetType")
    class Save {

        @Test
        @DisplayName("정상 저장")
        void 정상() {
            SetTypeDto dto = mock(SetTypeDto.class);
            given(dto.getName()).willReturn("단품");
            given(dto.getNote()).willReturn("낱개");
            given(setTypeRepository.existsBySetTypeName("단품")).willReturn(false);

            service.saveSetType(dto);

            verify(setTypeRepository).save(any(SetType.class));
        }

        @Test
        @DisplayName("중복 → 예외")
        void 중복() {
            SetTypeDto dto = mock(SetTypeDto.class);
            given(dto.getName()).willReturn("단품");
            given(setTypeRepository.existsBySetTypeName("단품")).willReturn(true);

            assertThatThrownBy(() -> service.saveSetType(dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getSetType")
    class Get {

        @Test
        @DisplayName("정상")
        void 정상() {
            SetType entity = mock(SetType.class);
            given(entity.getSetTypeName()).willReturn("단품");
            given(entity.getSetTypeNote()).willReturn("낱개");
            given(setTypeRepository.findById(ID)).willReturn(Optional.of(entity));

            SetTypeDto.ResponseSingle resp = service.getSetType(ID);

            assertThat(resp.getSetTypeId()).isEqualTo("1");
            assertThat(resp.getSetTypeName()).isEqualTo("단품");
        }

        @Test
        @DisplayName("없음 → 예외")
        void 없음() {
            given(setTypeRepository.findById(ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSetType(ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getSetTypes")
    class GetAll {

        @Test
        @DisplayName("위임")
        void 위임() {
            given(setTypeRepository.findAllOrderByAsc("단품")).willReturn(java.util.Collections.emptyList());
            assertThat(service.getSetTypes("단품")).isEmpty();
            verify(setTypeRepository).findAllOrderByAsc("단품");
        }
    }

    @Nested
    @DisplayName("updateSetType")
    class Update {

        @Test
        @DisplayName("이름 동일 → 중복 체크 스킵")
        void 이름동일() {
            SetTypeDto dto = mock(SetTypeDto.class);
            given(dto.getName()).willReturn("단품");
            SetType entity = mock(SetType.class);
            given(entity.getSetTypeName()).willReturn("단품");
            given(setTypeRepository.findById(ID)).willReturn(Optional.of(entity));

            service.updateSetType(ID, dto);

            verify(entity).updateSetType(dto);
            verify(setTypeRepository, never()).existsBySetTypeName(anyString());
        }

        @Test
        @DisplayName("이름 변경 + 중복 → 예외")
        void 이름변경_중복() {
            SetTypeDto dto = mock(SetTypeDto.class);
            given(dto.getName()).willReturn("세트");
            SetType entity = mock(SetType.class);
            given(entity.getSetTypeName()).willReturn("단품");
            given(setTypeRepository.findById(ID)).willReturn(Optional.of(entity));
            given(setTypeRepository.existsBySetTypeName("세트")).willReturn(true);

            assertThatThrownBy(() -> service.updateSetType(ID, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("대상 없음 → 예외")
        void 없음() {
            given(setTypeRepository.findById(ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateSetType(ID, mock(SetTypeDto.class)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("deletedSetType")
    class Delete {

        @Test
        @DisplayName("isDeletable=false → CANNOT_DELETE_DEFAULT")
        void 기본값_삭제불가() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            SetType entity = mock(SetType.class);
            given(entity.isDeletable()).willReturn(false);
            given(setTypeRepository.findById(ID)).willReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.deletedSetType(TOKEN, ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("권한 없음 → NOT_ACCESS")
        void 권한없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("USER");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            SetType entity = mock(SetType.class);
            given(entity.isDeletable()).willReturn(true);
            given(setTypeRepository.findById(ID)).willReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.deletedSetType(TOKEN, ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("정상 — Batch 실행")
        void 정상() throws Exception {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            SetType entity = mock(SetType.class);
            given(entity.isDeletable()).willReturn(true);
            given(setTypeRepository.findById(ID)).willReturn(Optional.of(entity));

            service.deletedSetType(TOKEN, ID);

            verify(jobLauncher).run(eq(updateSetTypeUpdateJob), any(JobParameters.class));
        }

        @Test
        @DisplayName("Batch 실패 → IllegalStateException")
        void Batch실패() throws Exception {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            SetType entity = mock(SetType.class);
            given(entity.isDeletable()).willReturn(true);
            given(setTypeRepository.findById(ID)).willReturn(Optional.of(entity));

            willThrow(new RuntimeException("boom"))
                    .given(jobLauncher).run(any(), any());

            assertThatThrownBy(() -> service.deletedSetType(TOKEN, ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("getSetTypeName")
    class Name {

        @Test
        @DisplayName("정상")
        void 정상() {
            SetType entity = mock(SetType.class);
            given(entity.getSetTypeName()).willReturn("단품");
            given(setTypeRepository.findById(ID)).willReturn(Optional.of(entity));

            assertThat(service.getSetTypeName(ID)).isEqualTo("단품");
        }

        @Test
        @DisplayName("없음 → NotFoundException")
        void 없음() {
            given(setTypeRepository.findById(ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSetTypeName(ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
