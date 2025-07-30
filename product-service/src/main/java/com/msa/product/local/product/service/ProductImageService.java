package com.msa.product.local.product.service;

import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.entity.ProductImage;
import com.msa.product.local.product.repository.image.ProductImageRepository;
import com.msa.product.local.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import static com.msa.product.global.exception.ExceptionMessage.NOT_FOUND;

@Service
@Transactional
public class ProductImageService {

    @Value("${file.base-upload-path}")
    private String baseUploadPath;

    private final ProductRepository productRepository;

    private final ProductImageRepository productImageRepository;

    public ProductImageService(ProductRepository productRepository, ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
    }

    public void uploadProductImages(Long productId, List<MultipartFile> images) {

        Product product = productRepository.findById(productId).orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (images != null && !images.isEmpty()) {
            String productDirPath = baseUploadPath + "/products/" + productId;
            File dir = new File(productDirPath);
            if (!dir.exists()) dir.mkdirs();

            for (MultipartFile file : images) {
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                String imagePath = "/products/" + productId + "/" + fileName;
                try {
                    ProductImage image = ProductImage.builder()
                            .imagePath(imagePath)
                            .imageOriginName(file.getOriginalFilename())
                            .imageName(fileName)
                            .product(product)
                            .build();

                    product.addImage(image);

                    productImageRepository.save(image);

                    Path path = Paths.get(productDirPath, fileName);
                    file.transferTo(path.toFile());
                } catch (IOException e) {
                    throw new RuntimeException("이미지 저장 실패: " + e.getMessage(), e);
                }
            }
        }
    }
}
