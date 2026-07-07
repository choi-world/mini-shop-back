# MiniShop Backend

> 결제/주문 도메인을 중심으로 **동시성 제어, 멱등성 보장, 보상 트랜잭션** 등 실무 패턴을 직접 구현하고 검증하는 개인 프로토타입 프로젝트입니다.  
> 운영 서비스가 아닌, 특정 기술 주제를 가설 → 구현 → 검증 사이클로 실험하는 것이 목적입니다.

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.4 |
| ORM | Spring Data JPA (Hibernate) |
| DB | MySQL 8.0 (로컬 Docker / 운영 AWS RDS) |
| Cache | Redis 7.2 |
| 인증 | Spring Security + JWT (jjwt 0.12) |
| 문서 | SpringDoc OpenAPI (Swagger UI) |
| 테스트 | JUnit 5, Mockito, SpringBootTest |
| 빌드 | Gradle (Groovy DSL) |

---

## 아키텍처

```
Controller
    ↓  (요청/응답만 담당)
Facade
    ↓  (유스케이스 조합, 트랜잭션 경계)
Service
    ↓  (단일 도메인 책임)
Repository
    ↓  (데이터 접근)
```

- **Controller**: 요청/응답 변환만 담당, 비즈니스 로직 없음
- **Facade**: 여러 Service를 조합해 하나의 유스케이스를 완성, `@Transactional` 경계
- **Service**: 단일 도메인의 원자적 작업 단위
- **Repository**: Spring Data JPA 기반 데이터 접근

### 디렉토리 구조

```
src/main/java/com/minishop/
├── auth/
│   ├── controller/
│   ├── facade/
│   ├── service/
│   ├── repository/
│   ├── dto/
│   └── entity/
├── user/
│   ├── service/
│   ├── repository/
│   ├── dto/
│   └── entity/
├── product/          # 예정
├── order/            # 예정
├── payment/          # 예정 (핵심 실험 도메인)
│   └── client/       # PG API 클라이언트
└── common/
    ├── config/
    ├── exception/
    ├── security/
    └── util/
```

---

## 구현 현황

| 도메인 | 상태 | 주요 내용 |
|--------|------|-----------|
| Auth | ✅ 완료 | 로그인/회원가입, JWT, Refresh Token Rotation |
| User | ✅ 완료 | 기본 엔티티 및 서비스 |
| Product | 🚧 예정 | 상품 CRUD, 재고 관리 |
| Order | 🚧 예정 | 주문 생성, 상태 관리 |
| Payment | 🚧 예정 | PG 연동, 결제 승인/취소 (핵심 실험 도메인) |

---

## API 명세

> Base URL: `http://localhost:8080/api`  
> Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`

### Auth

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| POST | `/auth/signup` | 회원가입 | 불필요 |
| POST | `/auth/login` | 로그인 | 불필요 |
| POST | `/auth/refresh` | Access Token 재발급 | 불필요 |
| POST | `/auth/logout` | 로그아웃 | 필요 |

<details>
<summary>POST /auth/signup</summary>

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "username": "홍길동",
  "phoneNumber": "01012345678",
  "birthday": "1990-01-01",
  "gender": 1
}
```

**Response** `201 Created`
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "user": {
    "id": 1,
    "username": "홍길동",
    "email": "user@example.com",
    "phoneNumber": "01012345678",
    "birthday": "1990-01-01",
    "gender": 1,
    "createdDt": "2026-07-07T00:00:00"
  }
}
```
</details>

<details>
<summary>POST /auth/login</summary>

**Request**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** `200 OK`
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "user": { ... }
}
```
</details>

<details>
<summary>POST /auth/refresh</summary>

**Request**
```json
{
  "refreshToken": "eyJ..."
}
```

**Response** `200 OK` — Access Token + Refresh Token 모두 재발급 (Token Rotation)
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "eyJ...",
  "user": { ... }
}
```
</details>

<details>
<summary>POST /auth/logout</summary>

**Header**: `Authorization: Bearer {accessToken}`

**Response** `200 OK` — Redis에서 Refresh Token 삭제
</details>

---

## 실험 및 검증 주제

실제 운영 환경에서 마주칠 수 있는 문제 상황을 직접 구현하고 테스트합니다.

### 🔐 인증
- [x] JWT Access / Refresh Token 분리 발급 (`type` claim으로 구분)
- [x] Token Rotation — Refresh 시 두 토큰 모두 재발급, Redis 갱신
- [x] Refresh Token으로 일반 API 접근 차단

### ⚡ 동시성 제어 (예정)
- [ ] 재고 차감 동시 요청 — Pessimistic Lock vs Optimistic Lock 성능/정합성 비교
- [ ] 선착순 쿠폰 발급 — Redis 원자 연산(`DECR`)을 활용한 Race Condition 방지

### 🔁 멱등성 (예정)
- [ ] 결제 승인 API — Idempotency Key로 중복 호출 방어
- [ ] PG 웹훅 중복 수신 — 처리 이력 기반 재처리 방지

### 🔄 보상 트랜잭션 (예정)
- [ ] 결제 실패 시 재고 롤백 — 동일 트랜잭션 경계 vs 보상 트랜잭션 방식 비교
- [ ] PG API 타임아웃 시 주문 상태 처리 및 재시도 정책

---

## 로컬 실행 방법

**사전 조건**: JDK 17+, Docker

```bash
# 1. 인프라 실행 (MySQL + Redis)
docker-compose up -d

# 2. 애플리케이션 실행
./gradlew bootRun --args='--spring.profiles.active=local'

# 3. Swagger UI 접속
http://localhost:8080/api/swagger-ui/index.html
```

**DB 접속 정보** (로컬)

| 항목 | 값 |
|------|----|
| Host | localhost:3306 |
| Database | mini_shop |
| Username | minishop |
| Password | 1234 |

---

## 테스트 실행

```bash
# 전체 테스트 (통합 테스트 포함 — Docker MySQL/Redis 필요)
./gradlew test

# 단위 테스트만
./gradlew test --tests "com.minishop.auth.service.*"
./gradlew test --tests "com.minishop.user.service.*"
```
