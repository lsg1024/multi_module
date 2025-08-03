package com.msa.product.local.product.entity;

import com.msa.product.local.product.dto.ProductStoneDto;
import com.msa.product.local.stone.stone.entity.Stone;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
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
    private Stone stone; // 판매 단가 호출 필요

    @Column(name = "PRODUCT_STONE_MAIN")
    private Boolean productStoneMain;

    @Column(name = "INCLUDE_QUANTITY")
    private Boolean includeQuantity;

    @Column(name = "INCLUDE_WEIGHT")
    private Boolean includeWeight;

    @Column(name = "INCLUDE_LABOR")
    private Boolean includeLabor;

    @Column(name = "STONE_QUANTITY")
    private Integer stoneQuantity;

    @Builder
    public ProductStone(Product product, Stone stone, Boolean productStoneMain, Boolean includeQuantity, Boolean includeWeight, Boolean includeLabor, Integer stoneQuantity) {
        this.product = product;
        this.stone = stone;
        this.productStoneMain = productStoneMain;
        this.includeQuantity = includeQuantity;
        this.includeWeight = includeWeight;
        this.includeLabor = includeLabor;
        this.stoneQuantity = stoneQuantity;
    }

    public void updateIncludeQuantity(Boolean includeQuantity) {
        this.includeQuantity = includeQuantity;
    }

    public void updateStoneQuantity(Integer stoneQuantity) {
        this.stoneQuantity = stoneQuantity;
    }

    public void updateStone(ProductStoneDto.Request dto) {
        this.includeQuantity = dto.isIncludeQuantity();
        this.includeWeight = dto.isIncludeWeight();
        this.includeLabor = dto.isIncludeLabor();
        this.stoneQuantity = dto.getStoneQuantity();
    }

    public void setProduct(Product product) {
        this.product = product;
    }
    public void setStone(Stone stone) {
        this.stone = stone;
    }
}
