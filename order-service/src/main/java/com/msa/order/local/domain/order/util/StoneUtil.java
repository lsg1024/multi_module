package com.msa.order.local.domain.order.util;

import com.msa.order.local.domain.order.entity.OrderStone;
import com.msa.order.local.domain.stock.dto.StockDto;
import com.msa.order.local.domain.stock.entity.domain.Stock;

import java.math.BigDecimal;
import java.util.List;
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
        if (!Objects.equals(os.getIsMainStone(), s.getIsMainStone())) return true;
        return !Objects.equals(os.getIsIncludeStone(), s.getIsIncludeStone());
    }

    public static void updateStoneCostAndPurchase(Stock stock) {
        int totalStonePurchaseCost = 0;
        int mainStoneCost = 0;
        int assistanceStoneCost = 0;

        for (OrderStone os : stock.getOrderStones()) {
            int qty = nvl(os.getStoneQuantity());
            int labor = nvl(os.getStoneLaborCost());
            int purchase = nvl(os.getStonePurchaseCost());
            if (Boolean.TRUE.equals(os.getIsIncludeStone())) {
                if (Boolean.TRUE.equals(os.getIsMainStone())) {
                    mainStoneCost += labor * qty;
                } else {
                    assistanceStoneCost += labor * qty;
                }
                totalStonePurchaseCost += purchase * qty;
            }
        }
        stock.updateStoneCost(totalStonePurchaseCost, mainStoneCost, assistanceStoneCost);
    }

    public static void countStoneQuantity(List<OrderStone> orderStoneList, int mainStoneQuantity, int assistanceStoneQuantity) {
        for (OrderStone orderStone : orderStoneList) {
            if (Boolean.TRUE.equals(orderStone.getIsIncludeStone())) {
                if (Boolean.TRUE.equals(orderStone.getIsMainStone())) {
                    mainStoneQuantity += orderStone.getStoneQuantity();
                } else {
                    assistanceStoneQuantity += orderStone.getStoneQuantity();
                }
            }
        }
    }

    public static void countStoneLabor(List<OrderStone> orderStoneList, int mainStoneLabor, int assistanceStoneLabor) {
        for (OrderStone orderStone : orderStoneList) {
            if (Boolean.TRUE.equals(orderStone.getIsIncludeStone())) {
                if (Boolean.TRUE.equals(orderStone.getIsMainStone())) {
                    mainStoneLabor += orderStone.getStoneLaborCost() * orderStone.getStoneQuantity();
                } else {
                    assistanceStoneLabor += orderStone.getStoneLaborCost() * orderStone.getStoneQuantity();
                }
            }
        }
    }

}
