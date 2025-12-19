package com.msa.product.local.product.service;

import com.msa.common.global.tenant.TenantContext;
import com.msa.product.local.product.dto.ProductImageDto;
import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.entity.ProductImage;
import com.msa.product.local.product.repository.ProductRepository;
import com.msa.product.local.product.repository.image.ProductImageRepository;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static com.msa.product.global.exception.ExceptionMessage.NOT_FOUND;

@Slf4j
@Service
@Transactional
public class ProductImageService {

    @Value("${FILE_UPLOAD_PATH}")
    private String baseUploadPath;

    private final ProductRepository productRepository;

    private final ProductImageRepository productImageRepository;

    public ProductImageService(ProductRepository productRepository, ProductImageRepository productImageRepository) {
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
    }

    public void bulkUploadByFileName(List<MultipartFile> files) {
        String tenant = TenantContext.getTenant();

        for (MultipartFile file : files) {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null) continue;

            String productName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));

            Optional<Product> productOpt = productRepository.findByProductName(productName);

            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                try {
                    saveImageFileAndEntity(file, product, tenant);
                    log.info("Success: {} -> ID: {}", productName, product.getProductId());
                } catch (Exception e) {
                    log.error("Failed to save image for product: " + productName, e);
                }
            } else {
                log.warn("Skipped: Product not found for name '{}'", productName);
            }
        }
    }

    public void uploadProductImage(Long productId, MultipartFile image) {
        String tenant = TenantContext.getTenant();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        List<ProductImage> currentImages = productImageRepository.findByProduct(product);

        if (!currentImages.isEmpty()) {
            for (ProductImage oldImage : currentImages) {
                // 기존 물리 파일 삭제
                deletePhysicalFile(oldImage.getImagePath(), tenant);
                // 기존 DB 데이터 삭제
                productImageRepository.delete(oldImage);
            }
            product.getProductImages().clear();
        }

        saveImageFileAndEntity(image, product, tenant);
    }

    public void deleteImage(Long imageId) {

        String tenant = TenantContext.getTenant();

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다. ID: " + imageId));

        deletePhysicalFile(image.getImagePath(), tenant);

        productImageRepository.delete(image);
    }

    @Transactional(readOnly = true)
    public Map<Long, ProductImageDto.ProductImageResponse> getImagesByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productImageRepository.findMainImagesByProductIds(productIds);
    }

    private void saveImageFileAndEntity(MultipartFile file, Product product, String tenant) {
        String absoluteDirPath = getAbsoluteProductDirPath(tenant, product.getProductId());

        // 디렉토리 생성
        File dir = new File(absoluteDirPath);
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) log.warn("Directory creation failed (might already exist): {}", absoluteDirPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String fileName = UUID.randomUUID() + extension;


        String dbRelativePath = "/products/" + product.getProductId() + "/" + fileName;

        try {
            Path targetPath = Paths.get(absoluteDirPath, fileName);

            Thumbnails.of(file.getInputStream())
                    .size(300, 300)
                    .outputQuality(1)
                    .toFile(targetPath.toFile());

            boolean existsMain = productImageRepository.existsByProduct_ProductId(product.getProductId());

            ProductImage image = ProductImage.builder()
                    .imagePath(dbRelativePath)
                    .imageOriginName(file.getOriginalFilename())
                    .imageName(fileName)
                    .product(product)
                    .imageMain(!existsMain)
                    .build();

            product.addImage(image);
            productImageRepository.save(image);

        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패: " + file.getOriginalFilename(), e);
        }
    }

    // 물리 파일 삭제
    private void deletePhysicalFile(String dbRelativePath, String tenant) {
        Path filePath = Paths.get(baseUploadPath, tenant, dbRelativePath);
        File file = filePath.toFile();

        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                log.error("파일 삭제 실패: {}", file.getAbsolutePath());
            }
        }
    }

    // 절대 경로 생성 헬퍼
    private String getAbsoluteProductDirPath(String tenant, Long productId) {
        return Paths.get(baseUploadPath, tenant, "products", String.valueOf(productId)).toString();
    }

}