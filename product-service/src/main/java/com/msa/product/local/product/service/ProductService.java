package com.msa.product.local.product.service;

import com.msa.product.local.classification.entity.Classification;
import com.msa.product.local.classification.repository.ClassificationRepository;
import com.msa.product.local.material.entity.Material;
import com.msa.product.local.material.repository.MaterialRepository;
import com.msa.product.local.product.controller.AccountClient;
import com.msa.product.local.product.dto.ProductDto;
import com.msa.product.local.product.dto.ProductImageDto;
import com.msa.product.local.product.dto.ProductStoneDto;
import com.msa.product.local.product.dto.ProductWorkGradePolicyDto;
import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.entity.ProductStone;
import com.msa.product.local.product.entity.ProductWorkGradePolicy;
import com.msa.product.local.product.repository.ProductRepository;
import com.msa.product.local.product.repository.image.ProductImageRepository;
import com.msa.product.local.product.repository.stone.ProductStoneRepository;
import com.msa.product.local.product.repository.work_grade_policy.ProductWorkGradePolicyRepository;
import com.msa.product.local.set.entity.SetType;
import com.msa.product.local.set.repository.SetTypeRepository;
import com.msa.product.local.stone.stone.entity.Stone;
import com.msa.product.local.stone.stone.repository.StoneRepository;
import com.msacommon.global.jwt.JwtUtil;
import com.msacommon.global.util.CustomPage;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;
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
    private final AccountClient accountClient;
    private final SetTypeRepository setTypeRepository;
    private final MaterialRepository materialRepository;
    private final ClassificationRepository classificationRepository;
    private final StoneRepository stoneRepository;
    private final ProductRepository productRepository;
    private final ProductStoneRepository productStoneRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductWorkGradePolicyRepository productWorkGradePolicyRepository;

    public ProductService(JwtUtil jwtUtil, AccountClient accountClient, SetTypeRepository setTypeRepository, MaterialRepository materialRepository, ClassificationRepository classificationRepository, StoneRepository stoneRepository, ProductRepository productRepository, ProductStoneRepository productStoneRepository, ProductImageRepository productImageRepository, ProductWorkGradePolicyRepository productWorkGradePolicyRepository) {
        this.jwtUtil = jwtUtil;
        this.accountClient = accountClient;
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
    public Long saveProduct(HttpServletRequest request, ProductDto productDto) {
        boolean existsByProductName = productRepository.existsByProductName(productDto.getProductName());
        if (existsByProductName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        String factoryName = accountClient.validateFactoryId(request, productDto.getFactoryId());
        if (factoryName == null || factoryName.isBlank()) {
            throw new IllegalArgumentException(productDto.getFactoryId() + " " + NOT_FOUND);
        }

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

        extractedProductWorkGradePolicy(productDto, product);
        extractedProductStone(productDto, product);

        Product savedProduct = productRepository.save(product);

        return savedProduct.getProductId();
    }

    //조회
    @Transactional(readOnly = true)
    public ProductDto.Detail getProduct(Long productId) {
        ProductDto.Detail productDetail = productRepository.findByProductId(productId);

        List<ProductStoneDto.Response> productStones = productStoneRepository.findProductStones(productId);
        List<ProductImageDto.Response> images = productImageRepository.findImagesByProductId(productId);
        List<ProductWorkGradePolicyDto.Response> policies = productWorkGradePolicyRepository.findWorkGradePolicyByProductId(productId);

        productDetail.setProductStoneDtos(productStones);
        productDetail.setProductImageDtos(images);
        productDetail.setGradePolicyDtos(policies);

        return productDetail;
    }
    //복수 조회
    @Transactional(readOnly = true)
    public CustomPage<ProductDto.Page> getProducts(String productName, Pageable pageable) {
        return productRepository.findByAllProductName(productName, pageable);
    }

    //수정 - 이미지 수정은 별도
    public void updateProduct(HttpServletRequest request, Long productId, ProductDto.Update updateDto) {
        boolean existsByProductName = productRepository.existsByProductName(updateDto.getProductName());

        if (existsByProductName) {
            throw new IllegalArgumentException(IS_EXIST);
        }

        String factoryName = accountClient.validateFactoryId(request, updateDto.getFactoryId());
        if (factoryName == null || factoryName.isBlank()) {
            throw new IllegalArgumentException(updateDto.getFactoryId() + " " + NOT_FOUND);
        }

        Product product = productRepository.findWithAllOptionsById(productId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        ProductDto productDto = new ProductDto(updateDto);

        product.updateProductInfo(productDto, factoryName);

        if (!product.getSetType().getSetTypeId().equals(Long.valueOf(productDto.getSetType()))) {
            SetType setType = setTypeRepository.getReferenceById(Long.valueOf(productDto.getSetType()));
            product.setSetType(setType);
        }

        if (!product.getMaterial().getMaterialId().equals(Long.valueOf(productDto.getMaterial()))) {
            Material material = materialRepository.getReferenceById(Long.valueOf(productDto.getMaterial()));
            product.setMaterial(material);
        }

        if (!product.getClassification().getClassificationId().equals(Long.valueOf(productDto.getClassification()))) {
            Classification classification = classificationRepository.getReferenceById(Long.valueOf(productDto.getClassification()));
            product.setClassification(classification);
        }

        extractedProductWorkGradePolicy(productDto, product);
        extractedProductStone(productDto, product);
    }

    //삭제
    public void deletedProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        productRepository.delete(product);
    }

    private void extractedProductStone(ProductDto productDto, Product product) {
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
    }

    private static void extractedProductWorkGradePolicy(ProductDto productDto, Product product) {
        List<ProductWorkGradePolicyDto> gradePolicyDtos  = productDto.getGradePolicyDtos();
        for (ProductWorkGradePolicyDto gradePolicyDto : gradePolicyDtos) {
            ProductWorkGradePolicy gradePolicy = ProductWorkGradePolicy.builder()
                    .grade(gradePolicyDto.getGrade())
                    .laborCost(gradePolicyDto.getLaborCost())
                    .build();

            product.addGradePolicy(gradePolicy);
        }
    }


}
