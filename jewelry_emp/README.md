# jewelry_emp — 모놀리식 후보 서버

기존 `multi_module` 의 `order-service`, `account-service`, `product-service`,
`user-service` 4개 마이크로서비스를 흡수해 단일 부트 앱으로 운영하기 위한 모듈.

## 빌드 및 실행

```bash
# 전체 multi_module 루트에서 빌드
cd /path/to/multi_module
./gradlew :jewelry_emp:bootJar

# 실행 (로컬 프로파일)
SPRING_PROFILES_ACTIVE=local \
DB_HOST=localhost DB_NAME=kkhan DB_USER=kkhan DB_PASSWORD=kkhan \
REDIS_HOST=localhost \
JWT_SECRET=your-secret-at-least-32-chars-long \
java -jar jewelry_emp/build/libs/jewelry_emp-0.0.1-SNAPSHOT.jar

# 또는 IDE 에서 JewelryEmpApplication 직접 실행
```

## 디렉토리 구조

```
jewelry_emp/
├── build.gradle                                              ← Spring Boot + Modulith
├── README.md
└── src/
    ├── main/
    │   ├── java/com/msa/jewelry/
    │   │   ├── JewelryEmpApplication.java                    ← 진입점 (@Modulithic)
    │   │   ├── package-info.java                             ← 모듈 구조 문서
    │   │   ├── config/
    │   │   │   ├── GlobalExceptionHandler.java               ← 통합 예외 처리
    │   │   │   ├── JpaConfig.java                            ← JPAQueryFactory 빈
    │   │   │   └── RedisConfig.java                          ← RedisTemplate
    │   │   ├── account/                                      ← 거래처/제조사/잔고
    │   │   │   ├── package-info.java                         ← @ApplicationModule
    │   │   │   ├── api/                                      ← 다른 모듈에 노출
    │   │   │   │   ├── StoreFinder.java
    │   │   │   │   ├── StoreView.java
    │   │   │   │   ├── FactoryFinder.java
    │   │   │   │   ├── FactoryView.java
    │   │   │   │   └── StoreBalanceUpdater.java
    │   │   │   └── internal/                                 ← 모듈 외부 import 차단
    │   │   │       └── AccountModulePlaceholder.java
    │   │   ├── order/                                        ← 주문/재고/판매
    │   │   │   ├── package-info.java
    │   │   │   ├── api/
    │   │   │   │   ├── StockFinder.java
    │   │   │   │   └── StockView.java
    │   │   │   └── internal/
    │   │   │       └── OrderModulePlaceholder.java
    │   │   ├── product/                                      ← 상품/스톤/카탈로그
    │   │   │   ├── package-info.java
    │   │   │   ├── api/
    │   │   │   │   ├── ProductFinder.java
    │   │   │   │   └── ProductView.java
    │   │   │   └── internal/
    │   │   │       └── ProductModulePlaceholder.java
    │   │   ├── user/                                         ← 사용자/SMS
    │   │   │   ├── package-info.java
    │   │   │   └── internal/
    │   │   │       └── UserModulePlaceholder.java
    │   │   └── shared/                                       ← 모듈 간 공유
    │   │       ├── event/
    │   │       │   ├── DomainEvent.java
    │   │       │   └── SaleRegisteredEvent.java
    │   │       └── exception/
    │   │           ├── DomainException.java
    │   │           └── NotFoundException.java
    │   └── resources/
    │       ├── application.yml                               ← 통합 설정
    │       ├── application-local.yml
    │       └── db/migration/
    │           └── V1__init_unified_schema.sql               ← Flyway baseline
    └── test/
        └── java/com/msa/jewelry/
            └── ModularityTests.java                          ← 모듈 경계 검증
```

## 모듈 의존 관계

```
              ┌──────────┐
              │  order   │ ─────┐
              └──────────┘      │
                    │           ↓
                    ↓     ┌──────────┐
              ┌──────────┐│ product  │
              │ account  │└──────────┘
              └──────────┘      │
                    │           │
                    └───────────┴──→  shared (event / exception)

              ┌──────────┐
              │   user   │ ──→ shared
              └──────────┘
```

`package-info.java` 의 `@ApplicationModule(allowedDependencies = ...)` 으로 강제됨.

## 마이그레이션 단계 (현재 위치)

| 단계 | 상태 | 설명 |
| --- | --- | --- |
| Phase 0 — 사전 준비 | ✅ 완료 | 디렉토리 + build.gradle + 모듈 스켈레톤 |
| Phase 1 — 코드 흡수 | ⏳ 다음 | account/order/product/user 의 internal 클래스 이동 |
| Phase 2 — Feign 제거 | ⏳ 대기 | StoreClient → StoreFinder 등 일괄 변환 (16개) |
| Phase 3 — Kafka 제거 | ⏳ 대기 | KafkaProducer/Consumer → @ApplicationModuleListener / 직접 호출 |
| Phase 4 — DB 통합 | ⏳ 대기 | V2~V5 데이터 이관 SQL 적용 |
| Phase 5 — 운영 절체 | ⏳ 대기 | 다운타임 1~3시간 |
| Phase 6 — 정리 | ⏳ 대기 | eureka 종료, Kafka 클러스터 종료 |

## 다음 작업 가이드

### 1. account 모듈 흡수 (가장 먼저 — leaf 모듈)

```bash
# 기존 코드 이동 (예시)
cp -r account-service/src/main/java/com/msa/account/local/store/domain/entity/* \
      jewelry_emp/src/main/java/com/msa/jewelry/account/internal/

# 패키지 선언 수정 (sed 일괄)
find jewelry_emp/src/main/java/com/msa/jewelry/account/internal/ -name "*.java" \
  -exec sed -i '' 's|com\.msa\.account\.local\.store\.domain\.entity|com.msa.jewelry.account.internal|g' {} \;

# Feign / Kafka 의존 코드 제거
# StoreFinderImpl 작성 → StoreFinder 인터페이스 구현
# AccountModulePlaceholder.java 삭제
```

### 2. 모듈 검증 테스트 실행

```bash
./gradlew :jewelry_emp:test --tests ModularityTests
```

실패하면 콘솔에 어떤 클래스가 모듈 경계를 위반했는지 표시됨. 차근차근 수정.

### 3. 통합 테스트 작성

기존 4개 서비스의 테스트를 가져와 단일 SpringBootTest 컨텍스트에서 실행.
같은 트랜잭션 내 동작을 검증하는 새 테스트 추가 권장:

```java
@Test
@Transactional
void salesRegistrationAndBalanceUpdateAreAtomic() {
    // 판매 등록 → 잔고 갱신이 같은 TX 에서 처리되는지 확인.
    // 중간에 예외 던지면 양쪽 모두 롤백되어야 한다.
}
```

## 관련 문서

- `monolith-skeleton/feign-migration/01_StoreClient_to_StoreFinder.md`
  — Feign → 모듈 API 변환 가이드 (시범 사례)
- `monolith-skeleton/db-migration/00_overview.md`
  — DB 통합 전략 / 옵션 비교
- `monolith-skeleton/db-migration/01_create_unified_schema.sql`
  — 통합 스키마 DDL (V1 으로 복사하여 적용)
