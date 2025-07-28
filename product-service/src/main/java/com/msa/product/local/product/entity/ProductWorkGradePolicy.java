package com.msa.product.local.product.entity;

import com.msa.product.global.domain.WorkGrade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PRODUCT_WORK_GRADE_POLICY")
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

    public ProductWorkGradePolicy(WorkGrade grade, Integer laborCost) {
        this.grade = grade;
        this.laborCost = laborCost;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
