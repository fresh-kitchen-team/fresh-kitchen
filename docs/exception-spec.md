# Exception / ErrorCode Specification

## 1. 목적

이 문서는 FreshKitchen 백엔드의 예외 처리 방식과 에러 코드 정책을 팀 내에서 일관되게 사용하기 위한 기준 문서다.

목표는 아래와 같다.

- 예외 타입을 도메인 의미에 맞게 통일한다.
- 클라이언트에 내려가는 에러 응답 포맷을 고정한다.
- 에러 코드를 도메인별로 관리해 유지보수성을 높인다.
- 신규 기능 개발 시 같은 규칙으로 예외를 추가할 수 있게 한다.

---

## 2. 기본 구조

예외 처리는 아래 계층으로 구성한다.

### 2.1 공통 계약

- `ErrorCode`
  - 모든 에러 코드가 구현해야 하는 공통 인터페이스
  - 포함 값
    - `HttpStatus status()`
    - `String code()`
    - `String message()`

- `BusinessException`
  - 서비스/도메인 예외의 공통 부모
  - 반드시 하나의 `ErrorCode` 를 가진다.

### 2.2 공통 예외

- `BusinessValidationException`
  - null, blank, positive 같은 입력값 검증 실패에 사용
  - 현재 `BaseEntity` 의 공통 검증 유틸이 이 예외를 사용한다.

### 2.3 도메인 예외

- `IngredientException`
- `ImageException`
- `UserException`

도메인별 예외는 각 도메인의 `ErrorCode` enum 과 1:1로 연결한다.

### 2.4 글로벌 핸들러

- `GlobalExceptionHandler`
  - `BusinessException` 처리
  - `IllegalArgumentException` 처리
  - `IllegalStateException` 처리
  - 그 외 `RuntimeException` 처리

---

## 3. 에러 응답 포맷

클라이언트에는 아래 JSON 구조로 응답한다.

```json
{
  "timestamp": "2026-04-06T12:34:56+09:00",
  "status": 404,
  "code": "INGREDIENT-404-1",
  "message": "ingredient not found",
  "path": "/api/v1/ingredients/1"
}
```

### 필드 설명

| 필드 | 타입 | 설명 |
|------|------|------|
| `timestamp` | `OffsetDateTime` | 예외 응답 생성 시각 |
| `status` | `int` | HTTP status code |
| `code` | `String` | 도메인/상황별 에러 코드 |
| `message` | `String` | 사용자/클라이언트가 읽을 수 있는 메시지 |
| `path` | `String` | 요청 URI |

---

## 4. ErrorCode 네이밍 규칙

### 4.1 형식

`{DOMAIN}-{HTTP_STATUS}-{SEQUENCE}`

예시:

- `COMMON-400`
- `INGREDIENT-404-1`
- `IMAGE-400-3`

### 4.2 규칙

- `DOMAIN`
  - uppercase 사용
  - 대표 도메인명 사용
  - 예: `COMMON`, `USER`, `INGREDIENT`, `IMAGE`

- `HTTP_STATUS`
  - 실제 응답 status 와 일치해야 한다.

- `SEQUENCE`
  - 같은 도메인과 status 안에서 순차 증가
  - 공통 코드처럼 범용 성격이면 생략 가능

### 4.3 enum 상수명 규칙

- 모두 대문자 + underscore 사용
- 원인과 의미가 드러나게 작성

좋은 예:

- `INGREDIENT_NOT_FOUND`
- `USER_UPLOAD_OWNER_REQUIRED`
- `STORAGE_NOT_OWNED_BY_USER`

피해야 할 예:

- `INVALID_ERROR`
- `BAD_REQUEST_1`
- `EXCEPTION_CASE`

---

## 5. 현재 정의된 공통 에러 코드

### 5.1 CommonErrorCode

| Enum | HTTP Status | Code | Message | 용도 |
|------|-------------|------|---------|------|
| `INVALID_INPUT` | `400` | `COMMON-400` | `Invalid input` | 범용 입력값 오류 fallback |
| `INVALID_STATE` | `409` | `COMMON-409` | `Invalid state` | 범용 상태 충돌 fallback |
| `INTERNAL_SERVER_ERROR` | `500` | `COMMON-500` | `Internal server error` | 미처리 예외 fallback |

