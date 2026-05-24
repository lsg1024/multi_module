package com.msa.jewelry.product.internal.stone.stone.entity;

import com.msa.jewelry.product.internal.grade.WorkGrade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "STONE_WORK_GRADE_POLICY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "스톤 등급별 공임 정책 — Stone 마스터에 등급별 공임 단가를 매핑")
public class StoneWorkGradePolicy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STONE_WORK_GRADE_POLICY_ID")
    @Schema(description = "스톤 공임 정책 PK", example = "401")
    private Long stoneWorkGradePolicyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STONE_ID", nullable = false)
    @Schema(description = "소속 스톤 마스터")
    private Stone stone;

    @Enumerated(EnumType.STRING)
    @Column(name = "GRADE", nullable = false)
    @Schema(description = "공임 등급 (GRADE_1 ~ GRADE_4)", example = "GRADE_1")
    private WorkGrade grade;

    @Column(name = "LABOR_COST")
    @Schema(description = "해당 등급의 스톤 공임 금액 (원)", example = "30000")
    private Integer laborCost;

    @Builder
    public StoneWorkGradePolicy(Long stoneWorkGradePolicyId, String grade, Integer laborCost) {
        this.stoneWorkGradePolicyId = stoneWorkGradePolicyId;
        this.grade = WorkGrade.valueOf(grade);
        this.laborCost = laborCost;
    }

    public void setStone(Stone stone) {
        this.stone = stone;
    }
}