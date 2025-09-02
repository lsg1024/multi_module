package com.msa.product.local.product.service;

import com.msa.product.local.product.dto.ProductImageDto;
import com.msa.product.local.product.entity.Product;
import com.msa.product.local.product.entity.ProductImage;
import com.msa.product.local.product.repository.ProductRepository;
import com.msa.product.local.product.repository.image.ProductImageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.msa.product.global.exception.ExceptionMessage.NOT_FOUND;

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

    public void uploadProductImages(Long productId, List<MultipartFile> images) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));

        boolean existsProductImage = productImageRepository.existsByProduct_ProductId(productId);

        if (images != null && !images.isEmpty()) {
            String productDirPath = baseUploadPath + "/products/" + productId;
            File dir = new File(productDirPath);
            if (!dir.exists()) dir.mkdirs();

            for (MultipartFile file : images) {
                String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
                String imagePath = "/products/" + productId + "/" + fileName;
                try {
                    ProductImage image;
                    if (existsProductImage) {
                        image = ProductImage.builder()
                                .imagePath(imagePath)
                                .imageOriginName(file.getOriginalFilename())
                                .imageName(fileName)
                                .product(product)
                                .build();
                    } else {
                        image = ProductImage.builder()
                                .imagePath(imagePath)
                                .imageOriginName(file.getOriginalFilename())
                                .imageName(fileName)
                                .product(product)
                                .imageMain(true)
                                .build();

                        existsProductImage = true;
                    }

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

    //metaData -> 기존 id 값들이 들어옴 사라지면 삭제, images에는 새로운 이미지 파일 업로드
    public void updateImages(Long productId, List<MultipartFile> files, ProductImageDto.Request metaData) {
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
            deletePhysicalFile(oldImage.getImagePath());
        }

        productImageRepository.deleteAll(imagesToDelete);

        List<ProductImage> newProductImages = new ArrayList<>();

        int fileIndex = 0;
        for (int i = 0; i < metaData.getImages().size(); i++){
            ProductImageDto.Request.ImageMeta imageMeta = metaData.getImages().get(i);

            //기존 이미지
            if (imageMeta.getId() != null) {
                ProductImage productImage = productImageRepository.findById(imageMeta.getId()).orElseThrow(() -> new IllegalArgumentException(NOT_FOUND));
                newProductImages.add(productImage);
            } else {
                if (fileIndex >= files.size()) {
                    throw new IllegalArgumentException("FILE 개수가 일치하지 않음");
                }
                ProductImage saveImages = uploadAndSave(files.get(fileIndex++), product);
                newProductImages.add(saveImages);
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

    private ProductImage uploadAndSave(MultipartFile file, Product product) {
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String dirPath = "/products/" + product.getProductId();
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();

        try {
            Path path = Paths.get(dirPath, fileName);
            file.transferTo(path.toFile());
        } catch (IOException e) {
            throw new RuntimeException("이미지 저장 실패", e);
        }

        return ProductImage.builder()
                .imageName(fileName)
                .imageOriginName(file.getOriginalFilename())
                .imagePath(dirPath + "/" + fileName)
                .imageMain(false)
                .build();
    }

    private void deletePhysicalFile(String imagePath) {
        File file = new File(baseUploadPath + "/products/" + imagePath);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (!deleted) {
                throw new RuntimeException("파일 삭제 실패: " + file.getAbsolutePath());
            }
        }
    }
}
