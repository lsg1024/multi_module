package com.msa.product.local.product.service;

import com.msa.common.global.jwt.JwtUtil;
import com.msa.common.global.util.CustomPage;
import com.msa.product.local.classification.entity.Classification;
import com.msa.product.local.classification.repository.ClassificationRepository;
import com.msa.product.local.color.entity.Color;
import com.msa.product.local.color.repository.ColorRepository;
import com.msa.product.local.material.entity.Material;
import com.msa.product.local.material.repository.MaterialRepository;
import com.msa.product.local.product.controller.AccountClient;
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
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.msa.product.global.exception.ExceptionMessage.*;

@Service
@Transactional
public class ProductService {
    private final JwtUtil jwtUtil;
    private final AccountClient accountClient;
    private final SetTypeRepository setTypeRepository;
    private final MaterialRepository materialRepository;
    private final ClassificationRepository classificationRepository;
    private final ColorRepository colorRepository;
    private final StoneRepository stoneRepository;
    private final ProductRepository productRepository;
    private final ProductStoneRepository productStoneRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductWorkGradePolicyGroupRepository productWorkGradePolicyGroupRepository;
    private final CustomProductWorkGradePolicyGroup customProductWorkGradePolicyGroupRepository;

    public ProductService(JwtUtil jwtUtil, AccountClient accountClient, SetTypeRepository setTypeRepository, MaterialRepository materialRepository, ClassificationRepository classificationRepository, ColorRepository colorRepository, StoneRepository stoneRepository, ProductRepository productRepository, ProductStoneRepository productStoneRepository, ProductImageRepository productImageRepository, ProductWorkGradePolicyGroupRepository productWorkGradePolicyGroupRepository, CustomProductWorkGradePolicyGroup customProductWorkGradePolicyGroupRepository) {
        this.jwtUtil = jwtUtil;
        this.accountClient = accountClient;
        this.setTypeRepository = setTypeRepository;
        this.materialRepository = materialRepository;
        this.classificationRepository = classificationRepository;
        this.colorRepository = colorRepository;
        this.stoneRepository = stoneRepository;
        this.productRepository = productRepository;
        this.productStoneRepository = productStoneRepository;
        this.productImageRepository = productImageRepository;
        this.productWorkGradePolicyGroupRepository = productWorkGradePolicyGroupRepository;
        this.customProductWorkGradePolicyGroupRepository = customProductWorkGradePolicyGroupRepository;
    }

