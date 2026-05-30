package com.msa.jewelry.local.product.entity;

import com.msa.common.global.domain.BaseTimeEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "PRODUCT_IMAGE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "상품 이미지 엔티티 — 한 상품은 여러 이미지를 가질 수 있으며 그중 하나가 메인 이미지")
public class ProductImage extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IMAGE_ID")
    @Schema(description = "이미지 PK", example = "5001")
    private Long imageId;
    @Column(name = "IMAGE_URL", nullable = false)
    @Schema(description = "이미지 저장 경로 (S3 등 URL)", example = "https://cdn.example.com/products/abc.jpg")
    private String imagePath;
    @Column(name = "IMAGE_NAME", nullable = false)
    @Schema(description = "저장된 이미지 파일명 (서버 측 고유명)", example = "abc-1715842322.jpg")
    private String imageName;
    @Column(name = "IMAGE_ORIGIN_NAME", nullable = false)
    @Schema(description = "사용자 업로드 시 원본 파일명", example = "내제품사진.jpg")
    private String imageOriginName;
    @Column(name = "IMAGE_MAIN", nullable = false)
    @Schema(description = "메인 이미지 여부 — TRUE 면 상품 대표 이미지", example = "true")
    private Boolean imageMain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID")
    @Schema(description = "소속 상품")
    private Product product;

    @Builder
    public ProductImage(String imagePath, String imageName, String imageOriginName, Product product, boolean imageMain) {
        this.imagePath = imagePath;
        this.imageName = imageName;
        this.imageOriginName = imageOriginName;
        this.product = product;
        this.imageMain = imageMain;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setImageMain(Boolean imageMain) {
        this.imageMain = imageMain;
    }
}
