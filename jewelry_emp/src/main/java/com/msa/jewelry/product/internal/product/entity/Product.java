package com.msa.jewelry.product.internal.product.entity;

import com.msa.common.global.domain.BaseEntity;
import com.msa.jewelry.product.internal.classification.entity.Classification;
import com.msa.jewelry.product.internal.material.entity.Material;
import com.msa.jewelry.product.internal.product.dto.ProductDto;
import com.msa.jewelry.product.internal.set.entity.SetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "PRODUCT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLDelete(sql = "UPDATE PRODUCT SET PRODUCT_DELETED = TRUE WHERE PRODUCT_ID = ?")
@Schema(description = "상품 마스터 엔티티 — 카탈로그 단위 상품. 가격은 ProductWorkGradePolicy/ProductStone 에 분산되어 있다.")
public class Product extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_ID")
    @Schema(description = "상품 PK", example = "1001")
    private Long productId;
    @Column(name = "FACTORY_ID")
    @Schema(description = "제조사 ID (account 모듈의 Factory FK)", example = "5")
    private Long factoryId;
    @Column(name = "FACTORY_NAME")
    @Schema(description = "제조사명 (Product 엔티티 자체 컬럼, P4 정규화 대상 외)", example = "한국주얼리")
    private String factoryName;
    @Column(name = "PRODUCT_FACTORY_NAME")
    @Schema(description = "제조사가 부여한 제품명/모델명", example = "R-2024-001")
    private String productFactoryName;
    @Column(name = "PRODUCT_NAME", nullable = false, unique = true)
    @Schema(description = "상품명 (고유)", example = "프로포즈 솔리테어 반지")
    private String productName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SET_TYPE_ID")
    @Schema(description = "세트 타입 (단품/세트 등)")
    private SetType setType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLASSIFICATION_ID")
    @Schema(description = "상품 분류 (반지/목걸이 등)")
    private Classification classification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MATERIAL_ID")
    @Schema(description = "재질 (14K/18K/PT900 등)")
    private Material material;

    @Column(name = "STANDARD_WEIGHT")
    @Schema(description = "기준 무게 (그램)", example = "3.50")
    private BigDecimal standardWeight;

    @Column(name = "PRODUCT_NOTE")
    @Schema(description = "상품 비고", example = "메인 스톤 0.3ct 다이아")
    private String productNote;

    @Column(name = "PRODUCT_RELATED_NUMBER")
    @Schema(description = "관련번호 — 같은 모델군 상품을 묶기 위한 식별자", example = "R-2024-SERIES")
    private String productRelatedNumber;

    @Column(name = "PRODUCT_DELETED")
    @Schema(description = "소프트 삭제 플래그 — TRUE 이면 사용자에게는 보이지 않음", example = "false")
    private Boolean productDeleted;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "상품 등급별 공임 정책 그룹 목록 (색상별로 분리됨)")
    private List<ProductWorkGradePolicyGroup> productWorkGradePolicyGroups = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Schema(description = "상품에 매핑된 스톤 목록")
    private List<ProductStone> productStones = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @Schema(description = "상품 이미지 목록")
    private List<ProductImage> productImages = new ArrayList<>();

    @Builder
    public Product(Long productId, Long factoryId, String factoryName, String productFactoryName, String productName, BigDecimal standardWeight, String productNote, String productRelatedNumber, boolean productDeleted, List<ProductStone> productStones, List<ProductImage> productImages) {
        this.productId = productId;
        this.factoryId = factoryId;
        this.factoryName = factoryName;
        this.productFactoryName = productFactoryName;
        this.productName = productName;
        this.standardWeight = standardWeight;
        this.productNote = productNote;
        this.productRelatedNumber = productRelatedNumber;
        this.productDeleted = productDeleted;
        this.productStones = productStones;
        this.productImages = productImages;
    }

    public void setSetType(SetType setType) {
        this.setType = setType;
    }

    public void setClassification(Classification classification) {
        this.classification = classification;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public void addPolicyGroup(ProductWorkGradePolicyGroup group) {
        productWorkGradePolicyGroups.add(group);
        group.setProduct(this);
    }

    public void addProductStone(ProductStone productStone) {
        productStones.add(productStone);
        productStone.setProduct(this);
    }

    public void addImage(ProductImage images) {
        productImages.add(images);
        images.setProduct(this);
    }

    public void updateProductFactoryName(String productFactoryName) {
        this.productFactoryName = productFactoryName;
    }

    public void appendProductNote(String note) {
        if (this.productNote == null || this.productNote.isBlank()) {
            this.productNote = note;
        } else {
            this.productNote = this.productNote + " | " + note;
        }
    }

    /**
     * Product 정보를 부분 업데이트한다.
     * null/빈 문자열로 전달된 필드는 기존 DB 값을 유지하여 payload 누락 시 데이터가 유실되지 않도록 한다.
     */
    public void updateProductInfo(ProductDto.Update productDto, String factoryName) {
        if (productDto == null) {
            return;
        }
        if (productDto.getFactoryId() != null) {
            this.factoryId = productDto.getFactoryId();
        }
        if (factoryName != null && !factoryName.isEmpty()) {
            this.factoryName = factoryName;
        }
        if (productDto.getProductFactoryName() != null && !productDto.getProductFactoryName().isEmpty()) {
            this.productFactoryName = productDto.getProductFactoryName();
        }
        if (productDto.getProductName() != null && !productDto.getProductName().isEmpty()) {
            this.productName = productDto.getProductName();
        }
        if (productDto.getStandardWeight() != null && !productDto.getStandardWeight().isEmpty()) {
            this.standardWeight = new BigDecimal(productDto.getStandardWeight());
        }
        if (productDto.getProductRelatedNumber() != null) {
            this.productRelatedNumber = productDto.getProductRelatedNumber();
        }
        if (productDto.getProductNote() != null) {
            this.productNote = productDto.getProductNote();
        }
    }

}
