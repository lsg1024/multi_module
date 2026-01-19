package com.msa.account.local.saleLog;

import com.msa.account.global.batch.saleLog.SaleLogStatefulProcessor;
import com.msa.account.local.transaction_history.domain.entity.SaleLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SaleLogStatefulProcessorTest {

    @Test
    @DisplayName("순차적인 거래 내역의 변동량을 계산하여 누적 잔액을 올바르게 맞춘다")
    void rebalanceLogicTest() {
        // given
        SaleLogStatefulProcessor processor = new SaleLogStatefulProcessor();

        // 시나리오: 3개의 거래 내역이 존재함
        // Item 1: 정상 (0 -> 100)
        // Item 2: 데이터 꼬임 (Previous가 100이어야 하는데 50으로 잘못 저장되어 있음, 변동량은 +50)
        // Item 3: 데이터 꼬임 (Previous가 100이어야 하는데 200으로 되어 있음, 변동량은 -30)

        // Mock 객체 대신 실제 엔티티와 유사한 동작을 하도록 Stubbing
        SaleLog item1 = createMockSaleLog(
                BigDecimal.ZERO, 0L,           // Prev (Gold, Money)
                BigDecimal.valueOf(100), 1000L // After (Gold, Money) -> Delta: +100, +1000
        );

        SaleLog item2 = createMockSaleLog(
                BigDecimal.valueOf(50), 500L,   // Wrong Prev (Should be 100, 1000)
                BigDecimal.valueOf(100), 1500L  // Wrong After -> Delta: +50, +1000
        );

        SaleLog item3 = createMockSaleLog(
                BigDecimal.valueOf(200), 3000L, // Wrong Prev (Should be 150, 2000)
                BigDecimal.valueOf(170), 2500L  // Wrong After -> Delta: -30, -500
        );

        // when & then

        // 1. 첫 번째 아이템 처리 (초기값 0에서 시작)
        SaleLog result1 = processor.process(item1);
        assertThat(result1.getPreviousGoldBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result1.getAfterGoldBalance()).isEqualByComparingTo(BigDecimal.valueOf(100)); // 0 + 100
        assertThat(result1.getAfterMoneyBalance()).isEqualTo(1000L); // 0 + 1000

        // 2. 두 번째 아이템 처리 (체인이 연결되어야 함)
        // 입력값은 50 -> 100 이었지만, 앞선 잔액이 100이므로 100 + 50(델타) = 150이 되어야 함
        SaleLog result2 = processor.process(item2);

        // 검증: Previous가 Item1의 After와 같아졌는가?
        assertThat(result2.getPreviousGoldBalance()).isEqualByComparingTo(BigDecimal.valueOf(100));
        assertThat(result2.getPreviousMoneyBalance()).isEqualTo(1000L);

        // 검증: After가 올바르게 재계산 되었는가? (100 + 50 = 150)
        assertThat(result2.getAfterGoldBalance()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(result2.getAfterMoneyBalance()).isEqualTo(2000L); // 1000 + 1000

        // 3. 세 번째 아이템 처리
        // 입력값은 200 -> 170 (-30) 이었지만, 앞선 잔액이 150이므로 150 - 30 = 120이 되어야 함
        SaleLog result3 = processor.process(item3);

        assertThat(result3.getPreviousGoldBalance()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(result3.getAfterGoldBalance()).isEqualByComparingTo(BigDecimal.valueOf(120)); // 150 - 30
        assertThat(result3.getAfterMoneyBalance()).isEqualTo(1500L); // 2000 - 500
    }

    // 테스트를 돕기 위한 헬퍼 메서드 (SaleLog는 @Builder나 Setter가 제한적일 수 있어 Mock 활용)
    private SaleLog createMockSaleLog(BigDecimal prevGold, Long prevMoney, BigDecimal afterGold, Long afterMoney) {
        // 실제 엔티티에 비즈니스 로직(updateBalance)이 포함되어 있으므로 Spy 사용 권장
        // 여기서는 간단하게 로직 검증을 위해 엔티티 내부 메서드 호출 시 동작을 시뮬레이션 합니다.
        // 실제로는 new SaleLog(...) 로 생성하는 것이 가장 좋지만, Builder 패턴 등으로 인해 복잡하다면 아래와 같이 Mocking 가능합니다.

        // 하지만 여기서는 updateBalance 메서드가 실제 필드값을 바꿔야 하므로
        // Mockito보다는 실제 객체를 생성하거나 익명 클래스를 사용하는 것이 낫습니다.
        // 아래 코드는 'updateBalance'가 호출되었을 때 실제 필드값을 바꾸는 가짜 객체입니다.

        return new SaleLog() {
            private BigDecimal pg = prevGold;
            private Long pm = prevMoney;
            private BigDecimal ag = afterGold;
            private Long am = afterMoney;

            @Override
            public BigDecimal getPreviousGoldBalance() { return pg; }
            @Override
            public Long getPreviousMoneyBalance() { return pm; }
            @Override
            public BigDecimal getAfterGoldBalance() { return ag; }
            @Override
            public Long getAfterMoneyBalance() { return am; }

            @Override
            public void updateBalance(BigDecimal newPrevGold, Long newPrevMoney, BigDecimal newAfterGold, Long newAfterMoney) {
                this.pg = newPrevGold;
                this.pm = newPrevMoney;
                this.ag = newAfterGold;
                this.am = newAfterMoney;
            }
        };
    }
}