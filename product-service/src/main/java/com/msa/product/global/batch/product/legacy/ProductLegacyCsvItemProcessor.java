package com.msa.product.global.batch.product.legacy;

import com.msa.product.local.classification.entity.Classification;
import com.msa.product.local.classification.repository.ClassificationRepository;
import com.msa.product.local.color.entity.Color;
import com.msa.product.local.color.repository.ColorRepository;
import com.msa.product.local.material.entity.Material;
import com.msa.product.local.material.repository.MaterialRepository;
import com.msa.product.local.product.dto.FactoryDto;
import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.entity.ProductWorkGradePolicy;
import com.msa.product.local.product.entity.ProductWorkGradePolicyGroup;
import com.msa.product.local.product.repository.ProductRepository;
import com.msa.product.local.set.entity.SetType;
import com.msa.product.local.set.repository.SetTypeRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;

/**
 * 레거시 기본정보 CSV → Product 엔티티 변환 프로세서.
 *
 * 저장 조건:
 * 1. 단종여부(K) = "Y"이면 skip
 * 2. 이미 동일 productName이 DB에 있으면 skip
 * 3. 제조사(Factory), 세트구분(SetType), 모델분류(Classification), 기본색상(Color)이
 *    DB에 없으면 실패 CSV로 분리 (skip)
 * 4. 기본재질(Material)은 best-effort (없으면 null 허용)
 */
@Slf4j
public class ProductLegacyCsvItemProcessor implements ItemProcessor<ProductLegacyCsvRow, Product> {

    private final ProductRepository productRepository;
    private final SetTypeRepository setTypeRepository;
    private final ClassificationRepository classificationRepository;
    private final MaterialRepository materialRepository;
    private final ColorRepository colorRepository;
    private final ProductMigrationFailureCollector failureCollector;

    /** Factory 캐시: name → (factoryId, factoryName) */
    private final Map<String, FactoryDto.ResponseBatch> factoryCache;

    // 엔티티 캐시 (이름 기반 반복 조회 방지)
    private final Map<String, Optional<SetType>> setTypeCache = new HashMap<>();
    private final Map<String, Optional<Classification>> classificationCache = new HashMap<>();
    private final Map<String, Optional<Material>> materialCache = new HashMap<>();
    private final Map<String, Optional<Color>> colorCache = new HashMap<>();
    private final Set<String> existingProductNames = new HashSet<>();
    private boolean productNamesCacheLoaded = false;

    public ProductLegacyCsvItemProcessor(
            ProductRepository productRepository,
            SetTypeRepository setTypeRepository,
            ClassificationRepository classificationRepository,
            MaterialRepository materialRepository,
            ColorRepository colorRepository,
            ProductMigrationFailureCollector failureCollector,
            Map<String, FactoryDto.ResponseBatch> factoryCache) {
        this.productRepository = productRepository;
        this.setTypeRepository = setTypeRepository;
        this.classificationRepository = classificationRepository;
        this.materialRepository = materialRepository;
        this.colorRepository = colorRepository;
        this.failureCollector = failureCollector;
        this.factoryCache = factoryCache;
    }

