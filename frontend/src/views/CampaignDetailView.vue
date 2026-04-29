<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import { GetCampaignDetails } from '@/api/campaigns'

const route = useRoute()
const store = usePlannerStore()

const activeTab = ref('캠페인 오버뷰')
const currentBoardView = ref('swimlane')
const metadataEditing = ref(false)
const selectedMemberIds = ref([])

const metadataDraft = ref({
  name: '',
  startDate: '',
  endDate: '',
  summary: '',
  partnersText: '',
})

const tabs = ["캠페인 오버뷰", "팀 보드 보기", "레퍼런스 탭", "참여자 설정", "캠페인 성과/KPI"];

const handleTabClick = async (tabName) => {
  activeTab.value = tabName;
  try {
    // tabName에 "참여자 설정" 같은 값이 들어와서 호출됨
    const data = await GetCampaignDetails(tabName);
    console.log("받아온 데이터:", data);
  } catch (error) {
    console.error("에러 발생:", error);
  }
};

const activeCampaign = computed(() => {
  const campaignId = String(route.params.campaignId ?? '')
  const routeCampaign = store.campaigns.find((campaign) => campaign.id === campaignId)

  return routeCampaign ?? store.activeCampaign
})

const campaignStatusLabel = computed(() => {
  const labels = {
    draft: '기획 중',
    review: '검토 중',
    in_review: '검토 중',
    live: '진행중',
    partner_done: '파트너 완료',
    paused: '일시 중지',
    completed: '완료',
  }

  return labels[activeCampaign.value?.status] ?? activeCampaign.value?.status ?? '진행중'
})

const partnerNames = computed(() => {
  if (activeCampaign.value?.partners?.length) {
    return activeCampaign.value.partners
  }

  return ['디자인 스튜디오 A', '미디어 랩 B', 'PR 에이전시 C']
})

const scheduleItems = ref([
  {
    id: 'schedule-kickoff',
    title: '캠페인 킥오프 미팅',
    owner: '본사',
    date: '2024.05.15',
    status: '확정',
  },
  {
    id: 'schedule-draft',
    title: '에셋 초안 제출 마감',
    owner: '디자인협력사',
    date: '2024.05.25',
    status: '진행 중',
  },
  {
    id: 'schedule-teaser',
    title: '1차 티저 영상 오픈',
    owner: '본사',
    date: '2024.06.01',
    status: '예정',
  },
  {
    id: 'schedule-live',
    title: '메인 프로모션 라이브',
    owner: '운영대행사',
    date: '2024.07.15',
    status: '예정',
  },
])

const statusColumns = [
  { id: 'draft', label: '작성중', sub: 'Draft' },
  { id: 'review', label: '검수요청', sub: 'Review' },
  { id: 'approved', label: '승인완료', sub: 'Approved' },
  { id: 'live', label: '라이브', sub: 'Live' },
]

const dateColumns = [
  { id: '2024-06-10', label: '~ 06.10' },
  { id: '2024-06-20', label: '~ 06.20' },
  { id: '2024-07-10', label: '~ 07.10' },
  { id: '2024-07-30', label: '~ 07.30' },
]

const teams = [
  {
    id: 'hq',
    name: '본사 마케팅팀',
    role: '기획 및 총괄',
    progress: 75,
    color: 'primary',
  },
  {
    id: 'design',
    name: '디자인 스튜디오 A',
    role: '에셋 제작',
    progress: 85,
    color: 'blue',
  },
  {
    id: 'media',
    name: '미디어 랩 B',
    role: '퍼포먼스 마케팅',
    progress: 60,
    color: 'green',
  },
  {
    id: 'pr',
    name: 'PR 에이전시 C',
    role: '보도자료 및 인플루언서',
    progress: 92,
    color: 'pink',
  },
]

const teamTasks = [
  {
    id: 'task-guide',
    teamId: 'hq',
    status: 'review',
    dateColumn: '2024-06-10',
    title: '캠페인 전체 가이드라인 V2',
    type: '문서',
    dueDate: 'D-2',
    review: '검수요청',
    ownerInitial: '김',
  },
  {
    id: 'task-storyboard',
    teamId: 'hq',
    status: 'approved',
    dateColumn: '2024-06-20',
    title: '티저 영상 스토리보드',
    type: '영상',
    dueDate: '완료',
    review: '승인완료',
    ownerInitial: '이',
  },
  {
    id: 'task-story-banner',
    teamId: 'design',
    status: 'draft',
    dateColumn: '2024-06-10',
    title: '인스타그램 스토리 배너 (3종)',
    type: '이미지',
    dueDate: '06.10',
    review: '작성중',
    ownerInitial: 'A',
  },
  {
    id: 'task-hero',
    teamId: 'design',
    status: 'draft',
    dateColumn: '2024-06-20',
    title: '메인 랜딩페이지 히어로 이미지',
    type: '이미지',
    dueDate: '지연됨',
    review: '수정요청',
    ownerInitial: 'A',
    urgent: true,
  },
  {
    id: 'task-landing',
    teamId: 'design',
    status: 'live',
    dateColumn: '2024-07-10',
    title: '사전예약 랜딩페이지 디자인',
    type: '웹',
    dueDate: '라이브',
    review: '라이브',
    ownerInitial: 'A',
  },
  {
    id: 'task-media',
    teamId: 'media',
    status: 'review',
    dateColumn: '2024-07-10',
    title: '전환 캠페인 소재 세트',
    type: '광고',
    dueDate: 'D-5',
    review: '검수요청',
    ownerInitial: 'B',
  },
  {
    id: 'task-pr',
    teamId: 'pr',
    status: 'approved',
    dateColumn: '2024-07-30',
    title: '인플루언서 브리프 문서',
    type: '문서',
    dueDate: '완료',
    review: '승인완료',
    ownerInitial: 'C',
  },
]

