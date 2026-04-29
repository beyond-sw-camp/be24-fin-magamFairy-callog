<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'

const route = useRoute()
const store = usePlannerStore()

const activeTab = ref('overview')
const currentBoardView = ref('swimlane')
const selectedTeamId = ref(null)
const metadataEditing = ref(false)
const selectedMemberIds = ref([])

const metadataDraft = ref({
  name: '',
  startDate: '',
  endDate: '',
  partnersText: '',
})

const tabs = [
  { id: 'metadata', label: '메타데이터', caption: 'CAMPAIGN_001' },
  { id: 'overview', label: '오버뷰', caption: 'CAMPAIGN_002' },
  { id: 'team-board', label: '팀 보드', caption: 'CAMPAIGN_003' },
  { id: 'references', label: '레퍼런스', caption: 'CAMPAIGN_004' },
  { id: 'participants', label: '참여자 설정', caption: 'CAMPAIGN_005' },
  { id: 'performance', label: '성과/KPI', caption: 'CAMPAIGN_006' },
]

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
    live: '진행 중',
    partner_done: '파트너 완료',
    paused: '일시 중지',
    completed: '완료',
  }

  return labels[activeCampaign.value?.status] ?? activeCampaign.value?.status ?? '진행 중'
})

const partnerNames = computed(() => {
  if (activeCampaign.value?.partners?.length) {
    return activeCampaign.value.partners
  }

  return ['한화 본사', '한화갤러리아', '한화호텔앤드리조트', '대행사A']
})

const scheduleItems = ref([
  {
    id: 'schedule-kickoff',
    title: '공동 캠페인 킥오프',
    owner: '한화 본사',
    date: '2026-05-01',
    status: '확정',
  },
  {
    id: 'schedule-review',
    title: '1차 산출물 검수 마감',
    owner: '한화갤러리아, 대행사A',
    date: '2026-05-17',
    status: '진행 중',
  },
  {
    id: 'schedule-launch',
    title: '캠페인 라이브 QA',
    owner: '전체 참여사',
    date: '2026-05-25',
    status: '예정',
  },
])

const statusColumns = [
  { id: 'todo', label: '할 일' },
  { id: 'progress', label: '진행 중' },
  { id: 'review', label: '검수 대기' },
  { id: 'done', label: '완료' },
]

const dateColumns = [
  { id: '2026-05-10', label: '~ 05.10' },
  { id: '2026-05-15', label: '~ 05.15' },
  { id: '2026-05-20', label: '~ 05.20' },
  { id: '2026-05-25', label: '~ 05.25' },
]

const teams = [
  {
    id: 'hq',
    name: '한화 본사',
    role: '총괄 기획 및 최종 검수',
    owner: '이성재 PM',
    progress: 72,
    tone: 'live',
    permission: '모든 업무 수정 가능',
  },
  {
    id: 'galleria',
    name: '한화갤러리아',
    role: 'VIP 고객 접점 및 팝업 운영',
    owner: '갤러리아 마케팅팀',
    progress: 84,
    tone: 'approved',
    permission: '당사 담당 업무 수정 가능',
  },
  {
    id: 'hotel',
    name: '한화호텔앤드리조트',
    role: '숙박 패키지 및 제휴 혜택',
    owner: '리조트 세일즈팀',
    progress: 58,
    tone: 'delayed',
    permission: '당사 담당 업무 수정 가능',
  },
  {
    id: 'agency',
    name: '대행사A',
    role: '콘텐츠 및 홍보물 제작',
    owner: 'Agency PM',
    progress: 46,
    tone: 'warning',
    permission: '당사 담당 업무 수정 가능',
  },
]

