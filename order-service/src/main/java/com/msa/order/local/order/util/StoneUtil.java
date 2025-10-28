package com.msa.order.local.order.util;

import com.msa.order.global.dto.StoneDto;
import com.msa.order.local.order.entity.OrderStone;
import com.msa.order.local.order.entity.Orders;
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

    public static boolean isChanged(OrderStone os, StoneDto.StoneInfo s) {
        if (!Objects.equals(os.getOriginStoneId(), parseLongOrNull(s.getStoneId()))) return true;
        if (!Objects.equals(os.getOriginStoneName(), s.getStoneName())) return true;
        BigDecimal w = (s.getStoneWeight() == null || s.getStoneWeight().isBlank())
                ? null : new BigDecimal(s.getStoneWeight());
        if (!Objects.equals(os.getOriginStoneWeight(), w)) return true;
        if (!Objects.equals(os.getStonePurchaseCost(), s.getPurchaseCost())) return true;
        if (!Objects.equals(os.getStoneLaborCost(), s.getLaborCost())) return true;
        if (!Objects.equals(os.getStoneQuantity(), s.getQuantity())) return true;
        if (!Objects.equals(os.getMainStone(), s.isMainStone())) return true;
        return !Objects.equals(os.getIncludeStone(), s.isIncludeStone());
    }


    public static int[] countStoneCost(List<OrderStone> orderStoneList) {
        int totalStonePurchaseCost = 0;
        int totalStoneLaborCost = 0;
        int mainStoneCost = 0;
        int assistanceStoneCost = 0;
        for (OrderStone orderStone : orderStoneList) {
            Integer stoneLaborCost = orderStone.getStoneLaborCost();
            Integer stoneQuantity = orderStone.getStoneQuantity();
            Integer stonePurchaseCost = orderStone.getStonePurchaseCost();
            if (Boolean.TRUE.equals(orderStone.getIncludeStone())) {
                if (Boolean.TRUE.equals(orderStone.getMainStone())) {
                    mainStoneCost += stoneLaborCost * stoneQuantity;
                } else {
                    assistanceStoneCost += stoneLaborCost * stoneQuantity;
                }
                totalStonePurchaseCost += stonePurchaseCost * stoneQuantity;
                totalStoneLaborCost += stoneLaborCost * stoneQuantity;
            }
        }
        return new int[] {totalStonePurchaseCost, totalStoneLaborCost, mainStoneCost, assistanceStoneCost};
    }

    public static int[] updateStoneCosts(List<StoneDto.StoneInfo> stoneInfos) {
        int totalStonePurchaseCost = 0;
        int totalStoneLaborCost = 0;
        int mainStoneCost = 0;
        int assistanceStoneCost = 0;
        for (StoneDto.StoneInfo stoneInfo : stoneInfos) {
            Integer laborCost = stoneInfo.getLaborCost();
            Integer quantity = stoneInfo.getQuantity();
            Integer purchaseCost = stoneInfo.getPurchaseCost();
            if (Boolean.TRUE.equals(stoneInfo.isIncludeStone())) {
                if (Boolean.TRUE.equals(stoneInfo.isMainStone())) {
                    mainStoneCost += laborCost * quantity;
                } else {
                    assistanceStoneCost += laborCost * quantity;
                }
                totalStonePurchaseCost += purchaseCost * quantity;
                totalStoneLaborCost += laborCost * quantity;
            }
        }
        return new int[] {totalStonePurchaseCost, totalStoneLaborCost, mainStoneCost, assistanceStoneCost};
    }

    public static void updateStockStoneInfo(List<StoneDto.StoneInfo> stoneInfos, Stock stock) {
        List<OrderStone> originOrderStone = stock.getOrderStones();
        Map<Long, OrderStone> orderByOriginId = originOrderStone.stream()
                .filter(s -> s.getOrderStoneId() != null)
                .collect(Collectors.toMap(OrderStone::getOriginStoneId, Function.identity()));

        Set<Long> keepIds = new HashSet<>();
        for (StoneDto.StoneInfo stoneInfo : stoneInfos) {
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
                        .mainStone(stoneInfo.isMainStone())
                        .includeStone(stoneInfo.isIncludeStone())
                        .build();

                orderStone.setStock(stock);
                stock.addStockStone(orderStone);
            }
        }

        originOrderStone.removeIf(os ->
                os.getOriginStoneId() != null && !keepIds.contains(os.getOriginStoneId()));
    }

    public static void updateToStockStoneInfo(List<StoneDto.StoneInfo> stoneInfos, Stock stock) {
        Orders order = stock.getOrder();
        List<OrderStone> originOrderStone = order.getOrderStones();
        Map<Long, OrderStone> orderByOriginId = originOrderStone.stream()
                .filter(s -> s.getOrderStoneId() != null)
                .collect(Collectors.toMap(OrderStone::getOriginStoneId, Function.identity()));

        Set<Long> keepIds = new HashSet<>();
        for (StoneDto.StoneInfo stoneInfo : stoneInfos) {
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
                        .mainStone(stoneInfo.isMainStone())
                        .includeStone(stoneInfo.isIncludeStone())
                        .build();

                orderStone.setStock(stock);
                orderStone.setOrder(order);
                stock.addStockStone(orderStone);
            }
        }

        originOrderStone.removeIf(os ->
                os.getOriginStoneId() != null && !keepIds.contains(os.getOriginStoneId()));
    }

    public static void updateOrderStoneInfo(List<StoneDto.StoneInfo> stoneInfos, Orders order, List<OrderStone> originOrderStone) {
        Map<Long, OrderStone> orderByOriginId = originOrderStone.stream()
                .filter(s -> s.getOrderStoneId() != null)
                .collect(Collectors.toMap(OrderStone::getOriginStoneId, Function.identity()));

        Set<Long> keepIds = new HashSet<>();
        for (StoneDto.StoneInfo stoneInfo : stoneInfos) {
            Long originId = Long.valueOf(stoneInfo.getStoneId());
            keepIds.add(originId);

            OrderStone os = orderByOriginId.get(originId);
            if (os != null) {
                if (isChanged(os, stoneInfo)) {
                    os.updateFrom(stoneInfo);
                }
                os.setOrder(order);
                order.getOrderStones().add(os);
            } else {
                OrderStone orderStone = OrderStone.builder()
                        .originStoneId(Long.valueOf(stoneInfo.getStoneId()))
                        .originStoneName(stoneInfo.getStoneName())
                        .originStoneWeight(new BigDecimal(stoneInfo.getStoneWeight()))
                        .stonePurchaseCost(stoneInfo.getPurchaseCost())
                        .stoneLaborCost(stoneInfo.getLaborCost())
                        .stoneQuantity(stoneInfo.getQuantity())
                        .mainStone(stoneInfo.isMainStone())
                        .includeStone(stoneInfo.isIncludeStone())
                        .build();

                orderStone.setOrder(order);
                order.addOrderStone(orderStone);
            }
        }

        originOrderStone.removeIf(os ->
                os.getOriginStoneId() != null && !keepIds.contains(os.getOriginStoneId()));
    }

}
