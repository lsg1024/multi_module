package com.msa.product.local.material.service;

import com.msa.product.local.material.dto.MaterialDto;
import com.msa.product.local.material.entity.Material;
import com.msa.product.local.material.repository.MaterialRepository;
import com.msacommon.global.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private MaterialService materialService;

    private MaterialDto materialDto;

    @BeforeEach
    void setUp() {
        materialDto = new MaterialDto("14K", "58.5");
    }

    @Test
    void saveMaterial_success() {
        given(materialRepository.existsByMaterialName("14K")).willReturn(false);
        materialService.saveMaterial(materialDto);
        then(materialRepository).should().save(any(Material.class));
    }

    @Test
    void getMaterial_success() {
        Material material = Material.builder()
                .materialName("14K")
                .materialGoldPurityPercent(new BigDecimal("58.5"))
                .build();

        given(materialRepository.findById(1L)).willReturn(Optional.of(material));

        MaterialDto.ResponseSingle result = materialService.getMaterial(1L);

        assertThat(result.getMaterialName()).isEqualTo("14K");
    }

    @Test
    void getMaterials_success() {
        List<MaterialDto.ResponseSingle> list = List.of(
                new MaterialDto.ResponseSingle("1", "14K", "58.5"),
                new MaterialDto.ResponseSingle("2", "18K", "75.0")
        );

        given(materialRepository.findAllOrderByAsc()).willReturn(list);

        List<MaterialDto.ResponseSingle> result = materialService.getMaterials();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getMaterialName()).isEqualTo("14K");
    }

    @Test
    void updateMaterial_success() {
        Material material = Material.builder()
                .materialName("18K")
                .materialGoldPurityPercent(new BigDecimal("75.0"))
                .build();

        given(materialRepository.findById(1L)).willReturn(Optional.of(material));
        given(materialRepository.existsByMaterialName("14K")).willReturn(false);

        materialService.updateMaterial(1L, materialDto);

        assertThat(material.getMaterialName()).isEqualTo("14K");
    }

    @Test
    void saveMaterial_sameName_exception() {
        given(materialRepository.existsByMaterialName("14K")).willReturn(true);

        assertThatThrownBy(() -> materialService.saveMaterial(materialDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getMaterial_no_exist_exception() {
        given(materialRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> materialService.getMaterial(1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateMaterial_sameName_exception() {
        Material material = Material.builder()
                .materialName("18K")
                .materialGoldPurityPercent(new BigDecimal("75.0"))
                .build();

        given(materialRepository.findById(1L)).willReturn(Optional.of(material));
        given(materialRepository.existsByMaterialName("14K")).willReturn(true);

        assertThatThrownBy(() -> materialService.updateMaterial(1L, materialDto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteMaterial_no_admin_exception() {
        given(jwtUtil.getRole("token")).willReturn("USER");

        assertThatThrownBy(() -> materialService.deleteMaterial("token", 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteMaterial_success() {
        Material material = Material.builder()
                .materialName("18K")
                .materialGoldPurityPercent(new BigDecimal("75.0"))
                .build();

        given(jwtUtil.getRole("admin-token")).willReturn("ADMIN");
        given(materialRepository.findById(1L)).willReturn(Optional.of(material));

        materialService.deleteMaterial("admin-token", 1L);

        verify(materialRepository).delete(material);
    }
}