const references = [
  {
    id: 'ref-crawl',
    title: '나이키 썸머 캠페인 랜딩페이지 분석',
    source: '크롤링 데이터',
    owner: '김본사',
    date: '2024.05.10',
    tags: ['UI', '여름'],
    tone: 'blue',
    icon: 'IMG',
  },
  {
    id: 'ref-ai',
    title: '해변 테마 프로모션 카피라이팅 아이디어',
    source: 'AI 생성',
    owner: '디자인 스튜디오 A',
    date: '2024.05.12',
    tags: ['카피', '아이디어'],
    tone: 'primary',
    icon: 'AI',
  },
  {
    id: 'ref-report',
    title: 'MZ세대 여름 휴가 트렌드 리포트',
    source: '직접 업로드',
    owner: 'PR 에이전시 C',
    date: '2024.05.15',
    tags: ['리서치', '타겟분석'],
    tone: 'neutral',
    icon: 'PDF',
  },
]

const participantCandidates = [
  { id: 'hq-kim', name: '김본사', team: '글로벌 본사', role: '본사 관리자' },
  { id: 'design-park', name: '박디자인', team: '디자인 스튜디오 A', role: '협력사 매니저' },
  { id: 'media-lee', name: '이마켓', team: '미디어 랩 B', role: '협력사 팀원' },
]

const campaignParticipants = ref([
  {
    id: 'hq-kim',
    name: '김본사',
    email: 'kim.hq@callog.com',
    team: '글로벌 본사',
    role: '본사 관리자',
    access: '읽기/수정',
    addedAt: '2024.04.10',
  },
  {
    id: 'design-park',
    name: '박디자인',
    email: 'park@studio-a.com',
    team: '디자인 스튜디오 A',
    role: '협력사 매니저',
    access: '당사 업무 수정',
    addedAt: '2024.04.12',
  },
  {
    id: 'media-lee',
    name: '이마켓',
    email: 'lee@media-b.com',
    team: '미디어 랩 B',
    role: '협력사 팀원',
    access: '당사 업무 조회',
    addedAt: '2024.04.15',
  },
])

const kpiRows = ref([
  {
    id: 'kpi-impression',
    name: '메인 배너 노출',
    category: '노출도',
    target: 1000000,
    actual: 1245000,
    unit: 'Views',
    source: '검증된 수치 직접 입력',
    owner: '미디어 랩 B',
    inputDate: '2024.08.31',
    memo: '목표 대비 초과 달성',
    nextAction: '고성과 소재 보관',
  },
  {
    id: 'kpi-conversion-rate',
    name: '랜딩페이지 전환율',
    category: '전환',
    target: 5,
    actual: 4.2,
    unit: '%',
    source: '랜딩 로그 수동 입력',
    owner: '글로벌 본사',
    inputDate: '2024.08.31',
    memo: '이탈률 개선 필요',
    nextAction: '상단 CTA 재설계',
  },
  {
    id: 'kpi-click',
    name: 'SNS 스토리 클릭',
    category: '참여도',
    target: 40000,
    actual: 40500,
    unit: 'Clicks',
    source: '플랫폼 리포트 입력',
    owner: '디자인 스튜디오 A',
    inputDate: '2024.08.31',
    memo: '목표 달성',
    nextAction: '유사 소재 확장',
  },
  {
    id: 'kpi-roas',
    name: '리타겟팅 광고 ROAS',
    category: '전환',
    target: 350,
    actual: 0,
    unit: '%',
    source: '측정 대기',
    owner: '미디어 랩 B',
    inputDate: '-',
    memo: '측정 전',
    nextAction: '종료 후 수치 입력',
  },
])

const activityItems = [
  {
    id: 'act-feedback',
    tone: 'primary',
    icon: '말',
    actor: '본사 김매니저',
    text: 'SNS 배너 시안 1차에 피드백을 남겼습니다.',
    quote: '폰트 크기를 조금 더 키우고, 버튼 색상을 브랜드 컬러로 변경 요청드립니다.',
    time: '10분 전',
  },
  {
    id: 'act-status',
    tone: 'success',
    icon: '완',
    actor: '디자인 스튜디오 A',
    text: '업무 상태를 승인완료로 변경했습니다.',
    time: '2시간 전',
  },
  {
    id: 'act-upload',
    tone: 'info',
    icon: '업',
    actor: '미디어 랩 B',
    text: '새로운 레퍼런스 타사 여름 프로모션 분석.pdf를 업로드했습니다.',
    time: '어제 오후 3:45',
  },
]

const overviewStats = computed(() => [
  { label: '전체 에셋', value: 124, unit: '건', tone: 'info' },
  { label: '승인 완료', value: 82, unit: '건', tone: 'success' },
  { label: '검수 대기', value: 15, unit: '건', tone: 'primary', badge: '검수요청' },
  { label: '지연 업무', value: 3, unit: '건', tone: 'warning', badge: '주의' },
])

const avgProgress = computed(() =>
  Math.round(teams.reduce((sum, team) => sum + team.progress, 0) / teams.length),
)

const circumference = 251.2
const progressOffset = computed(() => circumference - (circumference * avgProgress.value) / 100)

const esgScore = computed(() => 94)

const kpiSummary = computed(() => [
  { label: '목표 달성률', value: '105%', sub: '초과 달성 중', tone: 'success' },
  { label: '총 노출 수 (Views)', value: '1.2M', sub: '목표: 1M', tone: 'info' },
  { label: '총 클릭 수 (Clicks)', value: '45.2K', sub: '목표: 40K', tone: 'primary' },
  { label: '적시성 준수율 (협력사)', value: '92%', sub: '일부 지연 발생', tone: 'warning' },
])

function getTasksByStatus(teamId, statusId) {
  return teamTasks.filter((task) => task.teamId === teamId && task.status === statusId)
}

function getTasksByDate(teamId, dateColumnId) {
  return teamTasks.filter((task) => task.teamId === teamId && task.dateColumn === dateColumnId)
}

function getAchievement(row) {
  if (!row.target) {
    return 0
  }

  return Math.round((Number(row.actual) / Number(row.target)) * 100)
}

