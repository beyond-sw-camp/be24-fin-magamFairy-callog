# Callog Frontend

캠페인 운영, 협업, 매칭, AI 검수를 지원하는 Callog 프론트엔드입니다.

## 바로가기

| 구분 | 링크 |
|---|---|
| 서비스 배포 주소 | [https://www.magamfairy.kro.kr/](https://www.magamfairy.kro.kr/) |
| 화면 설계서 | [Figma 화면 설계서](https://www.figma.com/design/1AczfcXlvCL1qyRhlAYAPo/callog?node-id=3-2&t=nuKk7NuGMftcvFai-1) |
| 기능 테스트 영상 | 아래 `기능 테스트 영상` 참고 |
| 공통 README | [../README.md](../README.md) |
| 백엔드 README | `../backend/README.md` 작성 예정 |

## 주요 기능

- 인증 / 계정 관리
- 메인 대시보드, 캘린더, 협업 보드
- 캠페인 생성, 참여자 설정, 팀보드 입력
- 캠페인 KPI, 프레임, 레퍼런스실
- 혜택 제안, 매칭 평가
- AI 자료 검수, 승인 대기, 알림 센터

## 화면 설계서 구성

화면 설계서는 실제 사용 흐름 기준으로 정리했습니다.

<details>
<summary><strong>01 인증 / 계정 관리</strong></summary>

| 화면 | 설명 |
|---|---|
| 로그인 | 사용자가 서비스에 진입하는 화면 |
| 회원가입 | 협력사 매니저 계정을 생성하는 화면 |
| 권한별 계정 생성/관리 | 권한에 따라 생성/관리 가능한 계정을 구분 |
| 비밀번호 변경 | 로그인 계정의 비밀번호를 변경하는 화면 |

</details>

<details>
<summary><strong>02 메인 운영</strong></summary>

| 화면 | 설명 |
|---|---|
| 메인 통합 운영 대시보드 | 캠페인, KPI, 검수, 파트너 현황을 요약 |
| 캘린더 | 월간, 주간, 아젠다, 타임라인, 테이블 보기 제공 |
| 협업 보드 | 팀보드와 개인보드로 업무 진행 현황 확인 |
| 분기목표(KPI) | 본사 권한 전용 KPI 관리 화면 |
| 알림 센터 | 검수 요청, 승인/반려, 업무 변경 알림 확인 |

</details>

<details>
<summary><strong>03 캠페인 운영</strong></summary>

| 화면 | 설명 |
|---|---|
| 캠페인 생성 | 캠페인 기본 정보와 목표를 등록 |
| 캠페인 오버뷰 | 캠페인 진행률, 참여자, 업무, 검수, KPI 현황 확인 |
| 참여자 설정 | 팀원, 협력사, 그룹을 초대하고 관리 |
| 팀보드 입력 | 마일스톤, 업무 파트, 세부 업무를 등록 |
| 캠페인 KPI 설정 | 프레임워크 불러오기와 개별 지표 추가 |
| 캠페인 프레임 | 필수 조건, 금지어, 톤앤매너, 검수 기준 관리 |
| 레퍼런스실 | 이미지, 파일, 링크 자료를 저장하고 공유 |
| 혜택 제안 | 협력사가 매칭에 필요한 혜택 정보를 입력 |
| 매칭 / 혜택 제안함 | 혜택 제안을 캠페인 목표와 비교해 평가 요청 |
| 파트너 평가 | 적합도 점수와 분석 결과 확인 |
| AI 검수 요청 | 자료 업로드 후 AI 검수 진행 |
| 검수탭 / 자료실 | 내 검수 내역과 전체 검수 내역 확인 |
| 상세보기 / 승인 대기 | 검수 결과 상세와 최종 승인 상태 확인 |

</details>

## 기능 테스트 영상

영상은 화면 동작을 설명하는 용도로 간단히 정리했습니다.

```text
대시보드/
알림/
캠페인/
환경설정/
```

| 기능 | 영상 | 설명 |
|---|---|---|
| 프론트엔드 카나리 배포 | 준비 중 | 일부 사용자에게 먼저 배포해 안정성 확인 |
| 대시보드 | `대시보드 전체.gif` | 메인 운영 지표 확인 |
| 알림 센터 | `알림 초반.gif`, `알림 팝업.gif` | 알림 목록과 상세 팝업 확인 |
| AI 자료 검수 | `검수기능.gif` | 검수 요청, 진행 상황, 결과 확인 |
| 환경설정 | `환경설정.gif` | 프로필 및 계정 설정 확인 |

## 프론트엔드 카나리 배포

프론트엔드는 사용자가 가장 먼저 마주하는 화면입니다.
작은 UI 변경도 사용 흐름에 바로 영향을 주기 때문에, 신규 화면을 일부 사용자에게 먼저 노출하는 카나리 배포를 적용했습니다.

이를 통해 화면 오류, 라우팅 문제, API 응답 처리, 브라우저 호환성을 먼저 확인한 뒤 전체 배포로 확장할 수 있습니다.

## 권한별 화면 기준

| 권한 | 주요 화면 | 설명 |
|---|---|---|
| Admin | 사용자 관리 | 본사/협력사 계정 관리 |
| 본사 | 대시보드, KPI, 매칭, 검수 승인 | 운영 지표와 최종 승인 중심 |
| 협력사 | 회원가입, 혜택 제안, 검수 승인 요청 | 캠페인 참여와 제안 중심 |

검수 요청 및 승인/반려 결과는 알림 센터를 통해 전달됩니다.

## 기술 스택

### Core & Framework

![Vue.js](https://img.shields.io/badge/Vue.js-3-4FC08D?style=for-the-badge&logo=vuedotjs&logoColor=white)
![Vite](https://img.shields.io/badge/Vite-8-646CFF?style=for-the-badge&logo=vite&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-ESM-F7DF1E?style=for-the-badge&logo=javascript&logoColor=111111)

### State & Routing

![Pinia](https://img.shields.io/badge/Pinia-3-FFD859?style=for-the-badge&logo=vuedotjs&logoColor=111111)
![Vue Router](https://img.shields.io/badge/Vue_Router-5-42B883?style=for-the-badge&logo=vuedotjs&logoColor=white)

### API & Visualization

![Axios](https://img.shields.io/badge/Axios-1-5A29E4?style=for-the-badge&logo=axios&logoColor=white)
![ApexCharts](https://img.shields.io/badge/ApexCharts-5-00A8E8?style=for-the-badge)
![Editor.js](https://img.shields.io/badge/Editor.js-2-111111?style=for-the-badge)

### Styling & Build

![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-4-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)
![PostCSS](https://img.shields.io/badge/PostCSS-8-DD3A0A?style=for-the-badge&logo=postcss&logoColor=white)
![Prettier](https://img.shields.io/badge/Prettier-3-F7B93E?style=for-the-badge&logo=prettier&logoColor=111111)

## 실행 방법

```bash
npm install
npm run dev
```

개발 서버:

```text
http://localhost:5173
```

## 빌드

```bash
npm run build
```

## 주요 폴더

```text
frontend/
├─ plugins/
├─ public/
├─ src/
│  ├─ api/
│  ├─ components/
│  ├─ layout/
│  ├─ router/
│  ├─ stores/
│  └─ views/
└─ vite.config.js
```

## 주요 라우트

| Route | 화면 |
|---|---|
| `/user/login` | 로그인 |
| `/user/signup` | 회원가입 |
| `/dashboard` | 메인 대시보드 |
| `/calendar` | 캘린더 |
| `/team-board` | 협업 보드 |
| `/organization-kpis` | 분기목표(KPI) |
| `/notifications` | 알림 센터 |
| `/campaigns/:campaignId` | 캠페인 상세 |
| `/campaigns/:campaignId/proposal/new` | 혜택 제안 |
| `/frames` | 캠페인 프레임 |
| `/references` | 레퍼런스실 |
| `/matching` | 매칭 |
| `/usercreate` | 사용자 관리 |

## 테스트 계정

| 권한 | ID | Password |
|---|---|---|
| 본사 GM | `hqgm@callog.com` | `Qwer1234!` |
| 협력사 GM | `partner@callog.com` | `Qwer1234!` |
