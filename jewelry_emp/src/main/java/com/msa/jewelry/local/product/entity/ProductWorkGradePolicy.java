package com.msa.jewelry.local.product.entity;

import com.msa.jewelry.local.grade.entity.WorkGrade;
import com.msa.jewelry.local.product.dto.ProductWorkGradePolicyDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "PRODUCT_WORK_GRADE_POLICY") // 상품 판매 공임 (stone X)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "상품 등급별 공임 정책 — 등급별로 적용할 공임 금액. 상품 판매가 산정에 사용 (stone 제외).")
public class ProductWorkGradePolicy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRODUCT_WORK_GRADE_POLICY_ID")
    @Schema(description = "공임 정책 PK", example = "8001")
    private Long productWorkGradePolicyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "GRADE", nullable = false)
    @Schema(description = "공임 등급 (GRADE_1 ~ GRADE_4)", example = "GRADE_1")
    private WorkGrade grade;

    @Column(name = "LABOR_COST")
    @Schema(description = "해당 등급의 공임 금액 (원)", example = "50000")
    private Integer laborCost = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_WORK_GRADE_POLICY_GROUP_ID")
    @Schema(description = "소속 공임 정책 그룹")
    private ProductWorkGradePolicyGroup workGradePolicyGroup;

    @Builder
    public ProductWorkGradePolicy(Long productWorkGradePolicyId, String grade, Integer laborCost) {
        this.productWorkGradePolicyId = productWorkGradePolicyId;
        this.grade =  WorkGrade.valueOf(grade);
        this.laborCost = laborCost != null ? laborCost : 0;
    }

    public void setWorkGradePolicyGroup(ProductWorkGradePolicyGroup workGradePolicyGroup) {
        this.workGradePolicyGroup = workGradePolicyGroup;
    }

    public void updateWorkGradePolicyDto(ProductWorkGradePolicyDto.Request dto) {
        this.laborCost = dto.getLaborCost() != null ? dto.getLaborCost() : 0;
    }
}
