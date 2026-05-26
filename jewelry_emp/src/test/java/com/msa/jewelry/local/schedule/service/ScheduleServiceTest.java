package com.msa.jewelry.local.schedule.service;

import com.msa.jewelry.global.exception.NotFoundException;
import com.msa.jewelry.local.schedule.dto.ScheduleDto;
import com.msa.jewelry.local.schedule.entity.RepeatType;
import com.msa.jewelry.local.schedule.entity.Schedule;
import com.msa.jewelry.local.schedule.repository.ScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * ScheduleService 단위 테스트.
 *
 * <p>일정 CRUD — ScheduleRepository 만 mock.
 *
 * <p>커버리지:
 * <ul>
 *   <li>createSchedule — request.toEntity 위임 후 save</li>
 *   <li>getSchedulesByDateRange — LocalDate parse / Repository 위임 / 빈 결과</li>
 *   <li>getSchedule — 정상 / NOT_FOUND</li>
 *   <li>updateSchedule — 정상 / NOT_FOUND</li>
 *   <li>deleteSchedule — 정상 / NOT_FOUND</li>
 *   <li>잘못된 날짜 포맷 / 과거 날짜</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ScheduleService 단위 테스트")
class ScheduleServiceTest {

    private static final Long SCHEDULE_ID = 100L;

    @Mock ScheduleRepository scheduleRepository;

    @InjectMocks
    ScheduleService scheduleService;

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("createSchedule")
    class CreateSchedule {

        @Test
        @DisplayName("정상 — request.toEntity → save 후 PK 반환")
        void 정상생성() {
            ScheduleDto.Request req = mock(ScheduleDto.Request.class);
            Schedule entity = mock(Schedule.class);
            Schedule saved = mock(Schedule.class);

            given(req.toEntity()).willReturn(entity);
            given(scheduleRepository.save(entity)).willReturn(saved);
            given(saved.getScheduleId()).willReturn(SCHEDULE_ID);

            Long result = scheduleService.createSchedule(req);

            assertThat(result).isEqualTo(SCHEDULE_ID);
            verify(scheduleRepository).save(entity);
        }

        @Test
        @DisplayName("과거 날짜로도 생성 가능 — Service 가드 없음, 검증은 상위 계층")
        void 과거날짜_생성() {
            ScheduleDto.Request req = ScheduleDto.Request.builder()
                    .title("지난 미팅")
                    .content("이미 끝난 일정")
                    .startAt(LocalDateTime.of(2020, 1, 1, 9, 0))
                    .endAt(LocalDateTime.of(2020, 1, 1, 10, 0))
                    .allDay(false)
                    .repeatType(RepeatType.NONE)
                    .color("#000000")
                    .build();

            Schedule saved = mock(Schedule.class);
            given(saved.getScheduleId()).willReturn(SCHEDULE_ID);
            given(scheduleRepository.save(any(Schedule.class))).willReturn(saved);

            Long result = scheduleService.createSchedule(req);

            assertThat(result).isEqualTo(SCHEDULE_ID);
            verify(scheduleRepository).save(any(Schedule.class));
        }