function getKpiStatus(row) {
  if (!row.actual) {
    return '측정 전'
  }

  if (getAchievement(row) > 100) {
    return '초과달성'
  }

  if (getAchievement(row) >= 100) {
    return '달성'
  }

  return '미달성'
}

function getKpiTone(row) {
  const achievement = getAchievement(row)

  if (!row.actual) return 'muted'
  if (achievement > 100) return 'success'
  if (achievement >= 100) return 'info'
  return 'warning'
}

function syncMetadataDraft() {
  metadataDraft.value = {
    name: activeCampaign.value?.name ?? '2024 글로벌 썸머 프로모션 캠페인',
    startDate: activeCampaign.value?.startDate ?? '2024-06-01',
    endDate: activeCampaign.value?.endDate ?? '2024-08-31',
    summary:
      activeCampaign.value?.purpose ??
      '여름 시즌을 맞이하여 북미 및 아시아 시장을 타겟으로 한 대규모 할인 프로모션 및 신제품 런칭 캠페인.',
    partnersText: partnerNames.value.join(', '),
  }
}

function saveMetadata() {
  if (!activeCampaign.value?.id) {
    return
  }

  store.updateCampaign(activeCampaign.value.id, {
    ...activeCampaign.value,
    name: metadataDraft.value.name,
    startDate: metadataDraft.value.startDate,
    endDate: metadataDraft.value.endDate,
    purpose: metadataDraft.value.summary,
    partners: metadataDraft.value.partnersText
      .split(',')
      .map((partner) => partner.trim())
      .filter(Boolean),
  })

  metadataEditing.value = false
}

function addScheduleItem() {
  scheduleItems.value = [
    ...scheduleItems.value,
    {
      id: `schedule-${Date.now()}`,
      title: '새 캠페인 일정',
      owner: '본사',
      date: '2024.06.01',
      status: '예정',
    },
  ]
}

function removeScheduleItem(scheduleId) {
  scheduleItems.value = scheduleItems.value.filter((item) => item.id !== scheduleId)
}

function addSelectedParticipants() {
  const existingIds = new Set(campaignParticipants.value.map((participant) => participant.id))
  const additions = participantCandidates
    .filter((candidate) => selectedMemberIds.value.includes(candidate.id) && !existingIds.has(candidate.id))
    .map((candidate) => ({
      ...candidate,
      email: `${candidate.id}@callog.com`,
      access: candidate.team === '글로벌 본사' ? '읽기/수정' : '당사 업무 수정',
      addedAt: '2024.05.18',
    }))

  campaignParticipants.value = [...campaignParticipants.value, ...additions]
  selectedMemberIds.value = []
}

watch(
  () => route.params.campaignId,
  (campaignId) => {
    if (campaignId) {
      store.setActiveCampaign(String(campaignId))
    }
  },
  { immediate: true },
)

watch(activeCampaign, syncMetadataDraft, { immediate: true })
</script>

