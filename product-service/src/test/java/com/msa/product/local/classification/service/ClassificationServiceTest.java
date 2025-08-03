package com.msa.product.local.classification.service;

import com.msa.product.local.classification.dto.ClassificationDto;
import com.msa.product.local.classification.entity.Classification;
import com.msa.product.local.classification.repository.ClassificationRepository;
import com.msa.common.global.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ClassificationServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private ClassificationRepository classificationRepository;

    @InjectMocks
    private ClassificationService classificationService;

    private ClassificationDto classificationDto;

    @BeforeEach
    void setUp() {
        classificationDto = new ClassificationDto("귀걸이", "");
    }

    @Test
    void saveClassification_success() {
        given(classificationRepository.existsByClassificationName("귀걸이")).willReturn(false);

        classificationService.saveClassification(classificationDto);

        then(classificationRepository).should().save(any(Classification.class));
    }

    @Test
    void getClassification_success() {
        Classification classification = Classification.builder()
                .classificationName("귀걸이")
                .classificationNote("")
                .build();

        given(classificationRepository.findById(1L)).willReturn(Optional.of(classification));

        ClassificationDto.ResponseSingle result = classificationService.getClassification(1L);

        assertThat(result.getClassificationName()).isEqualTo("귀걸이");
    }

    @Test
    void getClassifications_success() {
        List<ClassificationDto.ResponseSingle> list = List.of(
                new ClassificationDto.ResponseSingle("1", "귀걸이", ""),
                new ClassificationDto.ResponseSingle("2", "반지", "")
        );

        given(classificationRepository.findAllOrderByAsc(null)).willReturn(list);

        List<ClassificationDto.ResponseSingle> result = classificationService.getClassifications(null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getClassificationName()).isEqualTo("귀걸이");
    }

    @Test
    void updateClassification_success() {
        Classification classification = Classification.builder()
                .classificationName("반지")
                .classificationNote("note")
                .build();

        given(classificationRepository.findById(1L)).willReturn(Optional.of(classification));
        given(classificationRepository.existsByClassificationName("귀걸이")).willReturn(false);

        classificationService.updateClassification(1L, classificationDto);

        assertThat(classification.getClassificationName()).isEqualTo("귀걸이");
    }

    @Test
    void saveClassification_sameName_exception() {
        given(classificationRepository.existsByClassificationName("귀걸이")).willReturn(true);

        assertThatThrownBy(() -> classificationService.saveClassification(classificationDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getClassification_no_have_exception() {
        given(classificationRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> classificationService.getClassification(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateClassification_sameName_exception() {
        Classification classification = Classification.builder()
                .classificationName("반지")
                .classificationNote("note")
                .build();

        given(classificationRepository.findById(1L)).willReturn(Optional.of(classification));
        given(classificationRepository.existsByClassificationName("귀걸이")).willReturn(true);

        assertThatThrownBy(() -> classificationService.updateClassification(1L, classificationDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deletedClassification_no_admin_exception() {
        given(jwtUtil.getRole("invalid-token")).willReturn("USER");

        assertThatThrownBy(() -> classificationService.deletedClassification("invalid-token", 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deletedClassification_success() {
        Classification classification = Classification.builder()
                .classificationName("반지")
                .classificationNote("note")
                .build();

        given(jwtUtil.getRole("admin-token")).willReturn("ADMIN");
        given(classificationRepository.findById(1L)).willReturn(Optional.of(classification));

        classificationService.deletedClassification("admin-token", 1L);

        verify(classificationRepository).delete(classification);
    }
}