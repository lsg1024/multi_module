package com.msa.order.local.domain.order.util;

import com.msa.order.local.domain.order.entity.OrderStone;
import com.msa.order.local.domain.stock.dto.StockDto;

import java.math.BigDecimal;
import java.util.Objects;

public class StoneUtil {

    public static Long parseLongOrNull(String s) {
        return (s == null || s.isBlank()) ? null : Long.valueOf(s);
    }

    public static int nvl(Integer v) { return v == null ? 0 : v; }

    public static boolean isChanged(OrderStone os, StockDto.StoneInfo s) {
        if (!Objects.equals(os.getOriginStoneId(), parseLongOrNull(s.getStoneId()))) return true;
        if (!Objects.equals(os.getOriginStoneName(), s.getStoneName())) return true;
        BigDecimal w = (s.getStoneWeight() == null || s.getStoneWeight().isBlank())
                ? null : new BigDecimal(s.getStoneWeight());
        if (!Objects.equals(os.getOriginStoneWeight(), w)) return true;
        if (!Objects.equals(os.getStonePurchaseCost(), s.getPurchaseCost())) return true;
        if (!Objects.equals(os.getStoneLaborCost(), s.getLaborCost())) return true;
        if (!Objects.equals(os.getStoneQuantity(), s.getQuantity())) return true;
        if (!Objects.equals(os.getProductStoneMain(), s.isProductStoneMain())) return true;
        if (!Objects.equals(os.getIncludeQuantity(), s.isIncludeQuantity())) return true;
        if (!Objects.equals(os.getIncludeWeight(), s.isIncludeWeight())) return true;
        return !Objects.equals(os.getIncludeLabor(), s.isIncludeLabor());
    }
}