<template>
  <section class="campaign-detail">
    <div class="campaign-sticky-bar">
      <header class="campaign-hero" aria-label="캠페인 메인 페이지 헤더">
        <div class="campaign-hero__copy">
          <div class="campaign-hero__title">
            <h1>{{ activeCampaign?.name ?? '2024 글로벌 썸머 프로모션 캠페인' }}</h1>
            <span class="status-chip status-chip--primary">
              <i aria-hidden="true"></i>
              {{ campaignStatusLabel }}
            </span>
          </div>
          <div class="campaign-hero__meta" aria-label="캠페인 메타데이터 요약">
            <span>{{ activeCampaign?.period ?? '2024.06.01 - 2024.08.31' }}</span>
            <span>본사 관리자 (수정 가능)</span>
          </div>
        </div>

        <div class="campaign-hero__actions">
          <button type="button" class="btn btn--secondary">내보내기</button>
          <button type="button" class="btn btn--primary" @click="activeTab = 'metadata'; metadataEditing = true">
            캠페인 편집
          </button>
        </div>
      </header>

      <nav class="campaign-tabs" aria-label="캠페인 상세 탭">
        <button
          v-for="tab in tabs"
          :key="tab"
          type="button"
          class="campaign-tabs__button"
          :class="{ active: activeTab === tab }"
          @click="handleTabClick(tab)"
        >
          {{ tab }}
        </button>
      </nav>
    </div>

    <section v-if="activeTab === 'metadata'" class="tab-surface">
      <div class="metadata-layout">
        <div class="stack">
          <article class="panel">
            <div class="panel__header">
              <div>
                <span class="requirement-badge">CAMPAIGN_001</span>
                <h2>기본 정보</h2>
              </div>
              <span class="permission-badge">본사 관리자만 수정</span>
            </div>

            <div class="form-stack">
              <label>
                <span>캠페인 제목</span>
                <input v-model="metadataDraft.name" :disabled="!metadataEditing" type="text" />
              </label>

              <div class="form-grid">
                <label>
                  <span>시작일</span>
                  <input v-model="metadataDraft.startDate" :disabled="!metadataEditing" type="text" />
                </label>
                <label>
                  <span>종료일</span>
                  <input v-model="metadataDraft.endDate" :disabled="!metadataEditing" type="text" />
                </label>
              </div>

              <label>
                <span>캠페인 개요</span>
                <textarea v-model="metadataDraft.summary" :disabled="!metadataEditing" rows="4" />
              </label>
            </div>
          </article>

          <article class="panel">
            <div class="panel__header">
              <div>
                <span class="requirement-badge">일정 추가/삭제/수정</span>
                <h2>주요 일정 관리</h2>
              </div>
              <button type="button" class="btn btn--ghost" @click="addScheduleItem">일정 추가</button>
            </div>

            <div class="data-table data-table--schedule">
              <div class="data-table__head">
                <span>마일스톤</span>
                <span>날짜</span>
                <span>담당</span>
                <span>상태</span>
                <span></span>
              </div>
              <div v-for="item in scheduleItems" :key="item.id" class="data-table__row">
                <strong>{{ item.title }}</strong>
                <span>{{ item.date }}</span>
                <span class="type-badge">{{ item.owner }}</span>
                <span class="status-pill status-pill--info">{{ item.status }}</span>
                <button type="button" class="table-action" @click="removeScheduleItem(item.id)">삭제</button>
              </div>
            </div>
          </article>
        </div>

        <aside class="stack">
          <article class="panel">
            <div class="panel__header">
              <div>
                <span class="requirement-badge">협력사 목록</span>
                <h2>참여 협력사</h2>
              </div>
            </div>

            <div class="partner-list">
              <div v-for="(partner, index) in partnerNames" :key="partner" class="partner-item">
                <span class="partner-item__avatar">{{ partner.slice(0, 1) }}</span>
                <div>
                  <strong>{{ partner }}</strong>
                  <small>{{ ['크리에이티브 제작', '퍼포먼스 마케팅', '보도자료 및 인플루언서'][index] ?? '협력 업무' }}</small>
                </div>
              </div>
            </div>

            <button type="button" class="dashed-button">협력사 추가</button>
          </article>

          <article class="panel">
            <div class="panel__header">
              <div>
                <span class="requirement-badge">권한 정보</span>
                <h2>시스템 정보</h2>
              </div>
            </div>

            <dl class="meta-list">
              <div>
                <dt>생성일자</dt>
                <dd>2024.04.10 14:32</dd>
              </div>
              <div>
                <dt>생성자</dt>
                <dd>김본사 (마케팅팀)</dd>
              </div>
              <div>
                <dt>최근 수정</dt>
                <dd>2024.05.18 09:15</dd>
              </div>
              <div>
                <dt>접근 권한</dt>
                <dd>전체 읽기, 본사 수정</dd>
              </div>
            </dl>
          </article>

          <div class="metadata-actions">
            <button type="button" class="btn btn--secondary" @click="metadataEditing = !metadataEditing">
              {{ metadataEditing ? '수정 취소' : '메타데이터 수정' }}
            </button>
            <button type="button" class="btn btn--primary" @click="saveMetadata">저장</button>
          </div>
        </aside>
      </div>
    </section>

    <section v-else-if="activeTab === '캠페인 오버뷰'" class="tab-surface">
      <div class="metric-grid">
        <article v-for="stat in overviewStats" :key="stat.label" class="metric-card" :class="`tone-${stat.tone}`">
          <span class="metric-card__icon">{{ stat.label.slice(0, 1) }}</span>
          <div>
            <p>{{ stat.label }}</p>
            <strong>{{ stat.value }}<small>{{ stat.unit }}</small></strong>
            <em v-if="stat.badge">{{ stat.badge }}</em>
          </div>
        </article>
      </div>

      <div class="overview-layout">
        <div class="stack">
          <article class="panel progress-panel">
            <div class="progress-ring">
              <svg viewBox="0 0 100 100" aria-hidden="true">
                <circle cx="50" cy="50" r="40" class="progress-ring__track" />
                <circle
                  cx="50"
                  cy="50"
                  r="40"
                  class="progress-ring__fill"
                  :stroke-dasharray="circumference"
                  :stroke-dashoffset="progressOffset"
                />
              </svg>
              <div>
                <strong>{{ avgProgress }}%</strong>
                <span>전체 진척도</span>
              </div>
            </div>

            <div class="progress-panel__list">
              <h2>협력사별 진행 상황</h2>
              <div v-for="team in teams.slice(1)" :key="team.id" class="progress-row">
                <div>
                  <span :class="`dot dot--${team.color}`"></span>
                  <strong>{{ team.name }}</strong>
                  <em>{{ team.progress }}%</em>
                </div>
                <div class="progress-track"><i :class="`fill-${team.color}`" :style="{ width: `${team.progress}%` }"></i></div>
              </div>
            </div>
          </article>

          <article class="panel performance-panel">
            <div class="panel__header">
              <h2>최근 성과 요약</h2>
              <button type="button" class="link-button" @click="handleTabClick('캠페인 성과/KPI')">상세보기</button>
            </div>
            <div class="performance-grid">
              <div>
                <span>누적 노출 수</span>
                <strong>1,245,000</strong>
                <small>목표대비 112%</small>
              </div>
              <div>
                <span>평균 클릭률(CTR)</span>
                <strong>3.24%</strong>
                <small>목표대비 105%</small>
              </div>
              <div>
                <span>전환 건수</span>
                <strong>8,420</strong>
                <small class="warning">목표대비 94%</small>
              </div>
            </div>
          </article>
        </div>

        <aside class="stack">
          <article class="panel esg-panel">
            <h2>ESG 컴플라이언스 점수</h2>
            <div class="esg-score">
              <strong>{{ esgScore }}</strong>
              <span>/ 100</span>
            </div>
            <p>우수 등급 유지중 (전월 대비 +2)</p>
            <div class="progress-track"><i class="fill-green" style="width: 88%"></i></div>
            <small>친환경 에셋 비율 88%</small>
          </article>

          <article class="panel activity-panel">
            <div class="panel__header">
              <h2>최근 활동</h2>
            </div>
            <div class="activity-list">
              <article v-for="activity in activityItems" :key="activity.id" class="activity-item">
                <span class="activity-item__icon" :class="`tone-${activity.tone}`">{{ activity.icon }}</span>
                <div>
                  <p><strong>{{ activity.actor }}</strong>님이 {{ activity.text }}</p>
                  <blockquote v-if="activity.quote">{{ activity.quote }}</blockquote>
                  <time>{{ activity.time }}</time>
                </div>
              </article>
            </div>
          </article>
        </aside>
      </div>
    </section>

    <section v-else-if="activeTab === '팀 보드 보기'" class="tab-surface">
      <div class="board-toolbar">
        <div class="segmented-control">
          <button type="button" :class="{ active: currentBoardView === 'swimlane' }" @click="currentBoardView = 'swimlane'">
            스윔레인
          </button>
          <button type="button" :class="{ active: currentBoardView === 'timeline' }" @click="currentBoardView = 'timeline'">
            칸반보드
          </button>
        </div>
        <div class="board-toolbar__actions">
          <input type="search" placeholder="업무 검색..." />
          <button type="button" class="btn btn--primary">업무 추가</button>
        </div>
      </div>

      <div class="board-grid">
        <div class="board-grid__head">
          <span>참여사</span>
          <span v-for="column in currentBoardView === 'swimlane' ? statusColumns : dateColumns" :key="column.id">
            {{ column.label }} <small v-if="column.sub">({{ column.sub }})</small>
          </span>
        </div>

        <div v-for="team in teams" :key="team.id" class="board-grid__row">
          <div class="team-cell">
            <strong>{{ team.name }}</strong>
            <span>{{ team.role }}</span>
          </div>

          <div
            v-for="column in currentBoardView === 'swimlane' ? statusColumns : dateColumns"
            :key="column.id"
            class="board-cell"
          >
            <article
              v-for="task in currentBoardView === 'swimlane'
                ? getTasksByStatus(team.id, column.id)
                : getTasksByDate(team.id, column.id)"
              :key="task.id"
              class="task-card"
              :class="{ 'task-card--urgent': task.urgent }"
            >
              <div class="task-card__top">
                <span class="status-pill" :class="task.urgent ? 'status-pill--warning' : 'status-pill--info'">
                  {{ task.review }}
                </span>
                <small>{{ task.type }}</small>
              </div>
              <strong>{{ task.title }}</strong>
              <div class="task-card__foot">
                <span :class="{ warning: task.urgent }">{{ task.dueDate }}</span>
                <em>{{ task.ownerInitial }}</em>
              </div>
            </article>
          </div>
        </div>
      </div>
    </section>

    <section v-else-if="activeTab === '레퍼런스 탭'" class="tab-surface">
      <div class="reference-toolbar">
        <div class="segmented-control segmented-control--icon">
          <button type="button" class="active">그리드</button>
          <button type="button">목록</button>
        </div>
        <div>
          <button type="button" class="btn btn--secondary">AI 생성</button>
          <button type="button" class="btn btn--secondary">URL 크롤링</button>
          <button type="button" class="btn btn--primary">파일 업로드</button>
        </div>
      </div>

      <div class="reference-grid">
        <article v-for="reference in references" :key="reference.id" class="reference-card">
          <div class="reference-card__thumb" :class="`tone-${reference.tone}`">
            <strong>{{ reference.icon }}</strong>
          </div>
          <div class="reference-card__body">
            <div>
              <span class="status-pill status-pill--info">{{ reference.source }}</span>
              <small>{{ reference.date }}</small>
            </div>
            <h3>{{ reference.title }}</h3>
            <div class="tag-row">
              <span v-for="tag in reference.tags" :key="tag">#{{ tag }}</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeTab === '참여자 설정'" class="tab-surface">
      <article class="panel">
        <div class="panel__header">
          <div>
            <span class="requirement-badge">CAMPAIGN_005</span>
            <h2>캠페인 참여자 관리</h2>
          </div>
          <button type="button" class="btn btn--primary" @click="addSelectedParticipants">참가자 추가</button>
        </div>

        <div class="candidate-list">
          <label v-for="candidate in participantCandidates" :key="candidate.id">
            <input v-model="selectedMemberIds" type="checkbox" :value="candidate.id" />
            <span>{{ candidate.name }}</span>
            <small>{{ candidate.team }} · {{ candidate.role }}</small>
          </label>
        </div>

        <div class="data-table data-table--participants">
          <div class="data-table__head">
            <span>이름 / 이메일</span>
            <span>소속 회사</span>
            <span>역할 (권한)</span>
            <span>추가된 날짜</span>
            <span>관리</span>
          </div>
          <div v-for="participant in campaignParticipants" :key="participant.id" class="data-table__row">
            <div class="person-cell">
              <span>{{ participant.name.slice(0, 1) }}</span>
              <div>
                <strong>{{ participant.name }}</strong>
                <small>{{ participant.email }}</small>
              </div>
            </div>
            <span>{{ participant.team }}</span>
            <span class="status-pill status-pill--info">{{ participant.role }}</span>
            <span>{{ participant.addedAt }}</span>
            <button type="button" class="table-action">관리</button>
          </div>
        </div>
      </article>

      <p class="info-callout">
        참가자로 추가된 인원은 기본적으로 캠페인 조회 권한을 가집니다. 본사 관리자는 수정 권한을 가지며,
        협력사는 자신에게 할당된 업무 및 직접 추가한 레퍼런스만 수정할 수 있습니다.
      </p>
    </section>

    <section v-else-if="activeTab === '캠페인 성과/KPI'" class="tab-surface">
      <div class="metric-grid">
        <article v-for="item in kpiSummary" :key="item.label" class="kpi-card" :class="`tone-${item.tone}`">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.sub }}</small>
        </article>
      </div>

      <article class="panel kpi-panel">
        <div class="panel__header">
          <div>
            <span class="requirement-badge">CAMPAIGN_006</span>
            <h2>세부 KPI 목록</h2>
          </div>
          <div>
            <button type="button" class="btn btn--secondary">프레임워크 불러오기</button>
            <button type="button" class="btn btn--primary">지표 추가</button>
          </div>
        </div>

        <div class="data-table data-table--kpi">
          <div class="data-table__head">
            <span>KPI 항목</span>
            <span>분류</span>
            <span>목표값</span>
            <span>실제값</span>
            <span>단위</span>
            <span>달성률</span>
            <span>상태</span>
            <span>담당자</span>
            <span>동작</span>
          </div>
          <div v-for="row in kpiRows" :key="row.id" class="data-table__row">
            <strong>{{ row.name }}</strong>
            <span class="type-badge">{{ row.category }}</span>
            <span class="number-cell">{{ row.target.toLocaleString() }}</span>
            <span class="number-cell">{{ row.actual ? row.actual.toLocaleString() : '-' }}</span>
            <span>{{ row.unit }}</span>
            <div class="achievement-cell">
              <div class="progress-track">
                <i :class="`fill-${getKpiTone(row)}`" :style="{ width: `${Math.min(getAchievement(row), 100)}%` }"></i>
              </div>
              <span>{{ getAchievement(row) }}%</span>
            </div>
            <span class="status-pill" :class="`status-pill--${getKpiTone(row)}`">{{ getKpiStatus(row) }}</span>
            <span>{{ row.owner }}</span>
            <button type="button" class="table-action">메모</button>
          </div>
        </div>

        <div class="kpi-note-box">
          <h3>성과 분석 및 개선 액션</h3>
          <textarea
            rows="3"
            value="노출 목표는 초과 달성하였으나, 랜딩페이지 내 이탈률이 높아 전환율이 목표에 미달함. 다음 캠페인에서는 랜딩페이지 최상단에 CTA 버튼을 명확히 배치하고 로딩 속도를 최적화하는 액션 필요."
          />
          <button type="button" class="btn btn--secondary">저장하기</button>
        </div>
      </article>
    </section>
  </section>
