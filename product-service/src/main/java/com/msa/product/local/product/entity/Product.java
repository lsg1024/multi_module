package com.msa.product.local.product.entity;

import com.msa.product.local.classification.entity.Classification;
import com.msa.product.local.material.entity.Material;
import com.msa.product.local.set.entity.SetType;
import com.msacommon.global.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PRODUCT")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_ID")
    private Long productId;
    @Column(name = "FACTORY_ID")
    private Long factoryId;
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

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductWorkGradePolicy> gradePolicies = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductStone> productStones = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> productImages = new ArrayList<>();

    public void addProductStone(ProductStone productStone) {
        productStones.add(productStone);
        productStone.setProduct(this);
    }
    public void addGradePolicy(ProductWorkGradePolicy policy) {
        gradePolicies.add(policy);
        policy.setProduct(this);
    }

    public void addImages(List<ProductImage> images) {
        productImages.addAll(images);
    }

}
