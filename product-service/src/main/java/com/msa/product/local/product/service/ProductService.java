package com.msa.product.local.product.service;

import com.msa.product.local.classification.entity.Classification;
import com.msa.product.local.classification.repository.ClassificationRepository;
import com.msa.product.local.material.entity.Material;
import com.msa.product.local.material.repository.MaterialRepository;
import com.msa.product.local.product.dto.ProductDto;
import com.msa.product.local.product.dto.ProductStoneDto;
import com.msa.product.local.product.dto.ProductWorkGradePolicyDto;
import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.entity.ProductImage;
import com.msa.product.local.product.entity.ProductStone;
import com.msa.product.local.product.entity.ProductWorkGradePolicy;
import com.msa.product.local.product.repository.ProductImageRepository;
import com.msa.product.local.product.repository.ProductRepository;
import com.msa.product.local.set.entity.SetType;
import com.msa.product.local.set.repository.SetTypeRepository;
import com.msa.product.local.stone.stone.entity.Stone;
import com.msa.product.local.stone.stone.repository.StoneRepository;
import com.msacommon.global.jwt.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.msa.product.global.exception.ExceptionMessage.IS_EXIST;
import static com.msa.product.global.exception.ExceptionMessage.NOT_FOUND;

@Service
@Transactional
public class ProductService {

    @Value("${file.base-upload-path}")
    private String baseUploadPath;
    private final JwtUtil jwtUtil;
    private final SetTypeRepository setTypeRepository;
    private final MaterialRepository materialRepository;
    private final ClassificationRepository classificationRepository;
    private final StoneRepository stoneRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    public ProductService(JwtUtil jwtUtil, SetTypeRepository setTypeRepository, MaterialRepository materialRepository, ClassificationRepository classificationRepository, StoneRepository stoneRepository, ProductRepository productRepository, ProductImageRepository productImageRepository) {
        this.jwtUtil = jwtUtil;
        this.setTypeRepository = setTypeRepository;
        this.materialRepository = materialRepository;
        this.classificationRepository = classificationRepository;
        this.stoneRepository = stoneRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
    }

    //생성
    public void saveProduct(ProductDto productDto, List<MultipartFile> images) {
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
                    .includeQuantity(productStoneDto.isIncludeQuantity())
                    .includeWeight(productStoneDto.isIncludeWeight())
                    .includeLabor(productStoneDto.isIncludeLabor())
                    .stoneQuantity(productStoneDto.getStoneQuantity())
                    .build();

            product.addProductStone(productStone);
        }

        Product savedProduct = productRepository.save(product);

        if (images != null && !images.isEmpty()) {
            String productDirPath = baseUploadPath + "/products/" + savedProduct.getProductId();
            File dir = new File(productDirPath);
            if (!dir.exists()) dir.mkdirs();

            for (MultipartFile file : images) {
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                String imagePath = "/products/" + savedProduct.getProductId() + "/" + fileName;
                try {
                    Path path = Paths.get(productDirPath, fileName);
                    file.transferTo(path.toFile());
                } catch (IOException e) {
                    throw new RuntimeException("이미지 저장 실패: " + e.getMessage(), e);
                }

                ProductImage image = ProductImage.builder()
                        .imagePath(imagePath)
                        .imageOriginName(file.getOriginalFilename())
                        .imageName(fileName)
                        .product(savedProduct)
                        .build();

                savedProduct.addImage(image);

                productImageRepository.save(image);
            }
        }
    }

    //조회

    //수정

    //삭제
}