    //생성
    public Long saveProduct(HttpServletRequest request, ProductDto productDto) {
        boolean existsByProductName = productRepository.existsByProductName(productDto.getProductName());

        if (existsByProductName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        String factoryName = validFactory(request, productDto.getFactoryId());

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
                .productDeleted(false)
                .productStones(new ArrayList<>())
                .productImages(new ArrayList<>())
                .build();

        if (productDto.getSetType() != null) {
            Long setTypeId = Long.valueOf(productDto.getSetType());
            SetType setType = setTypeRepository.findById(setTypeId)
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
            product.setSetType(setType);
        }

        if (productDto.getClassification() != null) {
            Long classificationId = Long.valueOf(productDto.getClassification());
            Classification classification = classificationRepository.findById(classificationId)
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
            product.setClassification(classification);
        }

        if (productDto.getMaterial() != null) {
            Long materialId = Long.valueOf(productDto.getMaterial());
            Material material = materialRepository.findById(materialId)
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
            product.setMaterial(material);
        }

        List<ProductStoneDto> productStoneDtos = productDto.getProductStoneDtos();
        for (ProductStoneDto productStoneDto : productStoneDtos) {
            Long stoneId = Long.valueOf(productStoneDto.getStoneId());

            Stone stone = stoneRepository.findById(stoneId)
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

            ProductStone productStone = ProductStone.builder()
                    .stone(stone)
                    .productStoneMain(productStoneDto.isProductStoneMain())
                    .includeQuantity(productStoneDto.isIncludeQuantity())
                    .includeWeight(productStoneDto.isIncludeWeight())
                    .includeLabor(productStoneDto.isIncludeLabor())
                    .stoneQuantity(productStoneDto.getStoneQuantity())
                    .build();

            product.addProductStone(productStone);
        }


        List<ProductWorkGradePolicyGroupDto> groupDtos = productDto.getProductWorkGradePolicyGroupDto();

        boolean isFirst = true;

        for (ProductWorkGradePolicyGroupDto groupDto : groupDtos) {
            Long colorId = Long.valueOf(groupDto.getColorId());
            Color color = colorRepository.findById(colorId)
                    .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

            ProductWorkGradePolicyGroup groups = ProductWorkGradePolicyGroup.builder()
                    .color(color)
                    .gradePolicies(new ArrayList<>())
                    .productWorkGradePolicyGroupDefault(isFirst)
                    .build();

            for (ProductWorkGradePolicyDto policyDto : groupDto.getPolicyDtos()) {
                ProductWorkGradePolicy policy = ProductWorkGradePolicy.builder()
                        .grade(policyDto.getGrade())
                        .laborCost(policyDto.getLaborCost())
                        .productPolicyNote(policyDto.getNote())
                        .build();
                groups.addGradePolicy(policy);
            }
            product.addPolicyGroup(groups);
            isFirst = false;
        }

        Product savedProduct = productRepository.save(product);

        return savedProduct.getProductId();
    }

    //조회
    @Transactional(readOnly = true)
    public ProductDto.Detail getProduct(Long productId) {
        ProductDto.Detail productDetail = productRepository.findByProductId(productId);

        List<ProductStoneDto.Response> productStones = productStoneRepository.findProductStones(productId);
        List<ProductImageDto.Response> images = productImageRepository.findImagesByProductId(productId);
        List<ProductWorkGradePolicyGroupDto.Response> group = customProductWorkGradePolicyGroupRepository.findByWorkGradePolicyGroupByProductId(productId);

        productDetail.setProductStoneDtos(productStones);
        productDetail.setProductImageDtos(images);
        productDetail.setProductWorkGradePolicyGroupDto(group);

        return productDetail;
    }
    //복수 조회
    @Transactional(readOnly = true)
    public CustomPage<ProductDto.Page> getProducts(String productName, Pageable pageable) {
        return productRepository.findByAllProductName(productName, pageable);
    }

    //수정 - 이미지 수정은 별도
    public void updateProduct(HttpServletRequest request, Long productId, ProductDto.Update updateDto) {
        validProductSameName(updateDto.getProductName(), productId);

        String factoryName = validFactory(request, updateDto.getFactoryId());

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

    private String validFactory(HttpServletRequest request, Long productDto) {
        String factoryName = accountClient.getFactoryInfo(request, productDto);
        if (factoryName == null || factoryName.isBlank()) {
            throw new IllegalArgumentException(productDto + " " + NOT_FOUND);
        }
        return factoryName;
    }

    private void extractedProductStone(ProductDto.Update productDto, Product product) {
        List<Long> groupIds = productDto.getProductStoneDtos().stream()
                .map(dto -> Long.valueOf(dto.getProductStoneId()))
                .toList();

        List<ProductStone> groups = productStoneRepository
                .findByProductStoneIds(groupIds);

        Map<Long, ProductStone> groupMap = groups.stream()
                .collect(Collectors.toMap(
                        ProductStone::getProductStoneId,
                        p -> p
                ));

        for (ProductStoneDto.Request productStoneDto : productDto.getProductStoneDtos()) {
            Long productStoneId = Long.valueOf(productStoneDto.getProductStoneId());
            ProductStone productStone = groupMap.get(productStoneId);

            Long newStoneId = Long.valueOf(productStoneDto.getStoneId());
            if (!productStone.getStone().getStoneId().equals(newStoneId)) {
                Stone stone = stoneRepository.findById(newStoneId)
                        .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
                productStone.setStone(stone);
            }
            productStone.updateStone(productStoneDto);
        }
    }

    private void extractedProductColorWorkGradePolicy(ProductDto.Update productDto, Product product) {
        List<Long> groupIds = productDto.getProductWorkGradePolicyGroupDto().stream()
                .map(dto -> Long.valueOf(dto.getProductGroupId()))
                .collect(Collectors.toList());

        List<ProductWorkGradePolicyGroup> groups = productWorkGradePolicyGroupRepository
                .findAllWithDetailsByGroupIds(groupIds, product.getProductId());

        Map<Long, ProductWorkGradePolicyGroup> groupMap = groups.stream()
                .collect(Collectors.toMap(
                        ProductWorkGradePolicyGroup::getProductWorkGradePolicyGroupId,
                        g -> g
                ));

        for (ProductWorkGradePolicyGroupDto.Request dto : productDto.getProductWorkGradePolicyGroupDto()) {
            Long groupId = Long.valueOf(dto.getProductGroupId());
            ProductWorkGradePolicyGroup group = groupMap.get(groupId);

            Color originColor = group.getColor();
            if (!originColor.getColorId().equals(Long.valueOf(dto.getColorId()))) {
                Color color = colorRepository.findById(Long.valueOf(dto.getColorId()))
                        .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
                group.setColor(color);
            }
            updatePolicies(group, dto.getGradePolicyDtos());
        }
    }

    private void updatePolicies(ProductWorkGradePolicyGroup group, List<ProductWorkGradePolicyDto.Request> policyDtos) {

        Map<Long, ProductWorkGradePolicy> entityPolicyMap = group.getGradePolicies().stream()
                .collect(Collectors.toMap(
                        ProductWorkGradePolicy::getProductWorkGradePolicyId,
                        p -> p
                ));

        for (ProductWorkGradePolicyDto.Request dto : policyDtos) {
            Long policyId = Long.valueOf(dto.getWorkGradePolicyId());
            ProductWorkGradePolicy policy = entityPolicyMap.get(policyId);
            policy.updateWorkGradePolicyDto(dto);
        }
    }

    public String getProductName(Long id) {
        return productRepository.findByProductName(id);
    }
}
