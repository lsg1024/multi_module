package com.msa.order.global.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
public class StatusHistoryDto {
    private String phase;
    private String kind;
    private String content;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    private OffsetDateTime statusCreateAt;
    private String statusCreateBy;

    public StatusHistoryDto(String phase, String kind, String content, OffsetDateTime statusCreateAt, String statusCreateBy) {
        this.phase = phase;
        this.kind = kind;
        this.content = content;
        this.statusCreateAt = statusCreateAt;
        this.statusCreateBy = statusCreateBy;
    }
}
