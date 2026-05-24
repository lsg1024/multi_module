package com.msa.jewelry.product.internal.product.entity;

import com.msa.jewelry.product.internal.color.entity.Color;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "PRODUCT_WORK_GRADE_POLICY_GROUP")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "상품 공임 정책 그룹 — 색상별로 묶이는 공임 정책 묶음 (한 상품은 색상별로 다른 정책 그룹을 가짐)")
public class ProductWorkGradePolicyGroup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_WORK_GRADE_POLICY_GROUP_ID")
    @Schema(description = "공임 정책 그룹 PK", example = "9001")
    private Long productWorkGradePolicyGroupId;

    @Column(name = "PRODUCT_PURCHASE_PRICE")
    @Schema(description = "해당 색상의 상품 매입가 (원)", example = "300000")
    private Integer productPurchasePrice = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID")
    @Schema(description = "소속 상품")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COLOR_ID")
    @Schema(description = "정책 그룹의 색상 (옐로골드/화이트골드 등)")
    private Color color;

    @Column(name = "PRODUCT_WORK_GRADE_POLICY_GROUP_DEFAULT")
    @Schema(description = "기본 정책 그룹 여부 — TRUE 면 색상 미지정 시 기본으로 사용", example = "true")
    private boolean productWorkGradePolicyGroupDefault;

    @OneToMany(mappedBy = "workGradePolicyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @Schema(description = "그룹에 속한 등급별 공임 정책 목록")
    private List<ProductWorkGradePolicy> gradePolicies = new ArrayList<>();

    @Column(name = "PRODUCT_NOTE")
    @Schema(description = "그룹 비고", example = "옐로골드 전용 정책")
    private String note;

    @Builder
    public ProductWorkGradePolicyGroup(Long productWorkGradePolicyGroupId, Integer productPurchasePrice, Product product, Color color, boolean productWorkGradePolicyGroupDefault, List<ProductWorkGradePolicy> gradePolicies, String note) {
        this.productWorkGradePolicyGroupId = productWorkGradePolicyGroupId;
        this.productPurchasePrice = productPurchasePrice != null ? productPurchasePrice : 0;
        this.product = product;
        this.color = color;
        this.productWorkGradePolicyGroupDefault = productWorkGradePolicyGroupDefault;
        this.gradePolicies = gradePolicies;
        this.note = note;
    }
    public void addGradePolicy(ProductWorkGradePolicy detail) {
        gradePolicies.add(detail);
        detail.setWorkGradePolicyGroup(this);
    }
    public void setProduct(Product product) {
        this.product = product;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    public void updateProductPurchasePrice(Integer productPurchasePrice, String note) {
        this.productPurchasePrice = productPurchasePrice != null ? productPurchasePrice : 0;
        this.note = note;
    }
}
