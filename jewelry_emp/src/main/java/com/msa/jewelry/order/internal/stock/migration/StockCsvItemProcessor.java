package com.msa.jewelry.order.internal.stock.migration;

import com.msa.jewelry.account.api.FactoryFinder;
import com.msa.jewelry.account.api.FactoryView;
import com.msa.jewelry.account.api.StoreFinder;
import com.msa.jewelry.account.api.StoreView;
import com.msa.jewelry.product.api.ProductDetailView;
import com.msa.jewelry.product.api.ProductFinder;
import com.msa.jewelry.product.api.ProductStoneView;
import com.msa.jewelry.order.internal.order.entity.OrderStone;
import com.msa.jewelry.order.internal.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.order.internal.stock.entity.ProductSnapshot;
import com.msa.jewelry.order.internal.stock.entity.Stock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CSV 행(StockCsvRow)을 Stock 엔티티로 변환하는 프로세서.
 * Store/Factory 조회 결과를 캐싱하여 반복 호출을 방지한다.
 * 변환 불가능한 행은 StockMigrationFailureCollector에 기록하고 null을 반환(skip)한다.
 */
@Slf4j
public class StockCsvItemProcessor implements ItemProcessor<StockCsvRow, Stock> {

    private final String token;
    private final StoreFinder storeFinder;
    private final FactoryFinder factoryFinder;
    private final ProductFinder productFinder;
    private final StockMigrationFailureCollector failureCollector;
    private final StockMigrationRecordCollector recordCollector;

    private final Map<String, StoreView> storeCache = new HashMap<>();
    private final Map<String, FactoryView> factoryCache = new HashMap<>();
    /** 상품명 → 카탈로그 스톤 목록 캐시 (product-service 호출 최소화) */
    private final Map<String, List<ProductStoneView>> productStoneCache = new HashMap<>();
    /** 상품명(모델명) → 카탈로그 상품 상세 캐시 (productId/productName 조회용) */
    private final Map<String, ProductDetailView> productInfoCache = new HashMap<>();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public StockCsvItemProcessor(String token,
                                 StoreFinder storeFinder,
                                 FactoryFinder factoryFinder,
                                 ProductFinder productFinder,
                                 StockMigrationFailureCollector failureCollector,
                                 StockMigrationRecordCollector recordCollector) {
        this.token = token;
        this.storeFinder = storeFinder;
        this.factoryFinder = factoryFinder;
        this.productFinder = productFinder;
        this.failureCollector = failureCollector;
        this.recordCollector = recordCollector;
    }