</template>

<style scoped>
.campaign-detail {
  --campaign-primary-surface: var(--color-primary-100);
  --campaign-primary-soft: var(--color-primary-50);
  --campaign-primary-text: var(--color-primary-700);
  --campaign-success-surface: var(--color-success-light);
  --campaign-success-text: var(--color-success-dark);
  --campaign-warning-surface: var(--color-warning-light);
  --campaign-warning-text: var(--color-warning-dark);
  --campaign-info-surface: var(--color-info-light);
  --campaign-info-text: var(--color-info-dark);
  --campaign-pink: #db2777;
  --campaign-muted-fill: var(--color-gray-300);
  --campaign-elevated-tint: var(--campaign-primary-soft);
  --campaign-table-row-hover: color-mix(in srgb, var(--panel-muted) 58%, var(--panel-color));
  display: flex;
  width: 100%;
  max-width: var(--content-max-width);
  flex-direction: column;
  gap: 18px;
  margin: 0 auto;
  z-index: 0;
}

:global(:root[data-theme='dark']) .campaign-detail {
  --campaign-primary-surface: rgba(139, 92, 246, 0.18);
  --campaign-primary-soft: rgba(139, 92, 246, 0.1);
  --campaign-primary-text: #ddd6fe;
  --campaign-success-surface: rgba(16, 185, 129, 0.16);
  --campaign-success-text: #6ee7b7;
  --campaign-warning-surface: rgba(245, 158, 11, 0.16);
  --campaign-warning-text: #fcd34d;
  --campaign-info-surface: rgba(59, 130, 246, 0.16);
  --campaign-info-text: #93c5fd;
  --campaign-pink: #f472b6;
  --campaign-muted-fill: #4b5563;
  --campaign-elevated-tint: rgba(139, 92, 246, 0.08);
  --campaign-table-row-hover: color-mix(in srgb, var(--panel-muted) 72%, var(--panel-color));
}