const teamTasks = [
  {
    id: 'task-hq-budget',
    teamId: 'hq',
    status: 'todo',
    dateColumn: '2026-05-10',
    title: '캠페인 통합 예산안 기안',
    instruction: '협력사별 집행 금액과 검수 예산을 하나의 기준표로 정리',
    dueDate: '2026-05-09',
    partner: '한화 본사',
    review: '내부 검토',
  },
  {
    id: 'task-hq-tone',
    teamId: 'hq',
    status: 'review',
    dateColumn: '2026-05-20',
    title: '초청장 톤앤매너 최종 확인',
    instruction: 'VIP 세그먼트 문구와 브랜드 표현의 일관성 확인',
    dueDate: '2026-05-18',
    partner: '한화 본사',
    review: '검수 대기',
  },
  {
    id: 'task-galleria-vmd',
    teamId: 'galleria',
    status: 'todo',
    dateColumn: '2026-05-15',
    title: '지점 팝업스토어 VMD 시안',
    instruction: 'VIP 고객 동선과 현장 배너 위치 기준 반영',
    dueDate: '2026-05-12',
    partner: '한화갤러리아',
    review: '초안',
  },
  {
    id: 'task-galleria-target',
    teamId: 'galleria',
    status: 'progress',
    dateColumn: '2026-05-15',
    title: 'VIP 대상자 명단 추출',
    instruction: '캠페인 수신 동의 고객만 대상으로 선별',
    dueDate: '2026-05-15',
    partner: '한화갤러리아',
    review: '진행 중',
  },
  {
    id: 'task-hotel-package',
    teamId: 'hotel',
    status: 'progress',
    dateColumn: '2026-05-25',
    title: '숙박 패키지 예약 페이지 구축',
    instruction: '패키지 혜택, 객실 조건, 예약 제한사항을 같은 구조로 노출',
    dueDate: '2026-05-20',
    partner: '한화호텔앤드리조트',
    review: '진행 중',
  },
  {
    id: 'task-hotel-guide',
    teamId: 'hotel',
    status: 'review',
    dateColumn: '2026-05-20',
    title: '제휴 혜택 가이드라인 안내문',
    instruction: '현장 직원과 CS팀이 동일하게 답변할 수 있는 문구 정리',
    dueDate: '2026-05-18',
    partner: '한화호텔앤드리조트',
    review: '수정 필요',
  },
  {
    id: 'task-agency-banner',
    teamId: 'agency',
    status: 'todo',
    dateColumn: '2026-05-15',
    title: '현장 배너 디자인',
    instruction: 'X배너, 데스크 배너, 모바일 안내 이미지 사이즈별 제작',
    dueDate: '2026-05-15',
    partner: '대행사A',
    review: '초안',
  },
  {
    id: 'task-agency-invite',
    teamId: 'agency',
    status: 'review',
    dateColumn: '2026-05-20',
    title: 'VIP 초청장 디자인 V2 업로드',
    instruction: '본사 피드백 반영 후 이미지와 문구를 함께 재제출',
    dueDate: '2026-05-17',
    partner: '대행사A',
    review: '지연',
  },
  {
    id: 'task-agency-kv',
    teamId: 'agency',
    status: 'done',
    dateColumn: '2026-05-10',
    title: '캠페인 메인 키비주얼',
    instruction: '메인 KV 최종 파일을 라이브러리에 보관',
    dueDate: '2026-05-10',
    partner: '대행사A',
    review: '승인 완료',
  },
]

const references = [
  {
    id: 'ref-vip',
    title: '2025 VIP 초청전 결과 리포트',
    source: '레퍼런스 보관함',
    owner: '한화 본사',
    status: '공유됨',
    permission: '본사 승인 후 삭제',
  },
  {
    id: 'ref-crawler',
    title: '프리미엄 리조트 프로모션 사례 요약',
    source: '크롤러 추출 데이터 AI 가공',
    owner: '한화호텔앤드리조트',
    status: '생성됨',
    permission: '생성자 수정 가능',
  },
  {
    id: 'ref-popup',
    title: '백화점 VIP 라운지 VMD 무드보드',
    source: '외부 캠페인 벤치마크',
    owner: '한화갤러리아',
    status: '검토 필요',
    permission: '당사 추가 자료',
  },
]

const participantCandidates = [
  { id: 'minji', name: '김민지', team: '한화 본사', role: 'HQ Admin' },
  { id: 'sungjae', name: '이성재', team: '한화 본사', role: 'Campaign PM' },
  { id: 'galleria-lead', name: '정하린', team: '한화갤러리아', role: 'Partner Manager' },
  { id: 'hotel-lead', name: '강윤서', team: '한화호텔앤드리조트', role: 'Partner Manager' },
  { id: 'agency-lead', name: '최도윤', team: '대행사A', role: 'Agency PM' },
]

const campaignParticipants = ref([
  { id: 'minji', name: '김민지', team: '한화 본사', role: 'HQ Admin', access: '읽기/수정' },
  { id: 'sungjae', name: '이성재', team: '한화 본사', role: 'Campaign PM', access: '읽기/수정' },
  { id: 'galleria-lead', name: '정하린', team: '한화갤러리아', role: 'Partner Manager', access: '당사 업무 수정' },
])