    @Override
    public Stock process(StockCsvRow row) {
        try {
            // 1. 현재고구분 → orderStatus 매핑
            OrderStatus orderStatus = mapOrderStatus(row.getCurrentStockType());
            if (orderStatus == null) {
                failureCollector.add(row, "알 수 없는 현재고구분 값: " + row.getCurrentStockType());
                recordCollector.recordSkipNullStatus(row);
                return null;
            }

            // 2. Store 매핑 (매장) — best-effort
            String storeName = row.getStoreName();
            Long storeId = null;
            String storeGrade = row.getStoreGrade();
            BigDecimal storeHarry = null;

            if (StringUtils.hasText(storeName) && !"NONE".equalsIgnoreCase(storeName.trim())) {
                StoreView storeInfo = lookupStore(storeName);
                if (storeInfo != null) {
                    storeId = storeInfo.storeId();
                    storeName = storeInfo.storeName();
                    if (!StringUtils.hasText(storeGrade) && StringUtils.hasText(storeInfo.storeGrade())) {
                        storeGrade = storeInfo.storeGrade();
                    }
                    if (StringUtils.hasText(storeInfo.storeHarry())) {
                        try {
                            storeHarry = new BigDecimal(storeInfo.storeHarry());
                        } catch (NumberFormatException e) {
                            // ignore, leave as null
                        }
                    }
                }
                // Store not found: use storeName only, storeId = null (no skip)
            }

            // 3. Factory 매핑 (매입처) — name-based lookup (best-effort)
            String factoryName = row.getFactoryName();
            Long factoryId = null;
            BigDecimal factoryHarry = null;

            if (StringUtils.hasText(factoryName) && !"NONE".equalsIgnoreCase(factoryName.trim())) {
                FactoryView factoryInfo = lookupFactoryByName(factoryName);
                if (factoryInfo != null) {
                    factoryId = factoryInfo.factoryId();
                    factoryName = factoryInfo.factoryName();
                    if (StringUtils.hasText(factoryInfo.goldHarryLoss())) {
                        try {
                            factoryHarry = new BigDecimal(factoryInfo.goldHarryLoss());
                        } catch (NumberFormatException e) {
                            // ignore, leave as null
                        }
                    }
                }
                // Factory not found: use factoryName only, factoryId = null (no skip)
            } else if (StringUtils.hasText(row.getFactoryHarry())) {
                try {
                    factoryHarry = new BigDecimal(row.getFactoryHarry());
                } catch (NumberFormatException e) {
                    // ignore
                }
            }

            // 4. ProductSnapshot 구성
            ProductSnapshot product = buildProductSnapshot(row);

            // 5. 비용 파싱 (쉼표 제거 후 정수 변환)
            //    productLaborCost / productAddLaborCost 는 buildProductSnapshot() 내부에서 별도로 다시 파싱한다.
            Integer stoneMainLaborCost = parseMoneyField(row.getStoneMainLaborCost());
            Integer stoneAssistanceLaborCost = parseMoneyField(row.getStoneSubLaborCost());
            Integer totalStonePurchaseCost = parseMoneyField(row.getTotalStonePurchaseCost());
            // stoneAddLaborCost: 카탈로그 매칭 결과에 따라 process() 후반부에서 결정한다.
            //   - 매칭 성공: 0  (메인/보조가 각 OrderStone 으로 정확히 분배되므로 추가버킷 비움)
            //   - 매칭 실패(카탈로그 없음 포함): stoneMainLaborCost + stoneAssistanceLaborCost
            //     스톤 정체를 알 수 없을 때 'Stock.stoneAddLaborCost' 한 곳에 합산한다.

            // 6. 금중량, 알중량 파싱
            BigDecimal goldWeight = parseWeight(row.getGoldWeight());
            BigDecimal stoneWeight = parseWeight(row.getStoneWeight());

            // 7. stockNote 구성 (시리얼 + 접수번호)
            StringBuilder noteBuilder = new StringBuilder();
            if (StringUtils.hasText(row.getSerialNumber())) {
                noteBuilder.append("[LEGACY_SERIAL:").append(row.getSerialNumber()).append("]");
            }
            if (StringUtils.hasText(row.getReceiptNumber())) {
                if (noteBuilder.length() > 0) noteBuilder.append(" ");
                noteBuilder.append("[LEGACY:").append(row.getReceiptNumber()).append("]");
            }
            if (StringUtils.hasText(row.getStockNote())) {
                if (noteBuilder.length() > 0) noteBuilder.append(" ");
                noteBuilder.append(row.getStockNote());
            }
            String stockNote = noteBuilder.toString();

            // 8. Stock 엔티티 구성 — 스톤 합산 필드는 placeholder 로 초기화하고
            //    카탈로그 매칭 결과에 따라 9단계 이후 stock.updateStoneCost(...) 로 확정한다.
            //    2026-05 P4: Stock 에 storeName/factoryName 컬럼 제거. id 만 저장.
            Stock stock = Stock.builder()
                    .storeId(storeId)
                    .storeHarry(storeHarry)
                    .storeGrade(storeGrade)
                    .factoryId(factoryId)
                    .factoryHarry(factoryHarry)
                    .stockNote(stockNote)
                    .stockMainStoneNote(row.getMainStone())
                    .stockAssistanceStoneNote(row.getSubStone())
                    .stoneMainLaborCost(stoneMainLaborCost)        // 매칭 결과 따라 추후 갱신
                    .stoneAssistanceLaborCost(stoneAssistanceLaborCost)
                    .stoneAddLaborCost(null)                        // 매칭 결과 따라 추후 갱신
                    .totalStonePurchaseCost(totalStonePurchaseCost)
                    .totalStoneLaborCost(sumLaborCosts(stoneMainLaborCost, stoneAssistanceLaborCost))
                    .product(product)
                    .orderStatus(orderStatus)
                    .stockDeleted(orderStatus == OrderStatus.DELETED)
                    .build();

            // 8-1. 원재고구분 값을 마이그레이션용 임시 필드에 설정
            stock.setMigrationSourceType(row.getSourceType());

            // 8-2. CSV 등록일/변경일을 마이그레이션용 임시 필드에 설정
            LocalDateTime csvCreatedDate = parseDateField(row.getCreatedDate());
            LocalDateTime csvModifiedDate = parseDateField(row.getChangedDate());
            if (csvCreatedDate != null) {
                stock.setMigrationCreatedDate(csvCreatedDate);
            }
            if (csvModifiedDate != null) {
                stock.setMigrationModifiedDate(csvModifiedDate);
            }

            // 9. OrderStone 생성 — 카탈로그 스톤(수량+공임) 매칭 시에만 생성
            Integer mainStoneQty = parseIntegerField(row.getMainStoneQuantity());
            Integer subStoneQty = parseIntegerField(row.getSubStoneQuantity());

            boolean catalogMatched = tryMatchCatalogStones(
                    stock, row.getModelName(), mainStoneQty, subStoneQty,
                    stoneMainLaborCost, stoneAssistanceLaborCost, totalStonePurchaseCost);

            // 9-1. 매칭 결과에 따라 Stock 의 스톤 합산 필드 확정
            int safeMain = stoneMainLaborCost != null ? stoneMainLaborCost : 0;
            int safeSub = stoneAssistanceLaborCost != null ? stoneAssistanceLaborCost : 0;
            int safePurchase = totalStonePurchaseCost != null ? totalStonePurchaseCost : 0;

            if (catalogMatched) {
                // 매칭 성공: 메인/보조 공임은 OrderStone 으로 정확히 분배되었으므로 그대로 두고
                //          stoneAddLaborCost 는 0 (미할당 누적 버킷 — 비어있음).
                int totalLabor = safeMain + safeSub;
                stock.updateStoneCost(safePurchase, totalLabor, safeMain, safeSub, 0);
            } else {
                // 매칭 실패(카탈로그 없음 포함): 스톤 정체를 모르므로 OrderStone 미생성.
                //    중심+보조 공임 합계를 stoneAddLaborCost 한 곳에 누적한다.
                //    이중계산 방지를 위해 main/assistance 는 0 으로 비운다 (totalStoneLaborCost 동일 유지).
                int sumStoneLabor = safeMain + safeSub;
                stock.updateStoneCost(safePurchase, sumStoneLabor, 0, 0, sumStoneLabor);
                if (sumStoneLabor > 0) {
                    log.debug("스톤 카탈로그 매칭 실패 → stoneAddLaborCost 누적 [모델:{}, 시리얼:{}, 합계:{}]",
                            row.getModelName(), row.getSerialNumber(), sumStoneLabor);
                }
            }

            return stock;

        } catch (Exception e) {
            String rowInfo = String.format("No=%s, 모델=%s, 접수번호=%s, 매장=%s, 현재고구분=%s",
                    row.getNo(), row.getModelName(), row.getReceiptNumber(),
                    row.getStoreName(), row.getCurrentStockType());
            log.error("Stock 변환 중 오류 [{}]: {}", rowInfo, e.getMessage(), e);
            String reason = String.format("처리 중 예외 발생 [%s]: %s", rowInfo, e.getMessage());
            failureCollector.add(row, reason);
            recordCollector.recordSkipException(row, reason);
            return null;
        }
    }

