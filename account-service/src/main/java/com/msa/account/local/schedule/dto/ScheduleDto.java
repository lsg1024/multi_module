package com.msa.account.local.schedule.dto;

import com.msa.account.local.schedule.entity.RepeatType;
import com.msa.account.local.schedule.entity.Schedule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ScheduleDto {

    @Getter
    @NoArgsConstructor
    public static class Request {

        @NotBlank(message = "일정 제목은 필수입니다.")
        private String title;

        private String content;

        @NotNull(message = "시작 일시는 필수입니다.")
        private LocalDateTime startAt;

        @NotNull(message = "종료 일시는 필수입니다.")
        private LocalDateTime endAt;

        private boolean allDay;

        private RepeatType repeatType;

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
    public static class Response {
        private String scheduleId;
        private String title;
        private String content;
        private LocalDateTime startAt;
        private LocalDateTime endAt;
        private boolean allDay;
        private RepeatType repeatType;
        private String repeatTypeDescription;
        private String color;
        private String createdBy;
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
