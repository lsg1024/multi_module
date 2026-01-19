package com.msa.account.global.batch.saleLog;

import com.msa.account.local.transaction_history.domain.entity.SaleLog;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.item.ItemProcessor;

import java.math.BigDecimal;

public class SaleLogStatefulProcessor implements ItemProcessor<SaleLog, SaleLog> {

    private BigDecimal currentRunningGoldBalance = BigDecimal.ZERO;
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
