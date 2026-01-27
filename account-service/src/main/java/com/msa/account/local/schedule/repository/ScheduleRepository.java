package com.msa.account.local.schedule.repository;

import com.msa.account.local.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    @Query("SELECT s FROM Schedule s WHERE " +
           "(s.startAt BETWEEN :start AND :end) OR " +
           "(s.endAt BETWEEN :start AND :end) OR " +
           "(s.startAt <= :start AND s.endAt >= :end) " +
           "ORDER BY s.startAt ASC")
    List<Schedule> findByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
