package com.msa.jewelry.local.priority.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Table(name = "PRIORITY")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "주문 출고 우선순위 엔티티 — 일반/급/초급 같은 우선순위와 그에 따른 출고 D-day(일수)를 관리.")
public class Priority {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRIORITY_ID")
    @Schema(description = "우선순위 PK", example = "1")
    private Long priorityId;
    @Column(name = "PRIORITY_NAME", unique = true)
    @Schema(description = "우선순위 이름 (유니크)", example = "일반")
    private String priorityName;
    @Column(name = "PRIORITY_DATE")
    @Schema(description = "출고까지 소요 일수 (일반/급/초급의 기준 일수)", example = "7")
    private Integer priorityDate; // 일반, 급, 초급 -> default 일반

    public Integer getPriorityDate() {
        return priorityDate;
    }

    @Builder
    public Priority(String priorityName, Integer priorityDate) {
        this.priorityName = priorityName;
        this.priorityDate = priorityDate;
    }

    public void updatePriority(String priorityName, Integer priorityDate) {
        this.priorityName = priorityName;
        this.priorityDate = priorityDate;
    }
}
