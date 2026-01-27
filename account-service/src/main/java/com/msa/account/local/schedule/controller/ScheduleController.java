package com.msa.account.local.schedule.controller;

import com.msa.account.local.schedule.dto.ScheduleDto;
import com.msa.account.local.schedule.service.ScheduleService;
import com.msa.common.global.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 일정 생성
     */
    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<Long>> createSchedule(
            @Valid @RequestBody ScheduleDto.Request request) {
        Long scheduleId = scheduleService.createSchedule(request);
        return ResponseEntity.ok(ApiResponse.success(scheduleId));
    }

    /**
     * 일정 목록 조회 (기간별)
     */
    @GetMapping("/schedules")
    public ResponseEntity<ApiResponse<List<ScheduleDto.Response>>> getSchedules(
            @RequestParam(name = "start") String startDate,
            @RequestParam(name = "end") String endDate) {
        List<ScheduleDto.Response> schedules = scheduleService.getSchedulesByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(schedules));
    }

    /**
     * 일정 상세 조회
     */
    @GetMapping("/schedule/{id}")
    public ResponseEntity<ApiResponse<ScheduleDto.Response>> getSchedule(
            @PathVariable("id") Long scheduleId) {
        ScheduleDto.Response schedule = scheduleService.getSchedule(scheduleId);
        return ResponseEntity.ok(ApiResponse.success(schedule));
    }

    /**
     * 일정 수정
     */
    @PatchMapping("/schedule/{id}")
    public ResponseEntity<ApiResponse<String>> updateSchedule(
            @PathVariable("id") Long scheduleId,
            @Valid @RequestBody ScheduleDto.Request request) {
        scheduleService.updateSchedule(scheduleId, request);
        return ResponseEntity.ok(ApiResponse.success("수정 완료"));
    }

    /**
     * 일정 삭제
     */
    @DeleteMapping("/schedule/{id}")
    public ResponseEntity<ApiResponse<String>> deleteSchedule(
            @PathVariable("id") Long scheduleId) {
        scheduleService.deleteSchedule(scheduleId);
        return ResponseEntity.ok(ApiResponse.success("삭제 완료"));
    }
}
