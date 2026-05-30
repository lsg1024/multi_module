package com.msa.jewelry.global.batch.saleLog;

import com.msa.jewelry.local.transaction_history.entity.SaleLog;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;

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
