package com.msa.jewelry.local.material.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.material.dto.MaterialDto;
import com.msa.jewelry.local.material.entity.Material;
import com.msa.jewelry.local.material.repository.MaterialRepository;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
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
 * MaterialServiceImpl 단위 테스트.
 *
 * <p>주의: Material.isDeletable() 은 defaultId 가 true 일 때 true 를 반환하므로
 * isDeletable()=true 시 삭제 불가 분기에 들어간다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MaterialServiceImpl 단위 테스트")
class MaterialServiceImplTest {

    private static final String TOKEN     = "Bearer test-token";
    private static final String TENANT_ID = "tenant-001";
    private static final Long   MAT_ID    = 1L;

    @Mock JwtUtil jwtUtil;
    @Mock JobLauncher jobLauncher;
    @Mock Job updateMaterialUpdateJob;
    @Mock MaterialRepository materialRepository;

    @InjectMocks
    MaterialServiceImpl service;

    @Nested
    @DisplayName("saveMaterial")
    class SaveMaterial {

        @Test
        @DisplayName("정상 저장")
        void 정상() {
            MaterialDto dto = mock(MaterialDto.class);
            given(dto.getName()).willReturn("18K");
            given(dto.getGoldPurityPercent()).willReturn("75.00");
            given(materialRepository.existsByMaterialName("18K")).willReturn(false);

            service.saveMaterial(dto);

            verify(materialRepository).save(any(Material.class));
        }

        @Test
        @DisplayName("중복 → 예외")
        void 중복() {
            MaterialDto dto = mock(MaterialDto.class);
            given(dto.getName()).willReturn("18K");
            given(materialRepository.existsByMaterialName("18K")).willReturn(true);

            assertThatThrownBy(() -> service.saveMaterial(dto))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(materialRepository, never()).save(any());
        }

        @Test
        @DisplayName("잘못된 숫자 문자열 → NumberFormatException 전파 (Save 단계서)")
        void 잘못된_숫자() {
            MaterialDto dto = mock(MaterialDto.class);
            given(dto.getName()).willReturn("BADK");
            given(dto.getGoldPurityPercent()).willReturn("not-a-number");
            given(materialRepository.existsByMaterialName("BADK")).willReturn(false);

            assertThatThrownBy(() -> service.saveMaterial(dto))
                    .isInstanceOf(NumberFormatException.class);
        }
    }

    @Nested
    @DisplayName("getMaterial")
    class GetMaterial {

        @Test
        @DisplayName("정상 — id/name/purity 변환")
        void 정상() {
            Material material = mock(Material.class);
            given(material.getMaterialName()).willReturn("18K");
            given(material.getMaterialGoldPurityPercent()).willReturn(new BigDecimal("75.00"));
            given(materialRepository.findById(MAT_ID)).willReturn(Optional.of(material));

            MaterialDto.ResponseSingle resp = service.getMaterial(MAT_ID);

            assertThat(resp.getMaterialId()).isEqualTo("1");
            assertThat(resp.getMaterialName()).isEqualTo("18K");
            assertThat(resp.getMaterialGoldPurityPercent()).isEqualTo("75.00");
        }

