package com.msa.product.local.stone.shape.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.product.local.stone.shape.dto.StoneShapeDto;
import com.msa.product.local.stone.shape.entity.StoneShape;
import com.msa.product.local.stone.shape.repository.StoneShapeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.msa.product.global.exception.ExceptionMessage.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class StoneShapeServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StoneShapeRepository stoneShapeRepository;

    @InjectMocks
    private StoneShapeService stoneShapeService;

    private StoneShapeDto stoneShapeDto;

    @BeforeEach
    void setUp() {
        stoneShapeDto = new StoneShapeDto("라운드", "둥근 모양");
    }

    @Test
    void saveStoneShape_success() {
        given(stoneShapeRepository.existsByStoneShapeName("라운드")).willReturn(false);

        stoneShapeService.saveStoneShape(stoneShapeDto);

        then(stoneShapeRepository).should().save(any(StoneShape.class));
    }

    @Test
    void saveStoneShape_sameName_exception() {
        given(stoneShapeRepository.existsByStoneShapeName("라운드")).willReturn(true);

        assertThatThrownBy(() -> stoneShapeService.saveStoneShape(stoneShapeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(IS_EXIST);
    }

    @Test
    void getStoneShape_success() {
        StoneShape stoneShape = StoneShape.builder()
                .stoneShapeName("라운드")
                .stoneShapeNote("둥근 모양")
                .build();

        given(stoneShapeRepository.findById(1L)).willReturn(Optional.of(stoneShape));

        StoneShapeDto.ResponseSingle result = stoneShapeService.getStoneShape(1L);

        assertThat(result.getStoneShapeName()).isEqualTo("라운드");
        assertThat(result.getStoneShapeNote()).isEqualTo("둥근 모양");
    }

    @Test
    void getStoneShape_notFound_exception() {
        given(stoneShapeRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stoneShapeService.getStoneShape(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    void getStoneShapes_success() {
        List<StoneShapeDto.ResponseSingle> list = List.of(
                StoneShapeDto.ResponseSingle.builder().stoneShapeId("1").stoneShapeName("라운드").stoneShapeNote("둥근 모양").build(),
                StoneShapeDto.ResponseSingle.builder().stoneShapeId("2").stoneShapeName("스퀘어").stoneShapeNote("네모 모양").build()
        );

        given(stoneShapeRepository.findByStoneShapeAllOrderByAsc(null)).willReturn(list);

        List<StoneShapeDto.ResponseSingle> result = stoneShapeService.getStoneShapes(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStoneShapeName()).isEqualTo("라운드");
    }

    @Test
    void updateStoneShape_success() {
        StoneShape stoneShape = StoneShape.builder()
                .stoneShapeName("스퀘어")
                .stoneShapeNote("네모 모양")
                .build();

        given(stoneShapeRepository.findById(1L)).willReturn(Optional.of(stoneShape));
        given(stoneShapeRepository.existsByStoneShapeName("라운드")).willReturn(false);

        stoneShapeService.updateStoneShape(1L, stoneShapeDto);

        assertThat(stoneShape.getStoneShapeName()).isEqualTo("라운드");
        assertThat(stoneShape.getStoneShapeNote()).isEqualTo("둥근 모양");
    }

    @Test
    void updateStoneShape_sameName_exception() {
        StoneShape stoneShape = StoneShape.builder()
                .stoneShapeName("스퀘어")
                .stoneShapeNote("네모 모양")
                .build();

        given(stoneShapeRepository.findById(1L)).willReturn(Optional.of(stoneShape));
        given(stoneShapeRepository.existsByStoneShapeName("라운드")).willReturn(true);

        assertThatThrownBy(() -> stoneShapeService.updateStoneShape(1L, stoneShapeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(IS_EXIST);
    }

    @Test
    void updateStoneShape_notFound_exception() {
        given(stoneShapeRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stoneShapeService.updateStoneShape(1L, stoneShapeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    void deleteStoneShape_success() {
        StoneShape stoneShape = StoneShape.builder()
                .stoneShapeName("스퀘어")
                .stoneShapeNote("네모 모양")
                .build();

        given(jwtUtil.getRole("admin-token")).willReturn("ADMIN");
        given(stoneShapeRepository.findById(1L)).willReturn(Optional.of(stoneShape));

        stoneShapeService.deleteStoneShape("admin-token", 1L);

        verify(stoneShapeRepository).delete(stoneShape);
    }

    @Test
    void deleteStoneShape_noAdmin_exception() {
        given(jwtUtil.getRole("user-token")).willReturn("USER");

        assertThatThrownBy(() -> stoneShapeService.deleteStoneShape("user-token", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_ACCESS);
    }

    @Test
    void deleteStoneShape_notFound_exception() {
        given(jwtUtil.getRole("admin-token")).willReturn("ADMIN");
        given(stoneShapeRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stoneShapeService.deleteStoneShape("admin-token", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }
}
