package com.msa.jewelry.local.gold_price.service;

import com.msa.jewelry.global.exception.ExceptionMessage;
import com.msa.jewelry.local.gold_price.dto.GoldDto;
import com.msa.jewelry.local.gold_price.entity.Gold;
import com.msa.jewelry.local.gold_price.repository.GoldRepository;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * GoldService 단위 테스트.
 *
 * <p>금시세 등록/조회 — Repository 만 mock 하면 충분.
 *
 * <p>커버리지:
 * <ul>
 *   <li>getGoldPrice — 최신 시세 / NOT_FOUND</li>
 *   <li>getGoldPrices — 빈 결과 / 다건 매핑 (createDate.toString 포함)</li>
 *   <li>createGoldPrice — 정상 save / null 입력</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("GoldService 단위 테스트")
class GoldServiceTest {

    @Mock GoldRepository goldRepository;

    @InjectMocks
    GoldService goldService;

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getGoldPrice")
    class GetGoldPrice {

        @Test
        @DisplayName("정상 — 최신 시세 반환")
        void 정상조회() {
            Gold gold = mock(Gold.class);
            given(gold.getGoldPrice()).willReturn(350_000);
            given(goldRepository.findTopByOrderByGoldIdDesc()).willReturn(Optional.of(gold));

            Integer price = goldService.getGoldPrice();

            assertThat(price).isEqualTo(350_000);
        }

        @Test
        @DisplayName("시세 미존재 → IllegalArgumentException(NOT_FOUND)")
        void 시세없음() {
            given(goldRepository.findTopByOrderByGoldIdDesc()).willReturn(Optional.empty());

            assertThatThrownBy(() -> goldService.getGoldPrice())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage(ExceptionMessage.NOT_FOUND);
        }

        @Test
        @DisplayName("0원 시세도 그대로 반환 — 가드 없는 단순 위임")
        void 영원_시세도_반환() {
            Gold gold = mock(Gold.class);
            given(gold.getGoldPrice()).willReturn(0);
            given(goldRepository.findTopByOrderByGoldIdDesc()).willReturn(Optional.of(gold));

            assertThat(goldService.getGoldPrice()).isZero();
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getGoldPrices")
    class GetGoldPrices {

        @Test
        @DisplayName("빈 결과 → 빈 리스트")
        void 빈결과() {
            given(goldRepository.findAllByOrderByCreateDateDesc()).willReturn(Collections.emptyList());

            List<GoldDto> result = goldService.getGoldPrices();

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("다건 — GoldDto 로 매핑, createDate.toString 포함")
        void 다건_매핑() {
            LocalDateTime t1 = LocalDateTime.of(2026, 5, 27, 9, 0);
            LocalDateTime t2 = LocalDateTime.of(2026, 5, 26, 9, 0);

            Gold g1 = mock(Gold.class);
            given(g1.getGoldPrice()).willReturn(350_000);
            given(g1.getCreateDate()).willReturn(t1);

            Gold g2 = mock(Gold.class);
            given(g2.getGoldPrice()).willReturn(348_000);
            given(g2.getCreateDate()).willReturn(t2);

            given(goldRepository.findAllByOrderByCreateDateDesc()).willReturn(List.of(g1, g2));

            List<GoldDto> result = goldService.getGoldPrices();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getGoldPrice()).isEqualTo(350_000);
            assertThat(result.get(0).getCreateDate()).isEqualTo(t1.toString());
            assertThat(result.get(1).getGoldPrice()).isEqualTo(348_000);
        }

        @Test
        @DisplayName("createDate null 인 엔티티 포함 시 NullPointerException — 알려진 잠재 버그")
        void createDate_null이면_NPE() {
            Gold g1 = mock(Gold.class);
            given(g1.getGoldPrice()).willReturn(100_000);
            given(g1.getCreateDate()).willReturn(null);
            given(goldRepository.findAllByOrderByCreateDateDesc()).willReturn(List.of(g1));

            // 현 구현이 createDate.toString() 을 무방비로 호출하므로 NPE.
            assertThatThrownBy(() -> goldService.getGoldPrices())
                    .isInstanceOf(NullPointerException.class);
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createGoldPrice")
    class CreateGoldPrice {

        @Test
        @DisplayName("정상 — Gold 엔티티 생성 후 save 호출")
        void 정상저장() {
            goldService.createGoldPrice(360_000);

            ArgumentCaptor<Gold> captor = ArgumentCaptor.forClass(Gold.class);
            verify(goldRepository).save(captor.capture());
            assertThat(captor.getValue().getGoldPrice()).isEqualTo(360_000);
        }

        @Test
        @DisplayName("null 가격도 통과 — 현 구현은 가드 없음")
        void null_가격도_통과() {
            goldService.createGoldPrice(null);

            ArgumentCaptor<Gold> captor = ArgumentCaptor.forClass(Gold.class);
            verify(goldRepository).save(captor.capture());
            assertThat(captor.getValue().getGoldPrice()).isNull();
        }

        @Test
        @DisplayName("음수 가격도 그대로 저장 — 검증은 상위 계층 책임")
        void 음수_가격도_저장() {
            goldService.createGoldPrice(-1);

            ArgumentCaptor<Gold> captor = ArgumentCaptor.forClass(Gold.class);
            verify(goldRepository).save(captor.capture());
            assertThat(captor.getValue().getGoldPrice()).isEqualTo(-1);
        }
    }
}
