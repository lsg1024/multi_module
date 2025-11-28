package com.msa.order.local.priority.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PriorityDto {
    private String priorityId;
    private String priorityName;
    private Integer priorityDate;

    @Builder
    public PriorityDto(String priorityId, String priorityName, Integer priorityDate) {
        this.priorityId = priorityId;
        this.priorityName = priorityName;
        this.priorityDate = priorityDate;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Request {
        private String priorityName;
        private Integer priorityDate;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Update {
        private String priorityName;
        private Integer priorityDate;
    }

}
