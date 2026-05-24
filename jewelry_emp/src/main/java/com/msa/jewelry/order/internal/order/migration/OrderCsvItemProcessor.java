package com.msa.jewelry.order.internal.order.migration;

import com.msa.jewelry.account.api.FactoryFinder;
import com.msa.jewelry.account.api.FactoryView;
import com.msa.jewelry.account.api.StoreFinder;
import com.msa.jewelry.account.api.StoreView;
import com.msa.jewelry.product.api.ProductDetailView;
import com.msa.jewelry.product.api.ProductFinder;
import com.msa.jewelry.order.internal.order.entity.OrderProduct;
import com.msa.jewelry.order.internal.order.entity.Orders;
import com.msa.jewelry.order.internal.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.order.internal.order.entity.order_enum.ProductStatus;
import com.msa.jewelry.order.internal.priority.entitiy.Priority;
import com.msa.jewelry.order.internal.priority.repository.PriorityRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

/**
 * CSV 행(OrderCsvRow)을 Orders 엔티티로 변환하는 프로세서.
 * Store/Factory 조회 결과를 캐싱하여 반복 호출을 방지한다.
 * 변환 불가능한 행은 MigrationFailureCollector에 기록하고 null을 반환(skip)한다.
 */
@Slf4j
public class OrderCsvItemProcessor implements ItemProcessor<OrderCsvRow, Orders> {

    private final String token;
    private final StoreFinder storeFinder;
    private final FactoryFinder factoryFinder;
    private final ProductFinder productFinder;
    private final MigrationFailureCollector failureCollector;
    private final boolean deletedOrder;
    private final boolean fixOrder;
    private final PriorityRepository priorityRepository;

    private final Map<String, StoreView> storeCache = new HashMap<>();
    private final Map<Object, FactoryView> factoryCache = new HashMap<>();

    /** 기본 Priority 캐시 (일반) — 첫 호출 시 조회 */
    private Priority cachedDefaultPriority;
    private boolean priorityLoaded = false;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    public OrderCsvItemProcessor(String token,
                                  StoreFinder storeFinder,
                                  FactoryFinder factoryFinder,
                                  ProductFinder productFinder,
                                  MigrationFailureCollector failureCollector,
                                  PriorityRepository priorityRepository) {
        this(token, storeFinder, factoryFinder, productFinder, failureCollector, priorityRepository, false, false);
    }

    public OrderCsvItemProcessor(String token,
                                  StoreFinder storeFinder,
                                  FactoryFinder factoryFinder,
                                  ProductFinder productFinder,
                                  MigrationFailureCollector failureCollector,
                                  PriorityRepository priorityRepository,
                                  boolean deletedOrder) {
        this(token, storeFinder, factoryFinder, productFinder, failureCollector, priorityRepository, deletedOrder, false);
    }

    public OrderCsvItemProcessor(String token,
                                  StoreFinder storeFinder,
                                  FactoryFinder factoryFinder,
                                  ProductFinder productFinder,
                                  MigrationFailureCollector failureCollector,
                                  PriorityRepository priorityRepository,
                                  boolean deletedOrder,
                                  boolean fixOrder) {
        this.token = token;
        this.storeFinder = storeFinder;
        this.factoryFinder = factoryFinder;
        this.productFinder = productFinder;
        this.failureCollector = failureCollector;
        this.priorityRepository = priorityRepository;
        this.deletedOrder = deletedOrder;
        this.fixOrder = fixOrder;
    }

