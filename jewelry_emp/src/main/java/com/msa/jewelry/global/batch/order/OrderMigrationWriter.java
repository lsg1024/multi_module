package com.msa.jewelry.global.batch.order;

import com.msa.jewelry.local.order.entity.OrderProduct;
import com.msa.jewelry.local.order.entity.OrderStone;
import com.msa.jewelry.local.order.entity.Orders;
import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.stock.entity.ProductSnapshot;
import com.msa.jewelry.local.stock.entity.Stock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Orders 저장 + 출고완료(STOCK) 주문은 같은 flow_code 의 Stock 행도 생성.
 * orderProduct/orderStones 는 Orders 의 cascade(PERSIST)로 함께 저장된다.
 */
@Slf4j
@Component
public class OrderMigrationWriter implements ItemWriter<Orders> {

    @PersistenceContext
    private EntityManager em;

    @Override
    public void write(Chunk<? extends Orders> chunk) {
        for (Orders order : chunk) {
            em.persist(order);              // @PrePersist 에서 flowCode 발급 + cascade 저장
            if (order.getOrderStatus() == OrderStatus.STOCK) {
                em.persist(buildStock(order));
            }
        }
    }

    /** 출고완료 주문 → 재고 스냅샷. 주문 화면에 중량이 없어 goldWeight/stoneWeight 는 대개 null. */
    private Stock buildStock(Orders order) {
        OrderProduct op = order.getOrderProduct();
        ProductSnapshot snapshot = (op == null) ? null : ProductSnapshot.builder()
                .id(op.getProductId())
                .productName(op.getProductName())
                .productFactoryName(op.getProductFactoryName())
                .size(op.getProductSize())
                .productLaborCost(op.getProductLaborCost())
                .productAddLaborCost(op.getProductAddLaborCost())
                .productPurchaseCost(op.getProductPurchaseCost())
                .materialId(op.getMaterialId())
                .materialName(op.getMaterialName())
                .colorId(op.getColorId())
                .colorName(op.getColorName())
                .goldWeight(op.getGoldWeight())
                .stoneWeight(op.getStoneWeight())
                .build();

        Integer stoneMainLabor = null, stoneSubLabor = null;
        if (order.getOrderStones() != null) {
            for (OrderStone s : order.getOrderStones()) {
                if (Boolean.TRUE.equals(s.getMainStone())) stoneMainLabor = s.getStoneLaborCost();
                else stoneSubLabor = s.getStoneLaborCost();
            }
        }

        Stock stock = Stock.builder()
                .storeId(order.getStoreId())
                .storeGrade(order.getStoreGrade())
                .storeHarry(order.getStoreHarry())
                .factoryId(order.getFactoryId())
                .factoryHarry(order.getFactoryHarry())
                .stockNote(order.getOrderNote())
                .stoneMainLaborCost(stoneMainLabor)
                .stoneAssistanceLaborCost(stoneSubLabor)
                .stockDeleted(false)
                .product(snapshot)
                .orderStatus(OrderStatus.STOCK)
                .build();
        stock.setOrder(order);              // flow_code 동기화 (order 의 flowCode 사용)
        return stock;
    }
}
