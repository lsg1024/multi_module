package com.msa.jewelry.local.stone_shape.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.jewelry.local.stone_shape.dto.StoneShapeDto;
import com.msa.jewelry.local.stone_shape.entity.StoneShape;
import com.msa.jewelry.local.stone_shape.repository.StoneShapeRepository;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StoneShapeService 단위 테스트")
class StoneShapeServiceTest {

    private static final String TOKEN = "Bearer test-token";
    private static final Long SHAPE_ID = 11L;

    @Mock JwtUtil jwtUtil;
    @Mock StoneShapeRepository stoneShapeRepository;

    @InjectMocks
    StoneShapeService service;

    // ============================================================
    // saveStoneShape
    // ============================================================
    @Nested
    @DisplayName("saveStoneShape")
    class SaveStoneShape {

        @Test
        @DisplayName("정상 — repository.save 호출, 빌더 필드 그대로 매핑")
        void 정상_저장() {
            StoneShapeDto dto = new StoneShapeDto("라운드", "원형 브릴리언트 컷");
            given(stoneShapeRepository.existsByStoneShapeName("라운드")).willReturn(false);

            service.saveStoneShape(dto);

            ArgumentCaptor<StoneShape> captor = ArgumentCaptor.forClass(StoneShape.class);
            verify(stoneShapeRepository).save(captor.capture());
            assertThat(captor.getValue().getStoneShapeName()).isEqualTo("라운드");
            assertThat(captor.getValue().getStoneShapeNote()).isEqualTo("원형 브릴리언트 컷");
        }

        @Test
        @DisplayName("중복 모양명 → IllegalArgumentException, save 호출되지 않음")
        void 중복_이름() {
            StoneShapeDto dto = new StoneShapeDto("라운드", "비고");
            given(stoneShapeRepository.existsByStoneShapeName("라운드")).willReturn(true);

            assertThatThrownBy(() -> service.saveStoneShape(dto))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(stoneShapeRepository, never()).save(any(StoneShape.class));
        }

        @Test
        @DisplayName("DB 유니크 제약 위반 — DataIntegrityViolationException 전파")
        void DB_유니크_위반() {
            StoneShapeDto dto = new StoneShapeDto("사각", "비고");
            given(stoneShapeRepository.existsByStoneShapeName("사각")).willReturn(false);
            willThrow(new DataIntegrityViolationException("unique"))
                    .given(stoneShapeRepository).save(any(StoneShape.class));

            assertThatThrownBy(() -> service.saveStoneShape(dto))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("note 가 null 이어도 저장은 정상 (note 제약 없음)")
        void note_null() {
            StoneShapeDto dto = new StoneShapeDto("하트", null);
            given(stoneShapeRepository.existsByStoneShapeName("하트")).willReturn(false);

            service.saveStoneShape(dto);

            verify(stoneShapeRepository).save(any(StoneShape.class));
        }
    }

    // ============================================================
    // getStoneShape
    // ============================================================
    @Nested
    @DisplayName("getStoneShape")
    class GetStoneShape {

        @Test
        @DisplayName("정상 — ResponseSingle 매핑")
        void 정상() {
            StoneShape shape = mock(StoneShape.class);
            given(shape.getStoneShapeName()).willReturn("라운드");
            given(shape.getStoneShapeNote()).willReturn("원형");
            given(stoneShapeRepository.findById(SHAPE_ID)).willReturn(Optional.of(shape));

            StoneShapeDto.ResponseSingle res = service.getStoneShape(SHAPE_ID);

            assertThat(res.getStoneShapeId()).isEqualTo("11");
            assertThat(res.getStoneShapeName()).isEqualTo("라운드");
            assertThat(res.getStoneShapeNote()).isEqualTo("원형");
        }