공통 에러 코드는 `code` 와 `status` 의 fallback 계약을 제공한다.

- `BusinessValidationException`, `IllegalArgumentException`, `IllegalStateException` 처럼 공통 코드로 처리되는 예외도 응답 `message` 는 항상 `CommonErrorCode` 의 고정 메시지를 사용한다.
- 예외 상세 내용은 서버 로그에만 남기고 외부 응답에는 포함하지 않는다.
- 예측 가능한 `4xx` 비즈니스 예외는 운영 로그 노이즈를 줄이기 위해 `debug` 레벨로 기록하고, `5xx` 비즈니스 예외는 `error` 레벨로 기록한다.
- 따라서 클라이언트 분기 기준은 `message` 가 아니라 `code` 로 고정한다.

---

## 6. 현재 정의된 도메인 에러 코드

### 6.1 IngredientErrorCode

| Enum | HTTP Status | Code | Message | 의미 |
|------|-------------|------|---------|------|
| `INGREDIENT_ID_REQUIRED` | `400` | `INGREDIENT-400-1` | `ingredientId must not be null` | 서비스 입력값 누락 |
| `INGREDIENT_IMAGE_ID_REQUIRED` | `400` | `INGREDIENT-400-2` | `ingredientImageId must not be null` | 서비스 입력값 누락 |
| `INGREDIENT_NOT_FOUND` | `404` | `INGREDIENT-404-1` | `ingredient not found` | 대상 식재료 조회 실패 |
| `INGREDIENT_IMAGE_NOT_BELONG_TO_INGREDIENT` | `400` | `INGREDIENT-400-3` | `ingredient image must belong to ingredient` | 식재료-이미지 소속 불일치 |
| `FIRST_IMAGE_MUST_BE_PRIMARY` | `400` | `INGREDIENT-400-4` | `first ingredient image must be primary` | 첫 이미지 생성 규칙 위반 |
| `PRIMARY_IMAGE_MUST_BELONG_TO_INGREDIENT` | `400` | `INGREDIENT-400-5` | `primary image must belong to ingredient` | 대표 이미지 지정 대상 불일치 |
| `INGREDIENT_PRIMARY_IMAGE_REQUIRED` | `400` | `INGREDIENT-400-6` | `ingredient must have one primary image` | 유일한 대표 이미지 제거 시도 |
| `INGREDIENT_PRIMARY_IMAGE_INVARIANT_BROKEN` | `409` | `INGREDIENT-409-1` | `ingredient must have exactly one primary image` | 엔티티 불변식 붕괴 |
| `STORAGE_NOT_OWNED_BY_USER` | `400` | `INGREDIENT-400-7` | `storage must belong to user` | 사용자-보관소 소유권 불일치 |

### 6.2 ImageErrorCode

| Enum | HTTP Status | Code | Message | 의미 |
|------|-------------|------|---------|------|
| `INGREDIENT_IMAGE_ALREADY_ATTACHED` | `400` | `IMAGE-400-1` | `ingredient image is already attached to another ingredient` | 다른 식재료에 연결된 이미지 재할당 시도 |
| `SYSTEM_DEFAULT_OWNER_MUST_BE_NULL` | `400` | `IMAGE-400-2` | `user must be null when assetType is SYSTEM_DEFAULT` | 시스템 기본 이미지 소유자 규칙 위반 |
| `USER_UPLOAD_OWNER_REQUIRED` | `400` | `IMAGE-400-3` | `user must not be null when assetType is USER_UPLOAD` | 사용자 업로드 이미지 소유자 누락 |

### 6.3 UserErrorCode

| Enum | HTTP Status | Code | Message | 의미 |
|------|-------------|------|---------|------|
| `USER_NOT_FOUND` | `404` | `USER-404-1` | `user not found` | 대상 사용자 조회 실패 |

---

## 7. 예외 사용 규칙

### 7.1 어떤 예외를 써야 하는가

- 공통 필드 검증
  - `BusinessValidationException`
  - 예: `name must not be blank`, `width must be positive`

