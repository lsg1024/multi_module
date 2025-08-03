package com.msa.product.local.color.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.product.global.kafka.KafkaProducer;
import com.msa.product.local.color.dto.ColorDto;
import com.msa.product.local.color.entity.Color;
import com.msa.product.local.color.repository.ColorRepository;
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
class ColorServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ColorRepository colorRepository;

    @Mock
    private KafkaProducer kafkaProducer;

    @InjectMocks
    private ColorService colorService;

    private ColorDto colorDto;

    @BeforeEach
    void setUp() {
        colorDto = new ColorDto("PINK", "pink note");
    }

    @Test
    void saveColor_success() {
        given(colorRepository.existsByColorName("PINK")).willReturn(false);

        colorService.saveColor(colorDto);

        then(colorRepository).should().save(any(Color.class));
    }

    @Test
    void saveColor_sameName_exception() {
        given(colorRepository.existsByColorName("PINK")).willReturn(true);

        assertThatThrownBy(() -> colorService.saveColor(colorDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(IS_EXIST);
    }

    @Test
    void getColor_success() {
        Color color = Color.builder()
                .colorName("PINK")
                .colorNote("pink note")
                .build();

        given(colorRepository.findById(1L)).willReturn(Optional.of(color));

        ColorDto.ResponseSingle result = colorService.getColor(1L);

        assertThat(result.getColorName()).isEqualTo("PINK");
        assertThat(result.getColorNote()).isEqualTo("pink note");
    }

    @Test
    void getColor_notFound_exception() {
        given(colorRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> colorService.getColor(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    void getColors_success() {
        List<ColorDto.ResponseSingle> list = List.of(
                ColorDto.ResponseSingle.builder().colorId("1").colorName("PINK").colorNote("note").build(),
                ColorDto.ResponseSingle.builder().colorId("2").colorName("GREEN").colorNote("note2").build()
        );

        given(colorRepository.findAllOrderByAsc(null)).willReturn(list);

        List<ColorDto.ResponseSingle> result = colorService.getColors(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getColorName()).isEqualTo("PINK");
    }

    @Test
    void updateColor_success() {
        Color color = Color.builder()
                .colorName("GREEN")
                .colorNote("note")
                .build();

        given(colorRepository.findById(1L)).willReturn(Optional.of(color));
        given(colorRepository.existsByColorName("PINK")).willReturn(false);

        colorService.updateColor(1L, colorDto);

        assertThat(color.getColorName()).isEqualTo("PINK");
        assertThat(color.getColorNote()).isEqualTo("pink note");
    }

    @Test
    void updateColor_sameName_exception() {
        Color color = Color.builder()
                .colorName("GREEN")
                .colorNote("note")
                .build();

        given(colorRepository.findById(1L)).willReturn(Optional.of(color));
        given(colorRepository.existsByColorName("PINK")).willReturn(true);

        assertThatThrownBy(() -> colorService.updateColor(1L, colorDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(IS_EXIST);
    }

    @Test
    void updateColor_notFound_exception() {
        given(colorRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> colorService.updateColor(1L, colorDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    void deleteColor_success() {
        Color color = mock(Color.class);
        given(jwtUtil.getRole("admin-token")).willReturn("ADMIN");
        given(jwtUtil.getTenantId("admin-token")).willReturn("lim");
        given(colorRepository.findById(1L)).willReturn(Optional.of(color));
        given(color.isDeletable()).willReturn(false);

        colorService.deleteColor("admin-token", 1L);
        verify(kafkaProducer).sendColorUpdate("lim", 1L);
    }

    @Test
    void deleteColor_isDefault_exception() {
        Color color = mock(Color.class);
        given(jwtUtil.getRole("admin-token")).willReturn("ADMIN");
        given(colorRepository.findById(1L)).willReturn(Optional.of(color));
        given(color.isDeletable()).willReturn(true); // 기본값이라 삭제 불가

        assertThatThrownBy(() -> colorService.deleteColor("admin-token", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(CANNOT_DELETE_DEFAULT);
    }

    @Test
    void deleteColor_noAdmin_exception() {
        Color color = mock(Color.class);
        given(jwtUtil.getRole("user-token")).willReturn("USER");
        given(colorRepository.findById(1L)).willReturn(Optional.of(color));
        given(color.isDeletable()).willReturn(false);

        assertThatThrownBy(() -> colorService.deleteColor("user-token", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_ACCESS);
    }

    @Test
    void deleteColor_notFound_exception() {
        given(colorRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> colorService.deleteColor("admin-token", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }
}