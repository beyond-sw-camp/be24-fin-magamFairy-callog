# feat/msajudge/storage

## 2026-05-21

### 요청
- 기존 캠페인 레퍼런스실을 AI 검수 산출물 전용 자료실로 전환한다.
- AI 판사 파일 검수 결과를 MongoDB `ad_check_analysis_results` 기준으로 조회한다.
- AI 판사 완료/실패 Kafka 이벤트를 알림으로 연결하고, 알림 바로가기는 `/references?analysisJobId={analysisJobId}`로 이동시킨다.
- 자료실 권한은 관리자/총괄/매니저/일반 사용자 계층을 따른다.

### 해석
- 브라우저는 `campaignId`만 보내고, requester/role/organization/company/department 정보는 app 서버에서 인증 사용자 기준으로 생성한다.
- `aijudge`는 Mongo 저장 문서와 Kafka 이벤트에 같은 `context`를 포함한다.
- 과거처럼 `context`가 없는 Mongo 문서는 관리자에게만 노출되어야 한다.
- 기존 `/references/create`, `/update`, `/delete`는 유지하되 새 화면에서는 사용하지 않는다.

### 구현
- app `/ad/check/file/aijudge` 요청에 서버 생성 `AnalysisContext`를 추가하고 `aijudge` multipart 요청에 JSON context part로 전달했다.
- `aijudge` 파일 검수 응답, Mongo 저장 문서, Kafka 이벤트에 `context`를 포함했다.
- app Kafka consumer가 `AI_JUDGE_COMPLETED`, `AI_JUDGE_FAILED` 이벤트를 소비해 요청자에게 알림을 생성하도록 연결했다.
- 알림 타입 `AI_JUDGE_COMPLETED`, `AI_JUDGE_FAILED`를 추가하고 QA 카테고리 및 알림 설정 조건에 포함했다.
- `GET /references/analysis`, `GET /references/analysis/{analysisJobId}` API를 추가했다.
- Mongo 조회 권한 필터를 `ROLE_ADMIN`, `ROLE_GENERAL_MANAGER`, `ROLE_MANAGER`, `ROLE_USER` 기준으로 적용했다.
- S3 objectKey 기반 presigned URL을 상세 조회 시 재생성하도록 했다.
- 프론트 `ReferencesView.vue`를 읽기 전용 자료실 화면으로 교체하고 검색, 타입 필터, 페이징, 상세 모달, URL query 자동 오픈을 구현했다.
- 헤더/사이드바/라우터 명칭을 자료실로 변경하고 헤더 알림에서 `targetUrl` 바로가기 버튼을 노출했다.
- AI 검수 파일 업로드 프론트 호출에서 `campaignId`만 추가 전송하도록 변경했다.

### 검증
- `.\gradlew.bat :app:compileJava :aijudge:compileJava` 성공.
- `npm run build` 성공.
- `.\gradlew.bat :aijudge:test` 성공.
- `.\gradlew.bat :app:test :aijudge:test`는 기존 테스트 환경 변수 누락으로 실패했다.
  - 1차 원인: `JAEGER_URL` placeholder 미해결.
  - `JAEGER_URL` 임시 지정 후 2차 원인: `${DB_URL}`이 실제 JDBC URL로 주입되지 않아 MariaDB 드라이버가 거부.

## 2026-05-22

### 요청
- 각 캠페인 상세 화면에 캠페인 자료실 탭을 추가한다.
- 자료실은 검수/승인 과정을 거친 데이터를 확인할 수 있는 화면으로 먼저 프론트 페이지부터 구현한다.

### 해석
- 현재 단계에서는 전용 자료실 API가 없으므로 기존 캠페인별 검수 요청 목록 API를 재사용한다.
- 기본 노출 대상은 `APPROVED`, `REJECTED` 상태의 처리 완료 자료로 두고, 필요 시 전체/진행중 자료도 필터로 확인할 수 있게 한다.
- 기존 캠페인 상세 탭 구조를 유지하고 새 탭만 추가한다.

### 구현
- 캠페인 상세 탭 목록에 `자료실` 탭을 추가했다.
- `?tab=library` 쿼리로 자료실 탭을 바로 열 수 있도록 매핑을 추가했다.
- `CampaignLibraryTab.vue`를 새로 만들고 검수 요청 목록을 자료실 형태로 표시했다.
- 요약 카운트, 상태 필터, 검색, 자료 선택, 상세 패널, 파일 열기, OCR 텍스트 및 검수 기록 표시를 구현했다.

### 변경 파일
- `frontend/src/views/CampaignDetailView.vue`
- `frontend/src/components/campaign/CampaignLibraryTab.vue`

### 검증
- `npm run build` 성공.
- Vite 개발 서버 `http://127.0.0.1:5174/` 응답 200 확인.

