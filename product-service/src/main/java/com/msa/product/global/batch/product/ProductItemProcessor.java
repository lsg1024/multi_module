package com.msa.product.global.batch.product;

import com.msa.product.global.feign_client.client.FactoryClient;
import com.msa.product.local.classification.repository.ClassificationRepository;
import com.msa.product.local.color.entity.Color;
import com.msa.product.local.color.repository.ColorRepository;
import com.msa.product.local.material.repository.MaterialRepository;
import com.msa.product.local.product.dto.FactoryDto;
import com.msa.product.local.product.dto.ProductBatchDto;
import com.msa.product.local.product.dto.ProductWorkGradePolicyDto;
import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.entity.ProductStone;
import com.msa.product.local.product.entity.ProductWorkGradePolicy;
import com.msa.product.local.product.entity.ProductWorkGradePolicyGroup;
import com.msa.product.local.product.repository.ProductRepository;
import com.msa.product.local.set.repository.SetTypeRepository;
import com.msa.product.local.stone.stone.repository.StoneRepository;
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

    private final FactoryClient factoryClient;

    private final ProductRepository productRepository;
    private final SetTypeRepository setTypeRepository;
    private final ClassificationRepository classificationRepository;
    private final MaterialRepository materialRepository;
    private final StoneRepository stoneRepository;
    private final ColorRepository colorRepository;

    private Map<String, Long> factoryCache;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {

        String accessToken = stepExecution.getJobParameters().getString("accessToken");
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException("JobParameters에 'accessToken'이 없습니다. 배치 실행 시 토큰을 전달해주세요.");
        }

        try {

            List<FactoryDto.ResponseBatch> factories = factoryClient.getFactories(accessToken);

            log.info(">>>> [Batch] 공장 개수 = {}", factories.size());

            if (factories.isEmpty()) {
                log.warn(">>>> [Batch] 공장 데이터가 비어있습니다.");
                factoryCache = new HashMap<>();

            } else {
                factoryCache = factories.stream()
                        .collect(Collectors.toMap(
                                FactoryDto.ResponseBatch::getFactoryName,
                                FactoryDto.ResponseBatch::getFactoryId,
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
            setTypeRepository.findBySetTypeName(dto.getSetTypeName().trim())
                    .ifPresent(product::setSetType);
        }

        if (StringUtils.hasText(dto.getClassificationName())) {
            classificationRepository.findByClassificationName(dto.getClassificationName().trim())
                    .ifPresent(product::setClassification);
        }

        if (StringUtils.hasText(dto.getMaterialName())) {
            materialRepository.findByMaterialName(dto.getMaterialName().trim())
                    .ifPresent(product::setMaterial);
        }

        if (dto.getProductStoneDtos() != null) {
            for (ProductBatchDto.BatchStone stoneDto : dto.getProductStoneDtos()) {
                stoneRepository.findByStoneName(stoneDto.getStoneName())
                        .ifPresent(stone -> {
                            ProductStone productStone = ProductStone.builder()
                                    .stone(stone)
                                    .mainStone(stoneDto.isMainStone())
                                    .includeStone(stoneDto.isIncludeStone())
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
                Optional<Color> colorOpt = colorRepository.findByColorName(groupDto.getColorName());

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