    /**
     * 현재고구분 → OrderStatus 매핑
     */
    private OrderStatus mapOrderStatus(String currentStockType) {
        if (!StringUtils.hasText(currentStockType)) {
            return null;
        }

        return switch (currentStockType.trim()) {
            case "주문" -> OrderStatus.STOCK;
            case "판매" -> OrderStatus.SALE;
            case "반납", "반품" -> OrderStatus.RETURN;
            case "삭제" -> OrderStatus.DELETED;
            case "수리" -> OrderStatus.FIX;
            case "일반" -> OrderStatus.NORMAL;
            case "대여" -> OrderStatus.RENTAL;
            default -> null;
        };
    }

    /**
     * ProductSnapshot 엔티티 구성
     * <p>
     * product-service에서 모델명(CSV의 "모델" 컬럼)으로 카탈로그 상품을 조회하여
     * productId/productName을 채워준다. 조회 실패 시 productName 은 모델명으로 fallback
     * (재고조사 UI에서 "-" 로 보이지 않도록).
     * productFactoryName 은 레거시 모델명 그대로 보존한다.
     */
    private ProductSnapshot buildProductSnapshot(StockCsvRow row) {
        Integer productLaborCost = parseMoneyField(row.getProductLaborCost());
        Integer productAddLaborCost = parseMoneyField(row.getProductAddLaborCost());
        Integer productPurchaseCost = parseMoneyField(row.getProductPurchaseCost());
        BigDecimal goldWeight = parseWeight(row.getGoldWeight());
        BigDecimal stoneWeight = parseWeight(row.getStoneWeight());

        String modelName = row.getModelName();
        Long productId = null;
        String productName = null;
        String classificationName = row.getClassification();
        Long classificationId = null;
        String setTypeName = null;
        Long setTypeId = null;

        if (StringUtils.hasText(modelName)) {
            ProductDetailView productInfo = lookupProductInfo(modelName);
            if (productInfo != null) {
                productId = productInfo.productId();
                if (StringUtils.hasText(productInfo.productName())) {
                    productName = productInfo.productName();
                }
                if (productInfo.classificationId() != null) {
                    classificationId = productInfo.classificationId();
                }
                if (StringUtils.hasText(productInfo.classificationName())) {
                    classificationName = productInfo.classificationName();
                }
                if (productInfo.setTypeId() != null) {
                    setTypeId = productInfo.setTypeId();
                }
                if (StringUtils.hasText(productInfo.setTypeName())) {
                    setTypeName = productInfo.setTypeName();
                }
            }
        }

        // productName fallback: 카탈로그 조회 실패 시 모델명으로 대체 (null 보다는 모델명 표시)
        if (!StringUtils.hasText(productName)) {
            productName = modelName;
        }

        return ProductSnapshot.builder()
                .id(productId)
                .productName(productName)
                .productFactoryName(modelName)
                .classificationId(classificationId)
                .classificationName(classificationName)
                .setTypeId(setTypeId)
                .setTypeName(setTypeName)
                .materialName(row.getMaterial())
                .colorName(row.getColor())
                .size(row.getSize())
                .productLaborCost(productLaborCost)
                .productAddLaborCost(productAddLaborCost)
                .productPurchaseCost(productPurchaseCost)
                .goldWeight(goldWeight)
                .stoneWeight(stoneWeight)
                .build();
    }

