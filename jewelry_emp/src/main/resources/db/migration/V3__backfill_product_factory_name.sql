-- ============================================================================
-- V3 — (1) product_factory_name 백필  +  (2) gold_harry soft delete 흔적 정리
-- ============================================================================

-- ----------------------------------------------------------------------------
-- (1) product_factory_name 백필
-- ----------------------------------------------------------------------------
-- 배경:
--   ProductBatchJob 의 ProductItemProcessor 에서 productFactoryName 이 비어 있으면
--   productName 으로 폴백하도록 패치했으나, 패치 전에 이미 등록된 상품들은
--   product_factory_name 이 NULL 또는 빈 문자열로 남아있을 수 있다.
--
-- 멱등성:
--   WHERE 조건이 비어 있으면 UPDATE 0건으로 통과.
-- ----------------------------------------------------------------------------
UPDATE product
   SET product_factory_name = product_name
 WHERE product_factory_name IS NULL
    OR TRIM(product_factory_name) = '';


-- ----------------------------------------------------------------------------
-- (2) gold_harry 의 soft delete 흔적 정리
-- ----------------------------------------------------------------------------
-- 배경:
--   GoldHarry 엔티티에서 @SQLDelete 와 boolean deleted 필드를 제거했다.
--   원래 V1 schema 의 gold_harry 테이블에는 deleted 컬럼이 없었지만, 만약
--   어떤 환경에서 실수로 컬럼이 추가되었거나, deleted = TRUE 로 표시되어
--   살아있는 row 가 남아 있다면 정리한다.
--
-- 멱등성:
--   IF EXISTS 가드로 컬럼/row 가 없는 환경에서도 안전하게 통과.
-- ----------------------------------------------------------------------------

-- 2-1) deleted = TRUE 로 표시된 row 가 있으면 실제 DELETE 로 정리
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'gold_harry' AND column_name = 'deleted'
    ) THEN
        DELETE FROM gold_harry WHERE deleted = TRUE;
    END IF;
END $$;

-- 2-2) deleted 컬럼 자체 제거 (없으면 no-op)
ALTER TABLE gold_harry DROP COLUMN IF EXISTS deleted;