### 추가 요청
- AI 검수 완료/실패 알림을 실제 알림 흐름에 연결한다.
- 검수 실패는 AI 검수 작업 실패와 AI 검수 결과 확인 필요 상태를 구분한다.
- Kafka가 꺼진 로컬 환경에서도 동기 응답 경로에서 알림이 생성되도록 한다.

### 추가 해석
- `status=pass`는 검수 완료, `status!=pass`는 확인 필요, `errorMessage`가 있는 partial response는 검수 실패로 분리한다.
- 알림 수신자는 AI 검수 파일 업로드 요청자 1명으로 한다.
- Kafka 이벤트와 동기 응답 알림은 같은 dedupe key를 사용해 중복 생성을 막는다.

### 추가 구현
- app 알림 타입에 `AI_JUDGE_COMPLETED`, `AI_JUDGE_REVIEW_REQUIRED`, `AI_JUDGE_FAILED`를 추가하고 QA 카테고리/설정 조건에 포함했다.
- app `/ad/check/file/aijudge`가 인증 사용자와 선택적 `campaignId`를 받아 ai-judge context로 전달하고, 응답/partial response 기준으로 즉시 알림을 생성하도록 했다.
- aijudge 파일 검수 응답과 Kafka 이벤트에 requester context를 포함했다.
- app Kafka consumer가 requester context를 읽어 같은 알림 생성 로직을 호출하도록 연결했다.
- 프론트 알림 store, 헤더, 알림 센터, 알림 설정 패널에서 AI 검수 완료/확인 필요/실패 배지와 결과 보기 CTA를 표시하도록 했다.

### 추가 변경 파일
- `backend/app/src/main/java/org/example/backend/notification/model/NotificationType.java`
- `backend/app/src/main/java/org/example/backend/notification/model/NotificationDto.java`
- `backend/app/src/main/java/org/example/backend/notification/service/NotificationPreferenceResolver.java`
- `backend/app/src/main/java/org/example/backend/notification/service/NotificationService.java`
- `backend/app/src/main/java/org/example/backend/adcheck/controller/AdCheckController.java`
- `backend/app/src/main/java/org/example/backend/adcheck/service/AdCheckService.java`
- `backend/app/src/main/java/org/example/backend/adcheck/client/AiJudgeClient.java`
- `backend/app/src/main/java/org/example/backend/adcheck/event/AiJudgeKafkaEventListener.java`
- `backend/app/src/main/java/org/example/backend/adcheck/model/AdCheckDto.java`
- `backend/aijudge/src/main/java/com/example/adcheck/controller/AiJudgeController.java`
- `backend/aijudge/src/main/java/com/example/adcheck/service/AiJudgeFileCheckService.java`
- `backend/aijudge/src/main/java/com/example/adcheck/service/AiJudgeKafkaEventPublisher.java`
- `backend/aijudge/src/main/java/com/example/adcheck/model/AdCheckDto.java`
- `frontend/src/api/adcheck/index.js`
- `frontend/src/stores/notifications.js`
- `frontend/src/components/Header.vue`
- `frontend/src/views/NotificationCenter.vue`
- `frontend/src/views/ReviewApprovalView.vue`
- `frontend/src/components/notifications/NotificationSettingsPanel.vue`

### 추가 검증
- `.\gradlew.bat :app:compileJava :aijudge:compileJava` 성공.
- `npm run build` 성공.
- `.\gradlew.bat :aijudge:test` 성공.
- `.\gradlew.bat :app:test :aijudge:test`는 app 테스트 환경의 `JAEGER_URL` placeholder 미해결로 실패했다.
## 2026-05-23

### 요청
- 분석 서버 보안 적용으로 Spring/MSA 서버의 layout/OCR 호출에 `X-API-Key: ${LAYOUT_SERVICE_API_KEY}` 헤더를 추가한다.
- 키가 없으면 서버 시작 시점 또는 호출 전 명확한 에러가 나도록 처리한다.

### 해석
- 분석 서버 호출부는 app 모듈과 aijudge 모듈의 `TextExtractorService`에 남아 있다.
- layout 분석, OCR 동기 호출, OCR job 생성/조회, layout crop 다운로드가 모두 같은 분석 서버 계열 호출이므로 같은 API Key 헤더를 공통 RestClient 기본 헤더로 적용한다.

### 구현
- app/aijudge `TextExtractorService` 생성자에서 `custom.layout.api-key`를 주입받아 비어 있으면 `IllegalStateException`을 발생시키도록 했다.
- layout/OCR/crop 전용 `RestClient`에 `X-API-Key` 기본 헤더를 설정해 분석 서버로 나가는 모든 요청에 API Key가 붙도록 했다.
- app dev/prod/tls 및 aijudge 설정에 `custom.layout.api-key: ${LAYOUT_SERVICE_API_KEY:}`를 추가했다.

