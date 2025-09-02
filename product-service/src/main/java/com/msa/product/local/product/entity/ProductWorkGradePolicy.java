package com.msa.product.local.product.entity;

import com.msa.product.local.grade.WorkGrade;
import com.msa.product.local.product.dto.ProductWorkGradePolicyDto;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "GRADE", nullable = false)
    private WorkGrade grade;

    @Column(name = "LABOR_COST")
    private Integer laborCost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_WORK_GRADE_POLICY_GROUP_ID")
    private ProductWorkGradePolicyGroup workGradePolicyGroup;

    @Builder
    public ProductWorkGradePolicy(Long productWorkGradePolicyId, String grade, Integer laborCost) {
        this.productWorkGradePolicyId = productWorkGradePolicyId;
        this.grade =  WorkGrade.valueOf(grade);
        this.laborCost = laborCost;
    }

    public void setWorkGradePolicyGroup(ProductWorkGradePolicyGroup workGradePolicyGroup) {
        this.workGradePolicyGroup = workGradePolicyGroup;
    }

    public void updateWorkGradePolicyDto(ProductWorkGradePolicyDto.Request dto) {
        this.laborCost = dto.getLaborCost();
    }
}
