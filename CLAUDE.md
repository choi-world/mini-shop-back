# Backend CLAUDE.md

## 프로젝트 요약 (Project Overview)
- 이 프로젝트는 쇼핑몰을 주로 만들되, 결제 도메인을 중심으로 여러 기능을 만들고 테스트한다.
- Spring Boot + Java + JPA + MySQL(AWS RDS) 기반으로 구성된다.

---

## 중요 개발 지침 (IMPORTANT Development Guidelines)

### Q&A 로그 규칙
- 답변 후 항상 **"이 내용을 로그로 저장할까요?"** 라고 사용자에게 물어본다.
- 저장 요청 시 `docs/qa-log.md`에 아래 형식으로 추가한다.

- 저장하지 않겠다고 하면 그냥 넘어간다.

---

### 코드 수정 전 필수 확인 사항
- **작업이 완료되었다고 절대 혼자 판단하지 않는다.**
- **모든 작업은 사용자가 반드시 확인**하고 진행할 수 있도록 한다.
- **지시 사항에 대한 추가 및 수정 작업**에 대해 수도코드로 먼저 계획을 제시한다.
- **사용자의 확인 이후로 실제 코드를 작성**할 수 있도록 한다.
- **수도코드에는 다음 내용을 포함**한다.
  - 수정할 파일 목록들
  - 파일별 변경 사항 요약 및 정리
  - 로직 흐름에 대한 설명
  - 예상되는 영향 범위

---

### 개발 규칙 (Development Rules)
- **Java 17+ 기준**으로 작성하며, Lombok은 최소한으로 사용한다 (`@Getter`, `@Builder` 정도만 허용, `@Data`는 지양).
- 원시 타입 대신 명확한 타입을 사용하고, `Optional`을 남용하지 않는다 (반환 타입에서만 제한적으로 사용).
- 커밋 전에 항상 **빌드 및 lint(Checkstyle/Spotless) 필수**이다.
- Controller에는 비즈니스 로직을 작성하지 않는다.
- Repository 외의 계층에서 JPA `EntityManager`/`Repository`를 직접 사용하지 않는다.
- 트랜잭션은 반드시 `@Transactional`을 통해서만 관리하며, 트랜잭션 경계는 Facade 최상단에만 둔다.
- 크로스 도메인 트랜잭션은 Facade에서 처리하며, 여러 Service 호출을 하나의 트랜잭션으로 묶는다.
- Entity를 Controller/Facade 응답에 직접 노출하지 않는다. DTO를 통해 요청/응답 타입을 명확하게 정의한다.
- 변수명은 **반드시 직관적이도록 설정**한다.
- 삼항 연산자는 **반드시 한 줄로 작성**한다. 줄바꿈 금지.
- 복잡한 메서드에 대해 **Javadoc 형식으로 주석을 남긴다.**
  - **API Endpoint, Request, Description, Author**를 작성하도록 한다.
- **절대 불필요한 의존성(라이브러리)을 추가하지 않는다.**
- **절대 DB 마이그레이션(Flyway/Liquibase 스크립트 등)을 임의로 진행하지 않는다.**
- **디렉토리를 벗어나 작업하지 않는다.**

---

### 아키텍처 (Architecture)

- **Controller**: 요청/응답만 담당한다. 비즈니스 로직을 작성하지 않는다.
- **Facade**: 비즈니스 로직을 구성하는 계층이다. 하나 이상의 Service를 조합하여 유스케이스를 완성하며, 트랜잭션 경계를 가진다.
- **Service**: 단일 도메인의 순수 책임만 수행한다. 외부 API 호출, 단일 엔티티 조작 등 원자적 작업 단위로 구성한다.
- **Repository**: 데이터 접근만 담당한다 (Spring Data JPA `JpaRepository` 인터페이스 기반).

> Controller는 항상 Facade를 통해 요청을 처리하며, Service를 직접 호출하지 않는다.

#### 디렉토리 구조 예시

```
src/main/java/com/project/
├── auth/
│   ├── controller/
│   ├── facade/
│   ├── service/
│   ├── repository/
│   ├── dto/
│   ├── entity/
│   └── AuthConfig.java
├── product/
│   ├── controller/
│   ├── facade/
│   ├── service/
│   ├── repository/
│   ├── dto/
│   └── entity/
├── order/
│   ├── controller/
│   ├── facade/
│   ├── service/
│   ├── repository/
│   ├── dto/
│   └── entity/
├── payment/
│   ├── controller/
│   ├── facade/
│   ├── service/
│   ├── repository/
│   ├── dto/
│   ├── entity/
│   └── client/        # PG API 클라이언트 (Toss/Portone)
└── common/
├── exception/
├── config/
└── util/
```

---

### 도메인 구조 설계 기반 (Domain Driven Structure)
- 프로젝트는 도메인 기준으로 패키지를 구성한다.
- 도메인에 대한 경계는 명확하게 유지한다.
- 관련 Controller, Facade, Service, Repository, Entity는 동일 도메인 패키지 내부에 위치시킨다.
- 도메인의 내부 구현(Entity, Repository)은 외부 도메인에 직접 노출하지 않는다.
- Service는 자신의 도메인 책임만 수행한다.
- Service 간 직접 호출은 금지한다.
- 여러 도메인의 협력이 필요한 경우 Facade Layer에서 조합한다.
- 새로운 기능 추가 시 기존 도메인에 포함 가능한지 먼저 검토한다.
- 충분한 근거가 없는 신규 도메인 생성은 지양한다.

---

### 결제 도메인 특이 사항 (Payment Domain Notes)
- 결제 승인/취소 API 호출 시 **멱등성 키(Idempotency Key)**를 반드시 포함한다.
- PG 웹훅 수신 시 서명(signature) 검증을 통과하지 못하면 즉시 거부한다.
- 웹훅은 중복 수신될 수 있음을 전제로, 이미 처리된 이벤트는 재처리하지 않는다 (처리 이력 기록 필요).
- 재고 차감과 결제 승인은 동일 트랜잭션 경계 내에서 처리하거나, 실패 시 보상 트랜잭션으로 롤백 가능해야 한다.
- 외부 PG API 실패/타임아웃 시 재시도 정책과 최종 실패 처리(주문 상태 반영)를 명확히 한다.