    @Override
    public Product process(ProductLegacyCsvRow row) {
        try {
            failureCollector.incrementProcessed();
            String productName = trim(row.getModelNumber());

            // ── 1. 단종여부 확인 ──
            if ("Y".equalsIgnoreCase(trim(row.getDiscontinued()))) {
                log.info("단종 상품 skip: {}", productName);
                failureCollector.addDiscontinued(row);
                return null;
            }

            // ── 2. 중복 확인 (productName) ──
            if (!StringUtils.hasText(productName)) {
                failureCollector.add(row, "모델번호(PRODUCT_NAME)가 비어있습니다");
                return null;
            }
            if (productRepository.existsByProductName(productName)) {
                log.info("이미 존재하는 상품 skip: {}", productName);
                failureCollector.addDuplicate(row, productName);
                return null;
            }

            // ── 3. 필수 참조 엔티티 검증 (없으면 실패 CSV 분리) ──

            // 3-1. 제조사 (Factory)
            String factoryName = trim(row.getManufacturer());
            Long factoryId = null;
            if (StringUtils.hasText(factoryName)) {
                FactoryDto.ResponseBatch factory = factoryCache.get(factoryName);
                if (factory == null) {
                    failureCollector.add(row, "DB에 존재하지 않는 제조사: " + factoryName);
                    return null;
                }
                factoryId = factory.getFactoryId();
            } else {
                failureCollector.add(row, "제조사가 비어있습니다");
                return null;
            }

            // 3-2. 세트구분 (SetType) — "NONE"은 SetType 없이 진행
            String setTypeName = trim(row.getSetType());
            SetType setType = null;
            if (StringUtils.hasText(setTypeName) && !"NONE".equalsIgnoreCase(setTypeName)) {
                Optional<SetType> setTypeOpt = lookupSetType(setTypeName);
                if (setTypeOpt.isEmpty()) {
                    failureCollector.add(row, "DB에 존재하지 않는 세트구분: " + setTypeName);
                    return null;
                }
                setType = setTypeOpt.get();
            }

            // 3-3. 모델분류 (Classification)
            String classificationName = trim(row.getClassification());
            Classification classification = null;
            if (StringUtils.hasText(classificationName)) {
                Optional<Classification> classOpt = lookupClassification(classificationName);
                if (classOpt.isEmpty()) {
                    failureCollector.add(row, "DB에 존재하지 않는 모델분류: " + classificationName);
                    return null;
                }
                classification = classOpt.get();
            }

            // 3-4. 기본색상 (Color) — "P/W" 같은 값은 색상 이름 자체임 (분리하지 않음)
            String colorName = trim(row.getDefaultColor());
            Color color = null;
            if (StringUtils.hasText(colorName)) {
                Optional<Color> colorOpt = lookupColor(colorName);
                if (colorOpt.isEmpty()) {
                    failureCollector.add(row, "DB에 존재하지 않는 기본색상: " + colorName);
                    return null;
                }
                color = colorOpt.get();
            }

            // ── 4. 기본재질 (Material) — best-effort ──
            String materialName = trim(row.getMaterial());
            Material material = null;
            if (StringUtils.hasText(materialName)) {
                material = lookupMaterial(materialName).orElse(null);
            }

            // ── 5. Product 엔티티 생성 ──
            BigDecimal weight = parseWeight(row.getStandardWeight());

            Product product = Product.builder()
                    .factoryId(factoryId)
                    .factoryName(factoryName)
                    .productFactoryName(trim(row.getManufacturingNo()))
                    .productName(productName)
                    .standardWeight(weight)
                    .productNote(trim(row.getNote()))
                    .productDeleted(false)
                    .productStones(new ArrayList<>())
                    .build();

            if (setType != null) product.setSetType(setType);
            if (classification != null) product.setClassification(classification);
            if (material != null) product.setMaterial(material);

            // ── 6. ProductWorkGradePolicyGroup 생성 ──
            if (color != null) {
                Integer purchasePrice = parseMoneyField(row.getPurchasePrice());
                Integer grade1 = parseMoneyField(row.getGrade1LaborCost());
                Integer grade2 = parseMoneyField(row.getGrade2LaborCost());
                Integer grade3 = parseMoneyField(row.getGrade3LaborCost());
                Integer grade4 = parseMoneyField(row.getGrade4LaborCost());
                String laborNote = trim(row.getLaborCostNote()); // 공임설명 그대로 저장

                ProductWorkGradePolicyGroup group = ProductWorkGradePolicyGroup.builder()
                        .productPurchasePrice(purchasePrice)
                        .color(color)
                        .gradePolicies(new ArrayList<>())
                        .productWorkGradePolicyGroupDefault(true)
                        .note(laborNote)
                        .build();

                // 등급별 공임 추가
                addGradePolicy(group, "GRADE_1", grade1);
                addGradePolicy(group, "GRADE_2", grade2);
                addGradePolicy(group, "GRADE_3", grade3);
                addGradePolicy(group, "GRADE_4", grade4);

                product.addPolicyGroup(group);
            }

            return product;

        } catch (Exception e) {
            log.error("상품 마이그레이션 처리 실패 - 모델번호: {}", row.getModelNumber(), e);
            failureCollector.add(row, "처리 오류: " + e.getMessage());
            return null;
        }
    }

    // ── 캐시 조회 헬퍼 ──

    private Optional<SetType> lookupSetType(String name) {
        return setTypeCache.computeIfAbsent(name,
                k -> setTypeRepository.findBySetTypeName(k));
    }

    private Optional<Classification> lookupClassification(String name) {
        return classificationCache.computeIfAbsent(name,
                k -> classificationRepository.findByClassificationName(k));
    }

    private Optional<Material> lookupMaterial(String name) {
        return materialCache.computeIfAbsent(name,
                k -> materialRepository.findByMaterialName(k));
    }

    private Optional<Color> lookupColor(String name) {
        return colorCache.computeIfAbsent(name,
                k -> colorRepository.findByColorName(k));
    }

    // ── 파싱 헬퍼 ──

    private void addGradePolicy(ProductWorkGradePolicyGroup group, String grade, Integer laborCost) {
        if (laborCost != null) {
            ProductWorkGradePolicy policy = ProductWorkGradePolicy.builder()
                    .grade(grade)
                    .laborCost(laborCost)
                    .build();
            group.addGradePolicy(policy);
        }
    }

    private Integer parseMoneyField(String value) {
        if (!StringUtils.hasText(value)) return null;
        try {
            String cleaned = value.replaceAll("[,\"' ]", "").trim();
            if (cleaned.isEmpty()) return null;
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal parseWeight(String value) {
        if (!StringUtils.hasText(value)) return BigDecimal.ZERO;
        try {
            String cleaned = value.replaceAll("[,\"' ]", "").trim();
            if (cleaned.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(cleaned);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private String trim(String value) {
        return value != null ? value.trim() : null;
    }

}
