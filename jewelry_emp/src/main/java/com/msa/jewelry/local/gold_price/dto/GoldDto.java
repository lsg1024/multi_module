package com.msa.jewelry.local.gold_price.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "금시세 DTO — 날짜별 금 단가")
public class GoldDto {

    @Schema(description = "금 단가 (원)", example = "350000")
    private Integer GoldPrice;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Seoul")
    @Schema(description = "기록 일시 (Asia/Seoul, yyyy-MM-dd HH:mm:ss)", example = "2026-05-16 09:00:00")
    private String createDate;

}