    @Override
    public Orders process(OrderCsvRow row) {
        try {
            // 1. 단계 매핑
            OrderStatus orderStatus;
            ProductStatus productStatus;

            if (deletedOrder) {
                // 삭재 주문: 원래 단계와 관계없이 삭제 상태로 설정
                orderStatus = OrderStatus.DELETED;
                productStatus = ProductStatus.DELETED;
            } else if (fixOrder) {
                // 수리 주문: API 파라미터로 수리 파일 구분
                orderStatus = mapFixOrderStatus(row.getPhase());
                productStatus = mapFixProductStatus(row.getPhase());
            } else {
                orderStatus = mapOrderStatus(row.getPhase());
                productStatus = mapProductStatus(row.getPhase());
            }

            if (orderStatus == null || productStatus == null) {
                failureCollector.add(row, "알 수 없는 단계 값: " + row.getPhase());
                return null;
            }

            // 2. Store 매핑 (거 래 처) — best-effort
            String storeName = row.getTradingPartner();
            StoreView storeInfo = null;
            Long storeId = null;

            if (StringUtils.hasText(storeName)) {
                storeInfo = lookupStore(storeName);
                if (storeInfo != null) {
                    storeId = storeInfo.storeId();
                    storeName = storeInfo.storeName();
                }
                // Store not found: use storeName only, storeId = null (no skip)
            }

            // 3. Factory 매핑 (제조사) — name-based lookup (best-effort)
            String factoryName = row.getManufacturer();
            FactoryView factoryInfo = null;
            Long factoryId = null;

            if (StringUtils.hasText(factoryName) && !"NONE".equalsIgnoreCase(factoryName.trim())) {
                factoryInfo = lookupFactoryByName(factoryName);
                if (factoryInfo != null) {
                    factoryId = factoryInfo.factoryId();
                    factoryName = factoryInfo.factoryName();
                }
                // Factory not found: use factoryName only, factoryId = null (no skip)
            }

            // 4. 날짜 파싱
            LocalDateTime createAt = parseDate(row.getReceiptDate());
            LocalDateTime shippingAt = parseDate(row.getShippingDate());

            // 5. 비고 + 레거시 접수번호 조합
            String orderNote = buildOrderNote(row.getReceiptNumber(), row.getCategory(), row.getNote(), deletedOrder, fixOrder);

            // 6. Orders 엔티티 생성 (2026-05 P4: storeName/factoryName 컬럼 제거됨)
            Orders order = Orders.builder()
                    .storeId(storeId)
                    .storeGrade(storeInfo != null ? storeInfo.storeGrade() : null)
                    .storeHarry(storeInfo != null && storeInfo.storeHarry() != null
                            ? new BigDecimal(storeInfo.storeHarry()) : null)
                    .factoryId(factoryId)
                    .orderNote(orderNote)
                    .createAt(createAt)
                    .shippingAt(shippingAt)
                    .productStatus(productStatus)
                    .orderStatus(orderStatus)
                    .build();

            // 6-1. 삭재 주문이면 soft delete 처리
            if (deletedOrder) {
                order.deletedOrder(shippingAt);
            }

            // 7. Product 조회 (best-effort) — 모델번호(K) 기반 검색
            String modelNumber = row.getModelNumber();           // 모델번호(K) — 검색 키
            String manufacturingNo = row.getManufacturingNo();   // 제조번호(G) — productFactoryName 저장용

            Long productId = null;
            String productName = null;

            if (StringUtils.hasText(modelNumber)) {
                try {
                    ProductDetailView productInfo = productFinder.findProductDetailByName(modelNumber);
                    if (productInfo != null && productInfo.productId() != null) {
                        productId = productInfo.productId();
                        productName = productInfo.productName();
                    }
                } catch (Exception e) {
                    log.warn("Product 조회 중 오류 (modelNumber={}, skip): {}", modelNumber, e.getMessage());
                }
            }

            // 8. OrderProduct 엔티티 생성
            //    productFactoryName은 레거시 제조번호(G) 값으로 강제 저장
            OrderProduct orderProduct = OrderProduct.builder()
                    .productId(productId)
                    .productName(productName)
                    .productFactoryName(manufacturingNo)
                    .classificationName(row.getClassification())
                    .materialName(row.getMaterial())
                    .colorName(row.getColor())
                    .productSize(row.getSize())
                    .orderMainStoneNote(row.getMainStone())
                    .orderAssistanceStoneNote(row.getSubStone())
                    .build();

            order.addOrderProduct(orderProduct);

            // 9. Priority 설정 (기본값: 일반) — inner join 조건 충족 필수
            order.addPriority(getDefaultPriority());

            return order;

        } catch (Exception e) {
            log.error("마이그레이션 처리 실패 - 접수번호: {}", row.getReceiptNumber(), e);
            failureCollector.add(row, "처리 오류: " + e.getMessage());
            return null;
        }
    }

    // ── Store/Factory 캐시 조회 ──