        @Test
        @DisplayName("Repository save 가 예외 던지면 그대로 전파")
        void save_예외_전파() {
            ScheduleDto.Request req = mock(ScheduleDto.Request.class);
            Schedule entity = mock(Schedule.class);
            given(req.toEntity()).willReturn(entity);
            given(scheduleRepository.save(entity))
                    .willThrow(new RuntimeException("DB 에러"));

            assertThatThrownBy(() -> scheduleService.createSchedule(req))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB 에러");
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getSchedulesByDateRange")
    class GetSchedulesByDateRange {

        @Test
        @DisplayName("정상 — start/end 변환 후 Repository 호출, Response 매핑")
        void 정상조회() {
            Schedule schedule = stubSchedule();
            given(scheduleRepository.findByDateRange(any(), any())).willReturn(List.of(schedule));

            List<ScheduleDto.Response> result =
                    scheduleService.getSchedulesByDateRange("2026-05-01", "2026-05-31");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getScheduleId()).isEqualTo(SCHEDULE_ID.toString());
            assertThat(result.get(0).getTitle()).isEqualTo("거래처 미팅");
        }

        @Test
        @DisplayName("빈 결과 → 빈 리스트")
        void 빈결과() {
            given(scheduleRepository.findByDateRange(any(), any())).willReturn(Collections.emptyList());

            List<ScheduleDto.Response> result =
                    scheduleService.getSchedulesByDateRange("2026-05-01", "2026-05-31");

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("잘못된 날짜 포맷 → DateTimeParseException")
        void 잘못된_날짜포맷() {
            assertThatThrownBy(() ->
                    scheduleService.getSchedulesByDateRange("2026/05/01", "2026/05/31"))
                    .isInstanceOf(java.time.format.DateTimeParseException.class);

            verify(scheduleRepository, never()).findByDateRange(any(), any());
        }

        @Test
        @DisplayName("start > end (역순) — 그대로 Repository 위임, 결과는 빈 리스트")
        void 역순_범위() {
            given(scheduleRepository.findByDateRange(any(), any())).willReturn(Collections.emptyList());

            List<ScheduleDto.Response> result =
                    scheduleService.getSchedulesByDateRange("2026-12-31", "2026-01-01");

            assertThat(result).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("getSchedule")
    class GetSchedule {

        @Test
        @DisplayName("정상 — 단건 Response 반환")
        void 정상조회() {
            Schedule schedule = stubSchedule();
            given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

            ScheduleDto.Response result = scheduleService.getSchedule(SCHEDULE_ID);

            assertThat(result.getScheduleId()).isEqualTo(SCHEDULE_ID.toString());
            assertThat(result.getTitle()).isEqualTo("거래처 미팅");
        }

        @Test
        @DisplayName("없음 → NotFoundException")
        void 없음() {
            given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> scheduleService.getSchedule(SCHEDULE_ID))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("일정을 찾을 수 없습니다");
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("updateSchedule")
    class UpdateSchedule {

        @Test
        @DisplayName("정상 — entity.update 호출")
        void 정상수정() {
            Schedule schedule = mock(Schedule.class);
            ScheduleDto.Request req = mock(ScheduleDto.Request.class);
            given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

            scheduleService.updateSchedule(SCHEDULE_ID, req);

            verify(schedule).update(req);
        }

        @Test
        @DisplayName("대상 없음 → NotFoundException, update 호출 안 함")
        void 없음() {
            Schedule schedule = mock(Schedule.class);
            ScheduleDto.Request req = mock(ScheduleDto.Request.class);
            given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> scheduleService.updateSchedule(SCHEDULE_ID, req))
                    .isInstanceOf(NotFoundException.class);

            verify(schedule, never()).update(any());
        }
    }

    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("deleteSchedule")
    class DeleteSchedule {

        @Test
        @DisplayName("정상 — Repository.delete 호출")
        void 정상삭제() {
            Schedule schedule = mock(Schedule.class);
            given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.of(schedule));

            scheduleService.deleteSchedule(SCHEDULE_ID);

            verify(scheduleRepository).delete(schedule);
        }

        @Test
        @DisplayName("대상 없음 → NotFoundException, delete 호출 안 함")
        void 없음() {
            given(scheduleRepository.findById(SCHEDULE_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> scheduleService.deleteSchedule(SCHEDULE_ID))
                    .isInstanceOf(NotFoundException.class);

            verify(scheduleRepository, never()).delete(any());
        }
    }

    // -----------------------------------------------------------------------
    private static Schedule stubSchedule() {
        Schedule schedule = mock(Schedule.class);
        given(schedule.getScheduleId()).willReturn(SCHEDULE_ID);
        given(schedule.getTitle()).willReturn("거래처 미팅");
        given(schedule.getContent()).willReturn("내용");
        given(schedule.getStartAt()).willReturn(LocalDateTime.of(2026, 5, 20, 10, 0));
        given(schedule.getEndAt()).willReturn(LocalDateTime.of(2026, 5, 20, 11, 30));
        given(schedule.isAllDay()).willReturn(false);
        given(schedule.getRepeatType()).willReturn(RepeatType.NONE);
        given(schedule.getColor()).willReturn("#FF5733");
        given(schedule.getCreatedBy()).willReturn("admin");
        given(schedule.getCreateDate()).willReturn(LocalDateTime.of(2026, 5, 16, 14, 30));
        return schedule;
    }
}
