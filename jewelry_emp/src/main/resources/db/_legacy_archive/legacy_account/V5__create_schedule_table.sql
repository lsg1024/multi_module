-- Schedule 테이블 생성 (일정 관리)
CREATE TABLE IF NOT EXISTS schedule (
    schedule_id         BIGSERIAL PRIMARY KEY,
    title               VARCHAR(100) NOT NULL,
    content             VARCHAR(500),
    start_at            TIMESTAMP NOT NULL,
    end_at              TIMESTAMP NOT NULL,
    all_day             BOOLEAN NOT NULL DEFAULT FALSE,
    repeat_type         VARCHAR(20) NOT NULL DEFAULT 'NONE',
    color               VARCHAR(20),
    create_date         TIMESTAMP,
    last_modified_date  TIMESTAMP,
    created_by          VARCHAR(255),
    last_modified_by    VARCHAR(255)
);

-- 인덱스 생성 (기간별 조회 성능 향상)
CREATE INDEX IF NOT EXISTS idx_schedule_start_at ON schedule(start_at);
CREATE INDEX IF NOT EXISTS idx_schedule_end_at ON schedule(end_at);
CREATE INDEX IF NOT EXISTS idx_schedule_date_range ON schedule(start_at, end_at);

COMMENT ON TABLE schedule IS '일정 관리 테이블';
COMMENT ON COLUMN schedule.schedule_id IS '일정 ID';
COMMENT ON COLUMN schedule.title IS '일정 제목';
COMMENT ON COLUMN schedule.content IS '일정 내용';
COMMENT ON COLUMN schedule.start_at IS '시작 일시';
COMMENT ON COLUMN schedule.end_at IS '종료 일시';
COMMENT ON COLUMN schedule.all_day IS '종일 여부';
COMMENT ON COLUMN schedule.repeat_type IS '반복 유형 (NONE, DAILY, WEEKLY, MONTHLY, YEARLY)';
COMMENT ON COLUMN schedule.color IS '일정 색상';
COMMENT ON COLUMN schedule.create_date IS '생성일시';
COMMENT ON COLUMN schedule.last_modified_date IS '수정일시';
COMMENT ON COLUMN schedule.created_by IS '생성자';
COMMENT ON COLUMN schedule.last_modified_by IS '수정자';
