package com.msa.product.local.stone.type.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.product.local.stone.type.dto.StoneTypeDto;
import com.msa.product.local.stone.type.entity.StoneType;
import com.msa.product.local.stone.type.repository.StoneTypeRepository;
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
class StoneTypeServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StoneTypeRepository stoneTypeRepository;

    @InjectMocks
    private StoneTypeService stoneTypeService;

    private StoneTypeDto stoneTypeDto;

    @BeforeEach
    void setUp() {
        stoneTypeDto = new StoneTypeDto("오벌", "타원형 타입");
    }

    @Test
    void saveStoneType_success() {
        given(stoneTypeRepository.existsByStoneTypeName("오벌")).willReturn(false);

        stoneTypeService.saveStoneType(stoneTypeDto);

        then(stoneTypeRepository).should().save(any(StoneType.class));
    }

    @Test
    void saveStoneType_sameName_exception() {
        given(stoneTypeRepository.existsByStoneTypeName("오벌")).willReturn(true);

        assertThatThrownBy(() -> stoneTypeService.saveStoneType(stoneTypeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(IS_EXIST);
    }

    @Test
    void getStoneType_success() {
        StoneType stoneType = StoneType.builder()
                .stoneTypeName("오벌")
                .stoneTypeNote("타원형 타입")
                .build();

        given(stoneTypeRepository.findById(1L)).willReturn(Optional.of(stoneType));

        StoneTypeDto.ResponseSingle result = stoneTypeService.getStoneType(1L);

        assertThat(result.getStoneTypeName()).isEqualTo("오벌");
        assertThat(result.getStoneTypeNote()).isEqualTo("타원형 타입");
    }

    @Test
    void getStoneType_notFound_exception() {
        given(stoneTypeRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stoneTypeService.getStoneType(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    void getStoneTypes_success() {
        List<StoneTypeDto.ResponseSingle> list = List.of(
                StoneTypeDto.ResponseSingle.builder().stoneTypeId("1").stoneTypeName("오벌").stoneTypeNote("타원형 타입").build(),
                StoneTypeDto.ResponseSingle.builder().stoneTypeId("2").stoneTypeName("라운드").stoneTypeNote("원형 타입").build()
        );

        given(stoneTypeRepository.findByStoneTypeAllOrderByAsc(null)).willReturn(list);

        List<StoneTypeDto.ResponseSingle> result = stoneTypeService.getStoneTypes(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStoneTypeName()).isEqualTo("오벌");
    }

    @Test
    void updateStoneType_success() {
        StoneType stoneType = StoneType.builder()
                .stoneTypeName("라운드")
                .stoneTypeNote("원형 타입")
                .build();

        given(stoneTypeRepository.findById(1L)).willReturn(Optional.of(stoneType));
        given(stoneTypeRepository.existsByStoneTypeName("오벌")).willReturn(false);

        stoneTypeService.updateStoneType(1L, stoneTypeDto);

        assertThat(stoneType.getStoneTypeName()).isEqualTo("오벌");
        assertThat(stoneType.getStoneTypeNote()).isEqualTo("타원형 타입");
    }

    @Test
    void updateStoneType_sameName_exception() {
        StoneType stoneType = StoneType.builder()
                .stoneTypeName("라운드")
                .stoneTypeNote("원형 타입")
                .build();

        given(stoneTypeRepository.findById(1L)).willReturn(Optional.of(stoneType));
        given(stoneTypeRepository.existsByStoneTypeName("오벌")).willReturn(true);

        assertThatThrownBy(() -> stoneTypeService.updateStoneType(1L, stoneTypeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(IS_EXIST);
    }

    @Test
    void updateStoneType_notFound_exception() {
        given(stoneTypeRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stoneTypeService.updateStoneType(1L, stoneTypeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    void deleteStoneType_success() {
        StoneType stoneType = StoneType.builder()
                .stoneTypeName("라운드")
                .stoneTypeNote("원형 타입")
                .build();

        given(jwtUtil.getRole("admin-token")).willReturn("ADMIN");
        given(stoneTypeRepository.findById(1L)).willReturn(Optional.of(stoneType));

        stoneTypeService.deleteStoneType("admin-token", 1L);

        verify(stoneTypeRepository).delete(stoneType);
    }

    @Test
    void deleteStoneType_noAdmin_exception() {
        given(jwtUtil.getRole("user-token")).willReturn("USER");

        assertThatThrownBy(() -> stoneTypeService.deleteStoneType("user-token", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_ACCESS);
    }

    @Test
    void deleteStoneType_notFound_exception() {
        given(jwtUtil.getRole("admin-token")).willReturn("ADMIN");
        given(stoneTypeRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stoneTypeService.deleteStoneType("admin-token", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }
}
