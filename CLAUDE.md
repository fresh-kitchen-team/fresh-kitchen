# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

사용자가 보유한 식재료 정보를 기반으로, pgvector와 RAG(검색 증강 생성) 기술을 활용하여 최적의 요리 레시피를 추천하고 조리법을 안내하는 백엔드 시스템.

## Tech Stack

- Language: Java 17
- Framework: Spring Boot 4.0.2
- Database: PostgreSQL 16 + pgvector extension
- ORM: Spring Data JPA / Hibernate
- AI/LLM: Gemini API (채팅 + 임베딩, `text-embedding-004` 768-dim)
- Auth: JWT (Stateless) + Redis (블랙리스트)
- Migration: Flyway
- Test: JUnit 5 + Testcontainers
- Data Scraping: Python (BeautifulSoup) — 초기 레시피 데이터 수집용

## Commands

```bash
./gradlew bootRun                  # 개발 서버 실행
./gradlew build                    # 빌드 (테스트 포함, Docker 필요)
./gradlew clean build -x test      # 테스트 제외 빌드 (CI/배포용)
./gradlew test                     # 전체 테스트 실행
./gradlew test --tests "com.example.freshkitchen.domain.JpaMappingTest"  # 단일 테스트

docker-compose up -d               # 로컬 DB(PostgreSQL + pgvector) 환경 실행
```

테스트는 Testcontainers로 `postgres:16-alpine` 컨테이너를 자동 기동. Docker 없는 환경에서는 자동 스킵(`@Testcontainers(disabledWithoutDocker = true)`).

## Architecture

DDD 스타일 도메인 패키징.

```
domain/
  user/        – User + UserProfile (닉네임, 알레르기, 조리도구, 식성)
  ingredient/  – Ingredient 집합체 + Storage; @Version 낙관적 락
  catalog/     – IngredientCatalog + 유통기한 규칙 (카탈로그/카테고리별)
  image/       – ImageAsset, ImageVariant, IngredientImage (DB 트리거로 exactly-one-primary 강제)
  recipe/      – Recipe 엔티티 (vector(768) 컬럼); IngestionService가 임베딩 후 저장
  chat/        – ChatSession, Message, GeminiClient, EmbeddingService, ChatService (RAG 흐름)
global/
  exception/   – ErrorCode 인터페이스, CommonErrorCode, BusinessException, GlobalExceptionHandler
  exception/config/ – SecurityConfig (현재 주석 처리됨), JWT 필터/프로바이더, RedisConfig
```

**핵심 데이터 흐름 — RAG:**
사용자 메시지 → `EmbeddingService`(Gemini 임베딩) → `RecipeRepository` 코사인 유사도 검색(`<=>`) → 레시피 컨텍스트 주입 프롬프트 → `GeminiClient` 응답 → `MessageRepository` 저장

**레시피 인제스트:**
`POST /api/chat/recipes/ingest` — `[{name, ingredients, steps}]` JSON → `IngestionService` → 임베딩 → `recipe` 테이블 저장

**DB 스키마:** Flyway 단독 관리 (`ddl-auto: validate`). 변경 시 `src/main/resources/db/migration/V{n}__설명.sql` 추가. 현재 최신: `V4` (pgvector `CREATE EXTENSION` + `recipe` 테이블 + HNSW 인덱스).

## Code Style Rules

**Naming:** 클래스명 PascalCase, 변수/메서드명 camelCase

**Language:** 코드와 주석은 영어로 작성. 커밋 메시지는 한글 작성

**Entities (DDD)**
- `@Setter` 사용 금지. 상태 변경은 비즈니스 메서드로만 (예: `markConsumed()`, `apply(UpdateCommand)`)
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 필수
- `@Builder`는 private 생성자에 붙이거나 정적 팩토리 `create(XxxCommand)` 패턴 사용
- 필드 검증은 `BaseEntity`의 `requireNonNull`, `requireNonBlank` 사용

**DTOs**
- Java `record` 사용
- Request/Response는 도메인별 래퍼 클래스의 inner record로 그룹화 (예: `UserDto.SignUpReq`)

**JPA**
- `FetchType.LAZY` 원칙 준수
- 양방향 매핑 시 연관관계 편의 메서드 작성

**Documentation**
- 모든 public 메서드에 기능 설명 Javadoc 주석 작성

**Controllers**
- `ResponseEntity<ApiResponse<T>>` 반환, `ApiResponse.onSuccess()` 사용

**Exception handling**
- `BusinessException(errorCode)` 사용 — raw `RuntimeException` 직접 throw 금지
- 도메인별 `{Domain}ErrorCode` enum → `{Domain}Exception` 패턴
- ErrorCode 형식: `{DOMAIN}-{HTTP_STATUS}-{SEQUENCE}` (예: `INGREDIENT-404-1`)
- `4xx` → `debug` 로그, `5xx` → `error` 로그

**두 가지 `ErrorCode` 혼용 주의:**
- `global.exception.ErrorCode` — 도메인 enum들이 구현하는 **인터페이스** (리팩터링 진행 중)
- `global.exception.enums.ErrorCode` — JWT 전용 **enum** (별개)

**DI / Transactions**
- `@RequiredArgsConstructor` 생성자 주입만 사용. `@Autowired` 필드 주입 금지
- 읽기 전용 메서드 `@Transactional(readOnly = true)` 필수

## Important Notes

**RAG 구현:** 단순 텍스트 검색이 아닌 pgvector 코사인 유사도 검색과 키워드 기반 검색을 혼합한 Hybrid Search를 지향함.

**Data Integrity:** `ChatSession`과 `Message` 간 관계에서 세션 없는 메시지가 생성되지 않도록 `nullable = false` 설정을 엄격히 관리함.

**Performance:** 벡터 검색 쿼리 성능을 위해 HNSW 인덱스 적용 (`V4` 마이그레이션에 포함).

**Scraping:** 데이터 수집 시 사이트 부하 방지를 위해 딜레이를 두고 진행하며, 수집된 재료 데이터는 정제(Normalization) 과정을 거쳐 저장함.

**Auth:** `SecurityConfig`가 현재 전체 주석 처리 상태 — 인증 미적용.

## Git / Branch Convention

```
feat/* → dev → main
배포: dev push → GitHub Actions → AWS ECR → CodeDeploy
```