### 변경 파일
- `backend/app/src/main/java/org/example/backend/adcheck/service/TextExtractorService.java`
- `backend/aijudge/src/main/java/com/example/adcheck/service/TextExtractorService.java`
- `backend/app/src/main/resources/application-dev.yml`
- `backend/app/src/main/resources/application-prod.yml`
- `backend/app/src/main/resources/application-tls.yml`
- `backend/aijudge/src/main/resources/application.yml`

### 검증
- `.\gradlew.bat :app:compileJava :aijudge:compileJava` 성공.
- `LAYOUT_SERVICE_API_KEY=dummy-layout-key .\gradlew.bat :aijudge:test` 성공.

### 추가 조치
- OCR/layout 추출 단계에서 실패하면 기존에는 `FileCheckException`으로 감싸지기 전에 예외가 빠져나가 Spring 기본 500 JSON만 반환됐다.
- ai-judge 추출 실패도 `FileCheckRes` partial response로 감싸 `errorMessage`와 `extractionMode=text_extraction_failed`를 app에 전달하도록 수정했다.

### 추가 변경 파일
- `backend/aijudge/src/main/java/com/example/adcheck/service/AiJudgeFileCheckService.java`

### 추가 검증
- `.\gradlew.bat :aijudge:compileJava` 성공.
- `LAYOUT_SERVICE_API_KEY=dummy-layout-key .\gradlew.bat :aijudge:test` 성공.

### 캠페인 자료실 후속
- 미커밋 상태의 `CampaignLibraryTab.vue` 한글 문자열과 템플릿 문법을 확인했다.
- 자료실 탭 연결 변경은 `CampaignDetailView.vue`의 import, 탭 목록, query map, 탭 렌더링으로 한정되어 있다.
- `rg`로 깨진 문자 패턴을 확인했고 자료실 컴포넌트에서는 발견되지 않았다.
- `npm run build` 성공.

### 알림 말풍선 후속
- SSE `notification.created` 이벤트로 새 알림이 들어왔을 때만 헤더 알림 아이콘 아래 말풍선을 표시하도록 했다.
- 기존 목록 로드 알림은 말풍선 대상에서 제외하고, 앱 내 알림 사용 여부/알림 정도/조건 설정을 통과한 알림만 표시하도록 했다.
- 말풍선은 자동 닫힘, 수동 닫기, 알림 상세 이동, 대상 화면 이동 CTA를 제공한다.
- `npm run build` 성공.

### 검수 프로세스 과정 시각화
- 기존 `/ad/check/file/aijudge` 동기 호출은 유지하고, 신규 비동기 Job API를 추가했다.
- `ad_check_job` RDB 엔티티에 요청자, 캠페인, 파일명, 상태, 단계, 진행률, 오류, 결과 payload, 시작/종료 시각을 저장하도록 했다.
- `POST /ad/check/jobs`, `GET /ad/check/jobs/{jobId}`, `GET /ad/check/jobs/active`, `POST /ad/check/jobs/{jobId}/cancel`을 추가했다.
- app 내부 단일 worker가 `QUEUED` Job을 하나씩 처리하고, ai-judge 원격 호출 중 단계/진행률을 RDB와 SSE로 갱신하도록 했다.
- 기존 Notification SSE 스트림에 `ad-check.job.created`, `ad-check.job.updated`, `ad-check.step.changed`, `ad-check.completed`, `ad-check.failed` 이벤트를 추가했다.
- 프론트에 `adCheckJobs` Pinia store를 추가해 SSE/REST 상태를 통합 관리하고, 새로고침 후 active Job을 복구하도록 했다.
- SSE 연결 누락/일시 끊김에 대비해 active Job은 `GET /ad/check/jobs/{jobId}` REST polling fallback으로도 갱신하도록 했다.
- `ReviewApprovalView` 업로드 흐름을 Job 생성 방식으로 바꾸고, 검수 창 내부에 단계형 progress UI와 결과 보기 CTA를 추가했다.
- 전역 `AdCheckFloatingProgress` 패널을 `App.vue`에 연결해 화면 이동 후에도 우측 하단에서 진행/대기/완료/실패 상태를 확인할 수 있게 했다.

