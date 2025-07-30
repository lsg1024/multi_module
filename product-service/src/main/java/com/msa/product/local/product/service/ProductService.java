package com.msa.product.local.product.service;

import com.msa.product.local.classification.dto.ClassificationDto;
import com.msa.product.local.classification.entity.Classification;
import com.msa.product.local.classification.repository.ClassificationRepository;
import com.msa.product.local.material.dto.MaterialDto;
import com.msa.product.local.material.entity.Material;
import com.msa.product.local.material.repository.MaterialRepository;
import com.msa.product.local.product.dto.ProductDto;
import com.msa.product.local.product.dto.ProductImageDto;
import com.msa.product.local.product.dto.ProductStoneDto;
import com.msa.product.local.product.dto.ProductWorkGradePolicyDto;
import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.entity.ProductImage;
import com.msa.product.local.product.entity.ProductStone;
import com.msa.product.local.product.entity.ProductWorkGradePolicy;
import com.msa.product.local.product.repository.ProductRepository;
import com.msa.product.local.product.repository.image.ProductImageRepository;
import com.msa.product.local.product.repository.stone.ProductStoneRepository;
import com.msa.product.local.product.repository.work_grade_policy.ProductWorkGradePolicyRepository;
import com.msa.product.local.set.dto.SetTypeDto;
import com.msa.product.local.set.entity.SetType;
import com.msa.product.local.set.repository.SetTypeRepository;
import com.msa.product.local.stone.stone.dto.StoneWorkGradePolicyDto;
import com.msa.product.local.stone.stone.entity.Stone;
import com.msa.product.local.stone.stone.entity.StoneWorkGradePolicy;
import com.msa.product.local.stone.stone.repository.StoneRepository;
import com.msacommon.global.jwt.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.msa.product.global.exception.ExceptionMessage.IS_EXIST;
import static com.msa.product.global.exception.ExceptionMessage.NOT_FOUND;

@Service
@Transactional
public class ProductService {
    private final JwtUtil jwtUtil;
    private final SetTypeRepository setTypeRepository;
    private final MaterialRepository materialRepository;
    private final ClassificationRepository classificationRepository;
    private final StoneRepository stoneRepository;
    private final ProductRepository productRepository;
    private final ProductStoneRepository productStoneRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductWorkGradePolicyRepository productWorkGradePolicyRepository;

    public ProductService(JwtUtil jwtUtil, SetTypeRepository setTypeRepository, MaterialRepository materialRepository, ClassificationRepository classificationRepository, StoneRepository stoneRepository, ProductRepository productRepository, ProductStoneRepository productStoneRepository, ProductImageRepository productImageRepository, ProductWorkGradePolicyRepository productWorkGradePolicyRepository) {
        this.jwtUtil = jwtUtil;
        this.setTypeRepository = setTypeRepository;
        this.materialRepository = materialRepository;
        this.classificationRepository = classificationRepository;
        this.stoneRepository = stoneRepository;
        this.productRepository = productRepository;
        this.productStoneRepository = productStoneRepository;
        this.productImageRepository = productImageRepository;
        this.productWorkGradePolicyRepository = productWorkGradePolicyRepository;
    }

