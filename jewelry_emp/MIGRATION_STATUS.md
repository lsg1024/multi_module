# jewelry_emp 마이그레이션 현황 보고서

**작업 일자**: 2026-05-05
**범위**: order/account/product/user 4개 서비스 코드 전체 흡수

---

## ✅ 완료된 작업

### 1. 디렉토리 / 빌드 구조

- [x] `multi_module/jewelry_emp/` 모듈 생성
- [x] `multi_module/settings.gradle` 에 `include 'jewelry_emp'` 추가
- [x] `multi_module/build.gradle` 에 `project(':jewelry_emp')` 블록 추가
- [x] `jewelry_emp/build.gradle` — Spring Boot 3.4.7 + Spring Modulith + Spring Cloud Config + Eureka

### 2. 코드 흡수 (총 415개 파일)

| 모듈 | 흡수 파일 수 | 출처 |
| --- | --- | --- |
| `account/internal/` | 102 | account-service/local + global (Kafka 제외) |
| `product/internal/` | 155 | product-service/local + global (Kafka 제외) |
| `order/internal/` | 137 | order-service/local + global (Kafka 제외) |
| `user/internal/` | 21 | user-service/domain + exception |

패키지 일괄 변환 (sed):
- `com.msa.account.local.*` → `com.msa.jewelry.account.internal.*`
- `com.msa.account.global.*` → `com.msa.jewelry.account.internal.global.*`
- (product / order / user 동일 패턴)

### 3. 인프라 설정

- [x] `bootstrap.yml` — config-server (`http://...@175.117.4.180:9000`) 연결, profile=dev
- [x] `application.yml` — fallback 기본값 + JPA / Modulith / Batch 설정
- [x] `application-local.yml` — 로컬 개발 프로파일
- [x] Eureka client 설정 — `${spring.application.name}:${server.port:8023}`
- [x] `db/migration/legacy_*` — 기존 4개 서비스의 SQL 28개 보존

### 4. Kafka 추상화 (Redis 전환 대비)

- [x] `shared/messaging/EventPublisher` — Kafka/Redis/ApplicationEvent 추상화 인터페이스
- [x] `shared/messaging/SpringApplicationEventPublisher` — 현재 구현체 (in-process)
- [x] `account/internal/global/kafka/KafkaProducer.java` — stub (시그니처 보존)
- [x] `product/internal/global/kafka/KafkaProducer.java` — stub
- [x] `order/internal/global/kafka/KafkaProducer.java` — stub

호출 측 코드 무수정 동작 가능. 추후 Redis Streams 구현체로 교체 시 stub 의 내부만 변경.

### 5. 모듈 API 인터페이스 + 일부 구현체

- [x] `account/api/StoreFinder` + `StoreFinderImpl` (실 구현)
- [x] `account/api/FactoryFinder` + `FactoryFinderImpl` (실 구현)
- [x] `account/api/StoreView`, `FactoryView`
- [x] `account/api/StoreBalanceUpdater` (인터페이스만)
- [x] `product/api/ProductFinder` + `ProductFinderImpl` (실 구현, 일부 메서드 미완)
- [x] `product/api/ProductView`
- [x] `order/api/StockFinder` + `StockView` (인터페이스만)

### 6. 공유 인프라

- [x] `shared/event/DomainEvent`, `SaleRegisteredEvent`
- [x] `shared/exception/DomainException`, `NotFoundException`
- [x] `shared/messaging/EventPublisher`
- [x] `config/GlobalExceptionHandler`
- [x] `config/JpaConfig` (JPAQueryFactory 빈)
- [x] `config/RedisConfig`
- [x] Modulith 모듈 경계 검증 테스트 (`ModularityTests`)

---

## ⚠️ 추가 작업 필요

### 1. Feign 클라이언트 호출 측 정리 (높은 우선순위)

`@EnableFeignClients` 비활성화 상태이므로, 다음 wrapper 클래스들은
런타임에 빈 주입 실패 (NoSuchBeanDefinition):

```
order/internal/global/feign_legacy/client/StoreClient.java
order/internal/global/feign_legacy/client/FactoryClient.java
order/internal/global/feign_legacy/client/ProductClient.java
order/internal/global/feign_legacy/client/MaterialClient.java
order/internal/global/feign_legacy/client/ColorClient.java
order/internal/global/feign_legacy/client/ClassificationClient.java
order/internal/global/feign_legacy/client/SetTypeClient.java
order/internal/global/feign_legacy/client/AssistantStoneClient.java
order/internal/global/feign_legacy/client/StoneClient.java
product/internal/global/feign_legacy/client/FactoryClient.java
product/internal/global/feign_legacy/client/StockClient.java
user/internal/feign_legacy/AccountClient.java
```

**대응 방법**:

- 각 `XxxClient` 에서 `private final XxxFeignClient` 를 제거하고
  대신 `private final XxxFinder` (모듈 API) 를 주입.
- 메서드 본문에서 Feign 호출 → 모듈 API 호출로 변환.
- 변환 끝나면 `feign_legacy/` 폴더 전체 삭제.

이 작업은 약 12 개 wrapper 클래스 × 평균 30 분 ≈ **6 시간** 소요 예상.

### 2. 흡수된 코드의 외부 모듈 참조 정리

다음 import 들이 코드 곳곳에 잔존 가능 (sed 변환에서 빠진 변형):

```bash
grep -rn "com\.msa\.account\.\|com\.msa\.product\.\|com\.msa\.order\.\|com\.msa\.userserver\." \
   /multi_module/jewelry_emp/src
```

원래 `local`/`global` 외 형태 (`com.msa.account.AccountServiceApplication.class` 등) 가
코드 안에 있을 수 있음. 컴파일 시도하면서 하나씩 해결.

