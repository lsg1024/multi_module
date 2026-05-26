package com.msa.jewelry.local.stone_type.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.jewelry.local.stone_type.dto.StoneTypeDto;
import com.msa.jewelry.local.stone_type.entity.StoneType;
import com.msa.jewelry.local.stone_type.repository.StoneTypeRepository;
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
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * StoneTypeService 단위 테스트.
 *
 * <p>스톤 타입 마스터(다이아/사파이어 등)의 CRUD 흐름을 검증한다.
 * 외부 의존성 (JwtUtil, StoneTypeRepository) 은 Mockito 로 대체한다.
 *
 * <p>업데이트 분기 주의: 이름 변경 여부와 무관하게 항상 existsByStoneTypeName 을 한 번 호출한다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StoneTypeService 단위 테스트")
class StoneTypeServiceTest {

    private static final String TOKEN = "Bearer test-token";
    private static final Long TYPE_ID = 22L;

    @Mock JwtUtil jwtUtil;
    @Mock StoneTypeRepository stoneTypeRepository;

    @InjectMocks
    StoneTypeService service;

    // ============================================================
    // saveStoneType
    // ============================================================
    @Nested
    @DisplayName("saveStoneType")
    class SaveStoneType {

        @Test
        @DisplayName("정상 — name/note 매핑 후 save 호출")
        void 정상() {
            StoneTypeDto dto = new StoneTypeDto("다이아몬드", "천연");
            given(stoneTypeRepository.existsByStoneTypeName("다이아몬드")).willReturn(false);

            service.saveStoneType(dto);

            ArgumentCaptor<StoneType> captor = ArgumentCaptor.forClass(StoneType.class);
            verify(stoneTypeRepository).save(captor.capture());
            assertThat(captor.getValue().getStoneTypeName()).isEqualTo("다이아몬드");
            assertThat(captor.getValue().getStoneTypeNote()).isEqualTo("천연");
        }

        @Test
        @DisplayName("중복 이름 → IS_EXIST")
        void 중복() {
            StoneTypeDto dto = new StoneTypeDto("다이아몬드", "x");
            given(stoneTypeRepository.existsByStoneTypeName("다이아몬드")).willReturn(true);

            assertThatThrownBy(() -> service.saveStoneType(dto))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(stoneTypeRepository, never()).save(any());
        }

