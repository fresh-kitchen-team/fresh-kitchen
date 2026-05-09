# Test Infrastructure Specification

## 1. 목적

이 문서는 FreshKitchen 백엔드의 테스트 인프라 구조와 테스트 작성 기준을
정리한다.

목표는 아래와 같다.

- 테스트가 어느 계층의 무엇을 검증하는지 명확히 한다.
- Spring context, PostgreSQL, Docker가 필요한 테스트와 필요 없는 테스트를
  구분한다.
- 전체 테스트 실행 시 Testcontainers, Spring context cache, Hikari pool이
  충돌하지 않도록 현재 정책을 고정한다.
- PR 리뷰에서 테스트 보강과 테스트 인프라 변경을 구분할 수 있게 한다.
- 테스트를 잘 모르는 상태에서도 GPT나 리뷰어에게 현재 프로젝트 맥락을
  정확히 전달할 수 있게 한다.

---

## 2. 현재 테스트 지도

| 테스트 유형 | 목적 | Spring context | DB | 대표 파일 |
| --- | --- | --- | --- | --- |
| 순수 단위 테스트 | 도메인 규칙, DTO/응답 계약, ErrorCode 계약 검증 | 없음 | 없음 | `domain/*/entity/*Test`, `ApiResponseTest`, `ErrorCodeContractTest` |
| standalone `MockMvc` | Controller HTTP 응답 계약과 예외 응답 검증 | 없음, controller 수동 구성 | 없음 | `presentation/*/*ControllerTest`, `GlobalExceptionHandlerTest` |
| `MockRestServiceServer` | 외부 HTTP client 요청/응답 매핑 검증 | 없음, `RestClient` 수동 구성 | 없음 | `AiServerClientTest` |
| `@DataJpaTest` + Testcontainers | JPA mapping, repository, DB 의존 application 흐름 검증 | JPA slice | PostgreSQL Testcontainer | repository test, DB 기반 application service test |
| `@SpringBootTest` | 전체 application context wiring smoke test | 전체 context | PostgreSQL Testcontainer | `FreshkitchenApplicationTests` |

기본 원칙은 **증명하려는 동작을 검증할 수 있는 가장 가벼운 테스트를
선택하는 것**이다. Spring context 로딩은 비용이 크고 인프라 실패 지점도
늘리므로, Spring wiring, JPA, transaction, Flyway, request handling이
필요한 경우에만 사용한다.

---

## 3. 테스트 유형 선택 기준

### 3.1 순수 단위 테스트

도메인 불변식, 값 검증, 응답 wrapper, ErrorCode 계약처럼 Spring 없이도
검증 가능한 코드는 순수 JUnit 테스트로 작성한다.

이 테스트는 Spring annotation을 사용하지 않는다. 객체를 직접 생성하고 public
behavior를 검증한다. Docker가 없는 환경에서도 항상 실행되어야 한다.

### 3.2 standalone Controller 테스트

HTTP status, JSON body, `path`, controller-level validation처럼 presentation
계층의 응답 계약을 검증할 때 `MockMvcBuilders.standaloneSetup(...)`을
사용한다.

이 프로젝트에서는 예외 응답 shape가 중요하면 standalone controller test에
`GlobalExceptionHandler`를 직접 붙인다. Controller dependency는 Mockito로
mocking한다. 단순 응답 body 검증을 위해 `@SpringBootTest`를 사용하지 않는다.

### 3.3 외부 HTTP client 테스트

Spring `RestClient` 기반 외부 호출 코드는 `MockRestServiceServer`로 검증한다.

검증 대상은 아래와 같다.

- 요청 URL/path
- `Authorization`, `X-Request-Id` 같은 필수 header
- 정상 응답 매핑
- timeout, connection failure, 5xx, 4xx, invalid response 예외 매핑

Gradle test suite에서 실제 외부 서버나 실제 AI 서버를 호출하지 않는다.

### 3.4 JPA slice 테스트

아래 동작은 `@DataJpaTest`와 PostgreSQL Testcontainer로 검증한다.

- entity mapping
- repository query
- persistence context / transaction에 의존하는 동작
- Flyway migration으로 생성된 PostgreSQL schema와의 호환성
- repository 동작에 강하게 묶인 application service 흐름

PostgreSQL이 필요한 JPA slice test는 반드시
`PostgreSqlTestContainerSupport`를 상속한다. datasource 설정, container
lifecycle, Flyway, pool 제한을 한 곳에서 유지하기 위함이다.

### 3.5 Full context 테스트

`@SpringBootTest`는 최소한으로 사용한다. 현재 목적은 application context가
필수 test property와 핵심 bean wiring으로 뜨는지 확인하는 smoke test다.

동작 검증을 `@SpringBootTest`로 옮기지 않는다. 검증하려는 동작이 전체 Spring
context wiring에 실제로 의존할 때만 사용한다.

---

## 4. PostgreSQL Testcontainer Support

`PostgreSqlTestContainerSupport`는 real PostgreSQL이 필요한 테스트의 공통
support class다.

현재 역할은 아래와 같다.

