## 🛠 기술 스택 (Tech Stack)

### Backend
* **Framework:** Java 17 / Spring Boot 3.x
* **Security:** Spring Security, JWT (Json Web Token)
* **Database:** MySQL, Spring Data JPA, MongoDB
* **AI Integration:** n8n, Python

### DevOps & Infrastructure
* **Containerization:** Docker, Docker Compose
* **Orchestration:** Kubernetes (K8s)
* **CI/CD:** Jenkins, Github
* **OS/Environment:** Linux (Ubuntu)

---

## 🏗 시스템 아키텍처 (Architecture)
> <img width="1251" height="1011" alt="시스템아키텍쳐(캘로그)(8)" src="https://github.com/user-attachments/assets/fc99ddaa-dd3f-47c1-95d8-add5a645b779" />


* **테스트 빌드:** sub 브랜치에 Push할 경우, Main과 동일하게 조성된 서버환경에 사전 배포, 각종 테스트를 거친 후 Main 브랜치 Push
* **CI/CD 흐름:** 코드 Push → Jenkins 웹훅 감지 → 빌드 및 Docker 이미지 패키징 → Kubernetes 클러스터 배포
* **무중단 배포 전략:** Blue Green 전환방식, 안정성 확보를 위해 Jenkins에서 자동으로 전환하지 않고 수동으로 전환.
* **MSA 적용:** Database Per Service, Circuit Breaker, Event Sourcing, API-Gateway 패턴 적용

---

## 🔥 주요 기능
* **제휴 캠페인 협업 및 일정 관리 인프라**
  * 마케팅 캠페인 파트너십 진행 프로세스 전반을 트래킹할 수 있는 API 설계
* **Spring Security & JWT 기반 권한 세분화**
  * 브랜드사 및 파트너사 간의 명확한 권한 관계를 정의하고 웹토큰 핸들링을 통해 보안성 확보
* **AI 기반 파트너 추천 및 제휴 평가**
  * 브랜드 자산 데이터를 파싱·비교하여 최적의 파트너 혜택을 매칭
* **AI 기반 캠페인 컨텐츠 사전 검수**
  * 브랜드 톤&매너, 금칙어, 법적 리스크등을 일차적으로 검수
* 상세 위키 보기

