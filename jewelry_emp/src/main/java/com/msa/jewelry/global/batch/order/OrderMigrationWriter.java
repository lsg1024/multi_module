package com.msa.jewelry.global.batch.order;

import com.msa.jewelry.local.order.entity.OrderProduct;
import com.msa.jewelry.local.order.entity.OrderStone;
import com.msa.jewelry.local.order.entity.Orders;
import com.msa.jewelry.local.order.entity.order_enum.BusinessPhase;
import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.order.entity.order_enum.SourceType;
import com.msa.jewelry.local.order.util.StatusHistoryHelper;
import com.msa.jewelry.local.stock.entity.ProductSnapshot;
import com.msa.jewelry.local.stock.entity.Stock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * Orders 저장 + 출고완료(STOCK) 주문은 같은 flow_code 의 Stock 행도 생성.
 * orderProduct/orderStones 는 Orders 의 cascade(PERSIST)로 함께 저장된다.
 * status_history 도 함께 생성 — 재고/주문 조회가 최신 이력(sourceType/phase)에 의존하므로
 * 이력이 없으면 조회 시 NPE 가 난다. (이관 흐름: SourceType.ORDER, phase=현재 상태)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMigrationWriter implements ItemWriter<Orders> {

    private static final String MIGRATION_ACTOR = "MIGRATION";

    @PersistenceContext
    private EntityManager em;

    private final StatusHistoryHelper statusHistoryHelper;

    @Override
    public void write(Chunk<? extends Orders> chunk) {
        for (Orders order : chunk) {
            em.persist(order);              // @PrePersist 에서 flowCode 발급 + cascade 저장

            // 최신 이력 1건 생성 (없으면 stock/order 조회의 SourceType.valueOf 가 NPE)
            BusinessPhase phase = (order.getOrderStatus() == OrderStatus.STOCK)
                    ? BusinessPhase.STOCK : BusinessPhase.WAITING;
            statusHistoryHelper.saveCreate(order.getFlowCode(), SourceType.ORDER, phase,
                    "주문 이관", MIGRATION_ACTOR);

            Stock stock = null;
            if (order.getOrderStatus() == OrderStatus.STOCK) {
                stock = buildStock(order);
                em.persist(stock);
            }

            // 원본 등록/수정 일시를 audit 컬럼으로 강제 지정.
            // JPA Auditing 이 persist 시 현재시각을 넣으므로, flush 후 native update 로 덮어쓴다.
            LocalDateTime created = order.getMigrationCreateDate();
            if (created != null) {
                LocalDateTime modified = order.getMigrationModifiedDate() != null
                        ? order.getMigrationModifiedDate() : created;
                em.flush();   // INSERT 실행 → PK 발급 + audit(now) 세팅
                overrideAudit("orders", "order_id", order.getOrderId(), created, modified);
                if (stock != null) {
                    overrideAudit("stock", "stock_id", stock.getStockId(), created, modified);
                }
            }
        }
    }

    /** audit(create_date/last_modified_date) 를 원본 시각으로 덮어쓴다. 테넌트 search_path 기준 테이블. */
    private void overrideAudit(String table, String idColumn, Long id,
                              LocalDateTime created, LocalDateTime modified) {
        em.createNativeQuery(
                        "UPDATE " + table + " SET create_date = ?1, last_modified_date = ?2 WHERE " + idColumn + " = ?3")
                .setParameter(1, created)
                .setParameter(2, modified)
                .setParameter(3, id)
                .executeUpdate();
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