    //생성
    public Long saveProduct(ProductDto productDto) {
        boolean existsByProductName = productRepository.existsByProductName(productDto.getProductName());
        if (existsByProductName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        BigDecimal weight = Optional.ofNullable(productDto.getStandardWeight())
                .filter(s -> !s.isBlank())
                .map(BigDecimal::new)
                .orElse(BigDecimal.ZERO);

        Product product = Product.builder()
                .factoryId(productDto.getFactoryId())
                .productFactoryName(productDto.getProductFactoryName())
                .productName(productDto.getProductName())
                .standardWeight(weight)
                .productNote(productDto.getProductNote())
                .gradePolicies(new ArrayList<>())
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

        for (ProductWorkGradePolicyDto gradePolicyDto : productDto.getGradePolicyDtos()) {
            ProductWorkGradePolicy gradePolicy = ProductWorkGradePolicy.builder()
                    .grade(gradePolicyDto.getGrade())
                    .laborCost(gradePolicyDto.getLaborCost())
                    .productPolicyNote(gradePolicyDto.getNote())
                    .build();

            product.addGradePolicy(gradePolicy);
        }

        for (ProductStoneDto productStoneDto : productDto.getProductStoneDtos()) {
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

        Product savedProduct = productRepository.save(product);

        return savedProduct.getProductId();
    }

    //조회
    @Transactional(readOnly = true)
    public ProductDto.Detail getProductV1(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        SetTypeDto.ResponseSingle setType = SetTypeDto.ResponseSingle.builder()
                .setTypeId(String.valueOf(product.getSetType().getSetTypeId()))
                .setTypeName(product.getSetType().getSetTypeName())
                .build();

        ClassificationDto.ResponseSingle classification = ClassificationDto.ResponseSingle
                .builder()
                .classificationId(String.valueOf(product.getClassification().getClassificationId()))
                .classificationName(product.getClassification().getClassificationName())
                .build();

        MaterialDto.ResponseSingle material = MaterialDto.ResponseSingle
                .builder()
                .materialId(String.valueOf(product.getMaterial().getMaterialId()))
                .materialName(product.getMaterial().getMaterialName())
                .materialGoldPurityPercent(product.getMaterial().getMaterialGoldPurityPercent().toPlainString())
                .build();

        List<ProductWorkGradePolicyDto.Response> policiesDto = new ArrayList<>();
        List<ProductWorkGradePolicy> gradePolicies = product.getGradePolicies();
        for (ProductWorkGradePolicy gradePolicy : gradePolicies) {
            ProductWorkGradePolicyDto.Response policies = ProductWorkGradePolicyDto.Response.fromEntity(gradePolicy);
            policiesDto.add(policies);
        }

        List<ProductStoneDto.Response> stonesDto = new ArrayList<>();
        List<ProductStone> productStones = product.getProductStones();
        for (ProductStone productStone : productStones) {
            ProductStoneDto.Response response = ProductStoneDto.Response.fromEntity(productStone);
            List<StoneWorkGradePolicyDto.Response> stoneWorkGradePoliciesList = new ArrayList<>();
            for (ProductStone stone : productStones) {
                List<StoneWorkGradePolicy> stoneWorkGradePolicies = stone.getStone().getGradePolicies();
                for (StoneWorkGradePolicy stoneWorkGradePolicy : stoneWorkGradePolicies) {
                    StoneWorkGradePolicyDto.Response stoneWorkGradePolicyDto = StoneWorkGradePolicyDto.Response.fromEntity(stoneWorkGradePolicy);
                    stoneWorkGradePoliciesList.add(stoneWorkGradePolicyDto);
                }
                response.setStoneWorkGradePolicyDtos(stoneWorkGradePoliciesList);
            }
            stonesDto.add(response);
        }
        List<ProductImageDto.Response> productImagesDto = new ArrayList<>();
        List<ProductImage> productImages = product.getProductImages();
        for (ProductImage productImage : productImages) {
            ProductImageDto.Response response = ProductImageDto.Response.fromEntity(productImage);
            productImagesDto.add(response);
        }

        return ProductDto.Detail.builder()
                .productId(String.valueOf(product.getProductId()))
                .factoryId(product.getFactoryId())
                .productFactoryName(product.getProductFactoryName())
                .productName(product.getProductName())
                .standardWeight(product.getStandardWeight().toPlainString())
                .productNote(product.getProductNote())
                .setTypeDto(setType)
                .classificationDto(classification)
                .materialDto(material)
                .gradePolicyDtos(policiesDto)
                .productStoneDtos(stonesDto)
                .productImageDtos(productImagesDto)
                .build();
    }

    @Transactional(readOnly = true)
    public ProductDto.Detail getProductV2(Long productId) {
        ProductDto.Detail productDetail = productRepository.findByProductId(productId);

        List<ProductStoneDto.Response> productStones = productStoneRepository.findProductStones(productId);
        List<ProductImageDto.Response> images = productImageRepository.findImagesByProductId(productId);
        List<ProductWorkGradePolicyDto.Response> policies = productWorkGradePolicyRepository.findWorkGradePolicyByProductId(productId);

        productDetail.setProductStoneDtos(productStones);
        productDetail.setProductImageDtos(images);
        productDetail.setGradePolicyDtos(policies);

        return productDetail;
    }

    @Transactional(readOnly = true)
    public List<ProductStoneDto.Response> getProductStone(Long productId) {
        return productStoneRepository.findProductStones(productId);
    }
    //수정

    //삭제
}
