package com.msa.product.local.stone.stone.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.product.local.stone.stone.dto.StoneDto;
import com.msa.product.local.stone.stone.dto.StoneWorkGradePolicyDto;
import com.msa.product.local.stone.stone.entity.Stone;
import com.msa.product.local.stone.stone.entity.StoneWorkGradePolicy;
import com.msa.product.local.stone.stone.repository.StoneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.msa.product.global.exception.ExceptionMessage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class StoneServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StoneRepository stoneRepository;

    @InjectMocks
    private StoneService stoneService;

    private StoneDto stoneDto;
    private List<StoneWorkGradePolicy> gradePolicies;

    @BeforeEach
    void setUp() {
        // grade policy dto stub
        StoneWorkGradePolicy policy1 = StoneWorkGradePolicy.builder()
                .stoneWorkGradePolicyId(1L)
                .grade("GRADE_1")
                .laborCost(10000)
                .build();

        StoneWorkGradePolicy policy2 = StoneWorkGradePolicy.builder()
                .stoneWorkGradePolicyId(2L)
                .grade("GRADE_2")
                .laborCost(5000)
                .build();

        gradePolicies = List.of(policy1, policy2);

        List<StoneWorkGradePolicyDto> stoneWorkGradePolicyDtos = List.of(
                new StoneWorkGradePolicyDto("GRADE_1", 10000),
                new StoneWorkGradePolicyDto("GRADE_2", 5000)
        );
        stoneDto = new StoneDto("아쿠아마린", "파란색 보석", "2.50", 15000, stoneWorkGradePolicyDtos);
    }

    @Test
    void saveStone_success() {
        given(stoneRepository.existsByStoneName("아쿠아마린")).willReturn(false);

        stoneService.saveStone(stoneDto);

        then(stoneRepository).should().save(any(Stone.class));
    }

    @Test
    void saveStone_duplicateName_exception() {
        given(stoneRepository.existsByStoneName("아쿠아마린")).willReturn(true);

        assertThatThrownBy(() -> stoneService.saveStone(stoneDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(IS_EXIST);
    }

    @Test
    void getStone_success() {
        Stone stone = Stone.builder()
                .stoneName("아쿠아마린")
                .stoneNote("파란색 보석")
                .stoneWeight(new BigDecimal("2.5"))
                .stonePurchasePrice(15000)
                .gradePolicies(gradePolicies)
                .build();
        given(stoneRepository.findFetchJoinById(1L)).willReturn(Optional.of(stone));

        StoneDto.ResponseSingle result = stoneService.getStone(1L);

        assertThat(result.getStoneName()).isEqualTo("아쿠아마린");
        assertThat(result.getStoneNote()).isEqualTo("파란색 보석");
        assertThat(result.getStoneWeight()).isEqualTo("2.5");
        assertThat(result.getStonePurchasePrice()).isEqualTo("15000");
        assertThat(result.getStoneWorkGradePolicyDto()).hasSize(2);
    }

    @Test
    void getStone_notFound_exception() {
        given(stoneRepository.findFetchJoinById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stoneService.getStone(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    void getStones_success() {
        CustomPage<StoneDto.PageDto> mockPage = mock(CustomPage.class);
        given(stoneRepository.findByAllOrderByAsc(null, Pageable.unpaged())).willReturn(mockPage);

        CustomPage<StoneDto.PageDto> result = stoneService.getStones(null, Pageable.unpaged());
        assertThat(result).isNotNull();
    }

    @Test
    void updateStone_success() {
        Stone stone = Stone.builder()
                .stoneName("토파즈")
                .stoneNote("노란 보석")
                .stoneWeight(new BigDecimal("1.0"))
                .stonePurchasePrice(12000)
                .gradePolicies(new java.util.ArrayList<>())
                .build();

        given(stoneRepository.findById(1L)).willReturn(Optional.of(stone));
        given(stoneRepository.existsByStoneName("아쿠아마린")).willReturn(false);

        stoneService.updateStone(1L, stoneDto);

        assertThat(stone.getStoneName()).isEqualTo("아쿠아마린");
        assertThat(stone.getStoneNote()).isEqualTo("파란색 보석");
        assertThat(stone.getGradePolicies()).hasSize(2);
    }

    @Test
    void updateStone_duplicateName_exception() {
        Stone stone = Stone.builder()
                .stoneName("토파즈")
                .stoneNote("노란 보석")
                .stoneWeight(new BigDecimal("1.0"))
                .stonePurchasePrice(12000)
                .gradePolicies(new java.util.ArrayList<>())
                .build();

        given(stoneRepository.findById(1L)).willReturn(Optional.of(stone));
        given(stoneRepository.existsByStoneName("아쿠아마린")).willReturn(true);

        assertThatThrownBy(() -> stoneService.updateStone(1L, stoneDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(IS_EXIST);
    }

    @Test
    void updateStone_notFound_exception() {
        given(stoneRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stoneService.updateStone(1L, stoneDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }

    @Test
    void deletedStone_success() {
        Stone stone = Stone.builder()
                .stoneName("토파즈")
                .stoneNote("노란 보석")
                .stoneWeight(new BigDecimal("1.0"))
                .stonePurchasePrice(12000)
                .gradePolicies(new java.util.ArrayList<>())
                .build();

        given(jwtUtil.getRole("admin-token")).willReturn("ADMIN");
        given(stoneRepository.findById(1L)).willReturn(Optional.of(stone));

        stoneService.deletedStone("admin-token", 1L);

        verify(stoneRepository).delete(stone);
    }

    @Test
    void deletedStone_noAdmin_exception() {
        given(jwtUtil.getRole("user-token")).willReturn("USER");

        assertThatThrownBy(() -> stoneService.deletedStone("user-token", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_ACCESS);
    }

    @Test
    void deletedStone_notFound_exception() {
        given(jwtUtil.getRole("admin-token")).willReturn("ADMIN");
        given(stoneRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> stoneService.deletedStone("admin-token", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(NOT_FOUND);
    }
}