### 검수 프로세스 시각화 변경 파일
- `backend/app/src/main/java/org/example/backend/adcheck/controller/AdCheckController.java`
- `backend/app/src/main/java/org/example/backend/adcheck/model/AdCheckJob.java`
- `backend/app/src/main/java/org/example/backend/adcheck/model/AdCheckJobDto.java`
- `backend/app/src/main/java/org/example/backend/adcheck/model/AdCheckJobStatus.java`
- `backend/app/src/main/java/org/example/backend/adcheck/model/AdCheckJobStep.java`
- `backend/app/src/main/java/org/example/backend/adcheck/repository/AdCheckJobRepository.java`
- `backend/app/src/main/java/org/example/backend/adcheck/service/AdCheckJobService.java`
- `backend/app/src/main/java/org/example/backend/notification/service/NotificationSseService.java`
- `frontend/src/App.vue`
- `frontend/src/api/adcheck/index.js`
- `frontend/src/components/adcheck/AdCheckFloatingProgress.vue`
- `frontend/src/components/adcheck/AdCheckJobProgress.vue`
- `frontend/src/stores/adCheckJobs.js`
- `frontend/src/stores/notifications.js`
- `frontend/src/views/ReviewApprovalView.vue`

### 검수 프로세스 시각화 검증
- `.\gradlew.bat :app:compileJava` 성공.
- `npm run build` 성공.

### 검수 프로세스 시각화 후속 수정
- 검수 Job이 `QUEUED`에서 멈추는 상황을 방지하기 위해 app worker에 대기 Job 재등록 스케줄러와 worker thread 재시작 보호 로직을 추가했다.
- worker 처리 초반에 예외가 발생하거나 파일 payload가 없는 경우 Job을 `FAILED`로 명확히 전환하고 SSE 실패 이벤트를 전송하도록 보강했다.
- 진행 UI를 중앙 상단 기준으로 노출하도록 floating progress 패널 위치를 변경했다.
- 검수 단계 목록은 세로형에서 `->` 방향의 수평 진행형으로 변경했다.

### 검수 단계 간소화 후속
- 검수 진행 단계를 `대기 중`, `데이터 추출`, `데이터 분석`, `결과 도출`, `완료` 5단계로 간소화했다.
- app Job worker가 새 5단계 기준으로 상태를 갱신하도록 `PROCESS_STEPS`를 정리했다.
- 기존 DB/SSE에 남아 있을 수 있는 이전 세부 단계명은 프론트 store에서 새 5단계로 정규화하도록 했다.
- 진행 UI는 5개 step을 수평으로 표시해 가로 스크롤이 생기지 않도록 폭을 조정했다.
- `.\gradlew.bat :app:compileJava` 성공.
- `npm run build` 성공.

### 검수 단계 실제 처리 연동 후속
- app Job worker의 시간 기반 단계 전환 로직을 제거했다.
- Job 생성 시 진행 콜백 토큰을 저장하고 ai-judge 호출 context에 `adCheckJobId`, `adCheckProgressToken`, `adCheckProgressCallbackUrl`을 포함하도록 했다.
- ai-judge는 실제 처리 지점에서만 app 콜백을 호출한다.
  - `DATA_EXTRACTION`: 파일 텍스트/레이아웃/OCR 추출 시작
  - `DATA_ANALYSIS`: 추출 데이터 기반 AI 분석 시작
  - `RESULT_BUILDING`: AI 분석 성공 후 결과 저장/응답 구성 시작
- app은 `POST /ad/check/jobs/internal/progress`에서 토큰을 검증한 뒤 RDB와 SSE 상태를 갱신한다.
- `.\gradlew.bat :app:compileJava :aijudge:compileJava` 성공.

### 검수 진행 UI 후속
- 검수 진행 컴포넌트의 header/step grid 폭을 부모 레이아웃 기준으로 정리해 다른 정보 패널과 비율을 맞췄다.
- active Job 진행률은 현재 단계 기준으로 정규화해 progress bar가 5단계 상태를 따라가도록 했다.
- 캠페인 상세 탭 클릭 시 URL `tab` query를 동기화해 검수 탭 여부를 전역 floating 패널이 판단할 수 있게 했다.
- 검수 탭에서는 내부 progress UI만 보이고, 다른 화면으로 벗어나면 오른쪽 하단 compact floating 패널에 파일명/단계/진행률만 표시한다.
- compact floating 패널은 닫기 버튼으로 숨길 수 있고, `진행 화면` 버튼으로 `/campaigns/{campaignId}?tab=review&adCheckJobId={jobId}`에 복귀한다.
- `npm run build` 성공.

### 검수 진행률/문구 후속
- 분석 서버 데이터 추출은 5~10초, AI 텍스트 분석은 10~30초 소요되는 흐름에 맞춰 단계 진행률을 `5% → 25% → 45% → 90% → 100%`로 재배분했다.
- 현재 단계는 `~중`, 완료된 이전 단계는 `~완료되었습니다` 문구로 표시하도록 frontend step 표시 로직을 분리했다.
- 완료된 대기 단계는 `작업 시작` / `작업이 시작되었습니다.`로 보여 더 이상 지나간 단계가 계속 대기 중처럼 보이지 않게 했다.
- backend `AdCheckJobStep` enum의 label/message/progress도 같은 기준으로 맞췄다.
- `.\gradlew.bat :app:compileJava` 성공.
- `npm run build` 성공.

