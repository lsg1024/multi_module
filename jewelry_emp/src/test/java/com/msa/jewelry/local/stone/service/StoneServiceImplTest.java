package com.msa.jewelry.local.stone.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.jewelry.local.stone.dto.StoneDto;
import com.msa.jewelry.local.stone.dto.StoneWorkGradePolicyDto;
import com.msa.jewelry.local.stone.entity.Stone;
import com.msa.jewelry.local.stone.entity.StoneWorkGradePolicy;
import com.msa.jewelry.local.stone.repository.StoneRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("StoneServiceImpl 단위 테스트")
class StoneServiceImplTest {

    private static final String TOKEN = "Bearer test";
    private static final Long   STONE_ID = 301L;

    @Mock JwtUtil jwtUtil;
    @Mock StoneRepository stoneRepository;

    @InjectMocks
    StoneServiceImpl service;

    @Nested
    @DisplayName("saveStone")
    class SaveStone {

        @Test
        @DisplayName("중복 → IllegalArgumentException")
        void 중복() {
            StoneDto dto = mock(StoneDto.class);
            given(dto.getStoneName()).willReturn("다이아 라운드 0.3");
            given(stoneRepository.existsByStoneName("다이아 라운드 0.3")).willReturn(true);

            assertThatThrownBy(() -> service.saveStone(dto))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(stoneRepository, never()).save(any());
        }

        @Test
        @DisplayName("정상 — 빈 grade policy 리스트로 save 호출")
        void 정상_빈정책() {
            StoneDto dto = mock(StoneDto.class);
            given(dto.getStoneName()).willReturn("다이아 라운드 0.3");
            given(dto.getStoneWeight()).willReturn("0.30");
            given(dto.getStoneNote()).willReturn("VS1");
            given(dto.getStonePurchasePrice()).willReturn(150_000);
            given(dto.getStoneWorkGradePolicyDto()).willReturn(Collections.emptyList());
            given(stoneRepository.existsByStoneName("다이아 라운드 0.3")).willReturn(false);

            service.saveStone(dto);

            verify(stoneRepository).save(any(Stone.class));
        }

        @Test
        @DisplayName("정상 — weight 빈문자열은 ZERO 로 대체")
        void 빈_무게_ZERO() {
            StoneDto dto = mock(StoneDto.class);
            given(dto.getStoneName()).willReturn("ruby");
            given(dto.getStoneWeight()).willReturn("");
            given(dto.getStoneNote()).willReturn(null);
            given(dto.getStonePurchasePrice()).willReturn(null);
            given(dto.getStoneWorkGradePolicyDto()).willReturn(Collections.emptyList());
            given(stoneRepository.existsByStoneName("ruby")).willReturn(false);

            service.saveStone(dto);

            verify(stoneRepository).save(any(Stone.class));
        }
    }

    @Nested
    @DisplayName("getStone")
    class GetStone {

        @Test
        @DisplayName("정상 — ResponseSingle 변환")
        void 정상() {
            Stone stone = mock(Stone.class);
            given(stone.getStoneName()).willReturn("다이아");
            given(stone.getStoneNote()).willReturn("VS1");
            given(stone.getStoneWeight()).willReturn(new BigDecimal("0.30"));
            given(stone.getStonePurchasePrice()).willReturn(150_000);
            given(stone.getGradePolicies()).willReturn(new ArrayList<StoneWorkGradePolicy>());
            given(stoneRepository.findFetchJoinById(STONE_ID)).willReturn(Optional.of(stone));

            StoneDto.ResponseSingle resp = service.getStone(STONE_ID);

            assertThat(resp.getStoneId()).isEqualTo("301");
            assertThat(resp.getStoneName()).isEqualTo("다이아");
            assertThat(resp.getStonePurchasePrice()).isEqualTo(150_000);
            assertThat(resp.getStoneWorkGradePolicyDto()).isEmpty();
        }

