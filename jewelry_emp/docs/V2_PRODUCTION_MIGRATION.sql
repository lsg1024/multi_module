-- ====================================================================
-- jewelry_emp 운영 DB 마이그레이션 SQL (V2 후보)
--
-- 적용 대상 : 기존 MSA 4개 서비스가 운영하던 PostgreSQL 데이터베이스
-- 적용 시점 : jewelry_emp 모놀로식 운영 절체 시점
-- 전제 조건 : V1__init_unified_schema.sql 은 신규 환경용. 기존 운영 DB 는
--             account-service / order-service / product-service / user-service 의
--             각 legacy_*.sql 이 이미 적용된 상태이며, 본 SQL 은 그 위에
--             "jewelry_emp 모놀로식 변경분(P0~P4)" 을 추가 반영하는 단방향 패치.
--
-- 무엇을 하나
--   1. (P0) ORDERS / STATUS_HISTORY / ORDER_PRODUCT / STOCK / OUT_BOX_EVENT 의
--      TIMESTAMPTZ → TIMESTAMP 변환 (Asia/Seoul 기준으로 데이터 보존)
--   2. (P1) ORDERS 에 audit 컬럼 4개 (create_date, last_modified_date,
--      created_by, last_modified_by) 추가 + 기존 데이터 backfill
--   3. (P3) out_box_event 테이블 + LISTEN/NOTIFY 트리거 제거
--   4. (P4) ORDERS / STOCK 의 store_name, factory_name 컬럼 제거
--
-- 권장 적용 순서
--   - 운영 절체 직전 staging 환경에서 동일 SQL 을 dry-run 시뮬레이션
--   - 백업 (pg_dump) 확보 후 다운타임 1시간 정도 잡고 실행
--   - 실행 후 jewelry_emp 부팅 → 응답 샘플 검증 → 트래픽 전환
-- ====================================================================

BEGIN;

-- --------------------------------------------------------------------
-- 1) TIMESTAMPTZ → TIMESTAMP 변환 (Asia/Seoul 시각으로 보존)
-- --------------------------------------------------------------------
-- ORDERS
ALTER TABLE ORDERS
    ALTER COLUMN CREATE_AT      TYPE TIMESTAMP USING CREATE_AT      AT TIME ZONE 'Asia/Seoul',
    ALTER COLUMN SHIPPING_AT    TYPE TIMESTAMP USING SHIPPING_AT    AT TIME ZONE 'Asia/Seoul';

-- STATUS_HISTORY
ALTER TABLE STATUS_HISTORY
    ALTER COLUMN CREATED_AT     TYPE TIMESTAMP USING CREATED_AT     AT TIME ZONE 'Asia/Seoul';

-- ORDER_PRODUCT
ALTER TABLE ORDER_PRODUCT
    ALTER COLUMN ASSISTANT_STONE_CREATE_AT TYPE TIMESTAMP
        USING ASSISTANT_STONE_CREATE_AT AT TIME ZONE 'Asia/Seoul';

-- STOCK (ProductSnapshot embedded)
ALTER TABLE STOCK
    ALTER COLUMN ASSISTANT_STONE_CREATE_AT TYPE TIMESTAMP
        USING ASSISTANT_STONE_CREATE_AT AT TIME ZONE 'Asia/Seoul';


-- --------------------------------------------------------------------
-- 2) ORDERS audit 컬럼 추가 + backfill
--    (Orders 가 BaseEntity 를 상속하면서 4개 audit 컬럼이 필요)
-- --------------------------------------------------------------------
ALTER TABLE ORDERS ADD COLUMN IF NOT EXISTS create_date         TIMESTAMP;
ALTER TABLE ORDERS ADD COLUMN IF NOT EXISTS last_modified_date  TIMESTAMP;
ALTER TABLE ORDERS ADD COLUMN IF NOT EXISTS CREATED_BY          VARCHAR(255);
ALTER TABLE ORDERS ADD COLUMN IF NOT EXISTS LAST_MODIFIED_BY    VARCHAR(255);

-- 기존 주문의 audit 값 backfill: CREATE_AT(도메인 접수일) 을 audit 시작점으로 사용.
UPDATE ORDERS SET create_date        = CREATE_AT          WHERE create_date        IS NULL;
UPDATE ORDERS SET last_modified_date = CREATE_AT          WHERE last_modified_date IS NULL;
UPDATE ORDERS SET CREATED_BY         = 'legacy-migration' WHERE CREATED_BY         IS NULL;
UPDATE ORDERS SET LAST_MODIFIED_BY   = 'legacy-migration' WHERE LAST_MODIFIED_BY   IS NULL;


-- --------------------------------------------------------------------
-- 3) Outbox 관련 객체 제거 (P3 에서 코드 자체 제거됨)
-- --------------------------------------------------------------------
-- 만약 V8__create_outbox_notify_trigger.sql 에서 만든 trigger 가 있다면 먼저 drop
DROP TRIGGER IF EXISTS trg_outbox_notify ON OUT_BOX_EVENT;
DROP FUNCTION IF EXISTS notify_outbox_event();
DROP TABLE  IF EXISTS OUT_BOX_EVENT CASCADE;


-- --------------------------------------------------------------------
-- 4) ORDERS / STOCK 의 store_name, factory_name 컬럼 제거 (P4)
--    이름은 runtime 에 storeFinder / factoryFinder 로 조회.
--    삭제 전 데이터 손실이 걱정되면 별도 archive 테이블에 복사하는 단계 권장.
-- --------------------------------------------------------------------
-- (선택) 데이터 백업 — 운영자가 별도 정책 결정
-- CREATE TABLE _archive_orders_names AS
--     SELECT order_id, store_name, factory_name FROM ORDERS WHERE store_name IS NOT NULL OR factory_name IS NOT NULL;
-- CREATE TABLE _archive_stock_names AS
--     SELECT stock_id, store_name, factory_name FROM STOCK  WHERE store_name IS NOT NULL OR factory_name IS NOT NULL;

ALTER TABLE ORDERS DROP COLUMN IF EXISTS store_name;
ALTER TABLE ORDERS DROP COLUMN IF EXISTS factory_name;

ALTER TABLE STOCK  DROP COLUMN IF EXISTS store_name;
ALTER TABLE STOCK  DROP COLUMN IF EXISTS factory_name;


-- --------------------------------------------------------------------
-- 5) 확인 쿼리 (커밋 전 검증)
-- --------------------------------------------------------------------
-- SELECT column_name, data_type, is_nullable
--   FROM information_schema.columns
--  WHERE table_name = 'orders' AND table_schema = current_schema();
--
-- SELECT column_name, data_type
--   FROM information_schema.columns
--  WHERE table_name = 'stock' AND table_schema = current_schema()
--    AND column_name IN ('store_name','factory_name','create_at','shipping_at');

COMMIT;

-- ====================================================================
-- 적용 후 jewelry_emp 부팅 → 검증 체크리스트
-- --------------------------------------------------------------------
-- [ ] SELECT CREATE_AT, create_date FROM ORDERS LIMIT 5 — 두 값이 같은 KST 시각
-- [ ] 주문 1건 생성 API 호출 후 CREATED_BY 가 토큰의 사용자명으로 채워졌는지
-- [ ] 응답 JSON 의 storeName 이 storeFinder 로 채워진 값인지 (placeholder "" 가 아닌)
-- [ ] StatusHistory.created_at 이 KST 로 잘 기록되는지
-- [ ] 기존 응답 시그니처 (storeName/factoryName 필드명) 가 그대로 노출되는지
-- ====================================================================