- 도메인 정책 위반
  - 도메인 전용 예외 사용
  - 예: `IngredientException`, `ImageException`

- 조회 실패, 소속 불일치, 상태 충돌
  - 가능하면 도메인 `ErrorCode` 를 추가해서 처리

- 예상하지 못한 시스템 오류
  - 직접 잡아서 숨기지 말고 상위로 전파
  - `GlobalExceptionHandler` 가 `COMMON-500` 으로 처리

### 7.2 메시지 작성 기준

- message 는 현재 영어로 통일한다.
- 클라이언트/프론트가 그대로 읽어도 의미가 통해야 한다.
- 너무 내부 구현적인 표현은 피한다.
- 도메인 `ErrorCode` 는 하나의 대표 메시지를 가진다.
- 공통 코드 fallback 응답은 예외별 상세 메시지를 외부로 노출하지 않는다.

### 7.3 status 선택 기준

- `400 Bad Request`
  - 입력값 오류
  - 소유권/연결 관계 불일치
  - 요청 자체가 도메인 규칙을 만족하지 못하는 경우

- `404 Not Found`
  - 조회 대상이 존재하지 않는 경우

- `409 Conflict`
  - 엔티티 불변식 충돌
  - 현재 상태에서 요청을 수행할 수 없는 경우

- `500 Internal Server Error`
  - 처리되지 않은 예외
  - 서버 내부 예상 밖 오류

### 7.4 로깅 기준

- `BusinessException` 은 `errorCode.status()` 기준으로 로그 레벨을 정한다.
- `4xx` `BusinessException` 과 `IllegalArgumentException`, `IllegalStateException` 은 `debug` 레벨로 기록한다.
- `5xx` `BusinessException` 과 처리되지 않은 예외는 `error` 레벨로 기록한다.
- 이 경우 stack trace 는 `debug` 로그에서만 확인할 수 있다.
- `error` 레벨로 기록되는 예외는 stack trace 가 운영 로그에 남는다.

---

## 8. 신규 에러 코드 추가 규칙

신규 기능에서 예외를 추가할 때는 아래 순서를 따른다.

1. 먼저 기존 `ErrorCode` 로 표현 가능한지 확인한다.
2. 기존 코드가 의미상 맞지 않으면 해당 도메인에 새 코드를 추가한다.
3. enum 이름, HTTP status, message 를 함께 설계한다.
4. 도메인 예외 또는 서비스 로직에서 새 코드를 사용한다.
5. 테스트에서 예외 타입과 메시지를 같이 검증한다.
6. 외부 API에 영향이 있으면 이 문서의 코드 표를 갱신한다.

---

## 9. 팀 합의 권장 사항

- 문자열만 다른 비슷한 예외를 여러 개 만들지 않는다.
- `IllegalArgumentException` 를 서비스/도메인에서 직접 던지는 방식은 지양한다.
- 도메인 정책이 명확한 경우 반드시 도메인 `ErrorCode` 로 승격한다.
- 프론트와 협업하는 API 오류는 `message` 보다 `code` 를 기준으로 처리하게 한다.
- 추후 `user`, `catalog` 등 도메인도 전용 `ErrorCode` enum 으로 확장한다.

---

## 10. 현재 코드 기준 구현 위치

- 공통 계약
  - `src/main/java/com/example/freshkitchen/global/exception`

- 글로벌 핸들러
  - `src/main/java/com/example/freshkitchen/global/exception/handler/GlobalExceptionHandler.java`

- 에러 응답
  - `src/main/java/com/example/freshkitchen/global/exception/response/ErrorResponse.java`

- 식재료 도메인 코드
  - `src/main/java/com/example/freshkitchen/domain/ingredient/exception`

- 이미지 도메인 코드
  - `src/main/java/com/example/freshkitchen/domain/image/exception`

---

## 11. 후속 확장 포인트

- `UserErrorCode`, `CatalogErrorCode` 추가
- Bean Validation 연동 시 필드 에러 목록 확장
- API 문서화 도구와 연결해 에러 코드 테이블 자동화
- 프론트 공통 에러 매핑 테이블 정의
