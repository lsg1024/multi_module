package com.msa.product.local.product.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.product.global.feign_client.client.FactoryClient;
import com.msa.product.local.classification.entity.Classification;
import com.msa.product.local.classification.repository.ClassificationRepository;
import com.msa.product.local.color.entity.Color;
import com.msa.product.local.color.repository.ColorRepository;
import com.msa.product.local.goldPrice.entity.Gold;
import com.msa.product.local.goldPrice.repository.GoldRepository;
import com.msa.product.local.grade.WorkGrade;
import com.msa.product.local.material.entity.Material;
import com.msa.product.local.material.repository.MaterialRepository;
import com.msa.product.local.product.dto.*;
import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.entity.ProductStone;
import com.msa.product.local.product.entity.ProductWorkGradePolicy;
import com.msa.product.local.product.entity.ProductWorkGradePolicyGroup;
import com.msa.product.local.product.repository.ProductRepository;
import com.msa.product.local.product.repository.image.ProductImageRepository;
import com.msa.product.local.product.repository.stone.ProductStoneRepository;
import com.msa.product.local.product.repository.work_grade_policy_group.CustomProductWorkGradePolicyGroup;
import com.msa.product.local.product.repository.work_grade_policy_group.ProductWorkGradePolicyGroupRepository;
import com.msa.product.local.set.entity.SetType;
import com.msa.product.local.set.repository.SetTypeRepository;
import com.msa.product.local.stone.stone.entity.Stone;
import com.msa.product.local.stone.stone.repository.StoneRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.msa.product.global.exception.ExceptionMessage.*;

@Slf4j
@Service
@Transactional
public class ProductService {
    private final JwtUtil jwtUtil;
    private final FactoryClient factoryClient;
    private final SetTypeRepository setTypeRepository;
    private final MaterialRepository materialRepository;
    private final ClassificationRepository classificationRepository;
    private final ColorRepository colorRepository;
    private final StoneRepository stoneRepository;
    private final ProductRepository productRepository;
    private final GoldRepository goldRepository;
    private final ProductStoneRepository productStoneRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductWorkGradePolicyGroupRepository productWorkGradePolicyGroupRepository;
    private final CustomProductWorkGradePolicyGroup customProductWorkGradePolicyGroupRepository;

    public ProductService(JwtUtil jwtUtil, FactoryClient factoryClient, SetTypeRepository setTypeRepository, MaterialRepository materialRepository, ClassificationRepository classificationRepository, ColorRepository colorRepository, StoneRepository stoneRepository, ProductRepository productRepository, GoldRepository goldRepository, ProductStoneRepository productStoneRepository, ProductImageRepository productImageRepository, ProductWorkGradePolicyGroupRepository productWorkGradePolicyGroupRepository, CustomProductWorkGradePolicyGroup customProductWorkGradePolicyGroupRepository) {
        this.jwtUtil = jwtUtil;
        this.factoryClient = factoryClient;
        this.setTypeRepository = setTypeRepository;
        this.materialRepository = materialRepository;
        this.classificationRepository = classificationRepository;
        this.colorRepository = colorRepository;
        this.stoneRepository = stoneRepository;
        this.productRepository = productRepository;
        this.goldRepository = goldRepository;
        this.productStoneRepository = productStoneRepository;
        this.productImageRepository = productImageRepository;
        this.productWorkGradePolicyGroupRepository = productWorkGradePolicyGroupRepository;
        this.customProductWorkGradePolicyGroupRepository = customProductWorkGradePolicyGroupRepository;
    }

