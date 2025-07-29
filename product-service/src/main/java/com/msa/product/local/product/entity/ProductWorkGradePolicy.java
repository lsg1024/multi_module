package com.msa.product.local.product.entity;

import com.msa.product.global.domain.WorkGrade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "PRODUCT_WORK_GRADE_POLICY") // 상품 판매 공임 (stone X)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductWorkGradePolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_WORK_GRADE_POLICY_ID")
    private Long productWorkGradePolicyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(name = "GRADE", nullable = false)
    private WorkGrade grade;

    @Column(name = "LABOR_COST")
    private Integer laborCost;

    @Column(name = "PRODUCT_POLICY_NOTE")
    private String productPolicyNote;

    @Builder
    public ProductWorkGradePolicy(String grade, Integer laborCost, String productPolicyNote) {
        this.grade =  WorkGrade.valueOf(grade);
        this.laborCost = laborCost;
        this.productPolicyNote = productPolicyNote;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