### 검수 자료 보여주기
- AI 검수 Job 완료 시 상세 결과를 MongoDB에 먼저 저장하고, 성공한 경우에만 RDB Job 요약을 `SUCCEEDED`로 갱신하도록 했다.
- RDB 요약에는 `resultStatus`, `riskLevel`, `summaryMessage`, `mongoDocumentId`를 저장해 목록/카드 조회에 필요한 정보만 빠르게 제공한다.
- Mongo 상세 문서에는 `jobId`, 원본 파일 메타데이터, 추출 텍스트, 문서 구조 요약, OCR/인식 결과, 문구 위험도 결과, 최종 판정, 오류 상세, context를 저장하도록 app/aijudge 저장 포맷을 맞췄다.
- `GET /ad/check/jobs`, `GET /ad/check/jobs/{jobId}`, `GET /ad/check/jobs/{jobId}/detail` 경로를 정리했다.
  - 목록은 RDB 요약을 반환한다.
  - 상세는 인증 사용자 소유 Job인지 확인한 뒤 `mongoDocumentId` 또는 `jobId`로 Mongo 상세를 조회한다.
- RDB transaction 안에서 요약 갱신과 Outbox 이벤트 저장을 함께 처리하고, 별도 `AdCheckOutboxPublisher`가 Kafka 발행을 담당하도록 했다.
- Mongo 저장 이후 RDB 요약/Outbox 저장 트랜잭션은 짧은 retry를 거쳐 일시 실패 시 재처리할 수 있게 했다.
- Outbox 이벤트 타입은 `ad-check.detail-saved`, `ad-check.summary-created`, `ad-check.summary-updated`, `ad-check.result-ready`를 사용한다.
- 프론트 `adCheckJobs` store에 RDB 요약 목록과 Mongo 상세 조회 상태를 추가했다.
- `ReviewApprovalView`에 AI 검수 자료 목록/카드와 상세 패널을 추가했다.
  - 카드에는 파일명, 상태, 판정, 위험도, 요약 메시지, 요청/완료 시각을 표시한다.
  - 상세 보기 시 Mongo 상세를 조회하고 로딩/실패 상태를 표시한다.
  - 상세 패널에는 원본 파일 메타데이터, 추출 텍스트, 문서 구조 분석, 글자 인식, 문구 위험도, 최종 판정, 오류 상세를 표시한다.
- `.\gradlew.bat :app:compileJava :aijudge:compileJava` 성공.
- `npm run build` 성공.

### 검수 결과 저장/알림 불일치 수정
- async Job 경로에서는 ai-judge 원격 응답 시점에 완료 알림을 만들지 않도록 했다.
- Mongo 상세 저장과 RDB 요약/Outbox 저장이 완료된 뒤 app Job 서비스가 완료 알림을 만들도록 알림 생성 시점을 옮겼다.
- Mongo 상세 저장 실패 등 Job 실패 시에는 완료 알림 대신 실패 알림을 만들고, 검수 진행 UI도 실패 상태로 맞춰 표시하도록 했다.
- Kafka consumer는 app이 관리하는 `adCheckJobId` context가 있는 ai-judge 이벤트를 알림 생성 대상에서 제외해 중복/선행 완료 알림을 방지한다.
- MongoDB 상세 저장소 연결 실패 메시지를 `MONGODB_STD_URL`, 포트, 인증 정보를 확인하라는 명확한 오류로 감싸고, 연결 대기 timeout을 짧게 제한했다.
- 실제 처리 시간 비중에 맞춰 진행률을 `대기 5% → 데이터 추출 20% → 데이터 분석 35% → 결과 도출 95% → 완료 100%`로 재조정했다.
- `.\gradlew.bat :app:compileJava :aijudge:compileJava` 성공.
- `npm run build` 성공.

### 캠페인 자료실 검수 자료 정합성 수정
- 캠페인 `자료실` 탭이 기존 수동 검수 요청 목록(`ad-review-requests`)을 조회해 `검사하기`로 생성된 AI 검수 Job 결과와 다른 자료를 보여주던 문제를 확인했다.
- `CampaignLibraryTab.vue`의 조회 소스를 `GET /ad/check/jobs?campaignId=...`와 `GET /ad/check/jobs/{jobId}/detail`로 변경해 검수/승인 화면의 AI 검수 자료와 같은 RDB 요약/Mongo 상세 데이터를 사용하도록 맞췄다.
- 자료실 카드에는 파일명, 상태, 판정, 위험도, 요약, 요청/완료 시간을 표시하고, 상세 패널에는 원본 파일 링크, Mongo ID, 추출 텍스트, 문서 구조 분석, 글자 인식, 문구 위험도 분석, 최종 상세 판정을 표시하도록 정리했다.
- 완료된 캠페인 Job의 `검수 결과 보기`와 floating progress 결과 이동은 캠페인 자료실 탭(`tab=library&adCheckJobId=...`)으로 연결해 같은 Job 상세가 선택되도록 변경했다.