        @Test
        @DisplayName("없음 → IllegalArgumentException(NOT_FOUND)")
        void 없음() {
            given(stoneShapeRepository.findById(SHAPE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getStoneShape(SHAPE_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ============================================================
    // getStoneShapes
    // ============================================================
    @Nested
    @DisplayName("getStoneShapes")
    class GetStoneShapes {

        @Test
        @DisplayName("정상 — repository 결과 그대로 반환")
        void 정상() {
            StoneShapeDto.ResponseSingle item = StoneShapeDto.ResponseSingle.builder()
                    .stoneShapeId("1").stoneShapeName("라운드").stoneShapeNote("n").build();
            given(stoneShapeRepository.findByStoneShapeAllOrderByAsc("라운드"))
                    .willReturn(List.of(item));

            List<StoneShapeDto.ResponseSingle> list = service.getStoneShapes("라운드");

            assertThat(list).hasSize(1);
            assertThat(list.get(0).getStoneShapeName()).isEqualTo("라운드");
        }

        @Test
        @DisplayName("빈 결과 — 빈 리스트 그대로 위임")
        void 빈결과() {
            given(stoneShapeRepository.findByStoneShapeAllOrderByAsc(null))
                    .willReturn(Collections.emptyList());

            assertThat(service.getStoneShapes(null)).isEmpty();
        }
    }

    // ============================================================
    // updateStoneShape
    // ============================================================
    @Nested
    @DisplayName("updateStoneShape")
    class UpdateStoneShape {

        @Test
        @DisplayName("대상 없음 → NOT_FOUND")
        void 없음() {
            given(stoneShapeRepository.findById(SHAPE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStoneShape(SHAPE_ID, new StoneShapeDto("a", "b")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("이름 동일 — 중복체크 스킵, 엔티티 updateStoneShape 호출")
        void 이름_동일() {
            StoneShape shape = mock(StoneShape.class);
            given(shape.getStoneShapeName()).willReturn("라운드");
            given(stoneShapeRepository.findById(SHAPE_ID)).willReturn(Optional.of(shape));

            StoneShapeDto dto = new StoneShapeDto("라운드", "수정 비고");
            service.updateStoneShape(SHAPE_ID, dto);

            verify(stoneShapeRepository, never()).existsByStoneShapeName(any());
            verify(shape).updateStoneShape(dto);
        }

        @Test
        @DisplayName("이름 변경 + 중복 → IS_EXIST")
        void 이름변경_중복() {
            StoneShape shape = mock(StoneShape.class);
            given(shape.getStoneShapeName()).willReturn("라운드");
            given(stoneShapeRepository.findById(SHAPE_ID)).willReturn(Optional.of(shape));
            given(stoneShapeRepository.existsByStoneShapeName("사각")).willReturn(true);

            assertThatThrownBy(() -> service.updateStoneShape(SHAPE_ID, new StoneShapeDto("사각", "x")))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(shape, never()).updateStoneShape(any());
        }

        @Test
        @DisplayName("이름 변경 + 중복 아님 → 엔티티 update 호출")
        void 이름변경_정상() {
            StoneShape shape = mock(StoneShape.class);
            given(shape.getStoneShapeName()).willReturn("라운드");
            given(stoneShapeRepository.findById(SHAPE_ID)).willReturn(Optional.of(shape));
            given(stoneShapeRepository.existsByStoneShapeName("사각")).willReturn(false);

            StoneShapeDto dto = new StoneShapeDto("사각", "수정");
            service.updateStoneShape(SHAPE_ID, dto);

            verify(shape).updateStoneShape(dto);
        }
    }

    // ============================================================
    // deleteStoneShape
    // ============================================================
    @Nested
    @DisplayName("deleteStoneShape")
    class DeleteStoneShape {

        @Test
        @DisplayName("관리자 아님 → NOT_ACCESS")
        void 권한없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("USER");

            assertThatThrownBy(() -> service.deleteStoneShape(TOKEN, SHAPE_ID))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(stoneShapeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("대상 없음 → NOT_FOUND")
        void 대상_없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(stoneShapeRepository.findById(SHAPE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteStoneShape(TOKEN, SHAPE_ID))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(stoneShapeRepository, never()).delete(any());
        }

        @Test
        @DisplayName("정상 — delete 호출")
        void 정상() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            StoneShape shape = mock(StoneShape.class);
            given(stoneShapeRepository.findById(SHAPE_ID)).willReturn(Optional.of(shape));

            service.deleteStoneShape(TOKEN, SHAPE_ID);

            verify(stoneShapeRepository).delete(shape);
        }

        @Test
        @DisplayName("사용 중 데이터 — repository.delete 가 DataIntegrityViolationException 전파")
        void 사용중_데이터() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            StoneShape shape = mock(StoneShape.class);
            given(stoneShapeRepository.findById(SHAPE_ID)).willReturn(Optional.of(shape));
            willThrow(new DataIntegrityViolationException("FK"))
                    .given(stoneShapeRepository).delete(shape);

            assertThatThrownBy(() -> service.deleteStoneShape(TOKEN, SHAPE_ID))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }
}