.campaign-sticky-bar {
  position: sticky;
  top: 0;
  z-index: 20;
  background: var(--app-bg);
  padding-bottom: 10px;
}

.campaign-hero {
  display: flex;
  min-height: 92px;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 6px 0 18px;
}

.campaign-hero__copy {
  display: grid;
  gap: 8px;
}

.campaign-hero__title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.campaign-hero h1 {
  color: var(--text-primary);
  font-size: 26px;
  font-weight: 800;
  line-height: 1.25;
}

.campaign-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 600;
}

.campaign-hero__meta span + span {
  border-left: 1px solid var(--border-color);
  padding-left: 12px;
}

.campaign-hero__actions,
.reference-toolbar,
.board-toolbar,
.board-toolbar__actions,
.metadata-actions,
.panel__header,
.performance-grid,
.task-card__top,
.task-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.campaign-tabs {
  display: flex;
  overflow-x: auto;
  border-bottom: 1px solid var(--border-color);
}

.campaign-tabs__button {
  min-height: 50px;
  flex: 0 0 auto;
  border-bottom: 2px solid transparent;
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 14px;
  font-weight: 750;
  padding: 0 24px;
}

.campaign-tabs__button.active {
  border-bottom-color: var(--color-primary-500);
  background: var(--campaign-primary-surface);
  color: var(--campaign-primary-text);
}

.tab-surface {
  display: grid;
  gap: 18px;
}

.panel,
.metric-card,
.kpi-card,
.board-grid,
.info-callout {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.panel {
  display: grid;
  gap: 18px;
  padding: 24px;
}

.panel__header {
  align-items: flex-start;
}

.panel__header h2,
.progress-panel__list h2,
.kpi-note-box h3 {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 800;
}

.requirement-badge {
  display: inline-flex;
  margin-bottom: 5px;
  color: var(--campaign-primary-text);
  font-size: 12px;
  font-weight: 800;
}

.btn {
  min-height: 38px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  font-weight: 750;
  padding: 0 14px;
  white-space: nowrap;
}

.btn--primary {
  background: var(--color-primary-500);
  color: #ffffff;
}

.btn--secondary,
.btn--ghost {
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.btn--ghost {
  background: var(--panel-muted);
}

.status-chip,
.status-pill,
.permission-badge,
.type-badge {
  display: inline-flex;
  min-height: 24px;
  width: fit-content;
  align-items: center;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 750;
  padding: 0 9px;
}

.status-chip {
  gap: 6px;
}

.status-chip i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-chip--primary,
.status-pill--info,
.status-pill--primary,
.permission-badge {
  background: var(--campaign-primary-surface);
  color: var(--campaign-primary-text);
}

.status-pill--success {
  background: var(--campaign-success-surface);
  color: var(--campaign-success-text);
}

.status-pill--warning {
  background: var(--campaign-warning-surface);
  color: var(--campaign-warning-text);
}

.status-pill--muted {
  background: var(--panel-muted);
  color: var(--muted-text);
}

.type-badge {
  border: 1px solid var(--border-color);
  background: var(--panel-muted);
  color: var(--text-secondary);
}

.metadata-layout,
.overview-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.7fr);
  gap: 18px;
}