### 캠페인 자료실 상세 모달 전환
- 자료실 목록 화면에 상세 분석 내용이 과하게 노출되지 않도록 카드 목록만 남기고 상세 정보는 모달에서 확인하도록 변경했다.
- 자료 카드를 선택하거나 `상세 보기` 버튼을 누르면 같은 Job의 Mongo 상세를 불러와 모달로 표시한다.
- 모달 안에는 `요약 보기`와 `전체 내용 보기` 탭을 두어 기본 요약 정보와 전체 추출/분석 데이터를 분리했다.
- `Escape` 키와 배경 클릭, 닫기 버튼으로 모달을 닫을 수 있게 했다.

### AI 검수 판단 등급 UI 세분화
- AI 검수 결과 표시 기준을 `통과`, `재확인 필요`, `수정 제안`, `수정 필요`, `위험` 5단계로 정리했다.
- frontend 공통 유틸 `adCheckVerdict`를 추가해 검수 화면과 캠페인 자료실이 같은 판정 문구와 색상 단계를 사용하도록 했다.
- 검수 결과 카드에는 5단계 스케일을 노출하고 현재 등급을 강조하도록 변경했다.
- 캠페인 자료실 카드/상세 모달의 판정 정보도 새 등급 기준으로 표시하도록 맞췄다.

### AI 검수 결과 가독성 보강
- AI 검수 결과 카드에서 금지/반려 위험, 중요 확인, 추천 수정 키워드를 배지와 하이라이트로 강조하도록 했다.
- 문제 표현은 별도 강조 영역으로 표시하고 위험 키워드가 눈에 띄도록 처리했다.
- 위반 사유는 문장 단위로 줄바꿈해 읽기 쉽게 표시한다.
- 수정 제안은 항상 번호 목록으로 표시해 제안 항목을 구분할 수 있게 했다.

### 다크 모드 적용 확인 및 보강
- 전역 다크 모드는 planner store가 `document.documentElement.dataset.theme`을 변경하고, `base.css`의 `:root[data-theme='dark']` 토큰이 전체 화면에 적용되는 구조임을 확인했다.
- 검수 결과 판정 단계와 자료실 모달의 신규 배지/하이라이트 색상에 다크 모드 전용 대비 색상을 추가했다.
- 자료실 모달 backdrop은 다크 모드에서 밝게 뜨지 않도록 검은 반투명 배경으로 보정했다.

### AI 검수 판정 과대평가 보정
- `violation` 응답과 `보장`, `단정`, `금지` 같은 단어만으로 5단계 `위험`으로 올리던 표시용 판정 기준을 완화했다.
- 5단계는 `제출 반려`, `사용 불가/사용 금지`, `불법`, `허위`, `기만`처럼 실제 제출 불가에 가까운 표현이 있을 때만 사용하도록 제한했다.
- 근거/증빙/출처를 보완하면 사용할 수 있는 표현은 `재확인 필요`로 분류되도록 조정했다.

### AI 검수 판단 단계 수신 구조 전환
- 판단 등급 1~5단계는 프론트에서 문구로 추론하지 않고 AI/n8n 응답의 `verdictLevel` 계열 값을 그대로 사용하도록 변경했다.
- app/aijudge DTO와 파서가 `verdictLevel`, `reviewLevel`, `riskLevel`, `level`, `grade` 등 1~5 숫자 응답을 수신할 수 있게 했다.
- n8n `output`이 JSON 객체가 아니라 `1`, `2단계`처럼 단계 값만 직접 내려오는 경우에도 `verdictLevel`로 보존하도록 보강했다.
- Job 요약, Mongo 상세, Kafka 이벤트, 프론트 store/자료실 화면까지 `verdictLevel`을 전달해 검수 화면과 자료실이 같은 판단 단계를 표시하도록 맞췄다.
- 프론트에서 판단 단계 값이 없는 경우 임의 분류하지 않고 `판정 대기`로 표시하도록 변경했다.

