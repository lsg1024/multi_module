package com.msa.jewelry.local.color.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.color.dto.ColorDto;
import com.msa.jewelry.local.color.entity.Color;
import com.msa.jewelry.local.color.repository.ColorRepository;
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
 * ColorServiceImpl 단위 테스트.
 *
 * <p>주의: Color.isDeletable() 은 색상 모듈만 defaultId 가 true 일 때 true 를 반환
 * (다른 모듈은 반대) — 본 테스트는 현재 운영 구현 그대로의 분기를 검증한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ColorServiceImpl 단위 테스트")
class ColorServiceImplTest {

    private static final String TOKEN     = "Bearer test-token";
    private static final String TENANT_ID = "tenant-001";
    private static final Long   COLOR_ID  = 1L;

    @Mock JwtUtil jwtUtil;
    @Mock JobLauncher jobLauncher;
    @Mock Job updateColorJob;
    @Mock ColorRepository colorRepository;

    @InjectMocks
    ColorServiceImpl service;

    @Nested
    @DisplayName("saveColor")
    class SaveColor {

        @Test
        @DisplayName("정상 저장")
        void 정상() {
            ColorDto dto = mock(ColorDto.class);
            given(dto.getName()).willReturn("옐로골드");
            given(dto.getNote()).willReturn("노란빛");
            given(colorRepository.existsByColorName("옐로골드")).willReturn(false);

            service.saveColor(dto);

            verify(colorRepository).save(any(Color.class));
        }

        @Test
        @DisplayName("중복 → 예외, save 미호출")
        void 중복_예외() {
            ColorDto dto = mock(ColorDto.class);
            given(dto.getName()).willReturn("옐로골드");
            given(colorRepository.existsByColorName("옐로골드")).willReturn(true);

            assertThatThrownBy(() -> service.saveColor(dto))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(colorRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getColor")
    class GetColor {

        @Test
        @DisplayName("정상 단건 조회")
        void 정상() {
            Color color = mock(Color.class);
            given(color.getColorName()).willReturn("옐로골드");
            given(color.getColorNote()).willReturn("노란빛");
            given(colorRepository.findById(COLOR_ID)).willReturn(Optional.of(color));

            ColorDto.ResponseSingle resp = service.getColor(COLOR_ID);

            assertThat(resp.getColorId()).isEqualTo("1");
            assertThat(resp.getColorName()).isEqualTo("옐로골드");
        }

        @Test
        @DisplayName("없음 → 예외")
        void 없음() {
            given(colorRepository.findById(COLOR_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getColor(COLOR_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getColors")
    class GetColors {

        @Test
        @DisplayName("위임")
        void 위임() {
            given(colorRepository.findAllOrderByAsc("골드")).willReturn(Collections.emptyList());
            assertThat(service.getColors("골드")).isEmpty();
            verify(colorRepository).findAllOrderByAsc("골드");
        }
    }

    @Nested
    @DisplayName("updateColor")
    class UpdateColor {

        @Test
        @DisplayName("이름 동일 → 중복 체크 스킵, 업데이트")
        void 이름동일() {
            ColorDto dto = mock(ColorDto.class);
            given(dto.getName()).willReturn("옐로골드");

            Color color = mock(Color.class);
            given(color.getColorName()).willReturn("옐로골드");
            given(colorRepository.findById(COLOR_ID)).willReturn(Optional.of(color));

            service.updateColor(COLOR_ID, dto);

            verify(color).updateColor(dto);
            verify(colorRepository, never()).existsByColorName(anyString());
        }

        @Test
        @DisplayName("이름 변경 + 중복 → 예외")
        void 이름변경_중복() {
            ColorDto dto = mock(ColorDto.class);
            given(dto.getName()).willReturn("로즈골드");

            Color color = mock(Color.class);
            given(color.getColorName()).willReturn("옐로골드");
            given(colorRepository.findById(COLOR_ID)).willReturn(Optional.of(color));
            given(colorRepository.existsByColorName("로즈골드")).willReturn(true);

            assertThatThrownBy(() -> service.updateColor(COLOR_ID, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("대상 없음 → 예외")
        void 없음() {
            given(colorRepository.findById(COLOR_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateColor(COLOR_ID, mock(ColorDto.class)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("deleteColor")
    class DeleteColor {

        @Test
        @DisplayName("색상 모듈은 isDeletable()=true 일 때 삭제 불가 분기 진입")
        void 기본값_삭제불가() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            Color color = mock(Color.class);
            given(color.isDeletable()).willReturn(true);
            given(colorRepository.findById(COLOR_ID)).willReturn(Optional.of(color));

            assertThatThrownBy(() -> service.deleteColor(TOKEN, COLOR_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("ADMIN 아님 → NOT_ACCESS")
        void 권한없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("USER");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            Color color = mock(Color.class);
            given(color.isDeletable()).willReturn(false);
            given(colorRepository.findById(COLOR_ID)).willReturn(Optional.of(color));

            assertThatThrownBy(() -> service.deleteColor(TOKEN, COLOR_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("대상 없음 → NOT_FOUND")
        void 없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);
            given(colorRepository.findById(COLOR_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteColor(TOKEN, COLOR_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("정상 — Batch 실행")
        void 정상() throws Exception {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            Color color = mock(Color.class);
            given(color.isDeletable()).willReturn(false);
            given(colorRepository.findById(COLOR_ID)).willReturn(Optional.of(color));

            service.deleteColor(TOKEN, COLOR_ID);

            verify(jobLauncher).run(eq(updateColorJob), any(JobParameters.class));
        }

        @Test
        @DisplayName("Batch 실패 → BATCH_FAIL")
        void Batch실패() throws Exception {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(jwtUtil.getTenantId(TOKEN)).willReturn(TENANT_ID);

            Color color = mock(Color.class);
            given(color.isDeletable()).willReturn(false);
            given(colorRepository.findById(COLOR_ID)).willReturn(Optional.of(color));

            willThrow(new RuntimeException("boom"))
                    .given(jobLauncher).run(any(), any());

            assertThatThrownBy(() -> service.deleteColor(TOKEN, COLOR_ID))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("getColorName")
    class GetColorName {

        @Test
        @DisplayName("정상")
        void 정상() {
            Color color = mock(Color.class);
            given(color.getColorName()).willReturn("화이트골드");
            given(colorRepository.findById(COLOR_ID)).willReturn(Optional.of(color));

            assertThat(service.getColorName(COLOR_ID)).isEqualTo("화이트골드");
        }

        @Test
        @DisplayName("없음 → NotFoundException")
        void 없음() {
            given(colorRepository.findById(COLOR_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getColorName(COLOR_ID))
                    .isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getColorIdByName")
    class GetColorIdByName {

        @Test
        @DisplayName("정상 — Long 반환")
        void 정상() {
            Color color = mock(Color.class);
            given(color.getColorId()).willReturn(42L);
            given(colorRepository.findByColorNameIgnoreCase("화이트골드")).willReturn(Optional.of(color));

            assertThat(service.getColorIdByName("화이트골드")).isEqualTo(42L);
        }

        @Test
        @DisplayName("없음 → null 반환 (예외 아님)")
        void 없음_null() {
            given(colorRepository.findByColorNameIgnoreCase("없는색")).willReturn(Optional.empty());

            assertThat(service.getColorIdByName("없는색")).isNull();
        }
    }
}
