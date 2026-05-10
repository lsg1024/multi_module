# DB 마이그레이션 (jewelry_emp 통합 DB)

기존 4개 서비스의 Flyway 마이그레이션을 통합하기 위한 작업 폴더.

## 폴더 구성

```
db/migration/
├── README.md                       ← 본 문서
├── V1__init_unified_schema.sql     ← (placeholder) 통합 베이스라인 작성 위치
├── legacy_account/                 ← account-service Flyway 마이그레이션 8 개 (그대로 보존)
│   ├── V1__create_account.sql
│   ├── V2__create_account.sql
│   ├── V3__add_material_column_to_transaction_history.sql
│   ├── V4__add_sale_log.sql
│   ├── V5__create_schedule_table.sql
│   ├── V6__alter_owner_name_nullable.sql
│   ├── V7__create_ledger_table.sql
│   └── V8__create_expense_tables.sql
├── legacy_product/                 ← product-service Flyway 마이그레이션 8 개
│   ├── V1__create_product_table.sql
│   ├── V2__create_product_table.sql
│   ├── V3__create_product_table.sql
│   ├── V4__update_gold_table.sql
│   ├── V5__create_factory_stone_price_table.sql
│   ├── V6__drop_factory_stone_price_table.sql
│   ├── V7__add_product_stone_include_columns.sql
│   └── V20260427_001__add_product_image_embedding.sql
├── legacy_order/                   ← order-service Flyway 마이그레이션 9 개
│   └── V1~V9 (주문/재고/판매 + Outbox + 트리거)
└── legacy_user/                    ← user-service Flyway 마이그레이션 3 개
    └── V1~V3 (users + sens_config + message_history)

db/migration_batch/
└── V1__create_spring_batch.sql     ← Spring Batch 메타 테이블
```

## 통합 전략

### Option A: 신규 통합 베이스라인 작성 (권장)

각 legacy 폴더의 SQL 을 분석해 단일 V1 (또는 V1~Vn) 으로 재구성.
이 방식의 장점은 깔끔한 스키마, 단점은 작성·검증 시간 필요.

`monolith-skeleton/db-migration/01_create_unified_schema.sql` 에 이미 초안이 있음
→ 본 폴더의 `V1__init_unified_schema.sql` 로 복사하여 적용 가능.

### Option B: 기존 V1~Vn 을 그대로 순차 적용

각 legacy 폴더의 SQL 을 도메인 prefix 를 붙여 본 폴더에 직접 복사:

```
V0001__account_create_account.sql       (legacy_account/V1)
V0002__account_create_account_v2.sql    (legacy_account/V2)
...
V0010__order_create_order_table.sql     (legacy_order/V1)
...
V0030__product_create_product_table.sql (legacy_product/V1)
...
```

이 방식은 기존 운영 DB 의 마이그레이션 이력과 호환되는 경로 (각 service DB
에서 수행되었던 V1, V2 등을 단일 DB 에 순차 재현) 지만, 4개 도메인의 의존
순서를 잘 잡아야 한다 (예: account → product → order, store FK 가 product/order
에서 참조되므로).

### Option C (점진 이전): 운영 데이터 dblink 이관

`monolith-skeleton/db-migration/02~04_migrate_*_data.sql` 의 `dblink` 기반
SQL 을 사용해 운영 중인 3 개 DB → 새 DB 로 데이터 이관. 본 jewelry_emp 의
Flyway 는 빈 스키마만 정의하고, 데이터는 운영 절체 시 한 번에 옮긴다.

## 권장 절차

1. **분석 단계** (현재): legacy_* 폴더 내용 검토. 도메인 별로 어떤 테이블·인덱스·트리거가 있는지 파악.
2. **통합 스키마 작성**: `monolith-skeleton/db-migration/01_create_unified_schema.sql`
   의 초안을 본 운영 DDL 과 1:1 비교하여 차이점 보완. 최종본을 V1 으로 적용.
3. **데이터 이관 SQL 작성**: `monolith-skeleton/db-migration/02~04_migrate_*_data.sql`
   를 실제 운영 DB 접속 정보로 수정.
4. **운영 절체 리허설**: staging 환경에서 마이그레이션 시뮬레이션.
5. **운영 절체**: 다운타임 1~3 시간 확보 후 실제 데이터 이관.

## 운영 정책

- **Flyway 자체는 운영 환경에서 비활성화** (`spring.flyway.enabled=false`).
  스키마 변경은 운영자가 별도 검증 후 수동 적용하는 정책 유지 (기존 서비스와 동일).
- 운영 DB 접속은 `config-server` 의 `jewelry-dev.properties` 가 통제 (port 8021).

## 멀티 테넌시 고려사항

기존 시스템은 `tenant_id` 기반 schema-per-tenant 또는 row-level filter 사용.
common 모듈의 `SchemaMultiTenantConnectionProvider` 가 schema-per-tenant 라면
통합 DB 에서도 동일 패턴 유지 가능 (테넌트 별 schema 가 같은 DB 에 존재).

각 마이그레이션 SQL 의 `tenant_id` 컬럼은 그대로 유지.