const kpiRows = ref([
  {
    id: 'kpi-impression',
    name: 'VIP 초청장 노출 수',
    category: '콘텐츠 노출',
    target: 120000,
    actual: 98000,
    unit: '회',
    source: '검증된 수치 직접 입력',
    owner: '한화갤러리아',
    inputDate: '2026-06-30',
    status: '미달',
    memo: '오픈 초반 발송량 보강 필요',
    nextAction: '알림톡 재발송 세그먼트 검토',
  },
  {
    id: 'kpi-click',
    name: '패키지 예약 페이지 클릭 수',
    category: '전환 전 단계',
    target: 8000,
    actual: 9200,
    unit: '건',
    source: '랜딩 로그 수동 입력',
    owner: '한화호텔앤드리조트',
    inputDate: '2026-06-30',
    status: '초과',
    memo: '객실 혜택 문구 반응 양호',
    nextAction: '예약 전환 구간 분석',
  },
  {
    id: 'kpi-conversion',
    name: '캠페인 목표 대비 실제 성과',
    category: '전환',
    target: 500,
    actual: 480,
    unit: '건',
    source: 'PM 검증 수치 직접 입력',
    owner: '한화 본사',
    inputDate: '2026-06-30',
    status: '미달',
    memo: '마감 직전 유입 대비 전환율 보완 필요',
    nextAction: '다음 캠페인 혜택 구조 조정',
  },
])

const selectedTeam = computed(() =>
  teams.find((team) => team.id === selectedTeamId.value) ?? null,
)

const avgProgress = computed(() =>
  Math.round(teams.reduce((sum, team) => sum + team.progress, 0) / teams.length),
)

const reviewCount = computed(() =>
  teamTasks.filter((task) => ['검수 대기', '수정 필요'].includes(task.review)).length,
)

const delayedCount = computed(() => teamTasks.filter((task) => task.review === '지연').length)

const approvedCount = computed(() => teamTasks.filter((task) => task.review === '승인 완료').length)

const esgScore = computed(() => 82)

const performanceRecordCount = computed(() => kpiRows.value.length)

function syncMetadataDraft() {
  metadataDraft.value = {
    name: activeCampaign.value?.name ?? '',
    startDate: activeCampaign.value?.startDate ?? '',
    endDate: activeCampaign.value?.endDate ?? '',
    partnersText: partnerNames.value.join(', '),
  }
}

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
    return '초과'
  }

  if (getAchievement(row) >= 100) {
    return '달성'
  }

  return '미달'
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
      owner: '한화 본사',
      date: activeCampaign.value?.startDate || '2026-05-01',
      status: '예정',
    },
  ]
  metadataEditing.value = true
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
      access: candidate.team === '한화 본사' ? '읽기/수정' : '당사 업무 수정',
    }))

  campaignParticipants.value = [...campaignParticipants.value, ...additions]
  selectedMemberIds.value = []
}

function openTeam(teamId) {
  selectedTeamId.value = teamId
}

