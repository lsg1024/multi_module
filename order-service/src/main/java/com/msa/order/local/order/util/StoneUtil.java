package com.msa.order.local.order.util;

import com.msa.order.local.order.entity.OrderStone;
import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.stock.dto.StockDto;
import com.msa.order.local.stock.entity.Stock;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
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

    public static void countStoneCost(List<OrderStone> orderStoneList, int msq, int asq, int tsq) {
        for (OrderStone orderStone : orderStoneList) {
            Integer stoneLaborCost = orderStone.getStoneLaborCost();
            Integer stoneQuantity = orderStone.getStoneQuantity();
            Integer stonePurchaseCost = orderStone.getStonePurchaseCost();
            if (Boolean.TRUE.equals(orderStone.getIsIncludeStone())) {
                if (Boolean.TRUE.equals(orderStone.getIsMainStone())) {
                    msq += stoneLaborCost * stoneQuantity;
                } else {
                    asq += stoneLaborCost * stoneQuantity;
                }
                tsq += stonePurchaseCost * stoneQuantity;
            }
        }
    }

    public static void updateStoneInfo(List<StockDto.StoneInfo> stoneInfos, Stock stock, List<OrderStone> originOrderStone) {
        Map<Long, OrderStone> orderByOriginId = originOrderStone.stream()
                .filter(s -> s.getOrderStoneId() != null)
                .collect(Collectors.toMap(OrderStone::getOriginStoneId, Function.identity()));

        Orders order = stock.getOrder();

        Set<Long> keepIds = new HashSet<>();
        for (StockDto.StoneInfo stoneInfo : stoneInfos) {
            Long originId = Long.valueOf(stoneInfo.getStoneId());
            keepIds.add(originId);

            OrderStone os = orderByOriginId.get(originId);
            if (os != null) {
                if (isChanged(os, stoneInfo)) {
                    os.updateFrom(stoneInfo);
                }
                os.setStock(stock);
                stock.getOrderStones().add(os);
            } else {
                OrderStone orderStone = OrderStone.builder()
                        .originStoneId(Long.valueOf(stoneInfo.getStoneId()))
                        .originStoneName(stoneInfo.getStoneName())
                        .originStoneWeight(new BigDecimal(stoneInfo.getStoneWeight()))
                        .stonePurchaseCost(stoneInfo.getPurchaseCost())
                        .stoneLaborCost(stoneInfo.getLaborCost())
                        .stoneQuantity(stoneInfo.getQuantity())
                        .isMainStone(stoneInfo.getIsMainStone())
                        .isIncludeStone(stoneInfo.getIsIncludeStone())
                        .build();

                orderStone.setStock(stock);
                orderStone.setOrder(order);
                stock.addStockStone(orderStone);
            }
        }

        originOrderStone.removeIf(os ->
                os.getOriginStoneId() != null && !keepIds.contains(os.getOriginStoneId()));
    }

}