    //생성
    public Long saveProduct(String token, ProductDto productDto) {
        boolean existsByProductName = productRepository.existsByProductName(productDto.getProductName());

        if (existsByProductName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        String factoryName = validFactory(token, productDto.getFactoryId());

        BigDecimal weight = Optional.ofNullable(productDto.getStandardWeight())
                .filter(s -> !s.isBlank())
                .map(BigDecimal::new)
                .orElse(BigDecimal.ZERO);

        Product product = Product.builder()
                .factoryId(productDto.getFactoryId())
                .factoryName(factoryName)
                .productFactoryName(productDto.getProductFactoryName())
                .productName(productDto.getProductName())
                .standardWeight(weight)
                .productNote(productDto.getProductNote())
                .productRelatedNumber(productDto.getProductRelatedNumber())
                .productDeleted(false)
                .productStones(new ArrayList<>())
                .productImages(new ArrayList<>())
                .build();

        if (StringUtils.hasText(productDto.getSetType())) {
            Long setTypeId = Long.valueOf(productDto.getSetType().trim());
            SetType setType = setTypeRepository.findById(setTypeId)
                    .orElseThrow(() -> new IllegalArgumentException("setType: " + NOT_FOUND));
            product.setSetType(setType);
        }

        if (StringUtils.hasText(productDto.getClassification())) {
            Long classificationId = Long.valueOf(productDto.getClassification().trim());
            Classification c = classificationRepository.findById(classificationId)
                    .orElseThrow(() -> new IllegalArgumentException("classification: " + NOT_FOUND));
            product.setClassification(c);
        }

        if (StringUtils.hasText(productDto.getMaterial())) {
            Long materialId = Long.valueOf(productDto.getMaterial().trim());
            Material m = materialRepository.findById(materialId)
                    .orElseThrow(() -> new IllegalArgumentException("material: " + NOT_FOUND));
            product.setMaterial(m);
        }

        List<ProductStoneDto> productStoneDtos = productDto.getProductStoneDtos();
        for (ProductStoneDto productStoneDto : productStoneDtos) {
            Long stoneId = Long.valueOf(productStoneDto.getStoneId());

            Stone stone = stoneRepository.findById(stoneId)
                    .orElseThrow(() -> new IllegalArgumentException("stone: " + NOT_FOUND));

            ProductStone productStone = ProductStone.builder()
                    .stone(stone)
                    .mainStone(productStoneDto.isMainStone())
                    .includeStone(productStoneDto.isIncludeStone())
                    .includeQuantity(productStoneDto.isIncludeQuantity())
                    .includePrice(productStoneDto.isIncludePrice())
                    .stoneQuantity(productStoneDto.getStoneQuantity())
                    .productStoneNote(productStoneDto.getProductStoneNote())
                    .build();

            product.addProductStone(productStone);
        }


        List<ProductWorkGradePolicyGroupDto> groupDtos = productDto.getProductWorkGradePolicyGroupDto();

        boolean isFirst = true;

        for (ProductWorkGradePolicyGroupDto groupDto : groupDtos) {
            Long colorId = Long.valueOf(groupDto.getColorId());
            Color color = colorRepository.findById(colorId)
                    .orElseThrow(() -> new IllegalArgumentException("color: " + NOT_FOUND));

            ProductWorkGradePolicyGroup groups = ProductWorkGradePolicyGroup.builder()
                    .productPurchasePrice(groupDto.getProductPurchasePrice())
                    .color(color)
                    .gradePolicies(new ArrayList<>())
                    .productWorkGradePolicyGroupDefault(isFirst)
                    .note(groupDto.getNote())
                    .build();

            for (ProductWorkGradePolicyDto policyDto : groupDto.getPolicyDtos()) {
                ProductWorkGradePolicy policy = ProductWorkGradePolicy.builder()
                        .grade(policyDto.getGrade())
                        .laborCost(policyDto.getLaborCost())
                        .build();
                groups.addGradePolicy(policy);
            }
            product.addPolicyGroup(groups);
            isFirst = false;
        }

        Product save = productRepository.save(product);
        return save.getProductId();
    }

    //조회
    @Transactional(readOnly = true)
    public ProductDto.Detail getProduct(Long productId) {
        ProductDto.Detail productDetail = productRepository.findByProductId(productId);

        List<ProductStoneDto.Response> productStones = productStoneRepository.findProductStones(productId);
        List<ProductImageDto.Response> images = productImageRepository.findImagesByProductId(productId);
        List<ProductWorkGradePolicyGroupDto.Response> group = customProductWorkGradePolicyGroupRepository.findByWorkGradePolicyGroupByProductIdOrderById(productId);

        productDetail.setProductStoneDtos(productStones);
        productDetail.setProductImageDtos(images);
        productDetail.setProductWorkGradePolicyGroupDto(group);

        return productDetail;
    }
    //복수 조회
    @Transactional(readOnly = true)
    public CustomPage<ProductDto.Page> getProducts(String search, String searchField, String searchMin, String searchMax, String sortField, String sortOrder, String grade,
                                                    String setTypeFilter, String classificationFilter, String factoryFilter, Pageable pageable) {

        CustomPage<ProductDto.Page> productList = productRepository.findByAllProductName(search, searchField, searchMin, searchMax, grade, sortField, sortOrder, setTypeFilter, classificationFilter, factoryFilter, pageable);

        Integer latestGoldPrice = goldRepository.findTopByOrderByGoldIdDesc()
                .map(Gold::getGoldPrice)
                .orElse(0);

        productList.getContent().forEach(dto -> dto.updateGoldPrice(latestGoldPrice));

        return productList;
    }

    //수정 - 이미지 수정은 별도
    public void updateProduct(String accessToken, Long productId, ProductDto.Update updateDto) {

        validProductSameName(updateDto.getProductName(), productId);

        String factoryName = validFactory(accessToken, updateDto.getFactoryId());

        Product product = productRepository.findWithAllOptionsById(productId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        product.updateProductInfo(updateDto, factoryName);

        if (!product.getSetType().getSetTypeId().equals(Long.valueOf(updateDto.getSetType()))) {
            SetType setType = setTypeRepository.getReferenceById(Long.valueOf(updateDto.getSetType()));
            product.setSetType(setType);
        }

        if (!product.getMaterial().getMaterialId().equals(Long.valueOf(updateDto.getMaterial()))) {
            Material material = materialRepository.getReferenceById(Long.valueOf(updateDto.getMaterial()));
            product.setMaterial(material);
        }

        if (!product.getClassification().getClassificationId().equals(Long.valueOf(updateDto.getClassification()))) {
            Classification classification = classificationRepository.getReferenceById(Long.valueOf(updateDto.getClassification()));
            product.setClassification(classification);
        }

        extractedProductColorWorkGradePolicy(updateDto, product);
        extractedProductStone(updateDto, product);
    }

    //삭제
    public void deletedProduct(String accessToken, Long productId) {
        String role = jwtUtil.getRole(accessToken);

        if (!role.equals("ADMIN")) {
            throw new IllegalArgumentException(NOT_ACCESS);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        productRepository.delete(product);
    }
    private void validProductSameName(String updateDto, Long productId) {
        boolean existsByProductName = productRepository.existsByProductNameAndProductIdNot(updateDto, productId);

        if (existsByProductName) {
            throw new IllegalArgumentException(IS_EXIST);
        }
    }

    private String validFactory(String token, Long productDto) {
        FactoryDto.Response factoryInfo = factoryClient.getFactoryInfo(token, productDto);
        if (factoryInfo.getFactoryName() == null || factoryInfo.getFactoryName().isBlank()) {
            throw new IllegalArgumentException(productDto + " " + NOT_FOUND);
        }
        return factoryInfo.getFactoryName();
    }

    private void extractedProductStone(ProductDto.Update productDto, Product product) {
        List<ProductStoneDto.Request> reqs =
                productDto.getProductStoneDtos() != null ? productDto.getProductStoneDtos() : Collections.emptyList();

        List<Long> existingIds = reqs.stream()
                .map(ProductStoneDto.Request::getProductStoneId)
                .filter(this::isNumericPositive)
                .map(Long::parseLong)
                .toList();

        Map<Long, ProductStone> existingMap = existingIds.isEmpty()
                ? Collections.emptyMap()
                : productStoneRepository.findByProductStoneIds(existingIds).stream()
                .collect(Collectors.toMap(ProductStone::getProductStoneId, Function.identity()));

        Set<Long> keepIds = new HashSet<>();

        // 2) 요청 하나씩 처리
        for (ProductStoneDto.Request r : reqs) {
            String idStr = r.getProductStoneId();

            if (isNumericPositive(idStr)) {
                // ---- 기존 행 업데이트 ----
                long psId = Long.parseLong(idStr);
                ProductStone ps = existingMap.get(psId);
                if (ps == null) {
                    continue;
                }

                long stoneId = parseLongRequired(r.getStoneId()); // 필수값
                if (!Objects.equals(ps.getStone().getStoneId(), stoneId)) {
                    Stone stone = stoneRepository.findById(stoneId)
                            .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
                    ps.setStone(stone);
                }
                ps.updateStone(r);
                keepIds.add(psId);

            } else {
                // ---- 신규 행 생성 ----
                long stoneId = parseLongRequired(r.getStoneId()); // 필수값
                Stone stone = stoneRepository.findById(stoneId)
                        .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

                ProductStone productStone = ProductStone.builder()
                        .stone(stone)
                        .mainStone(r.isMainStone())
                        .includeStone(r.isIncludeStone())
                        .includeQuantity(r.isIncludeQuantity())
                        .includePrice(r.isIncludePrice())
                        .stoneQuantity(r.getStoneQuantity())
                        .productStoneNote(r.getProductStoneNote())
                        .build();

                product.addProductStone(productStone);
            }
        }

        Set<Long> requestedExistingIds = reqs.stream()
                .map(ProductStoneDto.Request::getProductStoneId)
                .filter(this::isNumericPositive)
                .map(Long::parseLong)
                .collect(Collectors.toSet());

        List<Long> currentExistingIds = product.getProductStones().stream()
                .map(ProductStone::getProductStoneId)
                .filter(Objects::nonNull)
                .toList();

        List<Long> toDelete = currentExistingIds.stream()
                .filter(id -> !requestedExistingIds.contains(id))
                .toList();

        if (!toDelete.isEmpty()) {
            productStoneRepository.deleteAllByIdInBatch(toDelete);
        }
    }

    private void extractedProductColorWorkGradePolicy(ProductDto.Update productDto, Product product) {
        List<ProductWorkGradePolicyGroupDto.Request> reqs =
                productDto.getProductWorkGradePolicyGroupDto() != null
                        ? productDto.getProductWorkGradePolicyGroupDto() : Collections.emptyList();

        // 기존 그룹 ID 수집
        List<Long> existingIds = reqs.stream()
                .map(ProductWorkGradePolicyGroupDto.Request::getProductGroupId)
                .filter(this::isNumericPositive)
                .map(Long::parseLong)
                .toList();

        Map<Long, ProductWorkGradePolicyGroup> existingMap = existingIds.isEmpty()
                ? Collections.emptyMap()
                : productWorkGradePolicyGroupRepository
                .findAllWithDetailsByGroupIds(existingIds, product.getProductId()).stream()
                .collect(Collectors.toMap(
                        ProductWorkGradePolicyGroup::getProductWorkGradePolicyGroupId,
                        g -> g
                ));

        Set<Long> keepIds = new HashSet<>();

        for (ProductWorkGradePolicyGroupDto.Request dto : reqs) {
            String idStr = dto.getProductGroupId();

            if (isNumericPositive(idStr)) {
                // ---- 기존 그룹 업데이트 ----
                long groupId = Long.parseLong(idStr);
                ProductWorkGradePolicyGroup group = existingMap.get(groupId);
                if (group == null) continue;

                Color originColor = group.getColor();
                if (!originColor.getColorId().equals(Long.valueOf(dto.getColorId()))) {
                    Color color = colorRepository.findById(Long.valueOf(dto.getColorId()))
                            .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
                    group.setColor(color);
                }
                group.updateProductPurchasePrice(dto.getProductPurchasePrice(), dto.getNote());
                updatePolicies(group, dto.getPolicyDtos());
                keepIds.add(groupId);

            } else {
                Color color = colorRepository.findById(Long.valueOf(dto.getColorId()))
                        .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

                ProductWorkGradePolicyGroup newGroup = ProductWorkGradePolicyGroup.builder()
                        .productPurchasePrice(dto.getProductPurchasePrice())
                        .color(color)
                        .gradePolicies(new ArrayList<>())
                        .productWorkGradePolicyGroupDefault(false)
                        .note(dto.getNote())
                        .build();

                for (ProductWorkGradePolicyDto.Request policyDto : dto.getPolicyDtos()) {
                    ProductWorkGradePolicy policy = ProductWorkGradePolicy.builder()
                            .grade(policyDto.getGrade())
                            .laborCost(policyDto.getLaborCost())
                            .build();
                    newGroup.addGradePolicy(policy);
                }
                product.addPolicyGroup(newGroup);
            }
        }

        // ---- 삭제된 그룹 제거 (orphanRemoval) ----
        product.getProductWorkGradePolicyGroups()
                .removeIf(g -> g.getProductWorkGradePolicyGroupId() != null
                        && !keepIds.contains(g.getProductWorkGradePolicyGroupId()));
    }

    private boolean isNumericPositive(String s) {
        if (s == null || s.isBlank()) return false;
        try {
            return Long.parseLong(s) > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private long parseLongRequired(String s) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException("ID_REQUIRED");
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID_INVALID");
        }
    }

    private void updatePolicies(ProductWorkGradePolicyGroup group, List<ProductWorkGradePolicyDto.Request> policyDtos) {

        Map<Long, ProductWorkGradePolicy> entityPolicyMap = group.getGradePolicies().stream()
                .collect(Collectors.toMap(
                        ProductWorkGradePolicy::getProductWorkGradePolicyId,
                        p -> p
                ));

        // grade 기반 매핑 (임시 ID가 올 경우 grade로 매칭)
        Map<String, ProductWorkGradePolicy> gradePolicyMap = group.getGradePolicies().stream()
                .collect(Collectors.toMap(
                        p -> p.getGrade().name(),
                        p -> p,
                        (a, b) -> a
                ));

        for (ProductWorkGradePolicyDto.Request dto : policyDtos) {
            String policyIdStr = dto.getWorkGradePolicyId();
            ProductWorkGradePolicy policy = null;

            // 숫자 ID인 경우 기존 정책 조회
            if (isNumericPositive(policyIdStr)) {
                policy = entityPolicyMap.get(Long.valueOf(policyIdStr));
            }

            // ID로 못 찾으면 grade 기반으로 매칭 (프론트에서 임시 ID 보낸 경우)
            if (policy == null && dto.getGrade() != null) {
                policy = gradePolicyMap.get(dto.getGrade());
            }

            if (policy != null) {
                policy.updateWorkGradePolicyDto(dto);
            } else {
                // 새 등급 정책 추가
                ProductWorkGradePolicy newPolicy = ProductWorkGradePolicy.builder()
                        .grade(dto.getGrade())
                        .laborCost(dto.getLaborCost())
                        .build();
                group.addGradePolicy(newPolicy);
            }
        }
    }

    @Transactional(readOnly = true)
    public ProductDetailDto getProductInfoByName(String productName) {
        Product product = productRepository.findByProductName(productName)
                .orElse(null);

        if (product == null) {
            return null;
        }

        Long classificationId = product.getClassification() != null ? product.getClassification().getClassificationId() : null;
        String classificationName = product.getClassification() != null ? product.getClassification().getClassificationName() : null;
        Long setTypeId = product.getSetType() != null ? product.getSetType().getSetTypeId() : null;
        String setTypeName = product.getSetType() != null ? product.getSetType().getSetTypeName() : null;

        return new ProductDetailDto(
                product.getProductId(),
                product.getProductName(),
                product.getProductFactoryName(),
                classificationId,
                classificationName,
                setTypeId,
                setTypeName,
                null,
                null
        );
    }

    /**
     * 상품명으로 해당 상품의 스톤 목록 조회 (마이그레이션용).
     * 상품이 없거나 스톤이 없으면 빈 리스트 반환.
     */
    @Transactional(readOnly = true)
    public List<ProductDetailDto.StoneInfo> getProductStonesByName(String productName) {
        Product product = productRepository.findByProductNameIgnoreCase(productName)
                .orElse(null);

        if (product == null) {
            return List.of();
        }

        List<ProductStoneDto.Response> stones = productStoneRepository.findProductStones(product.getProductId());

        return stones.stream()
                .map(s -> new ProductDetailDto.StoneInfo(
                        s.getStoneId(),
                        s.getStoneName(),
                        s.getStoneWeight() != null ? s.getStoneWeight().toPlainString() : null,
                        s.getStonePurchase(),
                        null, // laborCost — grade별 정책에 따라 다르므로 null
                        s.getStoneQuantity(),
                        s.isMainStone(),
                        s.isIncludeStone(),
                        s.isIncludeQuantity(),
                        s.isIncludePrice(),
                        s.getProductStoneNote()
                ))
                .toList();
    }

    public void updateProductFactoryName(Long productId, String productFactoryName) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
        product.updateProductFactoryName(productFactoryName);
    }

    public ProductDetailDto getProductInfo(Long id, String grade) {
        WorkGrade t_grade = Arrays.stream(WorkGrade.values())
                .filter(g -> g.getLevel().equals(grade))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        return productRepository.findProductDetail(id, t_grade);
    }

    /**
     * 관련번호로 관련 상품 목록 조회
     */
    @Transactional(readOnly = true)
    public List<ProductDto.RelatedProduct> getRelatedProducts(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        String relatedNumber = product.getProductRelatedNumber();
        if (relatedNumber == null || relatedNumber.isBlank()) {
            return Collections.emptyList();
        }

        return productRepository.findRelatedProducts(productId, relatedNumber);
    }
}