- `postgres:16-alpine` 기반 공유 static `PostgreSQLContainer` 제공
- `@DynamicPropertySource`로 datasource property 등록
- `spring.jpa.hibernate.ddl-auto=validate` 설정
- `spring.flyway.enabled=true` 설정
- 전체 테스트 안정성을 위한 Hikari pool 제한
- `start()` 호출 전 Docker 사용 가능 여부 확인 후 테스트 abort

현재 Hikari 설정은 의도적인 테스트 인프라 정책이다.

```java
registry.add("spring.datasource.hikari.maximum-pool-size", () -> 2);
registry.add("spring.datasource.hikari.minimum-idle", () -> 0);
```

전체 테스트 실행 시 여러 JPA slice context가 생성되면, 각 context가 Hikari
pool을 만든다. 공유 PostgreSQLContainer를 사용하더라도 connection pool은
Spring context 단위로 누적될 수 있다. pool 크기를 제한하지 않으면 PostgreSQL
client 한도를 넘어서 `FATAL: sorry, too many clients already` 같은 실패가
발생할 수 있다. 이 실패는 기능 코드 실패가 아니라 테스트 인프라 capacity
문제다.

---

## 5. Testcontainers Lifecycle 정책

이 프로젝트는 공유 static PostgreSQLContainer를 두고
`@DynamicPropertySource` 시점에 lazy start한다.

공유 static container에 단순히 `@Container`를 붙이지 않는다. 또한 현재 구조는
`@Testcontainers` extension에 container lifecycle을 맡기지 않고 support class가
직접 Docker 사용 가능 여부를 확인한 뒤 container를 시작한다.

JUnit이 class 단위로 container lifecycle을 관리하면, Spring context cache가 아직
datasource를 들고 있는 동안 다른 테스트 class 경계에서 container가 stop될 수
있다. 그러면 repository나 transaction 문제가 아닌데도 DB 연결 실패처럼 보이는
테스트가 생긴다.

현재 정책은 아래와 같다.

- JVM 안에서 DB 기반 테스트가 하나의 공유 container를 사용한다.
- Spring이 datasource property를 요구할 때 lazy start한다.
- test class별 container stop/start 소유권이나 `@Testcontainers` extension
  소유권을 만들지 않는다.
- parallel startup race를 피하기 위해 start는 synchronized로 처리한다.
- Docker가 없으면 DB 기반 테스트는 실패가 아니라 abort/skip한다.

---

## 6. Docker 사용 가능 여부 정책

Docker는 PostgreSQL 기반 테스트에만 필요하다. 순수 단위 테스트, standalone
MVC 테스트, 외부 client 테스트에는 Docker가 필요 없다.

Docker가 없는 환경에서 기대하는 동작은 아래와 같다.

- DB 기반 테스트는 product regression처럼 실패하지 않고 abort/skip된다.
- compile, 순수 단위 테스트, non-DB 테스트는 계속 실행된다.
- PR 설명에서는 "Docker가 없어 DB integration test를 skip했다"와 "테스트가
  실패했다"를 구분해서 기록한다.

이 정책은 Docker 없는 로컬 환경에서도 의미 있는 테스트를 돌릴 수 있게 하면서,
Docker가 있는 환경에서는 PostgreSQL 실제 동작을 검증하기 위한 절충이다.

---

## 7. 리뷰 체크리스트

테스트 추가나 리뷰 대응 시 아래 항목을 확인한다.

- 검증하려는 동작을 증명할 수 있는 가장 가벼운 테스트 유형인가?
- Spring context가 필요한가, 아니면 객체를 직접 생성해 검증할 수 있는가?
- PostgreSQL 동작이 필요한가, 아니면 순수 단위 테스트로 충분한가?
- PostgreSQL이 필요하다면 `PostgreSqlTestContainerSupport`를 상속하는가?
- 실제 네트워크, 실제 AI 서버, 로컬 machine-specific state에 의존하지 않는가?
- 전체 테스트 실행 시 connection pool 또는 container lifecycle 문제가 생기지
  않는가?
- 실패 원인이 기능 동작 실패인지, 테스트 인프라 준비 실패인지 구분되는가?

---

## 8. PR 범위 기준

기능 동작 변경과 광범위한 테스트 인프라 변경은 분리한다.

기능 PR은 새 기능을 검증하는 focused test를 추가할 수 있다. 하지만 기능 검증에
꼭 필요하지 않다면 shared test lifecycle, Gradle test task, container policy,
global test support를 함께 바꾸지 않는다.

테스트 인프라 PR은 작고 명확해야 한다. PR 설명에는 아래를 적는다.

- 어떤 테스트 인프라 문제를 해결하는지
- 기능 동작 변경이 있는지 없는지
- Docker가 있는 환경에서 기대하는 결과
- Docker가 없는 환경에서 기대하는 결과
- 검증에 사용한 명령

리뷰 코멘트가 기능 테스트 보강과 인프라 정리를 동시에 요구하면, 기능 PR에서는
행동 계약을 검증하는 focused test를 먼저 추가하고, 넓은 인프라 정리는 별도 PR로
분리하는 것을 기본값으로 한다.
