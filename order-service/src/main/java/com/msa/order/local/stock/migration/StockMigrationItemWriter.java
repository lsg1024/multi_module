package com.msa.order.local.stock.migration;

import com.msa.order.local.order.entity.StatusHistory;
import com.msa.order.local.order.entity.order_enum.BusinessPhase;
import com.msa.order.local.order.entity.order_enum.Kind;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.order.entity.order_enum.SourceType;
import com.msa.order.local.stock.entity.Stock;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.orm.jpa.EntityManagerFactoryUtils;

/**
 * 재고 마이그레이션 전용 커스텀 Writer.
 *
 * 1. Stock 엔티티를 persist → flush (flowCode/stockCode 자동 생성)
 * 2. CSV 등록일/변경일이 있으면 native UPDATE로 JPA Auditing 날짜 덮어쓰기
 * 3. 생성된 flowCode로 StatusHistory 레코드를 생성·저장
 */
@Slf4j
public class StockMigrationItemWriter implements ItemWriter<Stock> {

    private final EntityManagerFactory entityManagerFactory;
    private final StockMigrationFailureCollector failureCollector;

    public StockMigrationItemWriter(EntityManagerFactory entityManagerFactory,
                                    StockMigrationFailureCollector failureCollector) {
        this.entityManagerFactory = entityManagerFactory;
        this.failureCollector = failureCollector;
    }

    @Override
    public void write(Chunk<? extends Stock> chunk) throws Exception {
        EntityManager em = EntityManagerFactoryUtils.getTransactionalEntityManager(entityManagerFactory);
        if (em == null) {
            throw new IllegalStateException("트랜잭션 EntityManager를 가져올 수 없습니다");
        }

        int successCount = 0;

        for (Stock stock : chunk) {
            try {
                // 1단계: Stock persist + flush (per-item)
                em.persist(stock);
                em.flush();

                // 2단계: CSV 등록일/변경일로 JPA Auditing 날짜 덮어쓰기
                if (stock.getMigrationCreatedDate() != null || stock.getMigrationModifiedDate() != null) {
                    StringBuilder sql = new StringBuilder("UPDATE STOCK SET ");
                    boolean needComma = false;

                    if (stock.getMigrationCreatedDate() != null) {
                        sql.append("create_date = :createDate");
                        needComma = true;
                    }
                    if (stock.getMigrationModifiedDate() != null) {
                        if (needComma) sql.append(", ");
                        sql.append("last_modified_date = :modifiedDate");
                    }
                    sql.append(" WHERE STOCK_ID = :stockId");

                    var query = em.createNativeQuery(sql.toString());
                    query.setParameter("stockId", stock.getStockId());

                    if (stock.getMigrationCreatedDate() != null) {
                        query.setParameter("createDate", stock.getMigrationCreatedDate());
                    }
                    if (stock.getMigrationModifiedDate() != null) {
                        query.setParameter("modifiedDate", stock.getMigrationModifiedDate());
                    }

                    query.executeUpdate();
                }

                // 3단계: StatusHistory 생성
                SourceType sourceType = determineSourceType(stock);
                BusinessPhase phase = determinePhase(stock);

                StatusHistory history = StatusHistory.create(
                        stock.getFlowCode(),
                        sourceType,
                        phase,
                        Kind.CREATE,
                        "LEGACY_MIGRATION",
                        "[레거시 재고 마이그레이션] " + (stock.getStockNote() != null ? stock.getStockNote() : "")
                );
                em.persist(history);

                successCount++;

            } catch (Exception e) {
                // persist 실패 시 해당 건만 스킵, 나머지는 계속 처리
                String stockInfo = String.format("모델=%s, 매장=%s, 상태=%s",
                        stock.getProduct() != null ? stock.getProduct().getProductFactoryName() : "N/A",
                        stock.getStoreName(),
                        stock.getOrderStatus());
                log.error("Stock Writer 저장 실패 [{}]: {}", stockInfo, e.getMessage());

                // failureCollector에 기록 (StockCsvRow가 없으므로 빈 row로 대체)
                StockCsvRow failRow = new StockCsvRow();
                failRow.setNo(stock.getStockNote());
                failRow.setModelName(stock.getProduct() != null ? stock.getProduct().getProductFactoryName() : "");
                failRow.setStoreName(stock.getStoreName());
                failRow.setCurrentStockType(stock.getOrderStatus() != null ? stock.getOrderStatus().getDisplayName() : "");
                failureCollector.add(failRow, "DB 저장 실패: " + e.getMessage());

                // 실패한 엔티티를 영속성 컨텍스트에서 제거
                em.detach(stock);
            }
        }

        log.info("재고 마이그레이션 Writer: 총 {}건 중 {}건 성공, {}건 실패",
                chunk.size(), successCount, chunk.size() - successCount);
    }

    /**
     * 원재고구분(CSV) → SourceType 매핑.
     * 원재고구분이 실제 출처(주문/수리/일반)를 나타낸다.
     */
    private SourceType determineSourceType(Stock stock) {
        String source = stock.getMigrationSourceType();
        if (source != null) {
            return switch (source.trim()) {
                case "주문" -> SourceType.ORDER;
                case "수리" -> SourceType.FIX;
                case "일반" -> SourceType.NORMAL;
                case "판매" -> SourceType.SALE;
                case "대여" -> SourceType.RENTAL;
                case "반품", "반납" -> SourceType.RETURN;
                default -> SourceType.ORDER;
            };
        }
        // fallback: migrationSourceType이 없으면 OrderStatus 기반
        OrderStatus status = stock.getOrderStatus();
        if (status == null) return SourceType.ORDER;
        return switch (status) {
            case FIX -> SourceType.FIX;
            case NORMAL -> SourceType.NORMAL;
            case SALE -> SourceType.SALE;
            case RENTAL -> SourceType.RENTAL;
            case RETURN -> SourceType.RETURN;
            default -> SourceType.ORDER;
        };
    }

    /**
     * OrderStatus → BusinessPhase 매핑
     */
    private BusinessPhase determinePhase(Stock stock) {
        OrderStatus status = stock.getOrderStatus();
        if (status == null) return BusinessPhase.STOCK;
        return switch (status) {
            case STOCK -> BusinessPhase.STOCK;
            case FIX -> BusinessPhase.FIX;
            case NORMAL -> BusinessPhase.NORMAL;
            case SALE -> BusinessPhase.SALE;
            case RENTAL -> BusinessPhase.RENTAL;
            case RETURN -> BusinessPhase.RETURN;
            case DELETED -> BusinessPhase.DELETED;
            default -> BusinessPhase.STOCK;
        };
    }
}
