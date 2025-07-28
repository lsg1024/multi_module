package com.msa.product.local.product.entity;

import com.msa.product.local.stone.stone.entity.Stone;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PRODUCT_STONE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductStone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_STONE_ID")
    private Long productStoneId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STONE_ID", nullable = false)
    private Stone stone;

    @Column(name = "INCLUDE_QUANTITY")
    private Boolean includeQuantity;

    @Column(name = "STONE_QUANTITY")
    private Integer stoneQuantity;

    public ProductStone(Product product, Stone stone, Boolean includeQuantity, Integer stoneQuantity) {
        this.product = product;
        this.stone = stone;
        this.includeQuantity = includeQuantity;
        this.stoneQuantity = stoneQuantity;
    }

    public void updateIncludeQuantity(Boolean includeQuantity) {
        this.includeQuantity = includeQuantity;
    }

    public void updateStoneQuantity(Integer stoneQuantity) {
        this.stoneQuantity = stoneQuantity;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
