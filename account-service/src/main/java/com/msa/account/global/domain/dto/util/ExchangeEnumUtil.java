package com.msa.account.global.domain.dto.util;

import com.msa.account.global.domain.entity.OptionTradeType;

public class ExchangeEnumUtil {

    public static String getTradeTypeTitle(String tradeTypeKey) {
        return OptionTradeType.getTitleByKey(tradeTypeKey);
    }
    public static String getLevelTypeTitle(String levelKey) {
        return OptionTradeType.getTitleByKey(levelKey);
    }

}
