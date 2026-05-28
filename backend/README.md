## 🛠 기술 스택 (Tech Stack)

### Backend
* **Framework:** Java 17 / Spring Boot 3.x
* **Security:** Spring Security, JWT (Json Web Token)
* **Database:** MySQL, Spring Data JPA, MongoDB
* **AI Integration:** *[사용한 AI 연동 기술 기술, 예: OpenAI API / n8n / Python Fast-API 등]*

### DevOps & Infrastructure
* **Containerization:** Docker, Docker Compose
* **Orchestration:** Kubernetes (K8s)
* **CI/CD:** Jenkins, *[사용한 형상관리/웹훅 툴]*
* **OS/Environment:** Linux (Ubuntu)

---

## 🏗 시스템 아키텍처 (Architecture)
> 여기에 아키텍처 구성도 이미지(인프라 구성 및 CI/CD 흐름)를 첨부하면 가독성이 극대화됩니다.

* **CI/CD 흐름:** 코드 Push → Jenkins 웹훅 감지 → 빌드 및 Docker 이미지 패키징 → Kubernetes 클러스터 배포 (Rolling Update를 통한 무중단 배포)

---

## 🔥 주요 기능 및 담당 역할
* **Spring Security & JWT 기반 권한 세분화**
  * 브랜드사 및 파트너사 간의 명확한 권한 관계를 정의하고 웹토큰 핸들링을 통해 보안성 확보
* **AI 기반 파트너 추천 및 제휴 평가 로직**
  * 브랜드 자산 데이터를 파싱·비교하여 최적의 파트너 혜택을 매칭하는 백엔드 코어 알고리즘 구현
* **제휴 캠페인 협업 및 일정 관리 인프라**
  * 마케팅 파트너십 진행 프로세스 전반을 트래킹할 수 있는 API 설계

---

## ⚡ 성능 개선 (Performance Improvements)

> 💡 **Tip:** 프로젝트 마무리 단계에서 진행한 최적화 경험을 수치와 함께 기록하면 신뢰도가 대폭 상승합니다. 아래 예시를 참고하여 실제 진행한 내용으로 채워보세요!

### 1. [예시: 데이터 조회 쿼리 최적화]
* **문제 상황:** 파트너 추천 및 매칭 로직에서 다중 조인(Join) 발생 시, 데이터양이 늘어남에 따라 응답 속도가 저하되는 현상 발견.
* **개선 방안:** JPA N+1 문제 해결을 위해 `Fetch Join`을 적용하고, 자주 조회되는 파트너 평가 항목에 대해 `DB Index`를 추가.
* **결과:** 평균 API 응답 시간 **XX% 단축** (00ms -> 00ms)

### 2. [예시: 커넥션 풀 및 배치 처리 최적화]
* **문제 상황:** 대용량 브랜드 자산 데이터 일괄 파싱 및 저장 시 DB 커넥션 병목 현상 발생.
* **개선 방안:** HikariCP 옵션 튜닝 및 `Spring Batch` (혹은 Bulk Insert) 방식을 도입하여 트랜잭션 단위 분리.
* **결과:** 배치 처리 속도 **XX% 향상**, 시스템 안정성 확보.

---

## 🎬 기능 테스트 및 배포 영상

### 🔹 기능 테스트 영상
> 마케팅 제휴 캠페인 생성, 파트너 매칭 Logic, 권한별 일정 관리 등 주요 기능의 정상 동작 시연 영상입니다.

| 파트너 추천 및 매칭 기능 | 권한 기반 일정 관리 |
| :---: | :---: |
| <img src="이미지 혹은 GIF 주소" width="400" alt="기능1"/> | <img src="이미지 혹은 GIF 주소" width="400" alt="기능2"/> |
| [여기에 영상 링크 삽입 가능] | [여기에 영상 링크 삽입 가능] |

<br>

### 🔹 쿠버네티스(K8s) 무중단 배포 시연 영상
> Jenkins 파이프라인을 통한 빌드 자동화 및 Kubernetes 환경에서의 Rolling Update 무중단 배포 검증 영상입니다.
> (서비스 중단 없이 신규 버전의 백엔드 애플리케이션이 파드(Pod)에 교체 적용되는 과정 포함)

> 📺 **[무중단 배포 테스트 영상 보러가기 (클릭 시 링크 이동)]**
> *(혹은 여기에 Jenkins 빌드 및 K8s 파드 교체 화면 GIF 첨부)*

---