package com.msa.jewelry.order.internal.priority.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "출고 우선순위 응답 DTO — 일반/긴급 등 우선순위와 D-day 일수.")
public class PriorityDto {
    @Schema(description = "우선순위 ID (문자열)", example = "1")
    private String priorityId;
    @Schema(description = "우선순위 이름", example = "일반")
    private String priorityName;
    @Schema(description = "출고까지 소요 일수", example = "7")
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
    @Schema(description = "우선순위 생성 요청 DTO.")
    public static class Request {
        @Schema(description = "우선순위 이름", example = "긴급")
        private String priorityName;
        @Schema(description = "출고까지 소요 일수", example = "3")
        private Integer priorityDate;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "우선순위 업데이트 요청 DTO.")
    public static class Update {
        @Schema(description = "우선순위 이름", example = "긴급")
        private String priorityName;
        @Schema(description = "출고까지 소요 일수", example = "3")
        private Integer priorityDate;
    }

}
