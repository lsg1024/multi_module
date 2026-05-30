package com.msa.jewelry.local.schedule.entity;

import com.msa.jewelry.local.schedule.dto.ScheduleDto;
import com.msa.common.global.domain.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "SCHEDULE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "일정/예약 엔티티 — 캘린더에 등록되는 일정 마스터")
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCHEDULE_ID")
    @Schema(description = "일정 PK", example = "100")
    private Long scheduleId;

    @Column(name = "TITLE", nullable = false, length = 100)
    @Schema(description = "일정 제목", example = "거래처 미팅")
    private String title;

    @Column(name = "CONTENT", length = 500)
    @Schema(description = "일정 상세 내용", example = "신규 거래처 계약 협의")
    private String content;

    @Column(name = "START_AT", nullable = false)
    @Schema(description = "일정 시작 일시", example = "2026-05-20T10:00:00")
    private LocalDateTime startAt;

    @Column(name = "END_AT", nullable = false)
    @Schema(description = "일정 종료 일시", example = "2026-05-20T11:30:00")
    private LocalDateTime endAt;

    @Column(name = "ALL_DAY", nullable = false)
    @Schema(description = "종일 일정 여부", example = "false")
    private boolean allDay = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "REPEAT_TYPE", nullable = false)
    @Schema(description = "반복 유형 (NONE/DAILY/WEEKLY/MONTHLY 등)", example = "NONE")
    private RepeatType repeatType = RepeatType.NONE;

    @Column(name = "COLOR", length = 20)
    @Schema(description = "일정 표시 색상", example = "#FF5733")
    private String color;

    @Builder
    public Schedule(String title, String content, LocalDateTime startAt, LocalDateTime endAt,
                    boolean allDay, RepeatType repeatType, String color) {
        this.title = title;
        this.content = content;
        this.startAt = startAt;
        this.endAt = endAt;
        this.allDay = allDay;
        this.repeatType = repeatType != null ? repeatType : RepeatType.NONE;
        this.color = color;
    }

    public void update(ScheduleDto.Request request) {
        this.title = request.getTitle();
        this.content = request.getContent();
        this.startAt = request.getStartAt();
        this.endAt = request.getEndAt();
        this.allDay = request.isAllDay();
        this.repeatType = request.getRepeatType() != null ? request.getRepeatType() : RepeatType.NONE;
        this.color = request.getColor();
    }
}
