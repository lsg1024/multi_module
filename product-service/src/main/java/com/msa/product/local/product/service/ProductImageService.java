package com.msa.product.local.product.service;

import com.msa.common.global.tenant.TenantContext;
import com.msa.product.local.product.dto.ProductImageDto;
import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.entity.ProductImage;
import com.msa.product.local.product.repository.ProductRepository;
import com.msa.product.local.product.repository.image.ProductImageRepository;
import lombok.extern.slf4j.Slf4j;
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

    public void uploadProductImages(Long productId, List<MultipartFile> images) {
        String tenant = TenantContext.getTenant();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        if (images != null && !images.isEmpty()) {
            for (MultipartFile file : images) {
                saveImageFileAndEntity(file, product, tenant);
            }
        }
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
            file.transferTo(targetPath.toFile());

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

    public void updateImages(Long productId, List<MultipartFile> files, ProductImageDto.Request metaData) {
        String tenant = TenantContext.getTenant();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        List<ProductImage> oldImages = productImageRepository.findByProduct(product);

        List<Long> remainIds = metaData.getImages().stream()
                .map(ProductImageDto.Request.ImageMeta::getId)
                .filter(Objects::nonNull)
                .toList();

        List<ProductImage> imagesToDelete = oldImages.stream()
                .filter(img -> !remainIds.contains(img.getImageId()))
                .toList();

        for (ProductImage oldImage : imagesToDelete) {
            deletePhysicalFile(oldImage.getImagePath(), tenant);
        }
        productImageRepository.deleteAll(imagesToDelete);

        List<ProductImage> newProductImages = new ArrayList<>();
        int fileIndex = 0;

        for (int i = 0; i < metaData.getImages().size(); i++) {
            ProductImageDto.Request.ImageMeta imageMeta = metaData.getImages().get(i);

            if (imageMeta.getId() != null) {
                ProductImage productImage = productImageRepository.findById(imageMeta.getId())
                        .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
                newProductImages.add(productImage);
            } else {
                if (files == null || fileIndex >= files.size()) {
                    throw new IllegalArgumentException("업로드된 파일 개수가 메타데이터와 일치하지 않습니다.");
                }
                ProductImage savedImage = uploadAndSaveForUpdate(files.get(fileIndex++), product, tenant);
                newProductImages.add(savedImage);
            }
        }

        product.getProductImages().clear();
        for (int i = 0; i < newProductImages.size(); i++) {
            ProductImage image = newProductImages.get(i);
            image.setImageMain(i == metaData.getMainImageIndex());
            image.setProduct(product);
            product.addImage(image);
        }
        productImageRepository.saveAll(newProductImages);
    }

    // Update 시 사용하는 단일 파일 업로드 헬퍼
    private ProductImage uploadAndSaveForUpdate(MultipartFile file, Product product, String tenant) {
        String absoluteDirPath = getAbsoluteProductDirPath(tenant, product.getProductId());

        File dir = new File(absoluteDirPath);
        if (!dir.exists()) dir.mkdirs();

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String dbRelativePath = "/products/" + product.getProductId() + "/" + fileName;

        try {
            Path path = Paths.get(absoluteDirPath, fileName);
            file.transferTo(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패", e);
        }

        return ProductImage.builder()
                .imageName(fileName)
                .imageOriginName(file.getOriginalFilename())
                .imagePath(dbRelativePath)
                .imageMain(false)
                .build();
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
        // 결과: /app/images/{tenant}/products/{productId}
        return Paths.get(baseUploadPath, tenant, "products", String.valueOf(productId)).toString();
    }

    public Map<Long, ProductImageDto.ApiResponse> getImagesByProductIds(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return productImageRepository.findMainImagesByProductIds(productIds);
    }

    public void deleteImage(Long imageId) {

        String tenant = TenantContext.getTenant();

        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지를 찾을 수 없습니다. ID: " + imageId));

        deletePhysicalFile(image.getImagePath(), tenant);

        productImageRepository.delete(image);
    }
}