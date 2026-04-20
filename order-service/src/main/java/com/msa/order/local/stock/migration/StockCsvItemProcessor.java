package com.msa.order.local.stock.migration;

import com.msa.order.global.feign_client.client.FactoryClient;
import com.msa.order.global.feign_client.client.ProductClient;
import com.msa.order.global.feign_client.client.StoreClient;
import com.msa.order.global.feign_client.dto.ProductDetailDto;
import com.msa.order.local.order.dto.FactoryDto;
import com.msa.order.local.order.dto.StoreDto;
import com.msa.order.local.order.entity.OrderStone;
import com.msa.order.local.order.entity.order_enum.OrderStatus;
import com.msa.order.local.stock.entity.ProductSnapshot;
import com.msa.order.local.stock.entity.Stock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
    private final StoreClient storeClient;
    private final FactoryClient factoryClient;
    private final ProductClient productClient;
    private final StockMigrationFailureCollector failureCollector;

    private final Map<String, StoreDto.Response> storeCache = new HashMap<>();
    private final Map<String, FactoryDto.Response> factoryCache = new HashMap<>();
    /** 상품명 → 카탈로그 스톤 목록 캐시 (product-service 호출 최소화) */
    private final Map<String, List<ProductDetailDto.StoneInfo>> productStoneCache = new HashMap<>();
    /** 상품명(모델명) → 카탈로그 상품 상세 캐시 (productId/productName 조회용) */
    private final Map<String, ProductDetailDto> productInfoCache = new HashMap<>();

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    public StockCsvItemProcessor(String token,
                                 StoreClient storeClient,
                                 FactoryClient factoryClient,
                                 ProductClient productClient,
                                 StockMigrationFailureCollector failureCollector) {
        this.token = token;
        this.storeClient = storeClient;
        this.factoryClient = factoryClient;
        this.productClient = productClient;
        this.failureCollector = failureCollector;
    }

    @Override
    public Stock process(StockCsvRow row) {
        try {
            // 1. 현재고구분 → orderStatus 매핑
            OrderStatus orderStatus = mapOrderStatus(row.getCurrentStockType());
            if (orderStatus == null) {
                failureCollector.add(row, "알 수 없는 현재고구분 값: " + row.getCurrentStockType());
                return null;
            }

            // 2. Store 매핑 (매장) — best-effort
            String storeName = row.getStoreName();
            Long storeId = null;
            String storeGrade = row.getStoreGrade();
            BigDecimal storeHarry = null;

            if (StringUtils.hasText(storeName) && !"NONE".equalsIgnoreCase(storeName.trim())) {
                StoreDto.Response storeInfo = lookupStore(storeName);
                if (storeInfo != null) {
                    storeId = storeInfo.getStoreId();
                    storeName = storeInfo.getStoreName();
                    if (!StringUtils.hasText(storeGrade) && StringUtils.hasText(storeInfo.getGrade())) {
                        storeGrade = storeInfo.getGrade();
                    }
                    if (StringUtils.hasText(storeInfo.getStoreHarry())) {
                        try {
                            storeHarry = new BigDecimal(storeInfo.getStoreHarry());
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
                FactoryDto.Response factoryInfo = lookupFactoryByName(factoryName);
                if (factoryInfo != null) {
                    factoryId = factoryInfo.getFactoryId();
                    factoryName = factoryInfo.getFactoryName();
                    if (StringUtils.hasText(factoryInfo.getFactoryHarry())) {
                        try {
                            factoryHarry = new BigDecimal(factoryInfo.getFactoryHarry());
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
            Integer productLaborCost = parseMoneyField(row.getProductLaborCost());
            Integer productAddLaborCost = parseMoneyField(row.getProductAddLaborCost());
            Integer stoneMainLaborCost = parseMoneyField(row.getStoneMainLaborCost());
            Integer stoneAssistanceLaborCost = parseMoneyField(row.getStoneSubLaborCost());
            Integer stoneAddLaborCost = parseMoneyField(row.getProductAddLaborCost()); // 추가공임/EA 재사용
            Integer totalStonePurchaseCost = parseMoneyField(row.getTotalStonePurchaseCost());

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

            // 8. Stock 엔티티 구성
            Stock stock = Stock.builder()
                    .storeId(storeId)
                    .storeName(storeName)
                    .storeHarry(storeHarry)
                    .storeGrade(storeGrade)
                    .factoryId(factoryId)
                    .factoryName(factoryName)
                    .factoryHarry(factoryHarry)
                    .stockNote(stockNote)
                    .stockMainStoneNote(row.getMainStone())
                    .stockAssistanceStoneNote(row.getSubStone())
                    .stoneMainLaborCost(stoneMainLaborCost)
                    .stoneAssistanceLaborCost(stoneAssistanceLaborCost)
                    .stoneAddLaborCost(stoneAddLaborCost)
                    .totalStonePurchaseCost(totalStonePurchaseCost)
                    .totalStoneLaborCost(sumLaborCosts(stoneMainLaborCost, stoneAssistanceLaborCost, stoneAddLaborCost))
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

            // 9. OrderStone 생성 — 카탈로그 스톤 매칭 우선, 불일치 시 fallback
            Integer mainStoneQty = parseIntegerField(row.getMainStoneQuantity());
            Integer subStoneQty = parseIntegerField(row.getSubStoneQuantity());

            boolean catalogMatched = tryMatchCatalogStones(
                    stock, row.getModelName(), mainStoneQty, subStoneQty,
                    stoneMainLaborCost, stoneAssistanceLaborCost, stoneAddLaborCost, totalStonePurchaseCost);

            if (!catalogMatched) {
                // Fallback: 카탈로그 매칭 실패 → stoneId 없이 수량+비용만으로 생성
                createFallbackOrderStones(
                        stock, row, mainStoneQty, subStoneQty,
                        stoneMainLaborCost, stoneAssistanceLaborCost, stoneAddLaborCost, totalStonePurchaseCost);
            }

            return stock;

        } catch (Exception e) {
            String rowInfo = String.format("No=%s, 모델=%s, 접수번호=%s, 매장=%s, 현재고구분=%s",
                    row.getNo(), row.getModelName(), row.getReceiptNumber(),
                    row.getStoreName(), row.getCurrentStockType());
            log.error("Stock 변환 중 오류 [{}]: {}", rowInfo, e.getMessage(), e);
            failureCollector.add(row, String.format("처리 중 예외 발생 [%s]: %s", rowInfo, e.getMessage()));
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
            ProductDetailDto productInfo = lookupProductInfo(modelName);
            if (productInfo != null) {
                productId = productInfo.getProductId();
                if (StringUtils.hasText(productInfo.getProductName())) {
                    productName = productInfo.getProductName();
                }
                if (productInfo.getClassificationId() != null) {
                    classificationId = productInfo.getClassificationId();
                }
                if (StringUtils.hasText(productInfo.getClassificationName())) {
                    classificationName = productInfo.getClassificationName();
                }
                if (productInfo.getSetTypeId() != null) {
                    setTypeId = productInfo.getSetTypeId();
                }
                if (StringUtils.hasText(productInfo.getSetTypeName())) {
                    setTypeName = productInfo.getSetTypeName();
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
    private ProductDetailDto lookupProductInfo(String modelName) {
        if (productInfoCache.containsKey(modelName)) {
            return productInfoCache.get(modelName);
        }

        try {
            ProductDetailDto info = productClient.getProductInfoByName(token, modelName);
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
    private StoreDto.Response lookupStore(String storeName) {
        if (storeCache.containsKey(storeName)) {
            return storeCache.get(storeName);
        }

        try {
            StoreDto.Response storeInfo = storeClient.getStoreInfoByName(token, storeName);
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
    private FactoryDto.Response lookupFactoryByName(String factoryName) {
        if (factoryCache.containsKey(factoryName)) {
            return factoryCache.get(factoryName);
        }

        try {
            FactoryDto.Response factoryInfo = factoryClient.getFactoryInfoByName(token, factoryName);
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
     * 1. 모델명으로 product-service에서 스톤 목록 조회 (캐싱)
     * 2. 카탈로그의 메인/보조 스톤 수량과 CSV의 메인알수/보조알수 비교
     * 3. 일치하면 카탈로그 스톤 정보(stoneId, stoneName, stoneWeight)를 사용하고
     *    공임 값은 CSV 값으로 덮어쓴다.
     *
     * @return true: 카탈로그 매칭 성공, false: 매칭 실패 (fallback 필요)
     */
    private boolean tryMatchCatalogStones(
            Stock stock, String modelName,
            Integer csvMainQty, Integer csvSubQty,
            Integer stoneMainLaborCost, Integer stoneAssistanceLaborCost,
            Integer stoneAddLaborCost, Integer totalStonePurchaseCost) {

        if (!StringUtils.hasText(modelName)) {
            return false;
        }

        // 카탈로그 스톤 목록 조회 (캐싱)
        List<ProductDetailDto.StoneInfo> catalogStones = lookupProductStones(modelName);
        if (catalogStones == null || catalogStones.isEmpty()) {
            return false;
        }

        // 카탈로그의 메인/보조 스톤 분리
        List<ProductDetailDto.StoneInfo> catalogMain = catalogStones.stream()
                .filter(ProductDetailDto.StoneInfo::isMainStone).toList();
        List<ProductDetailDto.StoneInfo> catalogSub = catalogStones.stream()
                .filter(s -> !s.isMainStone()).toList();

        // 카탈로그 총 수량 합산
        int catalogMainTotalQty = catalogMain.stream()
                .mapToInt(s -> s.getQuantity() != null ? s.getQuantity() : 0).sum();
        int catalogSubTotalQty = catalogSub.stream()
                .mapToInt(s -> s.getQuantity() != null ? s.getQuantity() : 0).sum();

        int csvMainCount = csvMainQty != null ? csvMainQty : 0;
        int csvSubCount = csvSubQty != null ? csvSubQty : 0;

        // 메인알수·보조알수 모두 일치해야 카탈로그 기반 생성
        if (catalogMainTotalQty != csvMainCount || catalogSubTotalQty != csvSubCount) {
            log.debug("스톤 수량 불일치 [모델:{}] 카탈로그 메인={}/보조={}, CSV 메인={}/보조={}",
                    modelName, catalogMainTotalQty, catalogSubTotalQty, csvMainCount, csvSubCount);
            return false;
        }

        // 매칭 성공 — 카탈로그 스톤 정보 기반으로 OrderStone 생성
        for (ProductDetailDto.StoneInfo cs : catalogMain) {
            OrderStone orderStone = OrderStone.builder()
                    .originStoneId(cs.getStoneId() != null ? Long.parseLong(cs.getStoneId()) : null)
                    .originStoneName(cs.getStoneName())
                    .originStoneWeight(cs.getStoneWeight() != null ? new BigDecimal(cs.getStoneWeight()) : null)
                    .stoneQuantity(cs.getQuantity())
                    .stoneLaborCost(stoneMainLaborCost)        // CSV 공임으로 덮어쓰기
                    .stonePurchaseCost(totalStonePurchaseCost)  // CSV 매입가로 덮어쓰기
                    .stoneAddLaborCost(stoneAddLaborCost)
                    .mainStone(true)
                    .includeStone(cs.isIncludeStone())
                    .build();
            stock.addStockStone(orderStone);
        }

        for (ProductDetailDto.StoneInfo cs : catalogSub) {
            OrderStone orderStone = OrderStone.builder()
                    .originStoneId(cs.getStoneId() != null ? Long.parseLong(cs.getStoneId()) : null)
                    .originStoneName(cs.getStoneName())
                    .originStoneWeight(cs.getStoneWeight() != null ? new BigDecimal(cs.getStoneWeight()) : null)
                    .stoneQuantity(cs.getQuantity())
                    .stoneLaborCost(stoneAssistanceLaborCost)  // CSV 보조공임으로 덮어쓰기
                    .stonePurchaseCost(totalStonePurchaseCost)
                    .stoneAddLaborCost(stoneAddLaborCost)
                    .mainStone(false)
                    .includeStone(cs.isIncludeStone())
                    .build();
            stock.addStockStone(orderStone);
        }

        log.info("카탈로그 스톤 매칭 성공 [모델:{}] 메인 {}건, 보조 {}건",
                modelName, catalogMain.size(), catalogSub.size());
        return true;
    }

    /**
     * Fallback: 카탈로그 매칭 실패 시 stoneId 없이 수량+비용만으로 OrderStone 생성.
     * originStoneName에 CSV 텍스트(중심스톤/보조스톤)를 보존한다.
     */
    private void createFallbackOrderStones(
            Stock stock, StockCsvRow row,
            Integer mainStoneQty, Integer subStoneQty,
            Integer stoneMainLaborCost, Integer stoneAssistanceLaborCost,
            Integer stoneAddLaborCost, Integer totalStonePurchaseCost) {

        if (mainStoneQty != null && mainStoneQty > 0) {
            OrderStone mainStone = OrderStone.builder()
                    .originStoneId(null)
                    .originStoneName(StringUtils.hasText(row.getMainStone()) ? row.getMainStone() : null)
                    .stoneQuantity(mainStoneQty)
                    .stoneLaborCost(stoneMainLaborCost)
                    .stonePurchaseCost(totalStonePurchaseCost)
                    .stoneAddLaborCost(stoneAddLaborCost)
                    .mainStone(true)
                    .includeStone(true)
                    .build();
            stock.addStockStone(mainStone);
        }

        if (subStoneQty != null && subStoneQty > 0) {
            OrderStone subStone = OrderStone.builder()
                    .originStoneId(null)
                    .originStoneName(StringUtils.hasText(row.getSubStone()) ? row.getSubStone() : null)
                    .stoneQuantity(subStoneQty)
                    .stoneLaborCost(stoneAssistanceLaborCost)
                    .stonePurchaseCost(totalStonePurchaseCost)
                    .stoneAddLaborCost(stoneAddLaborCost)
                    .mainStone(false)
                    .includeStone(true)
                    .build();
            stock.addStockStone(subStone);
        }
    }

    /**
     * 상품명으로 카탈로그 스톤 목록 조회 (캐싱)
     */
    private List<ProductDetailDto.StoneInfo> lookupProductStones(String modelName) {
        if (productStoneCache.containsKey(modelName)) {
            return productStoneCache.get(modelName);
        }

        try {
            List<ProductDetailDto.StoneInfo> stones = productClient.getProductStonesByName(token, modelName);
            productStoneCache.put(modelName, stones);
            return stones;
        } catch (Exception e) {
            log.debug("카탈로그 스톤 조회 실패: {}", modelName, e);
            productStoneCache.put(modelName, List.of());
            return List.of();
        }
    }
}
