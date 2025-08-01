package com.msa.product.local.product.entity;

import com.msacommon.global.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "PRODUCT_IMAGE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductImage extends BaseTimeEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "IMAGE_ID")
    private Long imageId;
    @Column(name = "IMAGE_URL", nullable = false)
    private String imagePath;
    @Column(name = "IMAGE_NAME", nullable = false)
    private String imageName;
    @Column(name = "IMAGE_ORIGIN_NAME", nullable = false)
    private String imageOriginName;
    @Column(name = "IMAGE_MAIN", nullable = false)
    private Boolean imageMain;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID")
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