        @Test
        @DisplayName("DB 유니크 위반 — DataIntegrityViolationException 전파")
        void DB_유니크() {
            StoneTypeDto dto = new StoneTypeDto("사파이어", "x");
            given(stoneTypeRepository.existsByStoneTypeName("사파이어")).willReturn(false);
            willThrow(new DataIntegrityViolationException("unique"))
                    .given(stoneTypeRepository).save(any(StoneType.class));

            assertThatThrownBy(() -> service.saveStoneType(dto))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    // ============================================================
    // getStoneType
    // ============================================================
    @Nested
    @DisplayName("getStoneType")
    class GetStoneType {

        @Test
        @DisplayName("정상 — ResponseSingle 매핑")
        void 정상() {
            StoneType type = mock(StoneType.class);
            given(type.getStoneTypeName()).willReturn("다이아몬드");
            given(type.getStoneTypeNote()).willReturn("천연");
            given(stoneTypeRepository.findById(TYPE_ID)).willReturn(Optional.of(type));

            StoneTypeDto.ResponseSingle res = service.getStoneType(TYPE_ID);

            assertThat(res.getStoneTypeId()).isEqualTo("22");
            assertThat(res.getStoneTypeName()).isEqualTo("다이아몬드");
            assertThat(res.getStoneTypeNote()).isEqualTo("천연");
        }

        @Test
        @DisplayName("없음 → NOT_FOUND")
        void 없음() {
            given(stoneTypeRepository.findById(TYPE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getStoneType(TYPE_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    // getStoneTypes
    // ============================================================
    @Nested
    @DisplayName("getStoneTypes")
    class GetStoneTypes {

        @Test
        @DisplayName("정상 위임")
        void 정상() {
            StoneTypeDto.ResponseSingle item = StoneTypeDto.ResponseSingle.builder()
                    .stoneTypeId("1").stoneTypeName("다이아몬드").stoneTypeNote("n").build();
            given(stoneTypeRepository.findByStoneTypeAllOrderByAsc("다")).willReturn(List.of(item));

            List<StoneTypeDto.ResponseSingle> list = service.getStoneTypes("다");

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getStoneTypeName()).isEqualTo("다이아몬드");
        }

        @Test
        @DisplayName("빈 결과 위임")
        void 빈결과() {
            given(stoneTypeRepository.findByStoneTypeAllOrderByAsc(null))
                    .willReturn(Collections.emptyList());

            assertThat(service.getStoneTypes(null)).isEmpty();
        }
    }

    // ============================================================
    // updateStoneType
    // ============================================================
    @Nested
    @DisplayName("updateStoneType")
    class UpdateStoneType {

        @Test
        @DisplayName("대상 없음 → NOT_FOUND")
        void 없음() {
            given(stoneTypeRepository.findById(TYPE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStoneType(TYPE_ID, new StoneTypeDto("a", "b")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("이름 동일 — exists 결과 true 라도 통과 (변경 없음)")
        void 이름_동일() {
            StoneType type = mock(StoneType.class);
            given(type.getStoneTypeName()).willReturn("다이아몬드");
            given(stoneTypeRepository.findById(TYPE_ID)).willReturn(Optional.of(type));
            // 이름이 동일하므로 existsByStoneTypeName 의 반환값은 분기에 영향 없음
            given(stoneTypeRepository.existsByStoneTypeName("다이아몬드")).willReturn(true);

            StoneTypeDto dto = new StoneTypeDto("다이아몬드", "수정 비고");
            service.updateStoneType(TYPE_ID, dto);

            verify(type).updateStoneType(dto);
        }

        @Test
        @DisplayName("이름 변경 + 중복 → IS_EXIST")
        void 이름변경_중복() {
            StoneType type = mock(StoneType.class);
            given(type.getStoneTypeName()).willReturn("다이아몬드");
            given(stoneTypeRepository.findById(TYPE_ID)).willReturn(Optional.of(type));
            given(stoneTypeRepository.existsByStoneTypeName("사파이어")).willReturn(true);

            assertThatThrownBy(() -> service.updateStoneType(TYPE_ID, new StoneTypeDto("사파이어", "x")))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(type, never()).updateStoneType(any());
        }

        @Test
        @DisplayName("이름 변경 + 중복 아님 → 엔티티 update 호출")
        void 이름변경_정상() {
            StoneType type = mock(StoneType.class);
            given(type.getStoneTypeName()).willReturn("다이아몬드");
            given(stoneTypeRepository.findById(TYPE_ID)).willReturn(Optional.of(type));
            given(stoneTypeRepository.existsByStoneTypeName("사파이어")).willReturn(false);

            StoneTypeDto dto = new StoneTypeDto("사파이어", "x");
            service.updateStoneType(TYPE_ID, dto);

            verify(type).updateStoneType(dto);
        }
    }

    // ============================================================
    // deleteStoneType
    // ============================================================
    @Nested
    @DisplayName("deleteStoneType")
    class DeleteStoneType {

        @Test
        @DisplayName("관리자 아님 → NOT_ACCESS")
        void 권한없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("USER");

            assertThatThrownBy(() -> service.deleteStoneType(TOKEN, TYPE_ID))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(stoneTypeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("대상 없음 → NOT_FOUND")
        void 대상_없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(stoneTypeRepository.findById(TYPE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteStoneType(TOKEN, TYPE_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("정상 — delete 호출")
        void 정상() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            StoneType type = mock(StoneType.class);
            given(stoneTypeRepository.findById(TYPE_ID)).willReturn(Optional.of(type));

            service.deleteStoneType(TOKEN, TYPE_ID);

            verify(stoneTypeRepository).delete(type);
        }

        @Test
        @DisplayName("사용 중 데이터 — repository.delete 가 DataIntegrityViolationException 전파")
        void 사용중_데이터() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            StoneType type = mock(StoneType.class);
            given(stoneTypeRepository.findById(TYPE_ID)).willReturn(Optional.of(type));
            willThrow(new DataIntegrityViolationException("FK")).given(stoneTypeRepository).delete(type);

            assertThatThrownBy(() -> service.deleteStoneType(TOKEN, TYPE_ID))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
