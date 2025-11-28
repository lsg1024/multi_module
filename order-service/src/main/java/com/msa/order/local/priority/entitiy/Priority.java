package com.msa.order.local.priority.entitiy;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Table(name = "PRIORITY")
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Priority {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "PRIORITY_ID")
    private Long priorityId;
    @Column(name = "PRIORITY_NAME", unique = true)
    private String priorityName;
    @Column(name = "PRIORITY_DATE")
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