    /**
     * 모델명으로 product-service의 상품 상세를 조회 (캐싱).
     * 조회 실패/미존재 시 null.
     */
    private ProductDetailView lookupProductInfo(String modelName) {
        if (productInfoCache.containsKey(modelName)) {
            return productInfoCache.get(modelName);
        }

        try {
            ProductDetailView info = productFinder.findProductDetailByName(modelName);
            productInfoCache.put(modelName, info);
            return info;
        } catch (Exception e) {
            log.debug("상품 조회 실패: {}", modelName, e);
            productInfoCache.put(modelName, null);
            return null;
        }
    }

    /**
     * 쉼표 형식의 금액 필드 파싱 (예: "5,000" → 5000)
     */
    private Integer parseMoneyField(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            String cleaned = value.replaceAll("[,\"']", "").trim();
            if (cleaned.isEmpty()) {
                return null;
            }
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 무게 필드 파싱 (예: "6.39" → BigDecimal)
     */
    private BigDecimal parseWeight(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            String cleaned = value.replaceAll("[,\"']", "").trim();
            if (cleaned.isEmpty()) {
                return null;
            }
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 정수 필드 파싱 (예: "5" → 5)
     */
    private Integer parseIntegerField(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            String cleaned = value.replaceAll("[,\"']", "").trim();
            if (cleaned.isEmpty()) {
                return null;
            }
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 비용 필드들의 합계 계산
     */
    private Integer sumLaborCosts(Integer... costs) {
        int sum = 0;
        for (Integer cost : costs) {
            if (cost != null) {
                sum += cost;
            }
        }
        return sum > 0 ? sum : null;
    }

    /**
     * 날짜 문자열 → LocalDateTime 파싱 (예: "2024-01-15" → 2024-01-15T00:00:00)
     * 다양한 포맷 지원: yyyy-MM-dd, yyyy/MM/dd, yyyy.MM.dd
     */
    private LocalDateTime parseDateField(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        String cleaned = value.trim();
        try {
            // "yyyy-MM-dd" 형식
            LocalDate date = LocalDate.parse(cleaned, DATE_FMT);
            return date.atStartOfDay();
        } catch (DateTimeParseException e1) {
            try {
                // "yyyy/MM/dd" 형식
                LocalDate date = LocalDate.parse(cleaned, DateTimeFormatter.ofPattern("yyyy/MM/dd"));
                return date.atStartOfDay();
            } catch (DateTimeParseException e2) {
                try {
                    // "yyyy.MM.dd" 형식
                    LocalDate date = LocalDate.parse(cleaned, DateTimeFormatter.ofPattern("yyyy.MM.dd"));
                    return date.atStartOfDay();
                } catch (DateTimeParseException e3) {
                    log.debug("날짜 파싱 실패: {}", value);
                    return null;
                }
            }
        }
    }

    /**
     * Store 정보 조회 (캐싱)
     */
    private StoreView lookupStore(String storeName) {
        if (storeCache.containsKey(storeName)) {
            return storeCache.get(storeName);
        }

        try {
            StoreView storeInfo = storeFinder.findStoreByName(storeName);
            storeCache.put(storeName, storeInfo);
            return storeInfo;
        } catch (Exception e) {
            log.debug("Store 조회 실패: {}", storeName, e);
            storeCache.put(storeName, null);
            return null;
        }
    }

    /**
     * Factory 정보 조회 (캐싱)
     */
    private FactoryView lookupFactoryByName(String factoryName) {
        if (factoryCache.containsKey(factoryName)) {
            return factoryCache.get(factoryName);
        }

        try {
            FactoryView factoryInfo = factoryFinder.findFactoryByName(factoryName);
            factoryCache.put(factoryName, factoryInfo);
            return factoryInfo;
        } catch (Exception e) {
            log.debug("Factory 조회 실패: {}", factoryName, e);
            factoryCache.put(factoryName, null);
            return null;
        }
    }

    /**
     * 카탈로그 기반 스톤 매칭 시도.
     *
     * <p>매칭 성공 조건 (모두 충족해야 함):
     * <ol>
     *   <li>모델명으로 product-service 카탈로그 스톤 목록을 조회할 수 있을 것</li>
     *   <li>카탈로그 메인 스톤 총 수량 == CSV 메인알수, 보조도 동일</li>
     *   <li>카탈로그 메인 공임 합계(Σ laborCost × quantity) == CSV 중심공임/EA, 보조도 동일</li>
     * </ol>
     *
     * <p>모두 일치하면 카탈로그 스톤 정보(stoneId, stoneName, stoneWeight)로 OrderStone 을 생성한다.
     * 공임/매입원가는 CSV 값으로 덮어쓴다. 매칭 실패 시 OrderStone 은 생성되지 않으며,
     * 호출 측(process)이 합계를 stoneAddLaborCost 에 누적하는 방식으로 처리한다.</p>
     *
     * @return true: 카탈로그 매칭 성공(OrderStone 생성됨), false: 매칭 실패(OrderStone 미생성)
     */
    private boolean tryMatchCatalogStones(
            Stock stock, String modelName,
            Integer csvMainQty, Integer csvSubQty,
            Integer stoneMainLaborCost, Integer stoneAssistanceLaborCost,
            Integer totalStonePurchaseCost) {

        if (!StringUtils.hasText(modelName)) {
            return false;
        }

        // 카탈로그 스톤 목록 조회 (캐싱)
        List<ProductStoneView> catalogStones = lookupProductStones(modelName);
        if (catalogStones == null || catalogStones.isEmpty()) {
            return false;
        }

        // 카탈로그의 메인/보조 스톤 분리
        List<ProductStoneView> catalogMain = catalogStones.stream()
                .filter(ProductStoneView::mainStone).toList();
        List<ProductStoneView> catalogSub = catalogStones.stream()
                .filter(s -> !s.mainStone()).toList();

        // (1) 수량 비교
        int catalogMainTotalQty = catalogMain.stream()
                .mapToInt(s -> s.quantity() != null ? s.quantity() : 0).sum();
        int catalogSubTotalQty = catalogSub.stream()
                .mapToInt(s -> s.quantity() != null ? s.quantity() : 0).sum();
        int csvMainCount = csvMainQty != null ? csvMainQty : 0;
        int csvSubCount = csvSubQty != null ? csvSubQty : 0;

        if (catalogMainTotalQty != csvMainCount || catalogSubTotalQty != csvSubCount) {
            log.debug("스톤 수량 불일치 [모델:{}] 카탈로그 메인={}/보조={}, CSV 메인={}/보조={}",
                    modelName, catalogMainTotalQty, catalogSubTotalQty, csvMainCount, csvSubCount);
            return false;
        }

        // (2) 가격(공임) 비교 — 카탈로그의 laborCost × quantity 합계가 CSV 공임과 일치해야 함
        int catalogMainLaborTotal = catalogMain.stream()
                .mapToInt(s -> {
                    int q = s.quantity() != null ? s.quantity() : 0;
                    int l = s.laborCost() != null ? s.laborCost() : 0;
                    return q * l;
                }).sum();
        int catalogSubLaborTotal = catalogSub.stream()
                .mapToInt(s -> {
                    int q = s.quantity() != null ? s.quantity() : 0;
                    int l = s.laborCost() != null ? s.laborCost() : 0;
                    return q * l;
                }).sum();
        int csvMainLabor = stoneMainLaborCost != null ? stoneMainLaborCost : 0;
        int csvSubLabor = stoneAssistanceLaborCost != null ? stoneAssistanceLaborCost : 0;

        if (catalogMainLaborTotal != csvMainLabor || catalogSubLaborTotal != csvSubLabor) {
            log.debug("스톤 공임 불일치 [모델:{}] 카탈로그 메인={}/보조={}, CSV 메인={}/보조={}",
                    modelName, catalogMainLaborTotal, catalogSubLaborTotal, csvMainLabor, csvSubLabor);
            return false;
        }

        // 매칭 성공 — 카탈로그 스톤 정보 기반으로 OrderStone 생성
        // (stoneAddLaborCost 는 0 으로 — 매칭 성공 케이스에서는 누적 버킷이 비어있음)
        for (ProductStoneView cs : catalogMain) {
            OrderStone orderStone = OrderStone.builder()
                    .originStoneId(cs.stoneId() != null ? Long.parseLong(cs.stoneId()) : null)
                    .originStoneName(cs.stoneName())
                    .originStoneWeight(cs.stoneWeight() != null ? new BigDecimal(cs.stoneWeight()) : null)
                    .stoneQuantity(cs.quantity())
                    .stoneLaborCost(stoneMainLaborCost)        // CSV 공임으로 덮어쓰기
                    .stonePurchaseCost(totalStonePurchaseCost)  // CSV 매입가로 덮어쓰기
                    .stoneAddLaborCost(0)
                    .mainStone(true)
                    .includeStone(cs.includeStone())
                    .build();
            stock.addStockStone(orderStone);
        }

        for (ProductStoneView cs : catalogSub) {
            OrderStone orderStone = OrderStone.builder()
                    .originStoneId(cs.stoneId() != null ? Long.parseLong(cs.stoneId()) : null)
                    .originStoneName(cs.stoneName())
                    .originStoneWeight(cs.stoneWeight() != null ? new BigDecimal(cs.stoneWeight()) : null)
                    .stoneQuantity(cs.quantity())
                    .stoneLaborCost(stoneAssistanceLaborCost)  // CSV 보조공임으로 덮어쓰기
                    .stonePurchaseCost(totalStonePurchaseCost)
                    .stoneAddLaborCost(0)
                    .mainStone(false)
                    .includeStone(cs.includeStone())
                    .build();
            stock.addStockStone(orderStone);
        }

        log.info("카탈로그 스톤 매칭 성공 [모델:{}] 메인 {}건, 보조 {}건",
                modelName, catalogMain.size(), catalogSub.size());
        return true;
    }

    /**
     * 상품명으로 카탈로그 스톤 목록 조회 (캐싱)
     */
    private List<ProductStoneView> lookupProductStones(String modelName) {
        if (productStoneCache.containsKey(modelName)) {
            return productStoneCache.get(modelName);
        }

        try {
            List<ProductStoneView> stones = productFinder.getProductStonesByName(modelName);
            productStoneCache.put(modelName, stones);
            return stones;
        } catch (Exception e) {
            log.debug("카탈로그 스톤 조회 실패: {}", modelName, e);
            productStoneCache.put(modelName, List.of());
            return List.of();
        }
    }
}
