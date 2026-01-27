package com.msa.account.local.schedule.service;

import com.msa.account.global.exception.NotFoundException;
import com.msa.account.local.schedule.dto.ScheduleDto;
import com.msa.account.local.schedule.entity.Schedule;
import com.msa.account.local.schedule.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    @Transactional
    public Long createSchedule(ScheduleDto.Request request) {
        Schedule schedule = request.toEntity();
        Schedule saved = scheduleRepository.save(schedule);
        return saved.getScheduleId();
    }

    public List<ScheduleDto.Response> getSchedulesByDateRange(String startDate, String endDate) {
        LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
        LocalDateTime end = LocalDate.parse(endDate).atTime(LocalTime.MAX);

        List<Schedule> schedules = scheduleRepository.findByDateRange(start, end);

        return schedules.stream()
                .map(ScheduleDto.Response::from)
                .toList();
    }

    public ScheduleDto.Response getSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다. ID: " + scheduleId));

        return ScheduleDto.Response.from(schedule);
    }

    @Transactional
    public void updateSchedule(Long scheduleId, ScheduleDto.Request request) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다. ID: " + scheduleId));

        schedule.update(request);
    }

    @Transactional
    public void deleteSchedule(Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다. ID: " + scheduleId));

        scheduleRepository.delete(schedule);
    }
}
