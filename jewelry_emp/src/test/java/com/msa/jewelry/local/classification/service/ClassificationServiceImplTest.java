package com.msa.jewelry.local.classification.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.classification.dto.ClassificationDto;
import com.msa.jewelry.local.classification.entity.Classification;
import com.msa.jewelry.local.classification.repository.ClassificationRepository;
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

import java.util.Collections;
import java.util.List;
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

/**
 * ClassificationServiceImpl 단위 테스트.
 *
 * <p>분류 마스터 CRUD 와 삭제 시 Spring Batch Job 위임 로직을 검증.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ClassificationServiceImpl 단위 테스트")
class ClassificationServiceImplTest {

    private static final String TOKEN     = "Bearer test-token";
    private static final String TENANT_ID = "tenant-001";
    private static final Long   CLF_ID    = 1L;

    @Mock JwtUtil jwtUtil;
    @Mock JobLauncher jobLauncher;
    @Mock Job updateClassificationJob;
    @Mock ClassificationRepository classificationRepository;

    @InjectMocks
    ClassificationServiceImpl service;

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("saveClassification")
    class SaveClassification {

        @Test
        @DisplayName("정상 — 중복 없으면 save 호출")
        void 정상_저장() {
            ClassificationDto dto = mock(ClassificationDto.class);
            given(dto.getName()).willReturn("반지");
            given(dto.getNote()).willReturn("결혼");
            given(classificationRepository.existsByClassificationName("반지")).willReturn(false);

            service.saveClassification(dto);

            verify(classificationRepository).save(any(Classification.class));
        }

        @Test
        @DisplayName("중복 이름 → IllegalArgumentException, save 호출 안 함")
        void 중복_예외() {
            ClassificationDto dto = mock(ClassificationDto.class);
            given(dto.getName()).willReturn("반지");
            given(classificationRepository.existsByClassificationName("반지")).willReturn(true);

            assertThatThrownBy(() -> service.saveClassification(dto))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(classificationRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getClassification")
    class GetClassification {

        @Test
        @DisplayName("정상 — 단건 응답 변환")
        void 정상조회() {
            Classification entity = mock(Classification.class);
            given(entity.getClassificationName()).willReturn("반지");
            given(entity.getClassificationNote()).willReturn("결혼");
            given(classificationRepository.findById(CLF_ID)).willReturn(Optional.of(entity));

            ClassificationDto.ResponseSingle resp = service.getClassification(CLF_ID);

            assertThat(resp.getClassificationId()).isEqualTo("1");
            assertThat(resp.getClassificationName()).isEqualTo("반지");
            assertThat(resp.getClassificationNote()).isEqualTo("결혼");
        }

        @Test
        @DisplayName("없음 → IllegalArgumentException(NOT_FOUND)")
        void 없음_예외() {
            given(classificationRepository.findById(CLF_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getClassification(CLF_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getClassifications")
    class GetClassifications {

        @Test
        @DisplayName("검색어 없을 때 전체 정렬 위임")
        void 전체조회() {
            given(classificationRepository.findAllOrderByAsc(null)).willReturn(Collections.emptyList());

            List<ClassificationDto.ResponseSingle> result = service.getClassifications(null);

            assertThat(result).isEmpty();
            verify(classificationRepository).findAllOrderByAsc(null);
        }

        @Test
        @DisplayName("검색어 위임 검증")
        void 검색어_위임() {
            given(classificationRepository.findAllOrderByAsc("반지")).willReturn(Collections.emptyList());

            service.getClassifications("반지");

            verify(classificationRepository).findAllOrderByAsc("반지");
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateClassification")
    class UpdateClassification {

        @Test
        @DisplayName("이름 동일 — 중복 체크 스킵, update 호출")
        void 이름동일_업데이트() {
            ClassificationDto dto = mock(ClassificationDto.class);
            given(dto.getName()).willReturn("반지");

            Classification entity = mock(Classification.class);
            given(entity.getClassificationName()).willReturn("반지");
            given(classificationRepository.findById(CLF_ID)).willReturn(Optional.of(entity));

            service.updateClassification(CLF_ID, dto);

            verify(entity).updateClassification(dto);
            verify(classificationRepository, never()).existsByClassificationName(anyString());
        }

        @Test
        @DisplayName("이름 변경 + 중복 없음 → 정상 업데이트")
        void 이름변경_중복없음() {
            ClassificationDto dto = mock(ClassificationDto.class);
            given(dto.getName()).willReturn("목걸이");

            Classification entity = mock(Classification.class);
            given(entity.getClassificationName()).willReturn("반지");
            given(classificationRepository.findById(CLF_ID)).willReturn(Optional.of(entity));
            given(classificationRepository.existsByClassificationName("목걸이")).willReturn(false);

            service.updateClassification(CLF_ID, dto);

            verify(entity).updateClassification(dto);
        }

        @Test
        @DisplayName("이름 변경 + 중복 → IllegalArgumentException")
        void 이름변경_중복() {
            ClassificationDto dto = mock(ClassificationDto.class);
            given(dto.getName()).willReturn("목걸이");

            Classification entity = mock(Classification.class);
            given(entity.getClassificationName()).willReturn("반지");
            given(classificationRepository.findById(CLF_ID)).willReturn(Optional.of(entity));
            given(classificationRepository.existsByClassificationName("목걸이")).willReturn(true);

            assertThatThrownBy(() -> service.updateClassification(CLF_ID, dto))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(entity, never()).updateClassification(any());
        }

        @Test
        @DisplayName("대상 없음 → IllegalArgumentException")
        void 대상없음() {
            ClassificationDto dto = mock(ClassificationDto.class);
            given(classificationRepository.findById(CLF_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateClassification(CLF_ID, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("deletedClassification")
    class DeletedClassification {

        @Test
        @DisplayName("기본값(defaultId=true → isDeletable=false) 삭제 시도 → 예외")
        void 기본값_삭제불가() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            Classification entity = mock(Classification.class);
            // isDeletable() == true 이면 예외 (서비스 로직)
            given(entity.isDeletable()).willReturn(true);
            given(classificationRepository.findById(CLF_ID)).willReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.deletedClassification(TOKEN, CLF_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("ADMIN 아님 → NOT_ACCESS 예외")
        void 권한없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("USER");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            Classification entity = mock(Classification.class);
            given(entity.isDeletable()).willReturn(false);
            given(classificationRepository.findById(CLF_ID)).willReturn(Optional.of(entity));

            assertThatThrownBy(() -> service.deletedClassification(TOKEN, CLF_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("대상 없음 → NOT_FOUND 예외")
        void 대상없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            given(classificationRepository.findById(CLF_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deletedClassification(TOKEN, CLF_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("정상 — Batch Job 호출됨")
        void 정상_Job실행() throws Exception {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            Classification entity = mock(Classification.class);
            given(entity.isDeletable()).willReturn(false);
            given(classificationRepository.findById(CLF_ID)).willReturn(Optional.of(entity));

            service.deletedClassification(TOKEN, CLF_ID);

            verify(jobLauncher).run(eq(updateClassificationJob), any(JobParameters.class));
        }

        @Test
        @DisplayName("Batch 실패 → IllegalStateException(BATCH_FAIL)")
        void Batch실패() throws Exception {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            Classification entity = mock(Classification.class);
            given(entity.isDeletable()).willReturn(false);
            given(classificationRepository.findById(CLF_ID)).willReturn(Optional.of(entity));

            willThrow(new RuntimeException("batch boom"))
                    .given(jobLauncher).run(any(), any());

            assertThatThrownBy(() -> service.deletedClassification(TOKEN, CLF_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getClassificationName")
    class GetClassificationName {

        @Test
        @DisplayName("정상 — 이름 반환")
        void 정상() {
            Classification entity = mock(Classification.class);
            given(entity.getClassificationName()).willReturn("반지");
            given(classificationRepository.findById(CLF_ID)).willReturn(Optional.of(entity));

            assertThat(service.getClassificationName(CLF_ID)).isEqualTo("반지");
        }

        @Test
        @DisplayName("없음 → NotFoundException")
        void 없음_예외() {
            given(classificationRepository.findById(CLF_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getClassificationName(CLF_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }
}