### AI 검수 알림 이동 경로 수정
- AI 검수 완료/확인 필요/실패 알림의 `검수 결과 보기` targetUrl이 레퍼런스실(`/references`)로 이동하던 문제를 수정했다.
- async Job 완료 알림은 Job의 `campaignId`와 `jobId`를 사용해 `/campaigns/{campaignId}?tab=review&adCheckJobId={jobId}`로 이동하도록 변경했다.
- 일반 ai-judge 알림도 응답 context의 `campaignId`, `adCheckJobId`를 우선 사용해 캠페인 검수 결과 창으로 이동하도록 보강했다.
- Job DTO의 `targetUrl`도 캠페인 Job이면 검수 탭 URL을 우선 반환하도록 맞췄다.
- 이미 `/references?analysisJobId=...`로 저장된 기존 AI 검수 알림도 알림센터/헤더 클릭 시 Job 목록에서 매칭되는 Job을 찾아 검수 결과 창 URL로 보정하도록 했다.

### 캠페인 자료실 상세 가독성 개선
- 자료실 상세 모달의 요약 보기에서 사용자가 알 필요 없는 내부 상태값과 MongoDB ID를 제거했다.
- 전체 내용 보기에서도 내부 추적용 Job ID와 MongoDB ID를 숨기고, 파일 유형과 파일 크기 중심으로 표시하도록 정리했다.
- AI 검수 요약, 관련 법령, 검수 사유는 문장 단위로 나눠 읽기 좋게 표시하도록 변경했다.
- 문제 표현은 강조 칩 목록으로 분리하고, 수정 제안은 번호 목록으로 렌더링해 긴 문장이 한 덩어리로 보이지 않도록 개선했다.

### AI 검수 상세 공통 모달 및 썸네일 보강
- 검수/승인 페이지와 캠페인 자료실이 함께 사용하는 `AdCheckDetailModal`을 추가했다.
- 검수/승인 페이지의 상세 보기를 기존 인라인 패널에서 모달 방식으로 전환했다.
- 자료실 상세 보기 역시 같은 공통 모달을 사용하도록 바꿔 요약/전체 내용 표시 방식을 통일했다.
- 전체 내용 보기에서 문서 구조 분석, 글자 인식, 문구 위험도 분석, 최종 상세 판정을 raw JSON 대신 요약 지표와 문장/칩/번호 목록으로 정제해 표시하도록 했다.
- 상세 모달에 원본/추출 이미지 미리보기를 추가하고, 추출 이미지는 페이지별로 묶어 페이지당 최대 2개씩 표시하도록 했다.
- 검수/승인 AI 검수 자료 목록과 자료실 목록에 대표 썸네일과 클라이언트 페이징을 추가했다.
- app Job 요약 응답에 `fileUrl`, `fileContentType`, `fileSize`, `thumbnailUrl`을 포함해 목록 단계에서 미리보기를 표시할 수 있게 했다.

### 알림 설정 간소화 및 자료실 썸네일 복구
- 알림 설정 패널을 `표시 방식`과 `받을 알림` 중심으로 재구성해 세부 조건 노출을 줄였다.
- 우측 하단 AI 검수 진행 패널 표시 여부를 사용자 알림 설정에서 켜고 끌 수 있게 했다.
- 전체 알림을 끄면 헤더 알림뿐 아니라 AI 검수 floating progress 패널도 함께 숨기도록 연결했다.
- 자료실 상세 모달을 닫을 때 URL의 `adCheckJobId` 쿼리를 제거해 같은 자료가 계속 자동으로 열리는 현상을 막았다.
- 자료실 목록 썸네일이 요약 응답에 없으면 현재 페이지 항목의 상세 데이터를 조용히 조회해 대표 이미지로 보강하도록 했다.
- app Job 요약/상세 조회 시 S3 objectKey 기반 보기 URL을 새로 발급해 만료된 URL 때문에 썸네일과 파일 미리보기가 깨지는 문제를 줄였다.

### AI 검수 이후 최종 검수 요청 및 자료 삭제 권한
- AI 검수 Job 완료 후 업로더 본인만 해당 Job을 최종 검수 요청으로 제출할 수 있도록 `adCheckJobId` 기반 요청 생성을 연결했다.
- 최종 검수 요청은 AI 판정이 pass가 아니어도 생성 가능하게 하여 AI 1차 검수 이후 PM 담당자가 최종 허가/반려를 판단하는 흐름으로 분리했다.
- 캠페인 자료실은 캠페인 멤버가 같은 캠페인의 AI 검수 자료를 함께 조회할 수 있게 하고, 상세 조회도 캠페인 멤버 권한으로 허용했다.
- 자료 삭제는 업로더 본인만 가능하도록 서버에서 검증하고, 진행 중 Job은 삭제할 수 없게 막았다.
- 자료실 목록에 본인 자료 삭제 버튼을 추가하고 삭제된 Job은 자료실/목록에서 숨기도록 처리했다.
- 상세 모달의 비개발자 불필요 항목인 `추출 방식` 표시를 제거했다.
- `.\gradlew.bat :app:compileJava` 성공.
- `npm run build` 성공.

