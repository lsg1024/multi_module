package com.msa.jewelry.global.batch.order;

import com.msa.jewelry.local.color.entity.Color;
import com.msa.jewelry.local.color.repository.ColorRepository;
import com.msa.jewelry.local.common_option.entity.CommonOption;
import com.msa.jewelry.local.common_option.entity.OptionLevel;
import com.msa.jewelry.local.common_option.entity.OptionTradeType;
import com.msa.jewelry.local.factory.entity.Factory;
import com.msa.jewelry.local.factory.repository.FactoryRepository;
import com.msa.jewelry.local.goldharry.entity.GoldHarry;
import com.msa.jewelry.local.goldharry.repository.GoldHarryRepository;
import com.msa.jewelry.local.material.entity.Material;
import com.msa.jewelry.local.material.repository.MaterialRepository;
import com.msa.jewelry.local.order.entity.OrderProduct;
import com.msa.jewelry.local.order.entity.OrderStone;
import com.msa.jewelry.local.order.entity.Orders;
import com.msa.jewelry.local.order.entity.order_enum.OrderStatus;
import com.msa.jewelry.local.order.entity.order_enum.ProductStatus;
import com.msa.jewelry.local.order.repository.OrdersRepository;
import com.msa.jewelry.local.priority.entity.Priority;
import com.msa.jewelry.local.priority.repository.PriorityRepository;
import com.msa.jewelry.local.product.entity.Product;
import com.msa.jewelry.local.product.repository.ProductRepository;
import com.msa.jewelry.local.stone.repository.StoneRepository;
import com.msa.jewelry.local.store.entity.Store;
import com.msa.jewelry.local.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * 레거시 주문 → Orders(+OrderProduct+OrderStone) 변환 프로세서.
 * 거래처/제조사/색상/재질/상품은 이름으로 찾고, 없으면 자동 생성한다(같은 트랜잭션, 대소문자 무시 캐시).
 * 멱등: 이미 적재된 legacyOrderNo 는 null 반환(skip).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderMigrationProcessor implements ItemProcessor<OrderBatchDto, Orders> {

    private final OrdersRepository ordersRepository;
    private final StoreRepository storeRepository;
    private final FactoryRepository factoryRepository;
    private final ProductRepository productRepository;
    private final ColorRepository colorRepository;
    private final MaterialRepository materialRepository;
    private final StoneRepository stoneRepository;
    private final PriorityRepository priorityRepository;
    private final GoldHarryRepository goldHarryRepository;

    // 이름(대소문자 무시) → 참조 캐시. chunk 내 신규 생성분도 즉시 반영.
    // Ref 는 DB 정규 표기(name)를 함께 보관 → denorm 컬럼에 "부산js" 가 아니라 DB 의 "부산JS" 로 저장.
    private Map<String, Long> storeCache;     // Orders 는 storeId 만 저장(이름 denorm 없음)
    private Map<String, Long> factoryCache;   // Orders 는 factoryId 만 저장
    private Map<String, Ref> productCache;    // OrderProduct.productName denorm → 정규 표기
    private Map<String, Ref> colorCache;      // OrderProduct.colorName denorm → 정규 표기
    private Map<String, Ref> materialCache;   // OrderProduct.materialName denorm → 정규 표기
    private GoldHarry defaultGoldHarry;

    // id + DB 정규 표기 묶음
    private record Ref(Long id, String name) {}

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        storeCache = caseInsensitiveMap();
        storeRepository.findAll().forEach(s -> storeCache.putIfAbsent(s.getStoreName(), s.getStoreId()));
        factoryCache = caseInsensitiveMap();
        factoryRepository.findAll().forEach(f -> factoryCache.putIfAbsent(f.getFactoryName(), f.getFactoryId()));
        productCache = caseInsensitiveRefMap();
        productRepository.findAll().forEach(p -> productCache.putIfAbsent(p.getProductName(), new Ref(p.getProductId(), p.getProductName())));
        colorCache = caseInsensitiveRefMap();
        colorRepository.findAll().forEach(c -> colorCache.putIfAbsent(c.getColorName(), new Ref(c.getColorId(), c.getColorName())));
        materialCache = caseInsensitiveRefMap();
        materialRepository.findAll().forEach(m -> materialCache.putIfAbsent(m.getMaterialName(), new Ref(m.getMaterialId(), m.getMaterialName())));

        // 거래처/제조사 자동 생성용 기본 GoldHarry (default_option=true 우선, 없으면 첫 행, 그것도 없으면 0 생성)
        defaultGoldHarry = goldHarryRepository.findAll().stream()
                .filter(GoldHarry::getDefaultOption).findFirst()
                .orElseGet(() -> goldHarryRepository.findAll().stream().findFirst()
                        .orElseGet(() -> goldHarryRepository.save(
                                GoldHarry.builder().goldHarryLoss(BigDecimal.ZERO).build())));
    }

    @Override
    public Orders process(OrderBatchDto dto) {
        if (dto.getLegacyOrderNo() == null || dto.getLegacyOrderNo().isBlank()) {
            log.warn("legacyOrderNo 없음 — skip");
            return null;
        }
        if (ordersRepository.existsByLegacyOrderNo(dto.getLegacyOrderNo())) {
            return null; // 멱등 skip
        }

        Long storeId = StringUtils.hasText(dto.getStoreName()) ? resolveStore(dto.getStoreName()) : null;
        Long factoryId = StringUtils.hasText(dto.getFactoryName()) ? resolveFactory(dto.getFactoryName()) : null;

        OrderStatus orderStatus = parseOrderStatus(dto.getOrderStatus());
        Priority priority = resolvePriority(dto.getPriorityName());

        Orders order = Orders.builder()
                .legacyOrderNo(dto.getLegacyOrderNo())
                .storeId(storeId)
                .factoryId(factoryId)
                .orderNote(buildNote(dto))
                .createAt(parseDate(dto.getCreateAt()))
                .shippingAt(parseDate(dto.getShippingAt()))
                .productStatus(ProductStatus.RECEIPT)
                .orderStatus(orderStatus)
                .build();

        order.addOrderProduct(buildOrderProduct(dto.getProduct()));
        for (OrderBatchDto.Stone s : dto.getStones()) {
            order.addOrderStone(buildOrderStone(s));
        }
        if (priority != null) {
            order.addPriority(priority);
        }
        return order;
    }

    // ---------------- master 해석/자동생성 ----------------

    private Long resolveStore(String name) {
        String key = name.trim();
        Long id = storeCache.get(key);
        if (id != null) return id;
        CommonOption co = newCommonOption();
        Store store = storeRepository.save(Store.builder().storeName(key).commonOption(co).build());
        storeCache.put(key, store.getStoreId());
        log.info("[이관] 거래처 자동 생성: {}", key);
        return store.getStoreId();
    }

    private Long resolveFactory(String name) {
        String key = name.trim();
        Long id = factoryCache.get(key);
        if (id != null) return id;
        CommonOption co = newCommonOption();
        Factory factory = factoryRepository.save(
                Factory.builder().factoryName(key).factoryDeleted(false).commonOption(co).build());
        factoryCache.put(key, factory.getFactoryId());
        log.info("[이관] 제조사 자동 생성: {}", key);
        return factory.getFactoryId();
    }

    /** 상품: 이름(model)으로 찾고 없으면 단종(product_deleted=true) 최소 행 생성. DB 정규 표기 반환. */
    private Ref resolveProduct(String modelName, Long factoryId, String factoryModelName) {
        if (!StringUtils.hasText(modelName)) return null;
        String key = modelName.trim();
        Ref ref = productCache.get(key);            // 대소문자 무시 → DB 정규 표기 매칭
        if (ref != null) return ref;
        Product product = productRepository.save(Product.builder()
                .productName(key)
                .factoryId(factoryId)
                .productFactoryName(StringUtils.hasText(factoryModelName) ? factoryModelName : key)
                .productDeleted(true)              // 단종 상품으로 생성
                .productStones(new ArrayList<>())
                .productImages(new ArrayList<>())
                .build());
        Ref nr = new Ref(product.getProductId(), product.getProductName());
        productCache.put(key, nr);
        log.info("[이관] 단종 상품 자동 생성: {}", key);
        return nr;
    }

    private Ref resolveColor(String name) {
        if (!StringUtils.hasText(name)) return null;
        String key = name.trim();
        Ref ref = colorCache.get(key);
        if (ref != null) return ref;
        Color color = colorRepository.save(Color.builder().colorName(key).build());
        Ref nr = new Ref(color.getColorId(), color.getColorName());
        colorCache.put(key, nr);
        log.info("[이관] 색상 자동 생성: {}", key);
        return nr;
    }

    private Ref resolveMaterial(String name) {
        if (!StringUtils.hasText(name)) return null;
        String key = name.trim();
        Ref ref = materialCache.get(key);
        if (ref != null) return ref;
        Material material = materialRepository.save(Material.builder().materialName(key).build());
        Ref nr = new Ref(material.getMaterialId(), material.getMaterialName());
        materialCache.put(key, nr);
        log.info("[이관] 재질 자동 생성: {}", key);
        return nr;
    }

    private Priority resolvePriority(String name) {
        String target = StringUtils.hasText(name) ? name.trim() : "일반";
        return priorityRepository.findByPriorityName(target)
                .orElseGet(() -> priorityRepository.findByPriorityName("일반").orElse(null));
    }

    private CommonOption newCommonOption() {
        return CommonOption.builder()
                .goldHarry(defaultGoldHarry)
                .goldHarryLoss(defaultGoldHarry.getGoldHarryLoss().toString())
                .optionLevel(OptionLevel.ONE)
                .optionTradeType(OptionTradeType.WEIGHT)
                .build();
    }

    // ---------------- 엔티티 조립 ----------------

    private OrderProduct buildOrderProduct(OrderBatchDto.Product p) {
        if (p == null) {
            return OrderProduct.builder().build();
        }
        Long factoryId = null; // 상품 자동생성 시 제조사 연결은 주문의 factoryId 와 분리(상품 마스터 단종)
        Ref product = resolveProduct(p.getModelName(), factoryId, p.getFactoryModelName());
        Ref material = resolveMaterial(p.getMaterialName());
        Ref color = resolveColor(p.getColorName());
        return OrderProduct.builder()
                // id 와 denorm 이름 모두 DB 정규 표기를 따른다(없으면 JSON 원본 유지).
                .productId(product != null ? product.id() : null)
                .productName(product != null ? product.name() : p.getModelName())
                .productFactoryName(p.getFactoryModelName())
                .productSize(p.getSize())
                .productLaborCost(p.getLaborBase())
                .productAddLaborCost(p.getLaborAdd())
                .materialId(material != null ? material.id() : null)
                .materialName(material != null ? material.name() : p.getMaterialName())
                .colorId(color != null ? color.id() : null)
                .colorName(color != null ? color.name() : p.getColorName())
                .goldWeight(toBigDecimal(p.getGoldWeight()))
                .stoneWeight(toBigDecimal(p.getStoneWeight()))
                .orderMainStoneNote(p.getMainStoneNote())
                .orderAssistanceStoneNote(p.getAssistanceStoneNote())
                .build();
    }

    private OrderStone buildOrderStone(OrderBatchDto.Stone s) {
        Long stoneId = StringUtils.hasText(s.getStoneName())
                ? stoneRepository.findByStoneNameIgnoreCase(s.getStoneName().trim())
                    .map(st -> st.getStoneId()).orElse(null)
                : null;
        return OrderStone.builder()
                .originStoneId(stoneId)
                .originStoneName(s.getStoneName())
                .stoneLaborCost(s.getLaborCost())
                .stoneQuantity(s.getQuantity())
                .mainStone(Boolean.TRUE.equals(s.getIsMain()))
                .includeStone(false)
                .build();
    }

    // ---------------- 유틸 ----------------

    private String buildNote(OrderBatchDto dto) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(dto.getNote())) sb.append(dto.getNote());
        if (StringUtils.hasText(dto.getCreatedBy())) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append("[이관 원본작성: ").append(dto.getCreatedBy());
            if (StringUtils.hasText(dto.getCreatedDate())) sb.append(" ").append(dto.getCreatedDate());
            sb.append("]");
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    private OrderStatus parseOrderStatus(String s) {
        if (StringUtils.hasText(s)) {
            try {
                return OrderStatus.valueOf(s.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("알 수 없는 orderStatus={} — ORDER 로 대체", s);
            }
        }
        return OrderStatus.ORDER;
    }

    private LocalDateTime parseDate(String yyyyMmDd) {
        if (!StringUtils.hasText(yyyyMmDd)) return null;
        try {
            return LocalDate.parse(yyyyMmDd.trim()).atStartOfDay();
        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", yyyyMmDd);
            return null;
        }
    }

    private BigDecimal toBigDecimal(Double v) {
        return v == null ? null : BigDecimal.valueOf(v);
    }

    private Map<String, Long> caseInsensitiveMap() {
        return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    private Map<String, Ref> caseInsensitiveRefMap() {
        return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }
}