    private StoreView lookupStore(String storeName) {
        if (!storeCache.containsKey(storeName)) {
            try {
                storeCache.put(storeName, storeFinder.findStoreByName(storeName));
            } catch (Exception e) {
                log.warn("Store 조회 실패: {}", storeName, e);
                storeCache.put(storeName, null);
            }
        }
        return storeCache.get(storeName);
    }

    @SuppressWarnings("unused")
    private FactoryView lookupFactory(Long factoryId) {
        if (!factoryCache.containsKey(factoryId)) {
            try {
                factoryCache.put(factoryId, factoryFinder.getFactoryInfo(factoryId));
            } catch (Exception e) {
                log.warn("Factory 조회 실패 (id={}): {}", factoryId, e.getMessage());
                factoryCache.put(factoryId, null);
            }
        }
        return factoryCache.get(factoryId);
    }

    private FactoryView lookupFactoryByName(String factoryName) {
        if (!factoryCache.containsKey(factoryName)) {
            try {
                factoryCache.put(factoryName, factoryFinder.findFactoryByName(factoryName));
            } catch (Exception e) {
                log.warn("Factory 조회 실패 (name={}): {}", factoryName, e.getMessage());
                factoryCache.put(factoryName, null);
            }
        }
        return factoryCache.get(factoryName);
    }

    // ── Priority 조회 (캐시) ──

    private Priority getDefaultPriority() {
        if (!priorityLoaded) {
            cachedDefaultPriority = priorityRepository.findByPriorityName("일반")
                    .orElseThrow(() -> new IllegalStateException(
                            "기본 Priority '일반'이 DB에 존재하지 않습니다. PRIORITY 테이블을 확인하세요."));
            priorityLoaded = true;
        }
        return cachedDefaultPriority;
    }

    // ── 매핑 헬퍼 ──

    private OrderStatus mapFixOrderStatus(String phase) {
        if (phase == null) return OrderStatus.FIX;
        return switch (phase.trim().toUpperCase()) {
            case "C", "F", "F1" -> OrderStatus.STOCK;
            default -> OrderStatus.FIX;
        };
    }

    private ProductStatus mapFixProductStatus(String phase) {
        if (phase == null) return ProductStatus.RECEIPT;
        return switch (phase.trim().toUpperCase()) {
            case "A" -> ProductStatus.RECEIPT;
            case "B" -> ProductStatus.WAITING;
            case "C", "F", "F1" -> ProductStatus.NONE;
            default -> ProductStatus.RECEIPT;
        };
    }

    private OrderStatus mapOrderStatus(String phase) {
        if (phase == null) return null;
        return switch (phase.trim().toUpperCase()) {
            case "A", "B" -> OrderStatus.ORDER;
            case "C", "F", "F1" -> OrderStatus.STOCK;
            default -> null;
        };
    }

    private ProductStatus mapProductStatus(String phase) {
        if (phase == null) return null;
        return switch (phase.trim().toUpperCase()) {
            case "A" -> ProductStatus.RECEIPT;
            case "B" -> ProductStatus.WAITING;
            case "C", "F", "F1" -> ProductStatus.NONE;
            default -> null;
        };
    }

    private LocalDateTime parseDate(String dateStr) {
        if (!StringUtils.hasText(dateStr)) return null;
        try {
            LocalDate date = LocalDate.parse(dateStr.trim(), DATE_FMT);
            // 애플리케이션·DB 가 KST 기준이므로 별도 ZoneId 변환 없이 그대로 사용
            return date.atStartOfDay();
        } catch (DateTimeParseException e) {
            log.warn("날짜 파싱 실패: {}", dateStr);
            return null;
        }
    }

    private String buildOrderNote(String receiptNumber, String category, String note, boolean deleted, boolean fixOrder) {
        StringBuilder sb = new StringBuilder();
        sb.append("[LEGACY:").append(receiptNumber != null ? receiptNumber : "").append("]");
        if (deleted) {
            sb.append("[DELETED]");
        }
        if (fixOrder) {
            sb.append("[수리]");
        } else if (StringUtils.hasText(category)) {
            sb.append("[").append(category).append("]");
        }
        if (StringUtils.hasText(note)) {
            sb.append(" ").append(note);
        }
        return sb.toString();
    }
}
