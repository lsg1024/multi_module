package com.msa.order.local.domain.order.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Table(name = "ORDER_PRODUCT")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderProduct {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ORDER_PRODUCT_ID")
    private Long orderProductId;
    @Column(name = "FACTORY_ID")
    private Long factoryId;
    @Column(name = "FACTORY_NAME")
    private String factoryName;
    @Column(name = "PRODUCT_ID")
    private Long productId;
    @Column(name = "PRODUCT_NAME")
    private String productName;
    @Column(name = "PRODUCT_SIZE")
    private String productSize;
    @Column(name = "PRODUCT_LABOR_COST")
    private Integer productLaborCost; // 상점 grade 등급에 따라 가격
    @Column(name = "PRODUCT_ADD_LABOR_COST")
    private Integer productAddLaborCost;
    @Column(name = "MATERIAL_NAME")
    private String materialName;
    @Column(name = "CLASSIFICATION_NAME")
    private String classificationName;
    @Column(name = "COLOR_NAME")
    private String colorName;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID")
    private Orders order;

    @Builder
    public OrderProduct(Long factoryId, String factoryName, Long productId, String productName, String productSize, Integer productLaborCost, Integer productAddLaborCost, String materialName, String classificationName, String colorName, Orders order) {
        this.factoryId = factoryId;
        this.factoryName = factoryName;
        this.productId = productId;
        this.productName = productName;
        this.productSize = productSize;
        this.productLaborCost = productLaborCost;
        this.productAddLaborCost = productAddLaborCost;
        this.materialName = materialName;
        this.classificationName = classificationName;
        this.colorName = colorName;
        this.order = order;
    }

    public void setOrder(Orders order) {
        this.order = order;
    }
}
