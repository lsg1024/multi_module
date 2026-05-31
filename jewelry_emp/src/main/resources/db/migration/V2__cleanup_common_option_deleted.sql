-- ============================================================================
-- V2 — CommonOption 의 soft delete 흔적 정리
-- ----------------------------------------------------------------------------
-- 배경:
--   CommonOption 은 Store/Factory 에 1:1 종속되는 엔티티라 자체 soft delete 가
--   불필요하다. 부모(거래처) 가 삭제되면 CASCADE 로 자동 삭제된다.
--
--   엔티티 측에서 @SQLDelete 와 boolean deleted 필드를 제거했으니, 만약 어떤
--   환경에서 실수로 common_option 테이블에 deleted 컬럼이 추가되었거나,
--   deleted = TRUE 로 표시되어 살아있는 row 가 남아 있다면 정리한다.
--
-- 멱등성:
--   IF EXISTS 가드를 사용해 컬럼/row 가 없는 환경에서도 안전하게 통과한다.
--   V1 만 적용된 깨끗한 환경에서는 no-op 으로 동작.
-- ============================================================================

-- 1) deleted = TRUE 로 표시된 row 가 남아 있으면 실제 DELETE 로 정리.
--    (컬럼 자체가 없으면 IF 가드로 스킵)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'common_option' AND column_name = 'deleted'
    ) THEN
        DELETE FROM common_option WHERE deleted = TRUE;
    END IF;
END $$;

-- 2) deleted 컬럼 자체 제거 (없으면 no-op)
ALTER TABLE common_option DROP COLUMN IF EXISTS deleted;
