package com.msa.jewelry.account.internal.schedule.dto;

import com.msa.jewelry.account.internal.schedule.entity.RepeatType;
import com.msa.jewelry.account.internal.schedule.entity.Schedule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "일정/예약 DTO 묶음 — 등록/수정 요청 및 응답")
public class ScheduleDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "일정 등록/수정 요청")
    public static class Request {

        @NotBlank(message = "일정 제목은 필수입니다.")
        @Schema(description = "일정 제목", example = "거래처 미팅")
        private String title;

        @Schema(description = "일정 상세 내용", example = "신규 거래처 계약 협의")
        private String content;

        @NotNull(message = "시작 일시는 필수입니다.")
        @Schema(description = "일정 시작 일시", example = "2026-05-20T10:00:00")
        private LocalDateTime startAt;

        @NotNull(message = "종료 일시는 필수입니다.")
        @Schema(description = "일정 종료 일시", example = "2026-05-20T11:30:00")
        private LocalDateTime endAt;

        @Schema(description = "종일 일정 여부", example = "false")
        private boolean allDay;

        @Schema(description = "반복 유형 (NONE/DAILY/WEEKLY 등)", example = "NONE")
        private RepeatType repeatType;

        @Schema(description = "일정 표시 색상", example = "#FF5733")
        private String color;

        @Builder
        public Request(String title, String content, LocalDateTime startAt, LocalDateTime endAt,
                       boolean allDay, RepeatType repeatType, String color) {
            this.title = title;
            this.content = content;
            this.startAt = startAt;
            this.endAt = endAt;
            this.allDay = allDay;
            this.repeatType = repeatType;
            this.color = color;
        }

        public Schedule toEntity() {
            return Schedule.builder()
                    .title(title)
                    .content(content)
                    .startAt(startAt)
                    .endAt(endAt)
                    .allDay(allDay)
                    .repeatType(repeatType)
                    .color(color)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "일정 응답")
    public static class Response {
        @Schema(description = "일정 PK (문자열)", example = "100")
        private String scheduleId;
        @Schema(description = "일정 제목", example = "거래처 미팅")
        private String title;
        @Schema(description = "일정 상세 내용", example = "신규 거래처 계약 협의")
        private String content;
        @Schema(description = "일정 시작 일시", example = "2026-05-20T10:00:00")
        private LocalDateTime startAt;
        @Schema(description = "일정 종료 일시", example = "2026-05-20T11:30:00")
        private LocalDateTime endAt;
        @Schema(description = "종일 일정 여부", example = "false")
        private boolean allDay;
        @Schema(description = "반복 유형", example = "NONE")
        private RepeatType repeatType;
        @Schema(description = "반복 유형 설명(표시명)", example = "반복 없음")
        private String repeatTypeDescription;
        @Schema(description = "일정 표시 색상", example = "#FF5733")
        private String color;
        @Schema(description = "생성자", example = "admin")
        private String createdBy;
        @Schema(description = "생성 일시", example = "2026-05-16T14:30:00")
        private LocalDateTime createdAt;

        @Builder
        public Response(Schedule schedule) {
            this.scheduleId = schedule.getScheduleId().toString();
            this.title = schedule.getTitle();
            this.content = schedule.getContent();
            this.startAt = schedule.getStartAt();
            this.endAt = schedule.getEndAt();
            this.allDay = schedule.isAllDay();
            this.repeatType = schedule.getRepeatType();
            this.repeatTypeDescription = schedule.getRepeatType().getDescription();
            this.color = schedule.getColor();
            this.createdBy = schedule.getCreatedBy();
            this.createdAt = schedule.getCreateDate();
        }

        public static Response from(Schedule schedule) {
            return Response.builder()
                    .schedule(schedule)
                    .build();
        }
    }
}