---
## 📃 모듈별 API 명세서
| 베이스 모듈 (APP) | AI Judge 서비스 모듈 | Matching 서비스 모듈 |
|:-----------------:| :---: |:----------:|
|[APP 서비스 API 명세서.pdf](https://github.com/user-attachments/files/28333506/APP.API.-.pdf)|[AI Judge 서비스 API 명세서.pdf](https://github.com/user-attachments/files/28331565/AI.Judge.API.pdf)|[matching 서비스 API 명세서.pdf](https://github.com/user-attachments/files/28331566/matching.API.pdf)|


---

# 🎬 Callog API 기능 테스트 및 시연 가이드

본 문서에는 **Callog** 서비스의 백엔드 주요 API 및 기능들에 대해 Swagger를 이용하여 정상 동작을 검증한 총 28개의 테스트 영상이 포함되어 있습니다. 
각 기능별로 드롭다운 메뉴를 통해 **역할 요약**과 **시연 영상**을 바로 확인할 수 있습니다.

---

## 🚀 기능 시연 및 테스트 영상 리스트

<details>
<summary><b>🎥 1. Auth_User</b></summary>
<br>

> **기능 요약**
> JWT 기반의 사용자 회원가입, 로그인/로그아웃, 토큰 재발급, 비밀번호 변경 및 조직 내 권한 및 관리직원 관리를 담당하는 인증 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/b9c8c11f-1306-463b-84c4-5311df814c0c" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 2. Swagger - Ai Judge</b></summary>
<br>

> **기능 요약**
> AI 기반의 텍스트 광고 카피 검수, 멀티파트 파일 업로드를 통한 대용량 콘텐츠 AI 정밀 분석 및 분석 결과를 상세 조회하는 AI Judge 서비스 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/85381ed4-03b8-4e69-b65f-856760cfef8b" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 3. Task</b></summary>
<br>

> **기능 요약**
> 캠페인 연동 업무(Task) 및 개인 업무의 생성, 수정, 삭제, 상세 조회 및 캠페인별 업무 리스트 조회를 처리하는 업무 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/1e59d7fa-3a53-4871-a03b-a62f2b84aa59" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 4. Task-Parts</b></summary>
<br>

> **기능 요약**
> 특정 캠페인의 마일스톤에 연동된 세부 업무 구성 요소(Task Part)의 목록 조회, 생성, 수정, 삭제를 지원하는 세부 업무 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/1c3708c1-ac1b-410d-9ac9-2220827469b4" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 5. ad-ai-analysis</b></summary>
<br>

> **기능 요약**
> 캠페인 내 광고 시안 파일에 대해 AI Judge를 원격 호출하여 금칙어, 톤앤매너 등을 검수(분석)하고 분석 내역 및 상세 결과를 관리하는 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/9c2d97ba-a5b5-4b02-aaf4-003c9acd3397" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 6. ad-analysis-benchmark</b></summary>
<br>

> **기능 요약**
> 개발 환경에서 대용량 광고 심의 데이터 생성(seed) 및 심의/검수 속도와 정확도를 측정하는 벤치마크 테스트 지원 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/01997ed0-2804-4562-87f6-1f9caa614943" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 7. ad-check</b></summary>
<br>

> **기능 요약**
> 사용자가 입력한 광고 카피 텍스트 또는 업로드한 시안 파일에 대해 비동기/동기 검수 작업(Job)을 등록, 진행 상태 조회, 상세 분석 결과 제공 및 작업을 취소/삭제하는 광고 자율 심의 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/69e3b4d6-46c0-40fe-b5ae-e428e03f9904" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 8. ad-review-request</b></summary>
<br>

> **기능 요약**
> 캠페인 진행 중 작성된 광고 시안에 대해 승인권자에게 심의(검토) 요청을 생성하고, 이를 승인 또는 반려(기각)하는 결재선 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/e582459a-bdd1-4a39-a416-b67609c60f8e" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 9. assets</b></summary>
<br>

> **기능 요약**
> 기업 또는 브랜드가 보유하고 있는 핵심 유무형 자산(Asset) 정보를 등록, 수정, 삭제, 개별 조회 및 전체 목록 조회하는 매칭용 자산 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/554ea305-76d6-4fd8-b0fd-52a2ef83a99d" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 10. benefit</b></summary>
<br>

> **기능 요약**
> 제휴 파트너십 매칭을 위한 파트너 혜택(Benefit) 정보의 신규 등록, 특정 캠페인별 제공 혜택 조회 및 전체 혜택 리스트 조회를 수행하는 혜택 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/8bd3c973-3346-4105-ba6b-f2f957e73058" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 11. calendar-io</b></summary>
<br>

> **기능 요약**
> 사용자의 업무 및 마일스톤 일정을 표준 구글 캘린더 형식(.ics) 파일로 내보내거나(다운로드), 외부 .ics 파일을 업로드하여 개인 업무 일정으로 가져오는(Import/Export) 캘린더 연동 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/01b7a09b-5860-4dbf-a050-2019bde445bb" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 12. campaign frame</b></summary>
<br>

> **기능 요약**
> 마케팅 캠페인의 기본 구조와 틀을 사전에 정의하는 캠페인 프레임(Frame)의 생성, 목록 조회, 상세 조회, 수정, 삭제를 담당하는 프레임워크 템플릿 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/a0f0dda8-c8c7-4dc3-a720-1124804cebbb" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 13. campaign</b></summary>
<br>

> **기능 요약**
> 신규 마케팅 캠페인의 개설, 캠페인 정보 및 진행 상태 수정, 파트너 초대, 캠페인 목록 및 캘린더 통합 일정 이벤트를 조회하는 캠페인 코어 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/9138d56a-f634-4f06-8070-192aae5a18ab" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 14. campaign-export</b></summary>
<br>

> **기능 요약**
> 캠페인 PM 소속 관리자가 캠페인의 세부 정보(멤버, 업무, KPI 등)를 CSV 또는 PDF 보고서 형태의 파일로 추출하여 다운로드할 수 있도록 지원하는 데이터 내보내기 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/8e443df6-7400-4b47-83cc-9f40246f9903" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 15. campaign-intro</b></summary>
<br>

> **기능 요약**
> 특정 캠페인의 제휴 모집 상세 소개글과 기본 정보 기획서를 조회하고 업데이트하는 캠페인 소개 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/38bac34c-a37c-4dac-b547-6a9a7017e7c0" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 16. campaign-kpi</b></summary>
<br>

> **기능 요약**
> 캠페인 내 핵심 성과 지표(KPI)의 추가, 수정(목표치/실적치), 삭제, 요약 분석 업데이트 및 공통 프레임워크로부터 KPI를 일괄 가져오는 KPI 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/24c2637a-f8c6-4192-b07d-0e1d8b1ee888" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 17. campaign-kpi-contribution</b></summary>
<br>

> **기능 요약**
> 캠페인 내 참여 부서 또는 조직별 KPI 기여도(목표값 및 실제 달성값)를 추가, 수정, 삭제하고 기여도 기여 분포 리스트를 조회하는 기여도 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/fcc08bfa-8a98-4272-b89f-4cf97c634389" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 18. campaign-member</b></summary>
<br>

> **기능 요약**
> 캠페인 소속 멤버 조회 및 관리, 자사 조직 팀원 추가, 파트너 조직 및 개인 초청장 생성/수락/거절, 그리고 멤버 권한 수정 및 삭제를 관리하는 멤버 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/b33a2681-7bbc-4c0a-b24c-b0a53cb1c044" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 19. dashboard</b></summary>
<br>

> **기능 요약**
> 캠페인 현황 요약, 매출 추이(YoY), 업무 진척도, 활동 피드 및 광고 검수 요청 상태 등 전체 마케팅 성과 정보를 통합 및 개별 조회할 수 있는 대시보드 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/4601fa4a-686b-4dae-8c30-afe99aa30944" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 20. evaluation</b></summary>
<br>

> **기능 요약**
> 캠페인 매칭 평가 프로세스의 기동(start), 최종 평가 완료 데이터의 수집(collect), 그리고 캠페인별 매칭 평가 결과 상세를 조회하는 제휴 매칭 평가 핵심 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/86fdfeba-3038-4c0d-a48c-8d71294b45e0" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 21. goals</b></summary>
<br>

> **기능 요약**
> 기업 제휴 목표(Goal) 정보를 새롭게 추가, 기존 목표의 수정, 삭제 및 개별/목록 조회를 지원하는 제휴 마케팅 목표 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/d3af9f1e-a4f2-487b-989c-47a6aaa99e63" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 22. kpi-template</b></summary>
<br>

> **기능 요약**
> 공통 또는 특정 조직 전용의 성과 지표(KPI) 표준 템플릿의 목록 조회, 생성, 그리고 템플릿 기반 실 적용 객체 생성을 처리하는 KPI 템플릿 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/d915ce4a-549c-407c-bd15-52055b9512a9" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 23. matching-evaluation</b></summary>
<br>

> **기능 요약**
> n8n 자동화 시나리오와 연동하여 캠페인 제휴 조건 및 브랜드 데이터를 기반으로 AI 매칭 평가 분석을 요청하고, 그 결과 데이터를 수집/조회하는 마이크로서비스용 매칭 평가 전용 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/df08ea82-917b-438d-ada0-e9dbe5e56b1e" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 24. milestones</b></summary>
<br>

> **기능 요약**
> 캠페인 내 주요 단계별 일정인 마일스톤(Milestone)의 생성, 수정, 삭제, 상세 조회 및 캠페인별 마일스톤 리스트 조회를 담당하는 핵심 일정 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/29b3a2ca-1475-4e08-bb57-c4c2791ff2b5" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 25. notification</b></summary>
<br>

> **기능 요약**
> SSE(Server-Sent Events)를 활용한 실시간 알림 구독(수신), 알림 수신 내역 조회 및 개별/일괄 확인 처리, 알림 사용자/관리자 수신 정책 설정을 관리하는 실시간 알림 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/fef07435-7c5d-40ce-885b-f4de4fb6a661" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 26. organization kpi</b></summary>
<br>

> **기능 요약**
> 조직(회사) 레벨의 분기/연간 핵심 성과 지표(KPI) 및 상위 KPI 목표를 설정, 수정, 조회 및 진행 상태를 일괄 관리하는 조직 KPI 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/70439d61-714c-4132-a006-e5ee492dae4c" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 27. references</b></summary>
<br>

> **기능 요약**
> 캠페인이나 기획서 작성 시 공동으로 참조하고 활용할 수 있는 다양한 레퍼런스(참고자료)의 생성, 수정, 삭제, 목록 및 상세 조회를 처리하는 공유 아카이브 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/0f569a03-57a0-4fe5-84b8-1fdf6a0d3668" width="100%" controls></video>

</details>

<br>

<details>
<summary><b>🎥 28. user profile</b></summary>
<br>

> **기능 요약**
> 로그인한 사용자 본인의 상세 프로필 조회 및 수정, 프로필 이미지 업로드를 위한 Presigned URL 발급, AI 프로필 이미지 생성 및 이미지 변경 이력을 관리하는 개인 정보 관리 API입니다.

**시연 영상**
<video src="https://github.com/user-attachments/assets/1b67f2df-d734-45ff-9bdb-ad912622a2db" width="100%" controls></video>

</details>


<br>

### 🔹 쿠버네티스(K8s) 무중단 배포 시연 영상
> K8S 환경에서의 Blue/Green 수동 전환시 배포가 중단되지 않음을 검증하는 영상입니다.

> 📺 **[무중단 배포 테스트 영상]**
> Blue/Green
     <img alt="Adobe Express - 백엔드_블루그린_캘로그(1)" src="https://github.com/user-attachments/assets/3c014f2f-2790-4756-9c5c-c9de008b2948" />

---
