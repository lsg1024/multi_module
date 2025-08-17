package com.msa.order.local.domain.stock.entity.domain;

import com.msa.order.local.domain.order.entity.OrderProduct;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductSnapshot {
    @Column(name = "PRODUCT_NAME")
    private String name;
    @Column(name = "PRODUCT_SIZE")
    private String size;
    @Column(name = "PRODUCT_ADD_LABOR_COST")
    private Integer addLaborCost;
    @Column(name = "MATERIAL_NAME")
    private String materialName;
    @Column(name = "COLOR_NAME")
    private String colorName;
    @Column(name = "PRODUCT_WEIGHT")
    private BigDecimal productWeight;
    @Column(name = "STONE_WEIGHT")
    private BigDecimal stoneWeight;

    @Builder
    private ProductSnapshot(String name, String size, Integer addLaborCost,
                            String materialName, String colorName,
                            BigDecimal productWeight, BigDecimal stoneWeight) {
        this.name = name;
        this.size = size;
        this.addLaborCost = addLaborCost;
        this.materialName = materialName;
        this.colorName = colorName;
        this.productWeight = productWeight;
        this.stoneWeight = stoneWeight;
    }

    public static ProductSnapshot fromOrderProduct(OrderProduct op) {
        return new ProductSnapshot(
                op.getProductName(),
                op.getProductSize(),
                op.getProductAddLaborCost(),
                op.getMaterialName(),
                op.getColorName(),
                op.getProductWeight(),
                op.getStoneWeight()
        );
    }
}