### 캠페인 자료 검수 탭 구조 및 단계 필터 정리
- 캠페인 상세 상위 탭에서 `검수/승인`과 `자료실`을 분리 노출하지 않고 `자료 검수` 상위 탭 아래에 `검수`, `승인`, `자료실` 보조 탭으로 묶었다.
- 기존 `tab=review`, `tab=library`, `adCheckJobId` 링크가 새 구조에서도 자연스럽게 `자료 검수` 내부 탭으로 복구되도록 라우팅 호환 처리를 추가했다.
- `ReviewApprovalView`를 `check`/`approval` 모드로 재사용해 AI 검수 자료 목록과 인간 최종 승인 요청 목록을 같은 컴포넌트 안에서 분리 표시하도록 했다.
- AI 검수 완료 자료는 업로더 본인이 `검수 요청하기`를 눌러야만 최종 승인 목록으로 올라가도록 버튼 문구와 표시 위치를 정리했다.
- 캠페인 자료실 필터와 요약 카운트를 `전체`, `완료`, `확인 필요`, `수정 제안`, `수정 필요`, `반려` 기준으로 바꿔 AI가 내려준 1~5단계 판정 값을 기준으로 조회하도록 했다.
- `npm run build` 성공.

### 캠페인 내부 보조 탭 버튼 스타일 통일
- 팀 보드 보기의 `업무 파트`/`마일스톤` 전환 버튼을 `자료 검수` 보조 탭과 같은 segmented 버튼 스타일로 맞췄다.
- 팀 보드 보기의 검색/업무 추가/생성 버튼 묶음도 같은 외곽선과 radius를 가진 컨트롤 그룹으로 정리했다.
- 매칭 탭의 `홈`/`혜택 목록`/`파트너 평가` 버튼도 같은 border, radius, active color 규칙을 사용하도록 수정했다.
- 매칭 탭 카운트 배지는 기존 정보를 유지하되 active 상태에서 primary 색상으로 통일했다.
- `npm run build` 성공.

### AI 검수 완료 자료 승인 요청 문구 정리
- AI 검수를 새로 시작하는 버튼은 `AI 검수 요청하기`로 표시해 분석 시작 액션임을 분리했다.
- AI 검수가 완료된 자료를 PM 최종 판단 단계로 넘기는 버튼은 `승인 요청하기`로 변경했다.
- 승인 요청 목록/실패 메시지/빈 상태 문구도 `검수 요청` 대신 `승인 요청` 기준으로 정리했다.

### AI 검수 자료 업로더 표시
- AI 검수 Job 요약/상세 응답에 업로더 로그인 ID, 이름, 조직명을 포함하도록 DTO를 확장했다.
- 검수 탭의 `AI 검수 자료` 카드에 누가 자료를 업로드했는지 표시하도록 했다.
- 승인 요청 목록의 요청자 표시도 이름과 조직이 함께 보이도록 정리했다.
- 캠페인 자료실 목록과 공통 상세 모달에서도 업로더 정보를 확인할 수 있게 했다.

### 알림센터 목록 페이징 처리
- 알림센터 목록이 필터 결과 전체를 한 번에 렌더링하지 않고 10개씩 페이지 단위로 표시되도록 했다.
- 필터별 전체 개수와 상단 통계는 유지하고, 목록 헤더에는 현재 페이지에서 보이는 범위를 표시하도록 정리했다.
- 필터 변경, 쿼리 `notificationId` 진입, 페이지 변경 시 선택 알림과 상세 패널이 현재 페이지에 맞게 동기화되도록 했다.

### AI 검수 app-ai-judge Kafka 통신 전환
- 검수 Job worker가 ai-judge HTTP API를 직접 호출하지 않고 `ai-judge.requests` Kafka 토픽으로 파일 검수 요청을 발행하도록 변경했다.
- ai-judge 서비스는 `ai-judge.requests`를 소비해 파일 분석을 수행하고, 진행/완료/실패 이벤트를 `ai-judge.completed` 토픽으로 발행한다.
- app은 `AI_JUDGE_PROGRESS`, `AI_JUDGE_COMPLETED`, `AI_JUDGE_FAILED` 이벤트를 소비해 Job 진행률, 완료, 실패 상태를 갱신한다.
- 기존 HTTP progress callback 대신 Kafka progress 이벤트를 사용하도록 연결해 MSA 간 AI 검수 흐름의 중심 통신을 Kafka로 맞췄다.
- `.\gradlew.bat :app:compileJava :aijudge:compileJava` 성공.
- `npm run build` 성공.
