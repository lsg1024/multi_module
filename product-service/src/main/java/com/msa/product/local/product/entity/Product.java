package com.msa.product.local.product.entity;

import com.msa.common.global.domain.BaseEntity;
import com.msa.product.local.classification.entity.Classification;
import com.msa.product.local.material.entity.Material;
import com.msa.product.local.product.dto.ProductDto;
import com.msa.product.local.set.entity.SetType;
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
public class Product extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_ID")
    private Long productId;
    @Column(name = "FACTORY_ID")
    private Long factoryId;
    @Column(name = "FACTORY_NAME")
    private String factoryName;
    @Column(name = "PRODUCT_FACTORY_NAME")
    private String productFactoryName;
    @Column(name = "PRODUCT_NAME", nullable = false, unique = true)
    private String productName;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SET_TYPE_ID")
    private SetType setType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CLASSIFICATION_ID")
    private Classification classification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MATERIAL_ID")
    private Material material;

    @Column(name = "STANDARD_WEIGHT")
    private BigDecimal standardWeight;

    @Column(name = "PRODUCT_NOTE")
    private String productNote;

    @Column(name = "PRODUCT_DELETED")
    private Boolean productDeleted;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductWorkGradePolicyGroup> productWorkGradePolicyGroups = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<ProductStone> productStones = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<ProductImage> productImages = new ArrayList<>();

    @Builder
    public Product(Long productId, Long factoryId, String factoryName, String productFactoryName, String productName, BigDecimal standardWeight, String productNote, boolean productDeleted, List<ProductStone> productStones, List<ProductImage> productImages) {
        this.productId = productId;
        this.factoryId = factoryId;
        this.factoryName = factoryName;
        this.productFactoryName = productFactoryName;
        this.productName = productName;
        this.standardWeight = standardWeight;
        this.productNote = productNote;
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

    public void updateProductInfo(ProductDto.Update productDto, String factoryName) {
        this.factoryId = productDto.getFactoryId();
        this.factoryName = factoryName;
        this.productFactoryName = productDto.getProductFactoryName();
        this.productName = productDto.getProductName();
        this.standardWeight = new BigDecimal(productDto.getStandardWeight());
        this.productNote = productDto.getProductNote();
    }

}
