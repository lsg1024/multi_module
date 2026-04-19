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

    @Column(name = "MAIN_STONE")
    private Boolean mainStone; // 메인 여부

    @Column(name = "INCLUDE_STONE")
    private Boolean includeStone; // 포함 여부

    @Column(name = "INCLUDE_QUANTITY")
    private Boolean includeQuantity; // 알 개수 포함 여부

    @Column(name = "INCLUDE_PRICE")
    private Boolean includePrice; // 가격 포함 여부

    @Column(name = "STONE_QUANTITY")
    private Integer stoneQuantity;

    @Column(name = "PRODUCT_STONE_NOTE")
    private String productStoneNote;

    @Builder
    public ProductStone(Product product, Stone stone, Boolean mainStone, Boolean includeStone, Boolean includeQuantity, Boolean includePrice, Integer stoneQuantity, String productStoneNote) {
        this.product = product;
        this.stone = stone;
        this.mainStone = mainStone;
        this.includeStone = includeStone;
        this.includeQuantity = includeQuantity != null ? includeQuantity : true;
        this.includePrice = includePrice != null ? includePrice : true;
        this.stoneQuantity = stoneQuantity;
        this.productStoneNote = productStoneNote;
    }

    public void updateStone(ProductStoneDto.Request dto) {
        this.mainStone = dto.isMainStone();
        this.includeStone = dto.isIncludeStone();
        this.includeQuantity = dto.isIncludeQuantity();
        this.includePrice = dto.isIncludePrice();
        this.stoneQuantity = dto.getStoneQuantity();
    }

    public void updateIncludeQuantity(boolean includeQuantity) {
        this.includeQuantity = includeQuantity;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
    public void setStone(Stone stone) {
        this.stone = stone;
    }
}
