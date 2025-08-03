package com.msa.product.local.product.entity;

import com.msa.product.local.color.entity.Color;
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
public class ProductWorkGradePolicyGroup {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_WORK_GRADE_POLICY_GROUP_ID")
    private Long productWorkGradePolicyGroupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COLOR_ID")
    private Color color;

    @Column(name = "PRODUCT_WORK_GRADE_POLICY_GROUP_DEFAULT")
    private boolean productWorkGradePolicyGroupDefault;

    @OneToMany(mappedBy = "workGradePolicyGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductWorkGradePolicy> gradePolicies = new ArrayList<>();

    @Builder
    public ProductWorkGradePolicyGroup(Long productWorkGradePolicyGroupId, Product product, Color color, boolean productWorkGradePolicyGroupDefault, List<ProductWorkGradePolicy> gradePolicies) {
        this.productWorkGradePolicyGroupId = productWorkGradePolicyGroupId;
        this.product = product;
        this.color = color;
        this.productWorkGradePolicyGroupDefault = productWorkGradePolicyGroupDefault;
        this.gradePolicies = gradePolicies;
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

}
