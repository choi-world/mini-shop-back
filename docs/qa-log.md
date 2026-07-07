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
