package com.msa.product.local.stone.stone.entity;

import com.msa.product.local.grade.WorkGrade;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "STONE_WORK_GRADE_POLICY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoneWorkGradePolicy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "STONE_WORK_GRADE_POLICY_ID")
    private Long stoneWorkGradePolicyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "STONE_ID", nullable = false)
    private Stone stone;

    @Enumerated(EnumType.STRING)
    @Column(name = "GRADE", nullable = false)
    private WorkGrade grade;

    @Column(name = "LABOR_COST")
    private Integer laborCost;

    @Builder
    public StoneWorkGradePolicy(String grade, Integer laborCost) {
        this.grade = WorkGrade.valueOf(grade);
        this.laborCost = laborCost;
    }

    public void setStone(Stone stone) {
        this.stone = stone;
    }
}