### 3. Outbox 정리

`order/internal/outbox/` 의 자체 구현 (`OutboxEvent`, `OutboxRelayService`,
`OutboxNotificationListener`, `OutboxEventScheduler`, `OutboxEventListener`) 은
모놀리스 통합 후 다음으로 대체 가능:

- **단순한 경우**: 같은 트랜잭션 내 직접 메서드 호출 (Outbox 불필요)
- **fan-out 필요**: Spring Modulith JPA Outbox (`event_publication` 테이블 자동 사용)

기존 Outbox 코드는 그대로 두면 동작은 하지만 Kafka stub 을 통해 자기 자신에게
이벤트 publish 하는 자기-루프가 됨. 점진적으로 제거 권장.

### 4. SQL 마이그레이션 통합

`db/migration/legacy_*` 폴더 28 개 SQL 을 다음 중 하나로 정리:

- **Option A**: `monolith-skeleton/db-migration/01_create_unified_schema.sql` 의
  통합 베이스라인을 검증 후 `V1__init_unified_schema.sql` 에 적용.
- **Option B**: 도메인 prefix 를 붙여 순차 V1~V30 으로 재배열.

운영 정책상 `spring.flyway.enabled=false` 이므로 본 폴더의 SQL 은 참고용.
실제 DDL 적용은 운영자가 수동.

### 5. 테스트 작성

```bash
./gradlew :jewelry_emp:test --tests ModularityTests
```

위 테스트가 통과해야 모듈 경계가 깨끗함을 보장. 처음 실행 시 다수 위반 검출 예상
(api/internal 구분이 아직 완벽하지 않음).

### 6. Q-class 재생성

QueryDSL 의 Q-class 들 (`QStore`, `QProduct`, `QOrders` 등) 은 패키지 이동에 따라
재생성 필요:

```bash
./gradlew :jewelry_emp:clean :jewelry_emp:compileJava
```

annotation processor 가 `build/generated/sources/annotationProcessor/java/main/com/msa/jewelry/.../Q*.java` 를 새로 생성.

### 7. config-server 의 jewelry-dev.properties 내용 확인

다음 항목이 필요:

```properties
server.port=8023
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:8021/jewelry_emp
spring.datasource.username=...
spring.datasource.password=...
kafka.uri=...   # Redis 전환 후 redis.uri 로 교체 예정
jwt.secret=...
qz.private-key=...
spring.data.redis.host=...
spring.data.redis.port=...
```

---

## 📊 마이그레이션 진척도

| 영역 | 진척도 | 비고 |
| --- | --- | --- |
| 코드 흡수 / 패키지 변환 | 100% | sed 일괄 처리로 완료 |
| 인프라 설정 (bootstrap, Eureka) | 100% | 다른 서비스와 동일 패턴 |
| Kafka 추상화 stub | 100% | 호출 측 무수정 동작 |
| 모듈 API 정의 | 80% | 핵심 인터페이스 존재 |
| 모듈 API 구현 | 30% | StoreFinder, FactoryFinder, ProductFinder 일부 |
| Feign → 직접 호출 변환 | 5% | 주요 wrapper 클래스 정리 필요 |
| Outbox / Spring Modulith Event | 10% | 기존 코드 유지 상태 |
| SQL 통합 | 분석 단계 | legacy_* 보존, 통합 베이스라인 작성 필요 |
| 컴파일 가능 여부 | **미확인** | Gradle 검증 필요 (샌드박스에서 불가) |

---

## 🎯 다음 단계 권장 순서

### Phase A — 컴파일 통과 (1~2 일)
1. `./gradlew :jewelry_emp:compileJava` 실행
2. 컴파일 에러 발생 위치 파악
3. 주로 import 변형, 누락된 의존성, Q-class 미생성 등 해결

### Phase B — Feign 제거 (3~5 일)
1. wrapper 클래스 12 개 변환 (StoreClient → StoreFinder 사용)
2. `feign_legacy/` 폴더 삭제
3. Spring Cloud OpenFeign 의존성 제거 (auth-service Feign 만 별도 패키지로 분리)

### Phase C — Outbox / Kafka 정리 (3~5 일)
1. KafkaProducer stub 의 호출 측을 직접 호출로 점진 변환
2. Outbox 자체 구현 → Spring Modulith JPA Outbox 로 점진 이전
3. KafkaProducer stub 호출이 0건이면 stub 도 삭제

### Phase D — DB 통합 (1 주)
1. legacy_* SQL 분석 → 통합 V1__init_schema.sql 작성
2. 운영 데이터 이관 SQL 작성 (`monolith-skeleton/db-migration/02~04`)
3. Staging 환경에서 시뮬레이션

### Phase E — 운영 절체 (1 일)
1. 다운타임 1~3 시간 확보
2. 데이터 이관 + 새 모놀리스 기동
3. 스모크 테스트 / 트래픽 전환

---

## 🔧 현재 빌드 시도 시 예상되는 에러

샌드박스에서 Gradle 컴파일 검증이 불가능해 다음 에러들이 잠복 가능:

1. `cannot find symbol: class XxxFeignClient` — feign_legacy 의 Fallback 클래스가 새 패키지를 못 찾는 경우
2. `cannot find symbol: variable kafkaProducer` — sed 가 놓친 일부 import
3. Q-class 미생성으로 인한 QueryDSL 코드 실패 (clean 후 재컴파일로 해결)
4. `spring.cloud.openfeign.client.config.X.url` 같은 미해결 placeholder
5. JPA EntityManager 가 모든 모듈의 엔티티를 스캔하는지 확인 필요 (`@EntityScan` 또는 자동 스캔)

이런 에러들은 **컴파일 → 수정 → 재컴파일** 의 반복적 정리 과정으로 해결.
