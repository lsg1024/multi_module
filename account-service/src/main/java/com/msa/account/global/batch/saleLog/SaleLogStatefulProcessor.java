package com.msa.account.global.batch.saleLog;

import com.msa.account.local.transaction_history.domain.entity.SaleLog;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;

/**
 * 상태 유지형 SaleLog 잔액 재계산 ItemProcessor.
 *
 * *{@link SaleLogUpdateBatchConfig}의 재계산 Step에서 사용되며,
 * 청크 단위로 {@link SaleLog}를 순차 처리하면서 running balance를 누적한다.
 * 인스턴스가 Step 실행 동안 유지되므로 아래 두 필드에 상태가 축적된다:
 *
 *   - {@code currentRunningGoldBalance}: 직전까지 처리된 금 누적 잔액
 *   - {@code currentRunningMoneyBalance}: 직전까지 처리된 돈 누적 잔액
 * 
 *
 * *재계산 로직:
 *
 *   - 기존 {@code after - previous}로 이번 거래의 delta를 추출한다.
 *   - 현재 running balance를 새로운 {@code previousBalance}로 설정한다.
 *   - {@code previousBalance + delta}를 새로운 {@code afterBalance}로 설정한다.
 *   - running balance를 새 {@code afterBalance}로 갱신하고 다음 레코드로 진행한다.
 * 
 *
 * *주의: 이 클래스는 Step 내에서 싱글톤처럼 동작하므로 병렬 처리 시 상태 오염에 주의해야 한다.
 * {@link SaleLogUpdateBatchConfig}에서는 단일 파티션으로 실행된다.
 */
public class SaleLogStatefulProcessor implements ItemProcessor<SaleLog, SaleLog> {

    /** 직전 처리 레코드까지의 누적 금 잔액. 각 SaleLog의 previousGoldBalance 기준값으로 사용된다. */
    private BigDecimal currentRunningGoldBalance = BigDecimal.ZERO;
    /** 직전 처리 레코드까지의 누적 돈 잔액. 각 SaleLog의 previousMoneyBalance 기준값으로 사용된다. */
    private Long currentRunningMoneyBalance = 0L;

    @Override
    public @NotNull SaleLog process(SaleLog item) {
        BigDecimal goldDelta = item.getAfterGoldBalance().subtract(item.getPreviousGoldBalance());
        Long moneyDelta = item.getAfterMoneyBalance() - item.getPreviousMoneyBalance();

        //누적 잔액 갱신
        BigDecimal newPreviousGold = currentRunningGoldBalance;
        Long newPreviousMoney = currentRunningMoneyBalance;

        // After 계산
        BigDecimal newAfterGold = newPreviousGold.add(goldDelta);
        Long newAfterMoney = newPreviousMoney + moneyDelta;

        item.updateBalance(newPreviousGold, newPreviousMoney, newAfterGold, newAfterMoney);

        currentRunningGoldBalance = newAfterGold;
        currentRunningMoneyBalance = newAfterMoney;

        return item;
    }
}