.stack,
.form-stack {
  display: grid;
  gap: 18px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

label {
  display: grid;
  gap: 7px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

input,
textarea {
  width: 100%;
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--control-color);
  color: var(--text-primary);
  font-size: 13px;
  padding: 10px 12px;
}

input {
  min-height: 38px;
}

textarea {
  resize: vertical;
}

input:disabled,
textarea:disabled {
  background: var(--panel-muted);
  color: var(--muted-text);
  opacity: 1;
}

.partner-list,
.activity-list,
.candidate-list,
.meta-list {
  display: grid;
  gap: 10px;
}

.partner-item,
.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.partner-item {
  align-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 12px;
}

.partner-item__avatar,
.person-cell > span,
.activity-item__icon {
  display: inline-flex;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--campaign-primary-surface);
  color: var(--campaign-primary-text);
  font-size: 12px;
  font-weight: 800;
}

.partner-item strong,
.person-cell strong,
.data-table__row strong,
.reference-card h3,
.task-card strong {
  display: block;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 800;
}

.partner-item small,
.person-cell small,
.activity-item time,
.reference-card small,
.task-card small,
.meta-list dt {
  color: var(--muted-text);
  font-size: 12px;
}

.dashed-button {
  min-height: 38px;
  border: 1px dashed var(--border-color);
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 750;
}

.meta-list div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 10px;
}

.meta-list div:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.meta-list dd {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 700;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.metric-card {
  position: relative;
  display: flex;
  min-height: 100px;
  align-items: center;
  gap: 16px;
  overflow: hidden;
  padding: 22px;
}

.metric-card__icon {
  display: inline-flex;
  width: 48px;
  height: 48px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--campaign-primary-surface);
  color: var(--campaign-primary-text);
  font-weight: 900;
}

.metric-card p,
.kpi-card span,
.performance-grid span {
  color: var(--muted-text);
  font-size: 13px;
  font-weight: 700;
}

.metric-card strong,
.kpi-card strong,
.performance-grid strong {
  display: block;
  margin-top: 3px;
  color: var(--text-primary);
  font-size: 28px;
  font-weight: 850;
  line-height: 1.1;
}

.metric-card small {
  margin-left: 3px;
  color: var(--muted-text);
  font-size: 13px;
}

.metric-card em {
  display: inline-flex;
  margin-top: 4px;
  border-radius: var(--radius-sm);
  background: var(--campaign-primary-surface);
  color: var(--campaign-primary-text);
  font-size: 11px;
  font-style: normal;
  font-weight: 800;
  padding: 2px 7px;
}

.tone-primary {
  border-color: var(--color-primary-200);
}

.tone-success {
  border-color: color-mix(in srgb, var(--color-success) 22%, var(--border-color));
}

.tone-warning {
  border-color: color-mix(in srgb, var(--color-warning) 28%, var(--border-color));
}

.progress-panel {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  align-items: center;
  gap: 28px;
}

.progress-ring {
  position: relative;
  display: grid;
  place-items: center;
}

.progress-ring svg {
  width: 176px;
  height: 176px;
  transform: rotate(-90deg);
}

.progress-ring circle {
  fill: none;
  stroke-width: 12;
}

.progress-ring__track {
  stroke: var(--campaign-primary-surface);
}

.progress-ring__fill {
  stroke: var(--color-primary-500);
  stroke-linecap: round;
}

.progress-ring > div {
  position: absolute;
  display: grid;
  place-items: center;
}

.progress-ring strong {
  color: var(--text-primary);
  font-size: 34px;
  font-weight: 850;
}

.progress-ring span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
}

.progress-panel__list {
  display: grid;
  gap: 17px;
}

.progress-row {
  display: grid;
  gap: 8px;
}

.progress-row div:first-child {
  display: grid;
  grid-template-columns: 12px minmax(0, 1fr) 46px;
  align-items: center;
  gap: 6px;
}

