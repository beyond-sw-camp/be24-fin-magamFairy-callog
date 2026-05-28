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
## 📃 Swagger API



---

## 🎬 기능 테스트 및 배포 영상

### 🔹 기능 테스트 영상
> 마케팅 제휴 캠페인 생성, 파트너 매칭 Logic, 권한별 일정 관리 등 주요 기능의 정상 동작 시연 영상입니다.

|                  마케팅 캠페인 생성 기능                  | 권한 기반 일정 관리 |        파트너 추천 및 매칭 기능        |
|:-----------------:| :---: |:----------:|
| <img src="이미지 혹은 GIF 주소" width="400" alt="기능1"/> | <img src="이미지 혹은 GIF 주소" width="400" alt="기능2"/> |                |
|                [여기에 영상 링크 삽입 가능]                 | [여기에 영상 링크 삽입 가능] |                |

<br>

### 🔹 쿠버네티스(K8s) 무중단 배포 시연 영상
> K8S 환경에서의 Blue/Green 수동 전환시 배포가 중단되지 않음을 검증하는 영상입니다.

> 📺 **[무중단 배포 테스트 영상]**
> Blue/Green
     <img alt="Adobe Express - 백엔드_블루그린_캘로그(1)" src="https://github.com/user-attachments/assets/3c014f2f-2790-4756-9c5c-c9de008b2948" />

---
