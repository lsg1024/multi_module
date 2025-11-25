package com.msa.account.global.domain.dto.util;

import com.msa.account.global.domain.entity.OptionLevel;
import com.msa.account.global.domain.entity.OptionTradeType;

public class ExchangeEnumUtil {

    public static String getTradeTypeTitle(String tradeTypeKey) {
        return OptionTradeType.getTitleByKey(tradeTypeKey);
    }
    public static String getLevelTypeTitle(String levelKey) {
        return OptionLevel.getLevelByKey(levelKey);
    }

    public static String getFormattedDate(String dateString) {
        if (dateString != null && dateString.length() >= 10) {
            return dateString.substring(0, 10);
        }
        return dateString;
    }

}
