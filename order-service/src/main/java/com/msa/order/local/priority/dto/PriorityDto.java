package com.msa.order.local.priority.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PriorityDto {
    private String priorityName;
    private Integer priorityDate;

    @Builder
    public PriorityDto(String priorityName, Integer priorityDate) {
        this.priorityName = priorityName;
        this.priorityDate = priorityDate;
    }
}
