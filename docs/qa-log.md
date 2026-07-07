# Q&A 로그

---

## 2026-07-06 — 프로젝트 초기 세팅

**Q. 프로젝트 뼈대를 어떻게 구성할까?**

- 빌드 도구: Gradle Groovy DSL
- Java 21 / Spring Boot 3.4.0
- 베이스 패키지: `com.minishop`
- Javadoc Author: `csd`
- 도메인: auth, product, order, payment, common
- 아키텍처: Controller → Facade → Service → Repository

**Q. Docker Compose로 MySQL 설정은?**

```yaml
image: mysql:8.0
MYSQL_ROOT_PASSWORD: 1234
MYSQL_DATABASE: mini_shop
MYSQL_USER: minishop
MYSQL_PASSWORD: 1234
port: 3306
```

---

## 2026-07-06 — 계정 통합 설계

**Q. 독립 계정으로 운영하다가 나중에 CI로 통합 시 어떻게 처리해야 하나?**

**배경**
- 현재는 CI 없이 독립 계정으로 운영 (LOCAL, GOOGLE 각각 별개 user_id)
- 나중에 CI(본인인증 Connecting Information)를 받을 수 있는 환경이 되면 통합 가능
- CI가 동일한 사용자를 발견하면 통합 여부를 사용자에게 질문

**결론**
- user_id를 직접 변경하는 데이터 이관은 위험 → 하지 않음
- 통합 관계를 별도 테이블(`user_integration`)로 관리
- 조회는 `findByUserIdIn(userIds)` 패턴으로 통일

```
user_integration
├── primary_user_id   ← 대표 계정 (통합 후 사용할 user_id)
├── linked_user_id    ← 통합된 계정
└── integrated_at
```

**조회 패턴**
```java
// 통합 전 → [1]
// 통합 후 → [1, 2]
List<Long> userIds = integrationService.resolveUserIds(currentUserId);

orderRepository.findByUserIdIn(userIds);
paymentRepository.findByUserIdIn(userIds);
```

- 통합 여부와 관계없이 항상 동일한 쿼리 패턴 유지
- 복잡도는 `resolveUserIds()` 한 곳에서만 처리
- 현재 스코프 밖이므로 테이블 설계만 참고용으로 보존

---

## 2026-07-07 — JWT + Redis Refresh Token 구현

**결정 사항**
- Access Token: 30분 (클라이언트 메모리 보관)
- Refresh Token: 1일 (Redis 보관, Token Rotation 적용)
- 로그아웃: Redis에서 Refresh Token 삭제, 프론트에서 Access Token 제거

**추가된 의존성**
- `spring-boot-starter-data-redis`
- `io.jsonwebtoken:jjwt-api:0.12.6` / `jjwt-impl` / `jjwt-jackson`

**Docker Compose Redis**
```yaml
redis:
  image: redis:7.2
  container_name: mini-shop-redis
  ports: 6379:6379
```

**Redis Key 구조**
```
Key   : rt:{userId}
Value : refreshToken 문자열
TTL   : 1일 (86400000ms)
```

**신규/변경 파일**
| 파일 | 내용 |
|---|---|
| `common/config/RedisConfig.java` | RedisTemplate<String, String> Bean |
| `common/util/JwtUtil.java` | 토큰 생성/검증/userId 추출 |
| `common/security/JwtAuthenticationFilter.java` | Bearer 토큰 파싱 → SecurityContext 등록 |
| `common/config/SecurityConfig.java` | STATELESS, JwtFilter 등록, /auth/** permitAll |
| `auth/service/TokenService.java` | Redis CRUD (save/get/delete) |
| `auth/dto/TokenResponse.java` | accessToken + refreshToken + UserResponse |
| `auth/dto/RefreshRequest.java` | refreshToken 필드 |
| `auth/facade/AuthFacade.java` | signup/login → TokenResponse, refresh, logout 추가 |
| `auth/controller/AuthController.java` | POST /auth/refresh, POST /auth/logout 추가 |

**API**
- `POST /auth/signup` → `TokenResponse`
- `POST /auth/login` → `TokenResponse`
- `POST /auth/refresh` `{ refreshToken }` → `TokenResponse` (Token Rotation)
- `POST /auth/logout` `Authorization: Bearer {accessToken}` → 204

**Token Rotation**
- /auth/refresh 호출 시 Access Token + Refresh Token 모두 재발급
- Redis에 저장된 기존 Refresh Token을 새 값으로 덮어씀
- 탈취된 토큰으로 재사용 시 Redis 값 불일치 → 401 거부

---

## 2026-07-07 — Swagger 추가 및 API 공통 prefix 적용

**결정 사항**
- 모든 API에 `/api` context-path 적용
- Swagger UI 추가 (springdoc-openapi 2.7.0)
- API Docs 경로를 기본 `/v3/api-docs` 대신 `/v1/api-docs`로 변경

**추가된 의존성**
- `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.7.0`

**설정 변경 (application.yml)**
```yaml
springdoc:
  api-docs:
    path: /v1/api-docs
  swagger-ui:
    path: /swagger-ui/index.html

server:
  servlet:
    context-path: /api
  port: 8080
```

**접속 URL**
- API: `http://localhost:8080/api/{도메인}/...`
- Swagger UI: `http://localhost:8080/api/swagger-ui/index.html`
- API Docs: `http://localhost:8080/api/v1/api-docs`

**Security 처리**
- `requestMatchers`는 context-path를 제외한 경로 기준으로 동작
- `/swagger-ui/**`, `/v1/api-docs/**` permitAll 추가
- 기존 `/auth/**` matcher는 변경 없이 그대로 유지

---
