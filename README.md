FreshKitchen (프레시 키친)
1. 서비스 개요 (Overview)
한 줄 요약: 냉장고·영수증 스캔 한 번으로 식재료를 자동 등록하고, 유통기한 관리부터 AI 맞춤 레시피 추천까지 제공하는 스마트 냉장고 관리 플랫폼
개발/운영 기간: 2026.03 ~ 2026.06 (26-1 산학프로젝트, 15주)
팀 구성: BE 2명(권도윤, 정찬영), FE 2명(신희주, 이동건), AI 1명(성종현), INFRA 1명(전한준) — 총 6명
주요 지표/성과: AI 분류 모델 검증 정확도 96.6%(70클래스, 목표 95% 초과 달성), 조회 성능 28.7배 개선(31.0ms → 1.08ms), 플레이스토어 출시 심사 진입, 3개월 누적 운영비 약 $218.7
2. 주요 기능 (Key Features)
AI 스캔: 냉장고/영수증 사진 한 장으로 식재료 자동 등록 (Document AI OCR + EfficientNet V2-M 분류 + Gemini 2.5 Flash VLM 보정 하이브리드 추론)
유통기한 알림: 임박·만료 식재료를 Firebase FCM 푸시로 알림, MANUAL/POLICY/UNKNOWN 방식의 유통기한 계산 로직 통합
AI 레시피 챗봇: 보유 재료 기반 맞춤 레시피 추천, '만개의 레시피' 2,184개 데이터 RAG 임베딩 + 쿼리 재작성(ReWrite) + Gemini 웹 검색 결합
소비/폐기 분석: 카테고리별 소비·폐기 패턴 시각화 및 보관 팁 제공, 부족한 재료는 쿠팡 파트너스 연동으로 구매 유도
3. 기술 스택 (Tech Stack)
Language & Framework: Kotlin(Android), Spring Boot, Spring Security(JWT+OAuth2.0), Spring AI, FastAPI
Database & Persistence: PostgreSQL(RDS), Redis(ElastiCache), JPA/Flyway
AI/ML: EfficientNet V2-M, Gemini 2.5 Flash(VLM), Google Document AI(OCR), RAG Vector Store
Infrastructure & DevOps: AWS EC2(ASG)·ALB·S3(Presigned URL)·CloudFront·Route53, GitHub Actions(CI/CD), Firebase FCM
Monitoring & Resilience: AWS CloudWatch, 컨테이너/Redis 자동복구(restart)
4. 시스템 아키텍처 & 설계 (Architecture)
아키텍처 변화 과정:
Client(Android/Kotlin) ↔ Backend(Spring Boot REST API) ↔ AWS Cloud(EC2·ALB·RDS·S3·ElastiCache·CloudFront) ↔ External(Gemini 2.5 Flash, Document AI, Firebase FCM) 구조로 구성
클라이언트 직접 업로드 방식(Presigned URL) 도입으로 서버 부하 감소 및 DB 경량화
리소스 부담이 큰 YOLO 단독 방식 대신, EfficientNet V2-M(1차 판별) → 저신뢰도 시 Gemini VLM(2차 보정)의 2단 하이브리드 추론 구조로 전환
주요 데이터 흐름:
식재료 이름 검색 시 PostgreSQL trigram(GIN) + LOWER 인덱스 적용으로 Full Table Scan(31.0ms, 5.2만 행 스캔) → Bitmap Index Scan(1.08ms, 1행 스캔)으로 최적화
미학습/저신뢰도 이미지를 자동 라벨링하여 /auto_labeled/에 저장, 서비스 운영과 동시에 데이터셋을 자가 축적하는 self-improving 파이프라인 구성
5. 나의 역할 및 주요 기여 (My Key Contributions) — 전한준 (INFRA, 조장·주제제안자)

[인프라 구축 및 배포 안정화]

문제 상황: AWS VPC·EC2·RDS·S3 기반 인프라를 설계하고, CI/CD(GitHub Actions) 배포 파이프라인을 구축하는 과정에서 안정적인 운영 환경 확보가 과제였음
해결 방법: ALB+ASG 기반 컴퓨팅/로드밸런싱 환경 구성, ElastiCache(Redis) 캐시 계층 도입, SSL 적용 및 AWS CloudWatch 대시보드로 CPU 사용률·DB 커넥션·HTTP 에러율 모니터링 체계 구축
결과/성과: 무중단 운영 기반 마련 및 3개월간 안정적인 서비스 운영(누적 인프라 비용 약 $218.7 관리)

[FCM 알림 및 부하테스트]

문제 상황: 유통기한 임박 알림을 위한 Firebase FCM 연동과, 실제 사용자 트래픽 환경에서의 서비스 안정성 검증이 필요했음
해결 방법: FCM 기반 유통기한 스케줄링 알림 구축, 이미지 5.2만 개 등록 상태에서 30초 50명 → 1분 200명 → 3분 200명 순으로 단계적 부하테스트 수행 (/api/v1/items, /analytics/summary 등 주요 API 대상)
결과/성과: p95/p99 응답시간 임계치(800ms/1500ms) 내 통과 확인, 상대적 부하 증가에도 안정적인 응답 성능 확보
6. 핵심 트러블슈팅 및 성과 (Troubleshooting & Lessons Learned)
이슈: AI 서버(인스턴스) 교체 후 OCR·탐지 API가 200 OK를 반환하면서도 실제로는 빈 결과를 주는 "조용한 실패" 발생, 재기동 시에는 500 에러 발생
원인 분석: 신규 인스턴스에 Gemini API 키가 누락되어 있었고, Redis가 미기동 상태였던 것이 원인으로 확인됨
해결 방안: 시크릿 정보를 AWS SSM 파라미터 스토어 및 GitHub Secrets로 체계화하여 관리 일원화, 컨테이너·Redis에 자동 복구(restart) 로직 적용
개선 결과: 재배포·재기동 시 무중단 자동 복구 체계 확보 + 불필요한 재기동 대응 인력 투입 감소로 운영비 절감
