
<div align="center">

# 🥬 FreshKitchen (프레시 키친)

### 냉장고 속 식재료를 스캔 한 번으로, 유통기한 관리부터 AI 레시피까지


## 📌 프로젝트 소개

**FreshKitchen**은 식재료 등록 부담을 최소화하고, 유통기한 관리 · 맞춤형 레시피 추천 · 폐기율 감소를 도와주는 **AI 기반 스마트 냉장고 관리 플랫폼**입니다.

| 구분 | 내용 |
|---|---|
| 📅 개발 기간 | 2026.03 ~ 2026.06 (26-1 산학프로젝트, 15주) |
| 👥 팀명 | 21의 4분의 3조 |
| 🎯 타겟 사용자 | 스마트 기능 없는 일반 냉장고 보유 가구, 식재료 등록/관리를 간편하게 하고 싶은 사용자 |
| 🏆 주요 성과 | AI 분류 정확도 **96.6%** (목표 초과 달성) · 조회 성능 **28.7배** 개선 · 플레이스토어 출시 심사 진입 |

<br>

## ✨ 주요 기능 (Key Features)

- 📸 **AI 스캔** — 냉장고/영수증 사진 한 장으로 식재료 자동 등록 (Document AI OCR + EfficientNet V2-M + Gemini 2.5 Flash VLM 하이브리드 추론)
- ⏰ **유통기한 알림** — 임박·만료 식재료를 Firebase FCM 푸시로 알림
- 💬 **AI 레시피 챗봇** — 보유 재료 기반 맞춤 레시피 추천 (RAG + 쿼리 재작성 + Gemini 웹 검색)
- 📊 **소비/폐기 분석** — 카테고리별 소비·폐기 패턴 시각화, 부족한 재료는 쿠팡 파트너스 연동

<br>

## 🛠️ 기술 스택 (Tech Stack)

**Language & Framework**
`Kotlin(Android)` `Spring Boot` `Spring Security(JWT+OAuth2.0)` `Spring AI` `FastAPI`

**Database & Persistence**
`PostgreSQL(RDS)` `Redis(ElastiCache)` `JPA` `Flyway(DB 관리용)`

**AI / ML**
`EfficientNet V2-M` `Gemini 2.5 Flash(VLM)` `Google Document AI(OCR)` `RAG Vector Store`

**Infra & DevOps**
`AWS EC2(ASG)` `ALB` `S3(Presigned URL)` `CloudFront` `Route53` `GitHub Actions(CI/CD)` `Firebase FCM`

**Monitoring**
`AWS CloudWatch`

<br>

## 🏗️ 시스템 아키텍처

```
Client (Android · Kotlin)
        │
        ▼
Backend (Spring Boot REST API)
        │
        ▼
AWS Cloud ── EC2(ASG) · ALB · RDS · S3 · ElastiCache(Redis) · CloudFront
        │
        ▼
External ── Gemini 2.5 Flash · Document AI(OCR) · Firebase FCM
```

- Presigned URL 도입 → 클라이언트가 S3에 직접 업로드, 서버 부하 감소 & DB 경량화
- YOLO 단독 방식 대신 **EfficientNet V2-M(1차) → Gemini VLM(2차 보정)** 하이브리드 추론 구조 채택
- 미학습/저신뢰도 이미지는 `/auto_labeled/`에 자동 저장되는 self-improving 데이터 파이프라인 구성

### ⚡ 성능 개선 — 대량 데이터 조회 최적화

| 항목 | Before | After | 개선 |
|---|---|---|---|
| Execution Time | 31.0 ms | 1.08 ms | **28.7× ↓ (96.5%)** |
| Scan 방식 | Seq Scan (Full Table Scan) | Bitmap Index Scan | — |
| Scanned Rows | 52,336 | 1 | **4758× ↓** |

> PostgreSQL **trigram(GIN) + LOWER 인덱스** 적용으로 식재료 이름 검색 성능 대폭 개선

<br>

## 🙋‍♂️ 나의 역할 및 주요 기여 — 전한준 (INFRA · 조장)

### ⚙️ 인프라 구축 및 배포 안정화
- **문제 상황**: AWS VPC·EC2·RDS·S3 기반 인프라 설계 및 CI/CD(GitHub Actions) 배포 파이프라인 구축 과정에서 안정적인 운영 환경 확보가 과제였음
- **해결 방법**: ALB+ASG 기반 컴퓨팅/로드밸런싱 구성, ElastiCache(Redis) 캐시 계층 도입, SSL 적용, AWS CloudWatch로 CPU·DB 커넥션·HTTP 에러율 모니터링 체계 구축
- **결과/성과**: 무중단 운영 기반 마련, 3개월간 안정적 서비스 운영 (누적 인프라 비용 약 $218.7 관리)

### 🔔 FCM 알림 및 부하테스트
- **문제 상황**: 유통기한 임박 알림을 위한 FCM 연동과, 실제 트래픽 환경에서의 서비스 안정성 검증 필요
- **해결 방법**: FCM 기반 유통기한 스케줄링 알림 구축, 이미지 5.2만 개 등록 상태에서 30초 50명 → 1분 200명 → 3분 200명 단계적 부하테스트 수행
- **결과/성과**: p95/p99 응답시간 임계치(800ms/1500ms) 내 통과, 부하 증가에도 안정적 응답 성능 확보

<br>

## 🔥 핵심 트러블슈팅

<details>
<summary><strong>🐛 AI 서버 교체 후 "조용한 실패" 및 재기동 시 500 에러</strong></summary>

<br>

- **이슈**: AI 서버(인스턴스) 교체 후 OCR·탐지 API가 200 OK를 반환하면서도 빈 결과를 주는 "조용한 실패" 발생, 재기동 시에는 500 에러 발생
- **원인 분석**: 신규 인스턴스에 Gemini API 키가 누락되어 있었고, Redis가 미기동 상태였던 것이 원인
- **해결 방안**: 시크릿을 AWS SSM 파라미터 스토어 & GitHub Secrets로 체계화, 컨테이너·Redis에 자동 복구(restart) 로직 적용
- **개선 결과**: 재배포·재기동 시 무중단 자동 복구 체계 확보 + 대응 인력 투입 감소로 운영비 절감

</details>

<br>

## 💬 배운 점

> "프로젝트를 하면서 배워야 할 게 많다는 걸 느꼈습니다. AWS 인프라가 생각보다 공부할 게 많다는 걸 느꼈습니다."
> — 전한준 (INFRA)

<br>

---

<div align="center">

**21의 4분의 3조 · FreshKitchen** · 26-1 산학프로젝트

[GitHub](https://github.com/fresh-kitchen-team) · Web (placeholder) · Figma (placeholder)

</div>
