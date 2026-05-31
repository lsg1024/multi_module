-- ============================================================================
-- V5 — order_product.stone_add_labor_cost 컬럼 추가
-- ----------------------------------------------------------------------------
-- 배경:
--   OrderProduct 엔티티에 @Column(name = "STONE_ADD_LABOR_COST") 매핑이
--   있는데 V1 schema 의 order_product 테이블에 컬럼 자체가 누락되어 있었음.
--   → POST /orders 등록 시 INSERT INTO order_product (... STONE_ADD_LABOR_COST ...)
--     가 PostgreSQL 42703 (column does not exist) 로 500 응답.
--
--   참고: stock, order_stone 테이블에는 이미 stone_add_labor_cost 가 있다.
--         order_product 만 누락된 패턴 일관성 보정.
--
-- 멱등성:
--   ADD COLUMN IF NOT EXISTS 로 가드.
-- ============================================================================

ALTER TABLE order_product
    ADD COLUMN IF NOT EXISTS stone_add_labor_cost INTEGER;