function closeTeamModal() {
  selectedTeamId.value = null
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
    <header class="campaign-detail__header">
      <div class="campaign-detail__identity">
        <span
          class="campaign-detail__mark"
          :style="{ background: activeCampaign?.color ?? 'var(--color-primary-500)' }"
        >
          {{ activeCampaign?.initials ?? 'CP' }}
        </span>
        <div>
          <p class="campaign-detail__eyebrow">캠페인 상세보기</p>
          <h1>{{ activeCampaign?.name ?? '캠페인 상세' }}</h1>
          <span>
            {{ activeCampaign?.purpose || '캠페인의 일정, 업무, 레퍼런스, 참여자, KPI를 한 화면에서 관리합니다.' }}
          </span>
        </div>
      </div>

      <div class="campaign-detail__meta">
        <span class="campaign-detail__status">{{ campaignStatusLabel }}</span>
        <strong>{{ activeCampaign?.period ?? '기간 미정' }}</strong>
        <small>읽기: 모두 · 수정: 본사 관리자</small>
      </div>
    </header>

    <nav class="campaign-detail__tabs" aria-label="캠페인 상세 탭">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        class="campaign-detail__tab"
        :class="{ active: activeTab === tab.id }"
        @click="activeTab = tab.id"
      >
        <span>{{ tab.label }}</span>
        <small>{{ tab.caption }}</small>
      </button>
    </nav>

    <section v-if="activeTab === 'metadata'" class="campaign-panel">
      <div class="campaign-panel__head">
        <div>
          <span class="requirement-badge">CAMPAIGN_001</span>
          <h2>캠페인 메타데이터</h2>
          <p>캠페인에 관한 정보를 캠페인 페이지 어디에서든 확인하고, 본사 관리자만 수정할 수 있습니다.</p>
        </div>
        <div class="campaign-actions">
          <span class="permission-badge">본사 관리자만 수정</span>
          <button type="button" class="secondary-button" @click="metadataEditing = !metadataEditing">
            {{ metadataEditing ? '수정 취소' : '메타데이터 수정' }}
          </button>
          <button type="button" class="primary-button-small" @click="saveMetadata">저장</button>
        </div>
      </div>

      <div class="metadata-grid">
        <label>
          <span>캠페인 제목</span>
          <input v-model="metadataDraft.name" :disabled="!metadataEditing" type="text" />
        </label>
        <label>
          <span>캠페인 시작일</span>
          <input v-model="metadataDraft.startDate" :disabled="!metadataEditing" type="date" />
        </label>
        <label>
          <span>캠페인 종료일</span>
          <input v-model="metadataDraft.endDate" :disabled="!metadataEditing" type="date" />
        </label>
        <label class="metadata-grid__wide">
          <span>협력사 목록</span>
          <input v-model="metadataDraft.partnersText" :disabled="!metadataEditing" type="text" />
        </label>
      </div>

      <div class="section-strip">
        <div>
          <h3>일정 추가/삭제/수정</h3>
          <p>캠페인 운영 일정은 본사 관리자 계정에서 관리합니다.</p>
        </div>
        <button type="button" class="secondary-button" @click="addScheduleItem">일정 추가</button>
      </div>

      <div class="data-table data-table--schedule">
        <div class="data-table__head">
          <span>일정명</span>
          <span>담당</span>
          <span>날짜</span>
          <span>상태</span>
          <span>관리</span>
        </div>
        <div v-for="item in scheduleItems" :key="item.id" class="data-table__row">
          <input v-model="item.title" :disabled="!metadataEditing" type="text" />
          <input v-model="item.owner" :disabled="!metadataEditing" type="text" />
          <input v-model="item.date" :disabled="!metadataEditing" type="date" />
          <input v-model="item.status" :disabled="!metadataEditing" type="text" />
          <button type="button" class="ghost-danger-button" @click="removeScheduleItem(item.id)">삭제</button>
        </div>
      </div>
    </section>

    <section v-else-if="activeTab === 'overview'" class="campaign-panel">
      <div class="campaign-panel__head">
        <div>
          <span class="requirement-badge">CAMPAIGN_002</span>
          <h2>캠페인 오버뷰</h2>
          <p>전체 일정, 협력사별 진행률, 검수 대기 자료, 지연 업무, ESG 점수, 성과 기록을 함께 봅니다.</p>
        </div>
      </div>

      <div class="metric-grid">
        <article class="metric-card">
          <span>캠페인 전체 진척도</span>
          <strong>{{ avgProgress }}%</strong>
          <p>협력사별 평균 진행률</p>
        </article>
        <article class="metric-card">
          <span>검수 대기 자료</span>
          <strong>{{ reviewCount }}</strong>
          <p>검수 대기 및 수정 필요</p>
        </article>
        <article class="metric-card">
          <span>지연 업무</span>
          <strong>{{ delayedCount }}</strong>
          <p>마감 초과 산출물</p>
        </article>
        <article class="metric-card">
          <span>ESG 점수</span>
          <strong>{{ esgScore }}</strong>
          <p>친환경 운영 기준 반영</p>
        </article>
        <article class="metric-card">
          <span>성과 기록</span>
          <strong>{{ performanceRecordCount }}</strong>
          <p>KPI 입력 항목</p>
        </article>
      </div>

      <div class="overview-grid">
        <article class="overview-panel">
          <h3>협력사별 진행률</h3>
          <div class="progress-list">
            <div v-for="team in teams" :key="team.id" class="progress-row">
              <span>{{ team.name }}</span>
              <div class="progress-track"><i :style="{ width: `${team.progress}%` }" /></div>
              <strong>{{ team.progress }}%</strong>
            </div>
          </div>
        </article>

        <article class="overview-panel">
          <h3>검수 대기 및 지연 업무</h3>
          <div class="compact-list">
            <div v-for="task in teamTasks.filter((item) => ['검수 대기', '수정 필요', '지연'].includes(item.review))" :key="task.id">
              <strong>{{ task.title }}</strong>
              <span>{{ task.partner }} · {{ task.review }} · {{ task.dueDate }}</span>
            </div>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeTab === 'team-board'" class="campaign-panel">
      <div class="campaign-panel__head">
        <div>
          <span class="requirement-badge">CAMPAIGN_003</span>
          <h2>팀 보드 보기</h2>
          <p>지시 내용, 업무 기한, 담당 협력사와 본사 피드백 및 검수 상태를 참여사별로 확인합니다.</p>
        </div>
        <div class="campaign-board__toggle" role="group" aria-label="보드 보기 방식">
          <button
            type="button"
            :class="{ active: currentBoardView === 'swimlane' }"
            @click="currentBoardView = 'swimlane'"
          >
            스윔레인 뷰
          </button>
          <button
            type="button"
            :class="{ active: currentBoardView === 'timeline' }"
            @click="currentBoardView = 'timeline'"
          >
            날짜별 타임라인 뷰
          </button>
        </div>
      </div>

      <div v-if="currentBoardView === 'swimlane'" class="board-wrap">
        <p class="axis-note">가로축: 상태 · 세로축: 참여사</p>
        <div class="board-grid board-grid--status">
          <div class="board-grid__head">
            <span>참여사</span>
            <span v-for="status in statusColumns" :key="status.id">{{ status.label }}</span>
          </div>
          <div v-for="team in teams" :key="team.id" class="board-grid__row">
            <button type="button" class="team-cell" @click="openTeam(team.id)">
              <strong>{{ team.name }}</strong>
              <span>{{ team.role }}</span>
              <small>{{ team.permission }}</small>
            </button>
            <div v-for="status in statusColumns" :key="status.id" class="board-cell">
              <article v-for="task in getTasksByStatus(team.id, status.id)" :key="task.id" class="task-card">
                <strong>{{ task.title }}</strong>
                <p>{{ task.instruction }}</p>
                <div>
                  <span>{{ task.partner }}</span>
                  <small>{{ task.dueDate }}</small>
                </div>
              </article>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="board-wrap">
        <p class="axis-note">가로축: 날짜 · 세로축: 참여사</p>
        <div class="board-grid board-grid--date">
          <div class="board-grid__head">
            <span>참여사</span>
            <span v-for="date in dateColumns" :key="date.id">{{ date.label }}</span>
          </div>
          <div v-for="team in teams" :key="team.id" class="board-grid__row">
            <button type="button" class="team-cell" @click="openTeam(team.id)">
              <strong>{{ team.name }}</strong>
              <span>{{ team.owner }}</span>
              <small>{{ team.progress }}% 진행</small>
            </button>
            <div v-for="date in dateColumns" :key="date.id" class="board-cell">
              <article v-for="task in getTasksByDate(team.id, date.id)" :key="task.id" class="task-card">
                <strong>{{ task.title }}</strong>
                <p>{{ task.review }}</p>
                <div>
                  <span>{{ task.partner }}</span>
                  <small>{{ task.dueDate }}</small>
                </div>
              </article>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section v-else-if="activeTab === 'references'" class="campaign-panel">
      <div class="campaign-panel__head">
        <div>
          <span class="requirement-badge">CAMPAIGN_004</span>
          <h2>레퍼런스탭</h2>
          <p>현재 캠페인과 연결된 레퍼런스를 공유하고, 크롤러 추출 데이터를 AI로 가공해 생성합니다.</p>
        </div>
        <div class="campaign-actions">
          <button type="button" class="secondary-button">레퍼런스 추가</button>
          <button type="button" class="primary-button-small">레퍼런스 생성</button>
        </div>
      </div>

      <div class="reference-grid">
        <article v-for="reference in references" :key="reference.id" class="reference-card">
          <span>{{ reference.status }}</span>
          <h3>{{ reference.title }}</h3>
          <p>{{ reference.source }}</p>
          <small>{{ reference.owner }} · {{ reference.permission }}</small>
        </article>
      </div>
    </section>

    <section v-else-if="activeTab === 'participants'" class="campaign-panel">
      <div class="campaign-panel__head">
        <div>
          <span class="requirement-badge">CAMPAIGN_005</span>
          <h2>참여자 설정</h2>
          <p>본사 및 협력사의 관리자가 자신의 팀원 목록에서 참가자를 복수 선택해 추가합니다.</p>
        </div>
        <button type="button" class="primary-button-small" @click="addSelectedParticipants">참가자 추가</button>
      </div>

      <div class="participants-grid">
        <article class="participant-panel">
          <h3>현재 참가자</h3>
          <div class="compact-list">
            <div v-for="participant in campaignParticipants" :key="participant.id">
              <strong>{{ participant.name }}</strong>
              <span>{{ participant.team }} · {{ participant.role }} · {{ participant.access }}</span>
            </div>
          </div>
        </article>

        <article class="participant-panel">
          <h3>팀원 목록</h3>
          <div class="candidate-list">
            <label v-for="candidate in participantCandidates" :key="candidate.id">
              <input v-model="selectedMemberIds" type="checkbox" :value="candidate.id" />
              <span>{{ candidate.name }}</span>
              <small>{{ candidate.team }} · {{ candidate.role }}</small>
            </label>
          </div>
        </article>
      </div>
    </section>

    <section v-else-if="activeTab === 'performance'" class="campaign-panel">
      <div class="campaign-panel__head">
        <div>
          <span class="requirement-badge">CAMPAIGN_006</span>
          <h2>캠페인 성과/KPI</h2>
          <p>성과 목표 및 KPI를 기준으로 실제 결과값을 직접 입력하고 목표 대비 달성률을 확인합니다.</p>
        </div>
        <span class="permission-badge">외부 성과 API 미연동 · 검증 수치 직접 입력</span>
      </div>

      <div class="data-table data-table--kpi">
        <div class="data-table__head">
          <span>KPI명</span>
          <span>분류</span>
          <span>목표값</span>
          <span>실제값</span>
          <span>단위</span>
          <span>담당자</span>
          <span>입력일</span>
          <span>달성률</span>
          <span>상태</span>
        </div>
        <div v-for="row in kpiRows" :key="row.id" class="data-table__row">
          <strong>{{ row.name }}</strong>
          <span>{{ row.category }}</span>
          <input v-model.number="row.target" type="number" aria-label="목표값" />
          <input v-model.number="row.actual" type="number" aria-label="실제값" />
          <span>{{ row.unit }}</span>
          <span>{{ row.owner }}</span>
          <span>{{ row.inputDate }}</span>
          <span>{{ getAchievement(row) }}%</span>
          <span class="status-pill">{{ getKpiStatus(row) }}</span>
        </div>
      </div>

      <div class="kpi-notes">
        <article v-for="row in kpiRows" :key="`${row.id}-memo`">
          <strong>{{ row.name }}</strong>
          <p>측정 출처: {{ row.source }}</p>
          <p>분석 메모: {{ row.memo }}</p>
          <p>다음 액션: {{ row.nextAction }}</p>
        </article>
      </div>
    </section>

    <Teleport to="body">
      <Transition name="campaign-modal">
        <div
          v-if="selectedTeam"
          class="team-modal"
          role="dialog"
          aria-modal="true"
          :aria-label="`${selectedTeam.name} 상세`"
          @click.self="closeTeamModal"
        >
          <section class="team-modal__panel">
            <header>
              <div>
                <p>팀 상세</p>
                <h2>{{ selectedTeam.name }}</h2>
                <span>{{ selectedTeam.role }} · {{ selectedTeam.owner }}</span>
              </div>
              <button type="button" aria-label="닫기" @click="closeTeamModal">×</button>
            </header>

            <div class="team-modal__stats">
              <div>
                <span>진행률</span>
                <strong>{{ selectedTeam.progress }}%</strong>
              </div>
              <div>
                <span>수정 권한</span>
                <strong>{{ selectedTeam.permission }}</strong>
              </div>
              <div>
                <span>담당 업무</span>
                <strong>{{ teamTasks.filter((task) => task.teamId === selectedTeam.id).length }}건</strong>
              </div>
            </div>

            <div class="team-modal__items">
              <article v-for="task in teamTasks.filter((item) => item.teamId === selectedTeam.id)" :key="task.id">
                <strong>{{ task.title }}</strong>
                <span>{{ task.instruction }} · {{ task.dueDate }} · {{ task.review }}</span>
              </article>
            </div>
          </section>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<style scoped>
.campaign-detail {
  display: flex;
  width: 100%;
  max-width: var(--content-max-width);
  flex-direction: column;
  gap: 14px;
  margin: 0 auto;
}

.campaign-detail__header,
.campaign-detail__tabs,
.campaign-panel,
.metric-card,
.overview-panel,
.participant-panel,
.reference-card {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
}

.campaign-detail__header {
  display: flex;
  min-height: 104px;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 20px 24px;
}

.campaign-detail__identity {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 14px;
}

.campaign-detail__mark {
  display: inline-flex;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  color: #ffffff;
  font-size: 14px;
  font-weight: 800;
}

.campaign-detail__eyebrow,
.requirement-badge,
.metric-card span,
.permission-badge,
.section-strip p,
.axis-note,
.team-cell small {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
}

.campaign-detail__identity h1 {
  margin-top: 3px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 750;
  line-height: 1.25;
}

.campaign-detail__identity span,
.campaign-detail__meta strong,
.campaign-detail__meta small,
.campaign-panel__head p,
.compact-list span,
.reference-card p,
.reference-card small,
.candidate-list small,
.task-card p,
.task-card small,
.kpi-notes p,
.team-modal__items span {
  color: var(--muted-text);
  font-size: 13px;
  line-height: 1.45;
}

.campaign-detail__meta {
  display: grid;
  flex-shrink: 0;
  justify-items: end;
  gap: 6px;
}

.campaign-detail__status,
.permission-badge,
.status-pill,
.reference-card span {
  display: inline-flex;
  min-height: 24px;
  align-items: center;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 700;
}

.campaign-detail__status {
  padding: 0 10px;
  background: var(--color-primary-500);
  color: #ffffff;
}

.permission-badge {
  padding: 0 10px;
  background: var(--color-primary-100);
  color: var(--color-primary-700);
  white-space: nowrap;
}

.campaign-detail__tabs {
  display: flex;
  align-items: stretch;
  gap: 4px;
  padding: 6px;
}

.campaign-detail__tab {
  display: grid;
  min-height: 52px;
  flex: 1;
  align-content: center;
  gap: 2px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  cursor: pointer;
  padding: 0 14px;
  text-align: left;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.campaign-detail__tab span {
  font-size: 14px;
  font-weight: 750;
}

.campaign-detail__tab small {
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 700;
}

.campaign-detail__tab.active {
  background: var(--color-primary-500);
  color: #ffffff;
}

.campaign-detail__tab.active small {
  color: rgba(255, 255, 255, 0.78);
}

.campaign-panel {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.campaign-panel__head,
.section-strip,
.campaign-actions,
.task-card div,
.data-table__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.campaign-panel__head h2,
.section-strip h3,
.overview-panel h3,
.participant-panel h3 {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 750;
}

.requirement-badge {
  display: inline-flex;
  margin-bottom: 5px;
  color: var(--color-primary-700);
}

.primary-button-small,
.secondary-button,
.ghost-danger-button {
  min-height: 34px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  padding: 0 13px;
  white-space: nowrap;
}

.primary-button-small {
  background: var(--color-primary-500);
  color: #ffffff;
}

.secondary-button {
  border: 1px solid var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.ghost-danger-button {
  background: var(--color-danger-light);
  color: var(--color-danger-dark);
}

.metadata-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.metadata-grid label {
  display: grid;
  gap: 6px;
}

.metadata-grid__wide {
  grid-column: 1 / -1;
}

.metadata-grid span,
.data-table__head,
.team-cell span,
.task-card span,
.candidate-list span,
.reference-card span {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 800;
}

input {
  width: 100%;
  min-width: 0;
  min-height: 36px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--control-color);
  color: var(--text-primary);
  padding: 0 10px;
  font-size: 13px;
}

input:disabled {
  background: var(--panel-muted);
  color: var(--muted-text);
  opacity: 1;
}

.section-strip {
  border-top: 1px solid var(--border-color);
  padding-top: 16px;
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
  padding: 10px 12px;
}

.data-table__head {
  background: var(--panel-muted);
}

.data-table__row:last-child {
  border-bottom: none;
}

.data-table--schedule .data-table__head,
.data-table--schedule .data-table__row {
  grid-template-columns: 1.4fr 1fr 150px 130px 76px;
}

.data-table--kpi .data-table__head,
.data-table--kpi .data-table__row {
  grid-template-columns: 1.5fr 1fr 110px 110px 70px 1fr 110px 90px 80px;
}

.data-table__row strong,
.compact-list strong,
.task-card strong,
.reference-card h3,
.team-cell strong,
.team-modal__items strong {
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 800;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.metric-card {
  min-height: 126px;
  padding: 18px;
}

.metric-card strong {
  display: block;
  margin-top: 8px;
  color: var(--text-primary);
  font-size: 30px;
  line-height: 1;
}

.metric-card p {
  margin-top: 8px;
  color: var(--subtle-text);
  font-size: 12px;
}

.overview-grid,
.participants-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(320px, 0.9fr);
  gap: 12px;
}

.overview-panel,
.participant-panel {
  padding: 18px;
}

.progress-list,
.compact-list,
.candidate-list,
.kpi-notes {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.progress-row {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr) 52px;
  align-items: center;
  gap: 12px;
}

.progress-row span {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.progress-track {
  height: 8px;
  overflow: hidden;
  border-radius: var(--radius-full);
  background: var(--color-primary-100);
}

.progress-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-primary-500);
}

.progress-row strong {
  color: var(--text-primary);
  font-size: 13px;
  text-align: right;
}

.compact-list div,
.candidate-list label,
.kpi-notes article,
.team-modal__items article {
  display: grid;
  gap: 3px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 12px;
}

.candidate-list label {
  grid-template-columns: 18px minmax(0, 0.5fr) minmax(0, 1fr);
  align-items: center;
}

.candidate-list input {
  min-height: auto;
}

.campaign-board__toggle {
  display: inline-flex;
  flex-shrink: 0;
  gap: 3px;
  padding: 3px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.campaign-board__toggle button {
  min-height: 32px;
  border-radius: var(--radius-sm);
  color: var(--muted-text);
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  padding: 0 12px;
}

.campaign-board__toggle button.active {
  background: var(--panel-color);
  color: var(--text-primary);
  box-shadow: var(--shadow-sm);
}

.board-wrap {
  display: grid;
  gap: 10px;
}

.board-grid {
  overflow-x: auto;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
}

.board-grid__head,
.board-grid__row {
  display: grid;
  min-width: 1060px;
  grid-template-columns: 190px repeat(4, minmax(180px, 1fr));
}

.board-grid__head {
  min-height: 40px;
  align-items: center;
  background: var(--panel-muted);
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.board-grid__head span,
.team-cell,
.board-cell {
  padding: 12px;
}

.board-grid__row {
  border-top: 1px solid var(--border-color);
}

.team-cell {
  display: grid;
  gap: 5px;
  border-right: 1px solid var(--border-color);
  cursor: pointer;
  text-align: left;
}

.board-cell {
  display: grid;
  align-content: start;
  gap: 8px;
  min-height: 150px;
  border-right: 1px solid var(--border-color);
  background: color-mix(in srgb, var(--panel-muted) 54%, var(--panel-color));
}

.board-cell:last-child {
  border-right: none;
}

.task-card {
  display: grid;
  gap: 8px;
  border: 1px solid var(--border-color);
  border-left: 3px solid var(--color-primary-400);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  padding: 11px;
}

.reference-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.reference-card {
  display: grid;
  gap: 8px;
  min-height: 170px;
  padding: 18px;
}

.reference-card span,
.status-pill {
  width: fit-content;
  padding: 0 9px;
  background: var(--badge-bg);
  color: var(--badge-text);
}

.status-pill {
  justify-content: center;
}

.kpi-notes {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.team-modal {
  position: fixed;
  inset: 0;
  z-index: 80;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(15, 23, 42, 0.34);
  padding: 24px;
}

.team-modal__panel {
  width: min(680px, 100%);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: var(--shadow-elevated);
  padding: 22px;
}

.team-modal__panel header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid var(--border-color);
  padding-bottom: 16px;
}

.team-modal__panel header p {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.team-modal__panel header h2 {
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 800;
}

.team-modal__panel header span {
  color: var(--muted-text);
  font-size: 13px;
}

.team-modal__panel header button {
  width: 34px;
  height: 34px;
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 22px;
  line-height: 1;
}

.team-modal__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.team-modal__stats div {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 12px;
}

.team-modal__stats span {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 700;
}

.team-modal__stats strong {
  display: block;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 16px;
}

.campaign-modal-enter-active,
.campaign-modal-leave-active {
  transition: opacity var(--transition-fast);
}

.campaign-modal-enter-from,
.campaign-modal-leave-to {
  opacity: 0;
}

@media (max-width: 1180px) {
  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .overview-grid,
  .participants-grid,
  .reference-grid,
  .kpi-notes {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .campaign-detail__header,
  .campaign-panel__head,
  .section-strip {
    align-items: flex-start;
    flex-direction: column;
  }

  .campaign-detail__meta {
    justify-items: start;
  }

  .campaign-detail__tabs,
  .campaign-actions,
  .campaign-board__toggle {
    overflow-x: auto;
  }

  .campaign-detail__tab {
    min-width: 142px;
  }

  .metadata-grid,
  .metric-grid,
  .team-modal__stats {
    grid-template-columns: 1fr;
  }
}
</style>
