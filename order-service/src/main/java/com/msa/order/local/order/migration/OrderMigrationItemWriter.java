package com.msa.order.local.order.migration;

import com.msa.order.local.order.entity.Orders;
import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.Kind;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.SourceType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

/**
 * 마이그레이션 전용 커스텀 Writer.
 *
 * 1. Orders 엔티티를 persist → flush (flowCode 자동 생성)
 * 2. 생성된 flowCode로 StatusHistory 레코드를 생성·저장
 *
 * StatusHistory가 있어야 클라이언트 조회 쿼리(hasStatusHistory 서브쿼리)에서
 * 마이그레이션된 주문이 검색된다.
 */
@Slf4j
public class OrderMigrationItemWriter implements ItemWriter<Orders> {

    private final EntityManagerFactory entityManagerFactory;

    public OrderMigrationItemWriter(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void write(Chunk<? extends Orders> chunk) throws Exception {
        EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
        if (em == null) {
            throw new IllegalStateException("트랜잭션 EntityManager를 가져올 수 없습니다");
        }

        // 1단계: Orders persist → flush로 flowCode 생성
        for (Orders order : chunk) {
            em.persist(order);
        }
        em.flush();

        // 2단계: 생성된 flowCode로 StatusHistory 레코드 생성
        for (Orders order : chunk) {
            SourceType sourceType = determineSourceType(order);
            BusinessPhase phase = determinePhase(order);

            StatusHistory history = StatusHistory.create(
                    order.getFlowCode(),
                    sourceType,
                    phase,
                    Kind.CREATE,
                    "LEGACY_MIGRATION",
                    "[레거시 마이그레이션] " + (order.getOrderNote() != null ? order.getOrderNote() : "")
            );
            em.persist(history);
        }

        log.debug("마이그레이션 Writer: {}건 Orders + StatusHistory 저장", chunk.size());
    }

    /**
     * orderNote에 포함된 구분 태그로 SourceType 결정.
     * [주문] → ORDER, [수리] → FIX, 기타 → ORDER
     */
    private SourceType determineSourceType(Orders order) {
        String note = order.getOrderNote();
        if (note != null) {
            if (note.contains("[수리]")) return SourceType.FIX;
            if (note.contains("[수리관리]")) return SourceType.FIX;
        }
        return SourceType.ORDER;
    }

    /**
     * OrderStatus → BusinessPhase 매핑.
     */
    private BusinessPhase determinePhase(Orders order) {
        OrderStatus status = order.getOrderStatus();
        if (status == null) return BusinessPhase.ORDER;
        return switch (status) {
            case ORDER -> BusinessPhase.ORDER;
            case STOCK -> BusinessPhase.STOCK;
            case FIX -> BusinessPhase.FIX;
            case DELETED -> BusinessPhase.DELETED;
            case NORMAL -> BusinessPhase.NORMAL;
            case RENTAL -> BusinessPhase.RENTAL;
            case SALE -> BusinessPhase.SALE;
            case RETURN -> BusinessPhase.RETURN;
            default -> BusinessPhase.ORDER;
        };
    }
}
