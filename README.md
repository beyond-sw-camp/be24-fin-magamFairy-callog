<div align="center">
  <img src="./docs/callog-logo.png" alt="Callog 로고" width="600" />
</div>

---

## 👥 팀원

<div align="center">

| **이성재** | **최재원** | **김미정** | **범윤준** | **김주형** |
| :---: | :---: | :---: | :---: | :---: |
| <img src="https://github.com/Tahcy-99.png?size=120" width="120" style="border-radius:50%"/> | <img src="https://github.com/Lumisia.png?size=120" width="120" style="border-radius:50%"/> | <img src="https://github.com/mihub02.png?size=120" width="120" style="border-radius:50%"/> | <img src="https://github.com/Yoonjoon13.png?size=120" width="120" style="border-radius:50%"/> | <img src="https://github.com/Joohyeng.png?size=120" width="120" style="border-radius:50%"/> |
| [@Tahcy-99](https://github.com/Tahcy-99) | [@Lumisia](https://github.com/Lumisia) | [@mihub02](https://github.com/mihub02) | [@Yoonjoon13](https://github.com/Yoonjoon13) | [@Joohyeng](https://github.com/Joohyeng) |

</div>

---

## 목차

1. [프로젝트 소개](#프로젝트-소개)
2. [서비스 구조](#서비스-구조)
3. [기술 스택](#기술-스택)
4. [레포지토리 구조](#레포지토리-구조)
5. [기능 테스트 및 시연](#기능-테스트-및-시연)
6. [배포 방식](#배포-방식)
7. [무중단 배포 테스트](#무중단-배포-테스트)

---

## 프로젝트 소개


Callog는 캠페인 기획부터 실행, 성과 관리, 콘텐츠 제작, 파트너 협업까지 하나의 흐름으로 관리할 수 있는 서비스입니다.

사용자는 캠페인별 목표와 KPI를 설정하고, 일정과 업무를 관리하며, 콘텐츠 작성 및 파트너 제안 검토를 진행할 수 있습니다.

프론트엔드와 백엔드의 자세한 실행 방법, 기술 스택, 기능 테스트, 배포 방식은 각 README에서 확인할 수 있습니다.

---

## 서비스 구조

```text
Client
  |
  |-- Frontend: Vue 3 + Vite
  |
Gateway
  |
  |-- Spring Gateway
  |-- Eureka
  |
Backend Services
  |
  |-- App Service
  |-- Matching Evaluation Service
  |-- AI Judge Service
```

---

## 기술 스택

| 영역 | 기술                                                  |
| --- |-----------------------------------------------------|
| Frontend | Vue 3, Vite, Pinia, Vue Router, Tailwind CSS, Axios |
| Backend | Java 17, Spring Boot, Spring Cloud, Gradle          |
| Service Discovery | Eureka                                              |
| Gateway | Spring Cloud Gateway                                |
| Deployment | Docker, Nginx                                       |
| CI/CD | Jenkins, K8s                                        |

---

## 레포지토리 구조

```text
.
├── frontend/              # 프론트엔드 Vue 애플리케이션
├── backend/               # 백엔드 멀티 모듈 Spring 프로젝트
│   ├── app/
│   ├── spring-gw/
│   ├── eureka/
│   ├── matching-evaluation/
│   └── aijudge/
├── docs/                  # 프로젝트 문서
├── cicd/                  # CI/CD 관련 파일
└── README.md              # 전체 프로젝트 안내 문서
```

## 기능 테스트 및 시연

기능 테스트 영상과 시연 자료는 프론트엔드/백엔드 README에서 기능별로 확인할 수 있습니다.

- 프론트 기능 테스트 영상: [frontend/README.md](./frontend/README.md)
- 백엔드 API 테스트 및 시나리오: [backend/README.md](./backend/README.md)
- 전체 기능 테스트 가이드: [docs/callog-feature-test-guide.md](./docs/callog-feature-test-guide.md)

---

## 배포 방식

프론트엔드와 백엔드는 Docker 기반으로 배포합니다.

| 영역 | 배포 방식 |
| --- | --- |
| Frontend | Docker 이미지 빌드 후 Nginx로 정적 파일 서빙 |
| Backend | Spring Boot 애플리케이션을 Docker 이미지로 빌드 후 서비스별 배포 |
| Frontend Strategy | Canary 배포 |
| Backend Strategy | Blue/Green 배포 |



---

## 무중단 배포 테스트

### Frontend Canary

프론트엔드는 Canary 배포 방식을 적용하여 신규 버전을 일부 트래픽에 먼저 배포하고, 정상 동작 확인 후 전체 트래픽으로 점진 확대하는 방식으로 테스트했습니다.

<img alt="Frontend Canary 배포 테스트" src="https://github.com/user-attachments/assets/93ecdef8-bf08-485a-8c43-a1433998de30" />

### Backend Blue/Green

백엔드는 Blue/Green 배포 방식을 적용하여 기존 버전과 신규 버전을 분리해 운영하고, 검증 완료 후 트래픽을 전환하는 방식으로 테스트했습니다.

<img alt="Backend Blue Green 배포 테스트" src="https://github.com/user-attachments/assets/3c014f2f-2790-4756-9c5c-c9de008b2948" />