        @Test
        @DisplayName("없음 → IllegalArgumentException")
        void 없음() {
            given(stoneRepository.findFetchJoinById(STONE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getStone(STONE_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("getStones")
    class GetStones {

        @Test
        @DisplayName("위임")
        void 위임() {
            Pageable pageable = PageRequest.of(0, 20);
            @SuppressWarnings("unchecked")
            CustomPage<StoneDto.PageDto> empty = mock(CustomPage.class);
            given(stoneRepository.findAllStones("a", "b", "c", "d", "e", "f", pageable)).willReturn(empty);

            assertThat(service.getStones("a", "b", "c", "d", "e", "f", pageable)).isSameAs(empty);
        }
    }

    @Nested
    @DisplayName("updateStone")
    class UpdateStone {

        @Test
        @DisplayName("없음 → 예외")
        void 없음() {
            given(stoneRepository.findById(STONE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateStone(STONE_ID, mock(StoneDto.class)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("이름 변경 + 중복 → 예외")
        void 이름변경_중복() {
            Stone stone = mock(Stone.class);
            given(stone.getStoneName()).willReturn("oldName");
            given(stoneRepository.findById(STONE_ID)).willReturn(Optional.of(stone));

            StoneDto dto = mock(StoneDto.class);
            given(dto.getStoneName()).willReturn("newName");
            given(stoneRepository.existsByStoneName("newName")).willReturn(true);

            assertThatThrownBy(() -> service.updateStone(STONE_ID, dto))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("이름 동일 → entity 업데이트")
        void 이름동일_업데이트() {
            Stone stone = mock(Stone.class);
            given(stone.getStoneName()).willReturn("name");
            List<StoneWorkGradePolicy> policies = new ArrayList<>();
            given(stone.getGradePolicies()).willReturn(policies);
            given(stoneRepository.findById(STONE_ID)).willReturn(Optional.of(stone));

            StoneDto dto = mock(StoneDto.class);
            given(dto.getStoneName()).willReturn("name");
            given(dto.getStoneWorkGradePolicyDto()).willReturn(Collections.<StoneWorkGradePolicyDto>emptyList());
            // existsByStoneName 어떻든 무관 — 동일 분기

            service.updateStone(STONE_ID, dto);

            verify(stone).updateStone(dto);
        }
    }

    @Nested
    @DisplayName("deletedStone")
    class Delete {

        @Test
        @DisplayName("ADMIN 아님 → NOT_ACCESS")
        void 권한없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("USER");

            assertThatThrownBy(() -> service.deletedStone(TOKEN, STONE_ID))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(stoneRepository, never()).delete(any());
        }

        @Test
        @DisplayName("대상 없음 → NOT_FOUND")
        void 없음() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            given(stoneRepository.findById(STONE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.deletedStone(TOKEN, STONE_ID))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("정상 — delete 호출")
        void 정상() {
            given(jwtUtil.getRole(TOKEN)).willReturn("ADMIN");
            Stone stone = mock(Stone.class);
            given(stoneRepository.findById(STONE_ID)).willReturn(Optional.of(stone));

            service.deletedStone(TOKEN, STONE_ID);

            verify(stoneRepository).delete(stone);
        }
    }

    @Nested
    @DisplayName("getExistStoneId / existsStoneId")
    class Exists {

        @Test
        @DisplayName("getExistStoneId true 위임")
        void existById_true() {
            given(stoneRepository.existsByStoneId(STONE_ID)).willReturn(true);
            assertThat(service.getExistStoneId(STONE_ID)).isTrue();
        }

        @Test
        @DisplayName("getExistStoneId false 위임")
        void existById_false() {
            given(stoneRepository.existsByStoneId(STONE_ID)).willReturn(false);
            assertThat(service.getExistStoneId(STONE_ID)).isFalse();
        }

        @Test
        @DisplayName("existsStoneId 위임 — 원시 boolean")
        void existsStoneId() {
            given(stoneRepository.existsByStoneId(STONE_ID)).willReturn(true);
            assertThat(service.existsStoneId(STONE_ID)).isTrue();
        }
    }

    @Nested
    @DisplayName("getExistStoneName")
    class ExistName {

        @Test
        @DisplayName("type/shape/size 조합 → '/' 합성으로 existsByStoneName")
        void 합성된_이름() {
            given(stoneRepository.existsByStoneName("다이아/라운드/0.3")).willReturn(true);
            assertThat(service.getExistStoneName("다이아", "라운드", "0.3")).isTrue();
            verify(stoneRepository).existsByStoneName("다이아/라운드/0.3");
        }
    }

    @Nested
    @DisplayName("getStonesExcel")
    class Excel {

        @Test
        @DisplayName("빈 리스트 — 헤더만 있는 워크북 byte 반환")
        void 빈리스트() throws IOException {
            given(stoneRepository.findStonesForExcel(eq("A"), eq("B"), eq("C")))
                    .willReturn(Collections.emptyList());

            byte[] bytes = service.getStonesExcel("A", "B", "C");

            assertThat(bytes).isNotEmpty();
        }
    }
}