.progress-row em {
  color: var(--text-primary);
  font-style: normal;
  font-weight: 800;
  text-align: right;
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.dot--blue,
.fill-blue {
  background: var(--color-info);
}

.dot--green,
.fill-green,
.fill-success {
  background: var(--color-success);
}

.dot--pink {
  background: var(--campaign-pink);
}

.fill-pink {
  background: var(--campaign-pink);
}

.fill-primary,
.fill-info {
  background: var(--color-primary-500);
}

.fill-warning {
  background: var(--color-warning);
}

.fill-muted {
  background: var(--campaign-muted-fill);
}

.progress-track {
  height: 8px;
  overflow: hidden;
  border-radius: var(--radius-full);
  background: var(--panel-muted);
}

.progress-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.performance-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.performance-grid div,
.kpi-note-box {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 16px;
}

.performance-grid small {
  display: block;
  margin-top: 5px;
  color: var(--campaign-success-text);
  font-size: 12px;
  font-weight: 800;
}

.performance-grid small.warning,
.task-card__foot .warning {
  color: var(--campaign-warning-text);
}

.link-button {
  color: var(--campaign-primary-text);
  cursor: pointer;
  font-size: 13px;
  font-weight: 800;
}

.esg-panel {
  overflow: hidden;
  background: linear-gradient(135deg, var(--panel-color), var(--campaign-elevated-tint));
}

.esg-score {
  display: flex;
  align-items: flex-end;
  gap: 8px;
}

.esg-score strong {
  color: var(--text-primary);
  font-size: 48px;
  font-weight: 900;
  line-height: 1;
}

.esg-panel p {
  color: var(--campaign-success-text);
  font-size: 13px;
  font-weight: 800;
}

.esg-panel small {
  color: var(--muted-text);
  font-size: 12px;
}

.activity-item {
  position: relative;
}

.activity-item p {
  color: var(--text-primary);
  font-size: 13px;
  line-height: 1.5;
}

.activity-item blockquote {
  margin-top: 8px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 13px;
  padding: 10px 12px;
}

.activity-item__icon.tone-success {
  background: var(--campaign-success-surface);
  color: var(--campaign-success-text);
}

.activity-item__icon.tone-info {
  background: var(--campaign-info-surface);
  color: var(--campaign-info-text);
}

.board-toolbar,
.reference-toolbar {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  padding: 12px;
}

.segmented-control {
  display: inline-flex;
  gap: 4px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 4px;
}

.segmented-control button {
  min-height: 32px;
  border-radius: var(--radius-sm);
  color: var(--muted-text);
  cursor: pointer;
  font-size: 13px;
  font-weight: 800;
  padding: 0 12px;
}

.segmented-control button.active {
  background: var(--panel-color);
  color: var(--text-primary);
  box-shadow: var(--shadow-sm);
}

.board-grid {
  overflow-x: auto;
}

.board-grid__head,
.board-grid__row {
  display: grid;
  min-width: 1080px;
  grid-template-columns: 190px repeat(4, minmax(205px, 1fr));
}

.board-grid__head {
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 850;
}

.board-grid__head span,
.team-cell,
.board-cell {
  padding: 14px;
}

.board-grid__row {
  border-top: 1px solid var(--border-color);
}

.team-cell {
  border-right: 1px solid var(--border-color);
}

.team-cell strong {
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 850;
}

.team-cell span {
  display: block;
  margin-top: 4px;
  color: var(--muted-text);
  font-size: 12px;
}

.board-cell {
  display: grid;
  align-content: start;
  gap: 10px;
  min-height: 140px;
  border-right: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--panel-muted) 50%, var(--panel-color));
}

.board-cell:last-child {
  border-right: 0;
}

.task-card {
  display: grid;
  gap: 10px;
  border: 1px solid var(--border-color);
  border-left: 4px solid var(--color-primary-400);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  padding: 12px;
}

.task-card--urgent {
  border-color: color-mix(in srgb, var(--color-warning) 45%, var(--border-color));
  border-left-color: var(--color-warning);
}

.task-card__foot em {
  display: inline-flex;
  width: 24px;
  height: 24px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--campaign-primary-surface);
  color: var(--campaign-primary-text);
  font-size: 11px;
  font-style: normal;
  font-weight: 850;
}

.task-card__foot span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.reference-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.reference-card {
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.reference-card__thumb {
  display: grid;
  height: 160px;
  place-items: center;
  background: var(--panel-muted);
}

.reference-card__thumb strong {
  color: var(--campaign-primary-text);
  font-size: 30px;
  font-weight: 900;
}

.reference-card__thumb.tone-blue {
  background: var(--campaign-info-surface);
}

.reference-card__thumb.tone-primary {
  background: var(--campaign-primary-surface);
}

.reference-card__body {
  display: grid;
  gap: 10px;
  padding: 16px;
}

.reference-card__body > div:first-child {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag-row span {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 700;
  padding: 2px 7px;
}

.candidate-list {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.candidate-list label {
  grid-template-columns: 18px minmax(0, 0.45fr) minmax(0, 1fr);
  align-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 12px;
}

.data-table {
  overflow-x: auto;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
}

.data-table__head,
.data-table__row {
  display: grid;
  min-width: 900px;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
  gap: 12px;
  padding: 12px 14px;
}

.data-table__head {
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 850;
}

.data-table__row {
  color: var(--text-secondary);
  font-size: 13px;
}

.data-table__row:hover {
  background: var(--campaign-table-row-hover);
}

.data-table__row:last-child {
  border-bottom: 0;
}

.data-table--schedule .data-table__head,
.data-table--schedule .data-table__row {
  grid-template-columns: 1.3fr 130px 140px 100px 70px;
}

.data-table--participants .data-table__head,
.data-table--participants .data-table__row {
  grid-template-columns: 1.4fr 1fr 150px 120px 80px;
}

.data-table--kpi .data-table__head,
.data-table--kpi .data-table__row {
  min-width: 1200px;
  grid-template-columns: 1.4fr 0.8fr 110px 110px 70px 1.1fr 110px 130px 80px;
}

.person-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.table-action {
  color: var(--campaign-primary-text);
  cursor: pointer;
  font-size: 12px;
  font-weight: 800;
}

.info-callout {
  color: var(--text-secondary);
  font-size: 13px;
  line-height: 1.6;
  padding: 16px 18px;
}

.kpi-card {
  display: grid;
  gap: 6px;
  min-height: 126px;
  border-left: 4px solid var(--color-primary-500);
  padding: 20px;
}

.kpi-card.tone-success {
  border-left-color: var(--color-success);
}

.kpi-card.tone-info {
  border-left-color: var(--color-info);
}

.kpi-card.tone-warning {
  border-left-color: var(--color-warning);
}

.achievement-cell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 42px;
  align-items: center;
  gap: 8px;
}

.number-cell {
  font-variant-numeric: tabular-nums;
  text-align: right;
}

.kpi-note-box {
  display: grid;
  gap: 10px;
}

.kpi-note-box .btn {
  justify-self: end;
}

@media (max-width: 1180px) {
  .metric-grid,
  .reference-grid,
  .candidate-list,
  .performance-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .metadata-layout,
  .overview-layout,
  .progress-panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .campaign-hero,
  .board-toolbar,
  .reference-toolbar,
  .panel__header,
  .campaign-hero__actions,
  .board-toolbar__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .campaign-hero__meta span + span {
    border-left: 0;
    padding-left: 0;
  }

  .metric-grid,
  .reference-grid,
  .candidate-list,
  .performance-grid,
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
