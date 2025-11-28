package com.msa.product.local.set.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.product.global.kafka.KafkaProducer;
import com.msa.product.local.set.dto.SetTypeDto;
import com.msa.product.local.set.entity.SetType;
import com.msa.product.local.set.repository.SetTypeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static com.msa.product.global.exception.ExceptionMessage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class SetTypeServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private SetTypeRepository setTypeRepository;

    @InjectMocks
    private SetTypeService setTypeService;

    private SetTypeDto setTypeDto;

    @BeforeEach
    void setUp() {
        setTypeDto = new SetTypeDto("커플링", "메모");
    }

    @Test
    void saveSetType_success() {
        given(setTypeRepository.existsBySetTypeName("커플링")).willReturn(false);

        setTypeService.saveSetType(setTypeDto);

        then(setTypeRepository).should().save(any(SetType.class));
    }

    @Test
    void saveSetType_sameName_exception() {
        given(setTypeRepository.existsBySetTypeName("커플링")).willReturn(true);

        assertThatThrownBy(() -> setTypeService.saveSetType(setTypeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(IS_EXIST);
    }

    @Test
    void getSetType_success() {
        SetType setType = SetType.builder()
                .setTypeName("커플링")
                .setTypeNote("메모")
                .build();

        given(setTypeRepository.findById(1L)).willReturn(Optional.of(setType));

        SetTypeDto.ResponseSingle result = setTypeService.getSetType(1L);

        assertThat(result.getSetTypeName()).isEqualTo("커플링");
        assertThat(result.getSetTypeNote()).isEqualTo("메모");
    }

    @Test
    void getSetType_notFound_exception() {
        given(setTypeRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> setTypeService.getSetType(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    void getSetTypes_success() {
        List<SetTypeDto.ResponseSingle> list = List.of(
                SetTypeDto.ResponseSingle.builder().setTypeId("1").setTypeName("커플링").setTypeNote("메모").build(),
                SetTypeDto.ResponseSingle.builder().setTypeId("2").setTypeName("우정링").setTypeNote("메모2").build()
        );

        given(setTypeRepository.findAllOrderByAsc("")).willReturn(list);

        List<SetTypeDto.ResponseSingle> result = setTypeService.getSetTypes("");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSetTypeName()).isEqualTo("커플링");
    }

    @Test
    void updateSetType_success() {
        SetType setType = SetType.builder()
                .setTypeName("우정링")
                .setTypeNote("메모2")
                .build();

        given(setTypeRepository.findById(1L)).willReturn(Optional.of(setType));
        given(setTypeRepository.existsBySetTypeName("커플링")).willReturn(false);

        setTypeService.updateSetType(1L, setTypeDto);

        assertThat(setType.getSetTypeName()).isEqualTo("커플링");
        assertThat(setType.getSetTypeNote()).isEqualTo("메모");
    }

    @Test
    void updateSetType_sameName_exception() {
        SetType setType = SetType.builder()
                .setTypeName("우정링")
                .setTypeNote("메모2")
                .build();

        given(setTypeRepository.findById(1L)).willReturn(Optional.of(setType));
        given(setTypeRepository.existsBySetTypeName("커플링")).willReturn(true);

        assertThatThrownBy(() -> setTypeService.updateSetType(1L, setTypeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(IS_EXIST);
    }

    @Test
    void updateSetType_notFound_exception() {
        given(setTypeRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> setTypeService.updateSetType(1L, setTypeDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    void deletedSetType_success() {
        SetType setType = mock(SetType.class);
        given(jwtUtil.getRole("admin-token")).willReturn("ADMIN");
        given(jwtUtil.getTenantId("admin-token")).willReturn("lim");
        given(setTypeRepository.findById(1L)).willReturn(Optional.of(setType));
        given(setType.isDeletable()).willReturn(false);

        setTypeService.deletedSetType("admin-token", 1L);

        verify(kafkaProducer).sendSetTypeUpdate("lim", 1L);
    }

    @Test
    void deletedSetType_isDefault_exception() {
        SetType setType = mock(SetType.class);
        given(jwtUtil.getRole("admin-token")).willReturn("ADMIN");
        given(setTypeRepository.findById(1L)).willReturn(Optional.of(setType));
        given(setType.isDeletable()).willReturn(true); // 기본값이면 삭제 불가

        assertThatThrownBy(() -> setTypeService.deletedSetType("admin-token", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(CANNOT_DELETE_DEFAULT);
    }

    @Test
    void deletedSetType_noAdmin_exception() {
        SetType setType = mock(SetType.class);
        given(jwtUtil.getRole("user-token")).willReturn("USER");
        given(setTypeRepository.findById(1L)).willReturn(Optional.of(setType));
        given(setType.isDeletable()).willReturn(false);

        assertThatThrownBy(() -> setTypeService.deletedSetType("user-token", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_ACCESS);
    }

    @Test
    void deletedSetType_notFound_exception() {
        given(setTypeRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> setTypeService.deletedSetType("admin-token", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }
}