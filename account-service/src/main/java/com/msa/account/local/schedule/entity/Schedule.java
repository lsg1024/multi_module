package com.msa.account.local.schedule.entity;

import com.msa.account.local.schedule.dto.ScheduleDto;
import com.msa.common.global.domain.BaseEntity;
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
public class Schedule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "SCHEDULE_ID")
    private Long scheduleId;

    @Column(name = "TITLE", nullable = false, length = 100)
    private String title;

    @Column(name = "CONTENT", length = 500)
    private String content;

    @Column(name = "START_AT", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "END_AT", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "ALL_DAY", nullable = false)
    private boolean allDay = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "REPEAT_TYPE", nullable = false)
    private RepeatType repeatType = RepeatType.NONE;

    @Column(name = "COLOR", length = 20)
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