        @Test
        @DisplayName("없음 → 예외")
        void 없음() {
            given(materialRepository.findById(MAT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMaterial(MAT_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getMaterials")
    class GetMaterials {

        @Test
        @DisplayName("위임")
        void 위임() {
            given(materialRepository.findAllOrderByAsc()).willReturn(java.util.Collections.emptyList());
            assertThat(service.getMaterials()).isEmpty();
            verify(materialRepository).findAllOrderByAsc();
        }
    }

    @Nested
    @DisplayName("updateMaterial")
    class UpdateMaterial {

        @Test
        @DisplayName("이름 동일 → 중복 체크 스킵")
        void 이름동일() {
            MaterialDto dto = mock(MaterialDto.class);
            given(dto.getName()).willReturn("18K");

            Material material = mock(Material.class);
            given(material.getMaterialName()).willReturn("18K");
            given(materialRepository.findById(MAT_ID)).willReturn(Optional.of(material));

            service.updateMaterial(MAT_ID, dto);

            verify(material).updateMaterial(dto);
            verify(materialRepository, never()).existsByMaterialName(anyString());
        }

        @Test
        @DisplayName("이름 변경 + 중복 → 예외")
        void 이름변경_중복() {
            MaterialDto dto = mock(MaterialDto.class);
            given(dto.getName()).willReturn("24K");

            Material material = mock(Material.class);
            given(material.getMaterialName()).willReturn("18K");
            given(materialRepository.findById(MAT_ID)).willReturn(Optional.of(material));
            given(materialRepository.existsByMaterialName("24K")).willReturn(true);

            assertThatThrownBy(() -> service.updateMaterial(MAT_ID, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("대상 없음 → 예외")
        void 없음() {
            given(materialRepository.findById(MAT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateMaterial(MAT_ID, mock(MaterialDto.class)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("deleteMaterial")
    class DeleteMaterial {

        @Test
        @DisplayName("isDeletable=true → 기본값 삭제 불가 예외")
        void 기본값() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            Material material = mock(Material.class);
            given(material.isDeletable()).willReturn(true);
            given(materialRepository.findById(MAT_ID)).willReturn(Optional.of(material));

            assertThatThrownBy(() -> service.deleteMaterial(TOKEN, MAT_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("권한 부족 → NOT_ACCESS")
        void 권한없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("USER");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            Material material = mock(Material.class);
            given(material.isDeletable()).willReturn(false);
            given(materialRepository.findById(MAT_ID)).willReturn(Optional.of(material));

            assertThatThrownBy(() -> service.deleteMaterial(TOKEN, MAT_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("정상 Batch 실행")
        void 정상() throws Exception {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            Material material = mock(Material.class);
            given(material.isDeletable()).willReturn(false);
            given(materialRepository.findById(MAT_ID)).willReturn(Optional.of(material));

            service.deleteMaterial(TOKEN, MAT_ID);

            verify(jobLauncher).run(eq(updateMaterialUpdateJob), any(JobParameters.class));
        }

        @Test
        @DisplayName("Batch 도중 DataIntegrityViolation 발생해도 IllegalStateException 으로 감싸짐")
        void Batch_DIV() throws Exception {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            Material material = mock(Material.class);
            given(material.isDeletable()).willReturn(false);
            given(materialRepository.findById(MAT_ID)).willReturn(Optional.of(material));

            willThrow(new DataIntegrityViolationException("FK constraint - 사용 중"))
                    .given(jobLauncher).run(any(), any());

            assertThatThrownBy(() -> service.deleteMaterial(TOKEN, MAT_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("getMaterialName")
    class GetMaterialName {

        @Test
        @DisplayName("정상")
        void 정상() {
            Material material = mock(Material.class);
            given(material.getMaterialName()).willReturn("18K");
            given(materialRepository.findById(MAT_ID)).willReturn(Optional.of(material));

            assertThat(service.getMaterialName(MAT_ID)).isEqualTo("18K");
        }

        @Test
        @DisplayName("없음 → NotFoundException")
        void 없음() {
            given(materialRepository.findById(MAT_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getMaterialName(MAT_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getMaterialIdByName")
    class GetMaterialIdByName {

        @Test
        @DisplayName("정상 — Long 반환")
        void 정상() {
            Material material = mock(Material.class);
            given(material.getMaterialId()).willReturn(7L);
            given(materialRepository.findByMaterialNameIgnoreCase("18K")).willReturn(Optional.of(material));

            assertThat(service.getMaterialIdByName("18K")).isEqualTo(7L);
        }

        @Test
        @DisplayName("없음 → null (예외 아님)")
        void 없음() {
            given(materialRepository.findByMaterialNameIgnoreCase("BADK")).willReturn(Optional.empty());

            assertThat(service.getMaterialIdByName("BADK")).isNull();
        }
    }
}
