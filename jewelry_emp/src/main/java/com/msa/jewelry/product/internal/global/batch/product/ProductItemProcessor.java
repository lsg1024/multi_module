package com.msa.jewelry.product.internal.global.batch.product;

import com.msa.jewelry.account.api.FactoryFinder;
import com.msa.jewelry.account.api.FactoryView;
import com.msa.jewelry.product.internal.classification.repository.ClassificationRepository;
import com.msa.jewelry.product.internal.color.entity.Color;
import com.msa.jewelry.product.internal.color.repository.ColorRepository;
import com.msa.jewelry.product.internal.material.repository.MaterialRepository;
import com.msa.jewelry.product.internal.product.dto.ProductBatchDto;
import com.msa.jewelry.product.internal.product.dto.ProductWorkGradePolicyDto;
import com.msa.jewelry.product.internal.product.entity.Product;
import com.msa.jewelry.product.internal.product.entity.ProductStone;
import com.msa.jewelry.product.internal.product.entity.ProductWorkGradePolicy;
import com.msa.jewelry.product.internal.product.entity.ProductWorkGradePolicyGroup;
import com.msa.jewelry.product.internal.product.repository.ProductRepository;
import com.msa.jewelry.product.internal.set.repository.SetTypeRepository;
import com.msa.jewelry.product.internal.stone.stone.repository.StoneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductItemProcessor implements ItemProcessor<ProductBatchDto, Product> {

    private final FactoryFinder factoryFinder;

    private final ProductRepository productRepository;
    private final SetTypeRepository setTypeRepository;
    private final ClassificationRepository classificationRepository;
    private final MaterialRepository materialRepository;
    private final StoneRepository stoneRepository;
    private final ColorRepository colorRepository;

    private Map<String, Long> factoryCache;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        // 같은 JVM 안 호출이므로 accessToken 검사 불필요. 모듈 API 만 호출.
        try {
            List<FactoryView> factories = factoryFinder.findAll();

            log.info(">>>> [Batch] 공장 개수 = {}", factories.size());

            if (factories.isEmpty()) {
                log.warn(">>>> [Batch] 공장 데이터가 비어있습니다.");
                factoryCache = new HashMap<>();
            } else {
                factoryCache = factories.stream()
                        .collect(Collectors.toMap(
                                f -> f.factoryName().toUpperCase(),
                                FactoryView::factoryId,
                                (existing, replacement) -> existing
                        ));
                log.info(">>>> [Batch] 공장 데이터 캐싱 완료. 총 개수: {}", factoryCache.size());
            }

        } catch (Exception e) {
            log.error(">>>> [Batch] 공장 정보를 불러오는데 실패했습니다.", e);
            throw new RuntimeException("Factory Data Load Failed", e);
        }
    }
    @Override
    public @NotNull Product process(ProductBatchDto dto) {
        if (productRepository.existsByProductName(dto.getProductName())) {
            log.debug("Skip duplicated product: {}", dto.getProductName());
            return null;
        }

        Long factoryId = factoryCache.get(dto.getFactoryName().toUpperCase());
        if (factoryId == null) {
            throw new IllegalArgumentException("존재하지 않는 공장 이름입니다: " + dto.getFactoryName());
        }

        BigDecimal weight = Optional.ofNullable(dto.getStandardWeight())
                .filter(StringUtils::hasText)
                .map(BigDecimal::new)
                .orElse(BigDecimal.ZERO);

        Product product = Product.builder()
                .factoryId(factoryId)
                .factoryName(dto.getFactoryName())
                .productFactoryName(dto.getProductFactoryName())
                .productName(dto.getProductName())
                .standardWeight(weight)
                .productNote(dto.getProductNote())
                .productDeleted(false)
                .productStones(new ArrayList<>())
                .build();

        if (StringUtils.hasText(dto.getSetTypeName())) {
            setTypeRepository.findBySetTypeNameIgnoreCase(dto.getSetTypeName().trim())
                    .ifPresent(product::setSetType);
        }

        if (StringUtils.hasText(dto.getClassificationName())) {
            classificationRepository.findByClassificationNameIgnoreCase(dto.getClassificationName().trim())
                    .ifPresent(product::setClassification);
        }

        if (StringUtils.hasText(dto.getMaterialName())) {
            materialRepository.findByMaterialNameIgnoreCase(dto.getMaterialName().trim())
                    .ifPresent(product::setMaterial);
        }

        if (dto.getProductStoneDtos() != null) {
            for (ProductBatchDto.BatchStone stoneDto : dto.getProductStoneDtos()) {
                stoneRepository.findByStoneNameIgnoreCase(stoneDto.getStoneName())
                        .ifPresent(stone -> {
                            ProductStone productStone = ProductStone.builder()
                                    .stone(stone)
                                    .mainStone(stoneDto.isMainStone())
                                    .includeStone(stoneDto.isIncludeStone())
                                    .includeQuantity(stoneDto.isIncludeQuantity())
                                    .includePrice(stoneDto.isIncludePrice())
                                    .stoneQuantity(stoneDto.getStoneQuantity())
                                    .productStoneNote(stoneDto.getProductStoneNote())
                                    .build();
                            product.addProductStone(productStone);
                        });
            }
        }

        if (dto.getProductWorkGradePolicyGroupDto() != null) {
            boolean isFirst = true;
            for (ProductBatchDto.BatchPolicyGroup groupDto : dto.getProductWorkGradePolicyGroupDto()) {
                Optional<Color> colorOpt = colorRepository.findByColorNameIgnoreCase(groupDto.getColorName());

                if (colorOpt.isPresent()) {
                    ProductWorkGradePolicyGroup group = ProductWorkGradePolicyGroup.builder()
                            .productPurchasePrice(groupDto.getProductPurchasePrice())
                            .color(colorOpt.get())
                            .gradePolicies(new ArrayList<>())
                            .productWorkGradePolicyGroupDefault(isFirst)
                            .note(groupDto.getNote())
                            .build();

                    if (groupDto.getPolicyDtos() != null) {
                        for (ProductWorkGradePolicyDto policyDto : groupDto.getPolicyDtos()) {
                            ProductWorkGradePolicy policy = ProductWorkGradePolicy.builder()
                                    .grade(policyDto.getGrade())
                                    .laborCost(policyDto.getLaborCost())
                                    .build();
                            group.addGradePolicy(policy);
                        }
                    }
                    product.addPolicyGroup(group);
                    isFirst = false;
                }
            }
        }

        return product;
    }
}