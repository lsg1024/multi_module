<h2>개요</h2>
<p>
프로젝트 이름: 상품 주문장<br>
프로젝트 진행 기간: 25.07.01 ~ 진행 중<br>

프로젝트 진행 사유: 기존 사용중인 프로그램 가격 변경으로 인해 기존 제품 대체를 위해 개발
<br>

<h2>Backend</h2>
Language: Java 17<br>
Framework: Spring Boot 3.4.7<br>
MSA: Spring Cloud (Eureka, Gateway, Config, OpenFeign, CircuitBreaker)<br>
Database: PostgreSQL, Redis (Caching, Session, Distributed Lock)<br>
Messaging: Apache Kafka (Event-Driven Architecture)<br>
Batch: Spring Batch<br>
ORM: JPA (Hibernate), QueryDSL<br>
Security: Spring Security, JWT<br>

<h2>DevOps & Tools</h2>
CI/CD: GitHub Actions<br>
Container: Docker<br>
Infrastructure: Synology NAS (Deployment Target), AWS EC2 (implied)<br>
Migration: Flyway<br>

<h2>사용 언어 및 기술</h2>
<div align="center">
<p style="font-weight: bolder">사용 언어 및 프레임 워크</p>
    <img src="https://img.shields.io/badge/java-007396?style=for-the-badge&logo=java&logoColor=white">
    <img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white">
    <img src="https://img.shields.io/badge/Spring_Cloud-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
    <img src="https://img.shields.io/badge/Spring_Batch-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
    <br>
    <img src="https://img.shields.io/badge/postgresql-003545?style=for-the-badge&logo=postgresql&logoColor=white">
    <img src="https://img.shields.io/badge/redis-FF4438.svg?&style=for-the-badge&logo=redis&logoColor=white">
    <img src="https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white">

<p style="font-weight: bolder">사용 기술</p>
    <img src="https://img.shields.io/badge/AWS-EC2-%23FF9900.svg?style=for-the-badge&logo=amazon-aws&logoColor=white">
    <img src="https://img.shields.io/badge/synology-black?style=for-the-badge&logo=synology&logoColor=white">
    <img src="https://img.shields.io/badge/flayway-CC0200?style=for-the-badge&logo=flyway&logoColor=white">
    <img src="https://img.shields.io/badge/docker-2496ED?style=for-the-badge&logo=docker&logoColor=white">
    <img src="https://img.shields.io/badge/githubactions-2088FF?style=for-the-badge&logo=githubactions&logoColor=white">

</div>

<h3>DB ERD</h3>
<div align="center"><img src="src/main/resources/static/images/ERD.png"></div>


<h2>주요 기능 상세 분석</h2>
🔐 인증 및 멀티 테넌시 (Auth & Security)
JWT 기반 인증: Gateway에서 AuthorizationHeaderFilter를 통해 토큰 유효성을 검증하고, X-User-ID, X-Tenant-ID 헤더를 다운스트림 서비스로 전달합니다.

Multi-tenancy: 모든 데이터 접근 시 TenantContext를 통해 테넌트 ID를 분리하여 데이터 격리를 수행합니다 (CurrentTenantIdentifierResolver).

📦 주문 및 재고 프로세스 (Order & Stock)
주문 접수: 상점, 공장, 상품 옵션(보석, 재질, 색상 등)을 선택하여 주문을 생성합니다.

재고 전환: 주문이 완료되면 해당 데이터는 고유한 FlowCode를 가진 Stock(재고) 상태로 변환됩니다.

판매 및 결제: 재고 상품을 판매 처리하면 매출이 발생하고, 결제 내역(현금/고금)이 기록됩니다.

히스토리 관리: StatusHistory 테이블을 통해 상품의 모든 상태 변경 이력(주문->재고->대여->반납 등)을 추적합니다.

🏭 거래처 및 정산 관리 (Account)
해리(Harry) 관리: 귀금속 거래의 핵심인 금 손실율(Harry)을 거래처별, 등급별로 관리합니다.

미수 관리: 주문/판매 발생 시 Kafka 이벤트를 수신하여 거래처의 **금 잔액(Gold Balance)**과 **현금 잔액(Money Balance)**을 실시간으로 업데이트합니다.

🔄 데이터 동기화 및 배치 (Kafka & Batch)
Event-Driven: order-service에서 판매가 발생하면 OutboxEvent를 발행하고, OutboxRelayService가 이를 Redis 큐를 통해 Kafka로 전송합니다. account-service는 이를 소비하여 미수금을 갱신합니다.

Batch Processing: 대량의 상품 데이터 업로드, 초기 데이터 마이그레이션 등을 위해 Spring Batch를 사용합니다.
