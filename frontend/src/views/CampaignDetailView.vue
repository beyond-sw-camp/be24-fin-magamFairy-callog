<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import { useAuthStore } from '@/stores/useAuthStore'
import { useCampaignKpiStore } from '@/stores/campaignKpi.js'
import { UpdateCampaign, UpdateCampaignStatus } from '@/api/campaigns'
import ReviewApprovalView from '@/views/ReviewApprovalView.vue'
import MatchOverview from '@/views/MatchOverview.vue'
import CampaignMembersPanel from '@/components/campaign/CampaignMembersPanel.vue'
import CampaignKpiTab from '@/components/campaign/kpi/CampaignKpiTab.vue'
import CampaignKpiCascadeView from '@/components/campaign/CampaignKpiCascadeView.vue'
import CampaignExportModal from '@/components/campaign/CampaignExportModal.vue'
import {
  ListMilestones,
  ListTaskParts,
  ListTasksByCampaign,
  CreateMilestone,
  CreateTaskPart,
  CreateTask,
  UpdateTask,
} from '@/api/teamboard'
import { getCampaignMembers, listCampaignParticipants } from '@/api/campaignMembers'

const TEAMBOARD_STATUS_MAP = {
  BACKLOG: 'backlog',
  TODO: 'backlog',
  IN_PROGRESS: 'in_progress',
  REVIEW: 'review',
  BLOCKED: 'revision',
  DONE: 'done',
}

const TEAMBOARD_PRIORITY_MAP = {
  LOW: 'low',
  MEDIUM: 'medium',
  HIGH: 'high',
  CRITICAL: 'critical',
}

const STATUS_TO_BACKEND = {
  backlog: 'BACKLOG',
  in_progress: 'IN_PROGRESS',
  review: 'REVIEW',
  approval_wait: 'REVIEW',
  revision: 'BLOCKED',
  done: 'DONE',
}

const TYPE_TO_BACKEND = {
  '문서': 'DOCUMENT',
  '이미지': 'DESIGN',
  '영상': 'VIDEO',
  '웹': 'OTHER',
  '광고': 'OTHER',
  '브리프': 'DOCUMENT',
  '기타': 'OTHER',
}

function toIsoDateTime(dateStr) {
  return dateStr ? `${dateStr}T00:00:00` : null
}

function formatTeamboardDate(iso) {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  return `${mm}.${dd}`
}

const route = useRoute()
const router = useRouter()
const store = usePlannerStore()

const activeTab = ref('캠페인 오버뷰')
const currentBoardView = ref('part')
const metadataEditing = ref(false)
const isSaving = ref(false)
const heroCollapsed = ref(false)
const exportModalOpen = ref(false)
const authStore = useAuthStore()
const canExport = computed(() => {
  const isManagerLevel = authStore.isManager || authStore.isGeneralManager
  return isManagerLevel && organizationIsPm.value
})

// 캠페인 편집 권한 — `GET /campaigns/{id}/members` 응답에서 채움
// 두 조건을 모두 만족해야 편집 가능: (1) 내 조직이 PM, (2) 내 캠페인 역할이 MANAGER 또는 GENERAL_MANAGER
const myCampaignRole = ref(null)
const organizationIsPm = ref(false)
const canEditMetadata = computed(() =>
  organizationIsPm.value
  && (myCampaignRole.value === 'MANAGER' || myCampaignRole.value === 'GENERAL_MANAGER'),
)

const metadataDraft = ref({
  name: '',
  startDate: '',
  endDate: '',
  summary: '',
  color: '',
  status: 'draft',
})

// 백엔드 CampaignService.CAMPAIGN_PALETTE와 동일한 20색 (2025–2026 트렌드 큐레이션)
// WGSN/Coloro 2026 + Pantone COY + 2025 디지털 디자인 시스템에서 추출
const CAMPAIGN_PALETTE = [
  '#7C3AED', '#B68BD9', '#4F4878', '#4A4E97', '#2740B2', // 01–05 violet · lavender · dusk · indigo · cobalt
  '#0EA5E9', '#14B8A6', '#BEF264', '#F59E0B', '#FFBE98', // 06–10 sky · mint · lime · amber · peach
  '#FF6B45', '#FB7185', '#C5174D', '#EC4899', '#A47864', // 11–15 lava · coral · cherry · petal · mocha
  '#2F5D3D', '#FCD34D', '#67E8F9', '#475569', '#D946EF', // 16–20 sage · buttercream · glacier · slate · magenta
]

// EXTERNAL_PARTNER 는 KPI 탭 차단 (회사 격리 — Task #3)
const isPartner = computed(() => {
  const t = String(authStore.user?.organization?.type ?? authStore.user?.orgType ?? '').toUpperCase();
  return t === 'EXTERNAL_PARTNER';
});
const tabs = computed(() => {
  const base = ["캠페인 오버뷰", "팀 보드 보기", "검수/승인", "참여자 설정", "캠페인 성과/KPI", "매칭 탭"];
  return isPartner.value ? base.filter((t) => t !== '캠페인 성과/KPI') : base;
});

const handleTabClick = (tabName) => {
  activeTab.value = tabName
}

const campaignId = computed(() => route.params.campaignId)

// ───── 캠페인 KPI cascade contributions (Phase B)
const kpiCascadeContributions = ref([])
const kpiCascadeLoading = ref(false)
async function loadKpiCascadeContributions() {
  const cid = campaignId.value
  if (!cid) return
  kpiCascadeLoading.value = true
  try {
    const { default: api } = await import('/plugins/interceptor.js')
    const response = await api.get(`/campaigns/${cid}/kpi-contributions`)
    const payload = response?.data
    const data = payload?.data ?? payload
    kpiCascadeContributions.value = Array.isArray(data) ? data : data?.items ?? []
  } catch (error) {
    console.warn('[mock fallback] cascade contributions 가져오기 실패', error)
    kpiCascadeContributions.value = []
  } finally {
    kpiCascadeLoading.value = false
  }
}
watch(
  [() => campaignId.value, () => activeTab.value],
  ([cid, tab]) => {
    if (cid && tab === '캠페인 성과/KPI') {
      void loadKpiCascadeContributions()
    }
  },
  { immediate: true },
)

const activeCampaign = computed(() => {
  const cid = String(route.params.campaignId ?? '')
  const routeCampaign = store.campaigns.find((campaign) => campaign.id === cid)

  return routeCampaign ?? store.activeCampaign
})

/* ───── 캠페인 상태(status) 관리 ───── */
const STATUS_OPTIONS = [
  { value: 'draft',       label: '초안',   desc: '아직 운영 시작 전. 비공개 작업 단계.',      tone: 'gray' },
  { value: 'in_progress', label: '진행중', desc: '캠페인이 활성화되어 운영되는 상태.',         tone: 'emerald' },
  { value: 'recruiting',  label: '모집중', desc: '파트너 모집을 진행 중. 소개 페이지 공개.',   tone: 'blue' },
  { value: 'review',      label: '검토중', desc: '본사·계열사 승인을 기다리는 단계.',          tone: 'amber' },
  { value: 'completed',   label: '완료',   desc: '캠페인 운영이 끝나 보관함으로 이동.',        tone: 'indigo' },
  { value: 'closed',      label: '종료',   desc: '캠페인이 최종 종료되었습니다.',              tone: 'slate' },
]

const currentStatusMeta = computed(() => {
  const status = String(activeCampaign.value?.status ?? 'draft').toLowerCase()
  return STATUS_OPTIONS.find(o => o.value === status) ?? { label: status, tone: 'gray' }
})

const campaignStatusLabel = computed(() => currentStatusMeta.value.label)
const campaignStatusTone  = computed(() => currentStatusMeta.value.tone)

const currentStatus = computed(() => currentStatusMeta.value.value)


const statusColumns = [
  { id: 'backlog', label: '백로그', sub: 'Backlog' },
  { id: 'in_progress', label: '진행 중', sub: 'WIP' },
  { id: 'review', label: '검수 요청', sub: 'QA' },
  { id: 'approval_wait', label: '본사 승인 대기', sub: 'Approval' },
  { id: 'revision', label: '수정 중', sub: 'Revision' },
  { id: 'done', label: '완료', sub: 'Done' },
]

const milestoneRows = ref([])

const campaignMemberOptions = ref([])
const campaignParticipantOptions = ref([])

const teamTasks = ref([])

const taskTypeOptions = ['문서', '이미지', '영상', '웹', '광고', '브리프', '기타']
const taskPartRecords = ref([])
const taskPartOptions = computed(() => taskPartRecords.value.map((part) => part.name))
const taskPriorityOptions = [
  { id: 'low', label: '낮음' },
  { id: 'medium', label: '보통' },
  { id: 'high', label: '높음' },
  { id: 'critical', label: '긴급' },
]
const isTaskModalOpen = ref(false)
const isPartModalOpen = ref(false)
const taskFormError = ref('')
const taskDetailOpen = ref(false)
const taskDetailItem = ref(null)
const partFormError = ref('')
const partCreateType = ref('part')
const milestoneFormError = ref('')

const taskPartRows = computed(() =>
  taskPartRecords.value.map((part) => ({
    id: part.name,
    label: part.name,
    sub: `${getTasksByPart(part.name).length}건`,
    meta: part,
  })),
)

const campaignBoardRows = computed(() => (currentBoardView.value === 'milestone' ? milestoneRows.value : taskPartRows.value))

function createDefaultTaskForm() {
  return {
    id: null,
    title: '',
    status: statusColumns[0]?.id ?? 'backlog',
    milestone: milestoneRows.value[0]?.id ?? '',
    dueDate: '',
    part: taskPartOptions.value[0],
    type: taskTypeOptions[0],
    priority: 'medium',
    assigneeId: null,
    participantId: null,
    description: '',
  }
}

const taskForm = ref(createDefaultTaskForm())

function createDefaultPartForm() {
  return {
    name: '',
    milestone: milestoneRows.value[0]?.id ?? '',
    reviewFlow: '검수 요청 → 본사 승인 대기 → 수정 중 → 완료',
    priority: 'medium',
    dependency: '',
    deliverable: '',
    description: '',
    participantId: null,
  }
}

const partForm = ref(createDefaultPartForm())

function createDefaultMilestoneForm() {
  return {
    label: '',
    startDate: '',
    endDate: '',
    description: '',
  }
}
const milestoneForm = ref(createDefaultMilestoneForm())

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

const kpiStore = useCampaignKpiStore()

const overviewStats = computed(() => {
  const tasks = teamTasks.value
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return [
    { label: '전체 업무', value: tasks.length, unit: '건', tone: 'info' },
    { label: '완료', value: tasks.filter((t) => t.status === 'done').length, unit: '건', tone: 'success' },
    {
      label: '검수 대기',
      value: tasks.filter((t) => t.status === 'review').length,
      unit: '건',
      tone: 'primary',
      badge: '검수요청',
    },
    {
      label: '지연 업무',
      value: tasks.filter(
        (t) => t.status !== 'done' && t.dueDateRaw && new Date(t.dueDateRaw) < today,
      ).length,
      unit: '건',
      tone: 'warning',
      badge: '주의',
    },
  ]
})

const topKpiItems = computed(() =>
  kpiStore.items
    .filter((k) => k.actualValue != null)
    .sort((a, b) => (b.achievementPercent ?? 0) - (a.achievementPercent ?? 0))
    .slice(0, 3),
)

const esgItems = computed(() => kpiStore.items.filter((k) => k.category === 'ESG'))

const esgScore = computed(() => {
  const measured = esgItems.value.filter((k) => k.achievementPercent != null)
  if (measured.length === 0) return null
  const avg = measured.reduce((s, k) => s + k.achievementPercent, 0) / measured.length
  return Math.min(Math.round(avg), 100)
})

const esgAchievedCount = computed(() =>
  esgItems.value.filter((k) => k.status === 'ACHIEVED' || k.status === 'OVER').length,
)

const GAUGE_CIRCUMFERENCE = 2 * Math.PI * 50

const kpiGaugeStyle = computed(() => {
  const pct = Math.min(kpiStore.summary?.overallAchievementPercent ?? 0, 100)
  return {
    strokeDasharray: GAUGE_CIRCUMFERENCE,
    strokeDashoffset: GAUGE_CIRCUMFERENCE * (1 - pct / 100),
    transition: 'stroke-dashoffset 0.8s ease',
  }
})

const esgGaugeStyle = computed(() => {
  const pct = esgScore.value ?? 0
  return {
    strokeDasharray: GAUGE_CIRCUMFERENCE,
    strokeDashoffset: GAUGE_CIRCUMFERENCE * (1 - pct / 100),
    transition: 'stroke-dashoffset 0.8s ease',
  }
})

const esgRating = computed(() => {
  const s = esgScore.value
  if (s == null) return ''
  if (s >= 90) return 'AAA'
  if (s >= 80) return 'AA'
  if (s >= 70) return 'A'
  if (s >= 60) return 'BBB'
  if (s >= 50) return 'BB'
  if (s >= 40) return 'B'
  return 'CCC'
})

function statBarWidth(stat) {
  const total = overviewStats.value[0]?.value || 1
  if (stat.label === '전체 업무') return '100%'
  return total > 0 ? `${Math.min(Math.round((stat.value / total) * 100), 100)}%` : '0%'
}

function getTasksByPart(partId) {
  return teamTasks.value.filter((task) => (task.part ?? '기타') === partId)
}

function getTasksForBoardRow(rowId, statusId) {
  return teamTasks.value.filter((task) => {
    const rowMatches =
      currentBoardView.value === 'milestone'
        ? (task.milestone ?? milestoneRows.value[0]?.id) === rowId
        : (task.part ?? '기타') === rowId

    return rowMatches && task.status === statusId
  })
}


function openTaskCreateModal() {
  taskForm.value = createDefaultTaskForm()
  taskFormError.value = ''
  isTaskModalOpen.value = true
}

function closeTaskCreateModal() {
  isTaskModalOpen.value = false
  taskFormError.value = ''
}

function openPartCreateModal() {
  partCreateType.value = 'part'
  partForm.value = createDefaultPartForm()
  partFormError.value = ''
  milestoneForm.value = createDefaultMilestoneForm()
  milestoneFormError.value = ''
  isPartModalOpen.value = true
}

function closePartCreateModal() {
  isPartModalOpen.value = false
  partFormError.value = ''
  milestoneFormError.value = ''
}

function openTaskDetail(task) {
  taskDetailItem.value = task
  taskDetailOpen.value = true
}

function closeTaskDetail() {
  taskDetailOpen.value = false
}

function isStepDone(stepId, currentStatus) {
  const order = ['backlog', 'in_progress', 'review', 'approval_wait', 'revision', 'done']
  return order.indexOf(stepId) < order.indexOf(currentStatus)
}

function milestoneLabelById(milestoneId) {
  return milestoneRows.value.find((m) => m.id === milestoneId)?.label ?? milestoneId
}

function editFromDetail() {
  if (!taskDetailItem.value) return
  const t = taskDetailItem.value

  const milestoneRow = milestoneRows.value.find((m) => m.label === t.milestone)
  const partRow = taskPartRecords.value.find((p) => p.name === t.part)

  taskForm.value = {
    ...createDefaultTaskForm(),
    id: t.id ?? null,
    title: t.title ?? '',
    status: t.status ?? statusColumns[0]?.id ?? 'backlog',
    milestone: milestoneRow?.id ?? '',
    part: partRow?.name ?? taskPartOptions.value[0] ?? '',
    type: t.type || taskTypeOptions[0],
    priority: t.priority ?? 'medium',
    dueDate: t.dueDateRaw ? String(t.dueDateRaw).slice(0, 10) : '',
    assigneeId: t.assigneeIdx ?? null,
    participantId: t.participantIdx ?? null,
    description: t.description ?? '',
  }
  taskFormError.value = ''
  closeTaskDetail()
  isTaskModalOpen.value = true
}

async function addTaskPart() {
  const name = partForm.value.name.trim()

  if (!name) {
    partFormError.value = '업무 파트명을 입력해주세요.'
    return
  }

  const isDuplicated = taskPartOptions.value.some((part) => part.toLowerCase() === name.toLowerCase())

  if (isDuplicated) {
    partFormError.value = '이미 등록된 업무 파트입니다.'
    return
  }

  const milestoneId = Number(partForm.value.milestone) || null
  if (!milestoneId) {
    partFormError.value = '기준 마일스톤을 선택해주세요.'
    return
  }

  const campaignId = route.params.campaignId
  if (!campaignId) return

  try {
    const result = await CreateTaskPart(campaignId, milestoneId, {
      name,
      reviewFlow: partForm.value.reviewFlow.trim() || null,
      taskPriority: (partForm.value.priority ?? 'medium').toUpperCase(),
      dependency: partForm.value.dependency.trim() || null,
      deliverable: partForm.value.deliverable.trim() || null,
      description: partForm.value.description.trim() || null,
      sortOrder: taskPartRecords.value.length,
      participantId: partForm.value.participantId ?? null,
    })

    taskPartRecords.value = [
      ...taskPartRecords.value,
      {
        id: `part-${result.idx}`,
        name: result.name,
        milestone: partForm.value.milestone,
        reviewFlow: result.reviewFlow ?? '',
        priority: (result.taskPriority ?? 'MEDIUM').toLowerCase(),
        dependency: result.dependency ?? '',
        deliverable: result.deliverable ?? '',
        description: result.description ?? '',
      },
    ]

    currentBoardView.value = 'part'
    closePartCreateModal()
  } catch (error) {
    partFormError.value = error?.message ?? '업무 파트 생성에 실패했습니다.'
  }
}

async function addMilestone() {
  const label = milestoneForm.value.label.trim()

  if (!label) {
    milestoneFormError.value = '마일스톤명을 입력해주세요.'
    return
  }

  const isDuplicated = milestoneRows.value.some((m) => m.label.toLowerCase() === label.toLowerCase())
  if (isDuplicated) {
    milestoneFormError.value = '이미 등록된 마일스톤입니다.'
    return
  }

  const campaignId = route.params.campaignId
  if (!campaignId) return

  try {
    const result = await CreateMilestone(campaignId, {
      name: label,
      startDate: toIsoDateTime(milestoneForm.value.startDate),
      endDate: toIsoDateTime(milestoneForm.value.endDate),
      description: milestoneForm.value.description.trim() || null,
      sortOrder: milestoneRows.value.length,
    })

    let sub = ''
    if (milestoneForm.value.endDate) {
      const parts = milestoneForm.value.endDate.split('-')
      sub = parts.length === 3 ? `~ ${parts[1]}.${parts[2]}` : ''
    }

    milestoneRows.value = [
      ...milestoneRows.value,
      {
        id: String(result.idx),
        label: result.name,
        sub,
        startDate: milestoneForm.value.startDate,
        endDate: milestoneForm.value.endDate,
        description: milestoneForm.value.description.trim(),
      },
    ]

    currentBoardView.value = 'milestone'
    closePartCreateModal()
  } catch (error) {
    milestoneFormError.value = error?.message ?? '마일스톤 생성에 실패했습니다.'
  }
}

function formatSysDate(dt) {
  if (!dt) return '-'
  const d = new Date(dt)
  if (Number.isNaN(d.getTime())) return '-'
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatTaskDueLabel(value) {
  const normalizedValue = String(value ?? '').trim()

  if (!normalizedValue) {
    return '일정 미정'
  }

  const dateParts = normalizedValue.split('-')

  if (dateParts.length === 3) {
    return `${dateParts[1]}.${dateParts[2]}`
  }

  return normalizedValue
}

function statusLabelById(statusId) {
  return statusColumns.find((column) => column.id === statusId)?.label ?? '작성중'
}

function priorityLabelById(priorityId) {
  return taskPriorityOptions.find((option) => option.id === priorityId)?.label ?? '보통'
}


function createOwnerInitial(task) {
  const explicitInitial = task.ownerInitial?.trim()

  if (explicitInitial) {
    return explicitInitial
  }

  const ownerName = task.ownerName?.trim()

  if (ownerName) {
    return ownerName.slice(0, 1)
  }

  return 'N'
}

function getTaskTone(task) {
  if (task.status === 'blocked' || task.status === 'revision') {
    return 'warning'
  }

  if (task.status === 'done') {
    return 'success'
  }

  return 'info'
}

async function addTeamTask() {
  const title = taskForm.value.title.trim()

  if (!title) {
    taskFormError.value = '업무명을 입력해주세요.'
    return
  }

  const campaignId = route.params.campaignId
  if (!campaignId) return

  const partRecord = taskPartRecords.value.find((p) => p.name === taskForm.value.part)
  const taskPartId = partRecord ? Number(partRecord.id.replace('part-', '')) || null : null
  const milestoneId = Number(taskForm.value.milestone) || null

  const payload = {
    name: title,
    participantId: taskForm.value.participantId ?? null,
    dueDate: toIsoDateTime(taskForm.value.dueDate),
    taskType: TYPE_TO_BACKEND[taskForm.value.type] ?? 'OTHER',
    status: STATUS_TO_BACKEND[taskForm.value.status] ?? 'BACKLOG',
    taskPartId,
    milestoneId,
    assigneeId: taskForm.value.assigneeId ?? null,
    priority: (taskForm.value.priority ?? 'medium').toUpperCase(),
    memo: taskForm.value.description.trim() || null,
  }

  const existingId = Number(taskForm.value.id) || null

  try {
    const result = existingId
      ? await UpdateTask(existingId, payload)
      : await CreateTask(campaignId, payload)

    const mapped = {
      id: String(result.idx),
      status: TEAMBOARD_STATUS_MAP[result.status] ?? 'backlog',
      milestone: result.milestoneName ?? '',
      title: result.name,
      part: result.taskPartName ?? taskForm.value.part ?? '',
      type: taskForm.value.type,
      priority: TEAMBOARD_PRIORITY_MAP[result.priority] ?? 'medium',
      dueDate: result.dueDate ? formatTeamboardDate(result.dueDate) : '',
      dueDateRaw: result.dueDate ?? '',
      ownerName: result.assigneeName ?? '',
      ownerInitial: result.assigneeName?.charAt(0) ?? '?',
      assigneeIdx: result.assigneeIdx ?? null,
      participantIdx: result.participantIdx ?? null,
      description: result.memo ?? taskForm.value.description.trim(),
    }

    if (existingId) {
      const idx = teamTasks.value.findIndex((t) => t.id === String(existingId))
      if (idx !== -1) {
        teamTasks.value.splice(idx, 1, mapped)
      } else {
        teamTasks.value.unshift(mapped)
      }
    } else {
      teamTasks.value.unshift(mapped)
    }

    closeTaskCreateModal()
  } catch (error) {
    taskFormError.value = error?.message ?? '업무 저장에 실패했습니다.'
  }
}

function syncMetadataDraft() {
  metadataDraft.value = {
    name: activeCampaign.value?.name ?? '2024 글로벌 썸머 프로모션 캠페인',
    startDate: activeCampaign.value?.startDate ?? '2024-06-01',
    endDate: activeCampaign.value?.endDate ?? '2024-08-31',
    summary:
      activeCampaign.value?.purpose ??
      '여름 시즌을 맞이하여 북미 및 아시아 시장을 타겟으로 한 대규모 할인 프로모션 및 신제품 런칭 캠페인.',
    color: activeCampaign.value?.color ?? '',
    status: activeCampaign.value?.status ?? 'draft',
  }
}

async function saveMetadata() {
  const campaignId = activeCampaign.value?.id
  if (!campaignId) {
    return
  }

  if (!canEditMetadata.value) {
    console.warn('캠페인 편집 권한 없음', {
      organizationIsPm: organizationIsPm.value,
      myCampaignRole: myCampaignRole.value,
    })
    alert('캠페인을 편집할 권한이 없습니다. (PM사 매니저/총괄 매니저만 가능)')
    metadataEditing.value = false
    return
  }

  // 백엔드 UpsertReq 형식. 폼에서 안 건드는 필드는 기존 값 유지.
  const payload = {
    name: metadataDraft.value.name,
    purpose: metadataDraft.value.summary,
    tags: Array.isArray(activeCampaign.value.tags) ? activeCampaign.value.tags : [],
    startDate: metadataDraft.value.startDate || null,
    endDate: metadataDraft.value.endDate || null,
    goals: activeCampaign.value.goals ?? null,
    color: metadataDraft.value.color || activeCampaign.value.color || null,
  }

  isSaving.value = true
  try {
    const updated = await UpdateCampaign(campaignId, payload)

    const nextStatus = metadataDraft.value.status
    if (nextStatus && nextStatus !== currentStatus.value) {
      await UpdateCampaignStatus(campaignId, nextStatus)
    }

    store.updateCampaign(campaignId, {
      ...activeCampaign.value,
      ...updated,
      status: nextStatus || activeCampaign.value?.status,
      id: String(updated.id ?? updated.idx ?? campaignId),
    })

    metadataEditing.value = false
    activeTab.value = '캠페인 오버뷰'
  } catch (error) {
    console.error('캠페인 메타데이터 저장 실패', error)
    const httpStatus = error?.response?.status
    const serverMsg = error?.response?.data?.message ?? error?.message
    if (httpStatus === 403) {
      alert('캠페인을 편집할 권한이 없습니다.')
    } else if (httpStatus === 400) {
      alert(serverMsg || '입력값을 확인해 주세요. (이름은 필수, 날짜 형식 YYYY-MM-DD)')
    } else {
      alert('캠페인 정보를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.')
    }
  } finally {
    isSaving.value = false
  }
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

async function loadCampaignTeamboard(campaignId) {
  if (!campaignId) return

  try {
    const [milestonesData, taskPartsData, tasksData, membersRes, participantsRes] = await Promise.all([
      ListMilestones(campaignId),
      ListTaskParts(campaignId),
      ListTasksByCampaign(campaignId),
      getCampaignMembers(campaignId).then((r) => r.data?.data ?? null).catch(() => null),
      listCampaignParticipants(campaignId).then((r) => r.data?.data ?? []).catch(() => []),
    ])

    const memberPayload = membersRes ?? {}
    const membersList = Array.isArray(membersRes?.members) ? membersRes.members : []
    myCampaignRole.value = membersRes?.me?.campaignRole ?? null
    organizationIsPm.value = Boolean(membersRes?.organizationIsPm)

    campaignMemberOptions.value = membersList.map((m) => ({
      userIdx: m.userIdx,
      name: m.name,
      companyName: m.companyName,
    }))
    campaignParticipantOptions.value = Array.isArray(participantsRes)
      ? participantsRes.map((p) => ({ idx: p.idx, organizationName: p.organizationName }))
      : []

    if (Array.isArray(milestonesData) && milestonesData.length > 0) {
      milestoneRows.value = milestonesData.map((m) => ({
        id: String(m.idx),
        label: m.name,
        sub: m.endDate ? `~ ${formatTeamboardDate(m.endDate)}` : '',
      }))
    }

    if (Array.isArray(taskPartsData) && taskPartsData.length > 0) {
      taskPartRecords.value = taskPartsData.map((p) => ({
        id: `part-${p.idx}`,
        name: p.name,
        milestone: p.milestoneIdx != null ? String(p.milestoneIdx) : '',
        reviewFlow: p.reviewFlow ?? '',
        dependency: p.dependency ?? '',
        deliverable: p.deliverable ?? '',
        description: p.description ?? '',
      }))
    }

    if (Array.isArray(tasksData) && tasksData.length > 0) {
      teamTasks.value = tasksData.map((t) => ({
        id: String(t.idx),
        status: TEAMBOARD_STATUS_MAP[t.status] ?? 'backlog',
        title: t.name ?? '',
        type: t.taskType ?? '',
        dueDate: t.dueDate ? formatTeamboardDate(t.dueDate) : '',
        dueDateRaw: t.dueDate ?? '',
        ownerInitial: t.assigneeName ? t.assigneeName.charAt(0) : '?',
        ownerName: t.assigneeName ?? '',
        assigneeIdx: t.assigneeIdx ?? null,
        participantIdx: t.participantIdx ?? null,
        part: t.taskPartName ?? '',
        priority: TEAMBOARD_PRIORITY_MAP[t.priority] ?? 'medium',
        milestone: t.milestoneName ?? '',
      }))
    }
  } catch (error) {
    console.error('캠페인 팀보드 로딩 실패:', error)
  }
}

onMounted(() => {
  const initialCampaignId = route.params.campaignId
  if (initialCampaignId) {
    loadCampaignTeamboard(String(initialCampaignId))
    kpiStore.fetch(String(initialCampaignId))
  }
})

watch(
  () => route.params.campaignId,
  (campaignId) => {
    if (campaignId) {
      loadCampaignTeamboard(String(campaignId))
      kpiStore.fetch(String(campaignId))
    }
  },
)
</script>

<template>
  <section class="campaign-detail">
    <div class="hero-collapse" :class="{ 'hero-collapse--closed': heroCollapsed }" aria-hidden="false">
    <div class="hero-collapse__inner">
    <header class="campaign-hero" aria-label="캠페인 메인 페이지 헤더">
      <div class="campaign-hero__copy">
        <div class="campaign-hero__title">
          <button type="button" class="btn-back" @click="metadataEditing ? (metadataEditing = false, activeTab = '캠페인 오버뷰') : router.back()" aria-label="뒤로 가기">
            ←
          </button>
          <h1>{{ activeCampaign?.name ?? '2024 글로벌 썸머 프로모션 캠페인' }}</h1>
          <span class="status-chip" :class="`status-chip--${campaignStatusTone}`">
            <i aria-hidden="true"></i>
            {{ campaignStatusLabel }}
          </span>
        </div>
        <div class="campaign-hero__meta" aria-label="캠페인 메타데이터 요약">
          <span>{{ activeCampaign?.period ?? '2024.06.01 - 2024.08.31' }}</span>
          <span>본사 관리자 (수정 가능)</span>
        </div>
        <div class="campaign-hero__sub-actions">
          <button type="button" class="btn btn--primary" @click="activeTab = 'metadata'; metadataEditing = true">
            캠페인 편집
          </button>
        </div>
      </div>

      <div class="campaign-hero__actions">
        <router-link
          v-if="campaignId"
          :to="{ name: 'campaign-intro', params: { campaignId } }"
          class="btn btn--secondary"
        >
          캠페인 소개
        </router-link>
        <button
          v-if="canExport"
          type="button"
          class="btn btn--secondary"
          :disabled="!campaignId"
          @click="exportModalOpen = true"
        >
          내보내기
        </button>
      </div>
    </header>
    </div>
    </div>

    <CampaignExportModal
      v-if="exportModalOpen && campaignId && canExport"
      :campaign-id="campaignId"
      :campaign-name="activeCampaign?.name ?? ''"
      @close="exportModalOpen = false"
    />

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
      <button
        type="button"
        class="campaign-tabs__toggle"
        :class="{ 'campaign-tabs__toggle--collapsed': heroCollapsed }"
        :aria-label="heroCollapsed ? '헤더 펼치기' : '헤더 접기'"
        :title="heroCollapsed ? '헤더 펼치기' : '헤더 접기'"
        @click="heroCollapsed = !heroCollapsed"
      >
        <svg
          class="campaign-tabs__toggle-icon"
          width="18" height="18" viewBox="0 0 24 24"
          fill="none" stroke="currentColor"
          stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"
          aria-hidden="true"
        >
          <polyline points="18 15 12 9 6 15"></polyline>
        </svg>
      </button>
    </nav>

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

              <div class="color-picker">
                <span class="color-picker__label">캠페인 색상</span>
                <div class="color-picker__swatches" role="radiogroup" aria-label="캠페인 색상 선택">
                  <button
                    v-for="color in CAMPAIGN_PALETTE"
                    :key="color"
                    type="button"
                    class="color-swatch"
                    :class="{ 'color-swatch--active': metadataDraft.color === color }"
                    :style="{ background: color }"
                    :disabled="!metadataEditing"
                    :aria-label="`색상 ${color}`"
                    :aria-checked="metadataDraft.color === color"
                    role="radio"
                    @click="metadataDraft.color = color"
                  />
                </div>
              </div>
            </div>
          </article>

        </div>

        <aside class="stack">
          <article class="panel">
            <div class="panel__header">
              <div>
                <span class="requirement-badge">상태 전이</span>
                <h2>캠페인 상태</h2>
              </div>
              <span class="status-pill" :class="`status-pill--${(STATUS_OPTIONS.find(s => s.value === (metadataEditing ? metadataDraft.status : currentStatus)) || STATUS_OPTIONS[0]).tone}`">
                현재 · {{ (STATUS_OPTIONS.find(s => s.value === (metadataEditing ? metadataDraft.status : currentStatus)) || STATUS_OPTIONS[0]).label }}
              </span>
            </div>

            <div class="status-grid">
              <button
                v-for="opt in STATUS_OPTIONS"
                :key="opt.value"
                type="button"
                class="status-card"
                :class="[
                  `status-card--${opt.tone}`,
                  { 'is-active': (metadataEditing ? metadataDraft.status : currentStatus) === opt.value, 'is-disabled': !canEditMetadata || !metadataEditing }
                ]"
                :disabled="!canEditMetadata || !metadataEditing"
                :aria-pressed="(metadataEditing ? metadataDraft.status : currentStatus) === opt.value"
                @click="metadataDraft.status = opt.value"
              >
                <div class="status-card__head">
                  <span class="status-card__label">{{ opt.label }}</span>
                  <span v-if="currentStatus === opt.value" class="status-card__check" aria-hidden="true">✓</span>
                </div>
                <p class="status-card__desc">{{ opt.desc }}</p>
              </button>
            </div>
            <p v-if="!canEditMetadata" class="status-grid__note">상태를 변경하려면 PM 조직의 매니저/총괄 매니저 권한이 필요합니다.</p>
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
                <dd>{{ formatSysDate(activeCampaign?.createdAt) }}</dd>
              </div>
              <div>
                <dt>최근 수정</dt>
                <dd>{{ formatSysDate(activeCampaign?.updatedAt) }}</dd>
              </div>
              <div>
                <dt>내 권한</dt>
                <dd>{{
                  !myCampaignRole
                    ? '열람 전용'
                    : myCampaignRole === 'GENERAL_MANAGER'
                      ? '전체 관리'
                      : myCampaignRole === 'MANAGER'
                        ? '수정·관리'
                        : '읽기·참여'
                }}</dd>
              </div>
            </dl>
          </article>

          <div v-if="canEditMetadata" class="metadata-actions">
            <button type="button" class="btn btn--secondary" :disabled="isSaving" @click="metadataEditing = !metadataEditing">
              {{ metadataEditing ? '수정 취소' : '메타데이터 수정' }}
            </button>
            <button type="button" class="btn btn--primary" :disabled="isSaving" @click="saveMetadata">
              {{ isSaving ? '저장 중…' : '저장' }}
            </button>
          </div>
        </aside>
      </div>
    </section>

    <section v-else-if="activeTab === '캠페인 오버뷰'" class="tab-surface">

      <!-- ── 업무 현황 4-card row ───────────────────────── -->
      <div class="ov-stats">
        <article
          v-for="stat in overviewStats"
          :key="stat.label"
          class="ov-stat"
          :class="`ov-stat--${stat.tone}`"
        >
          <div class="ov-stat__top">
            <span class="ov-stat__label">{{ stat.label }}</span>
            <em v-if="stat.badge" class="ov-stat__badge">{{ stat.badge }}</em>
          </div>
          <strong class="ov-stat__value">{{ stat.value }}<small>{{ stat.unit }}</small></strong>
          <div class="ov-stat__bar-track">
            <div class="ov-stat__bar-fill" :style="{ width: statBarWidth(stat) }"></div>
          </div>
        </article>
      </div>

      <!-- ── Bento 2-col: KPI + ESG ───────────────────── -->
      <div class="ov-bento">

        <!-- KPI 성과 패널 -->
        <article class="panel ov-panel">
          <div class="panel__header">
            <h2 class="ov-panel__title">KPI 성과 요약</h2>
            <button type="button" class="link-button" @click="handleTabClick('캠페인 성과/KPI')">상세보기</button>
          </div>

          <div class="ov-kpi-hero">
            <div class="ov-gauge">
              <svg class="ov-gauge__svg" viewBox="0 0 120 120" aria-hidden="true">
                <circle class="ov-gauge__track" cx="60" cy="60" r="50"/>
                <circle class="ov-gauge__fill ov-gauge__fill--kpi" cx="60" cy="60" r="50" :style="kpiGaugeStyle"/>
              </svg>
              <div class="ov-gauge__center">
                <strong class="ov-gauge__num">{{ kpiStore.summary?.overallAchievementPercent ?? '-' }}</strong>
                <span v-if="kpiStore.summary?.overallAchievementPercent != null" class="ov-gauge__unit">%</span>
                <span class="ov-gauge__sub">전체 달성률</span>
              </div>
            </div>

            <div class="ov-kpi-stats">
              <div class="ov-kpi-stat">
                <span class="ov-kpi-stat__label">달성 지표</span>
                <strong class="ov-kpi-stat__val">
                  {{ kpiStore.summary?.achievedCount ?? 0 }}<small> / {{ kpiStore.summary?.totalKpiCount ?? 0 }}건</small>
                </strong>
              </div>
              <div class="ov-kpi-stat">
                <span class="ov-kpi-stat__label">측정 대기</span>
                <strong class="ov-kpi-stat__val">{{ kpiStore.summary?.pendingCount ?? 0 }}<small>건</small></strong>
              </div>
            </div>
          </div>

          <div class="ov-kpi-list">
            <p v-if="kpiStore.loading" class="ov-empty">불러오는 중…</p>
            <p v-else-if="topKpiItems.length === 0" class="ov-empty">
              {{ kpiStore.items.length === 0 ? '등록된 KPI가 없습니다.' : '아직 측정된 KPI가 없습니다.' }}
            </p>
            <div v-else v-for="kpi in topKpiItems" :key="kpi.idx" class="ov-kpi-row">
              <div class="ov-kpi-row__info">
                <span class="ov-kpi-row__name">{{ kpi.name }}</span>
                <span class="ov-kpi-row__actual">
                  {{ kpi.actualValue != null ? Number(kpi.actualValue).toLocaleString() : '-' }}{{ kpi.unit }} / 목표 {{ kpi.targetValue != null ? Number(kpi.targetValue).toLocaleString() : '-' }}{{ kpi.unit }}
                </span>
              </div>
              <div class="ov-kpi-row__bar-wrap">
                <div class="ov-kpi-row__bar">
                  <div
                    class="ov-kpi-row__fill"
                    :class="(kpi.achievementPercent ?? 0) >= 100 ? 'ov-fill--success' : 'ov-fill--warn'"
                    :style="{ width: Math.min(kpi.achievementPercent ?? 0, 100) + '%' }"
                  ></div>
                </div>
                <span
                  class="ov-kpi-row__pct"
                  :class="(kpi.achievementPercent ?? 0) >= 100 ? 'ov-pct--success' : 'ov-pct--warn'"
                >{{ kpi.achievementPercent != null ? kpi.achievementPercent + '%' : '-' }}</span>
              </div>
            </div>
          </div>
        </article>

        <!-- ESG 점수 패널 -->
        <article class="panel ov-panel ov-panel--esg">
          <div class="panel__header">
            <h2 class="ov-panel__title">ESG 점수</h2>
            <button type="button" class="link-button" @click="handleTabClick('캠페인 성과/KPI')">상세보기</button>
          </div>

          <div class="ov-esg-hero">
            <div class="ov-gauge ov-gauge--esg">
              <svg class="ov-gauge__svg" viewBox="0 0 120 120" aria-hidden="true">
                <circle class="ov-gauge__track" cx="60" cy="60" r="50"/>
                <circle class="ov-gauge__fill ov-gauge__fill--esg" cx="60" cy="60" r="50" :style="esgGaugeStyle"/>
              </svg>
              <div class="ov-gauge__center">
                <strong class="ov-gauge__num ov-gauge__num--esg">{{ esgScore ?? '-' }}</strong>
                <span v-if="esgScore != null" class="ov-gauge__unit">점</span>
                <span class="ov-esg-rating-badge" v-if="esgRating">{{ esgRating }}</span>
              </div>
            </div>
          </div>

          <div class="ov-esg-list">
            <p v-if="esgItems.length === 0" class="ov-empty">등록된 ESG KPI가 없습니다.</p>
            <template v-else>
              <div v-for="kpi in esgItems.slice(0, 3)" :key="kpi.idx" class="ov-kpi-row">
                <div class="ov-kpi-row__info">
                  <span class="ov-kpi-row__name">{{ kpi.name }}</span>
                  <span class="ov-kpi-row__actual">
                    {{ kpi.actualValue != null ? Number(kpi.actualValue).toLocaleString() + kpi.unit : '미측정' }}
                  </span>
                </div>
                <div class="ov-kpi-row__bar-wrap">
                  <div class="ov-kpi-row__bar">
                    <div
                      class="ov-kpi-row__fill ov-fill--esg"
                      :style="{ width: Math.min(kpi.achievementPercent ?? 0, 100) + '%' }"
                    ></div>
                  </div>
                  <span class="ov-kpi-row__pct ov-pct--esg">
                    {{ kpi.achievementPercent != null ? kpi.achievementPercent + '%' : '-' }}
                  </span>
                </div>
              </div>
              <p v-if="esgItems.length > 3" class="ov-esg-more">외 {{ esgItems.length - 3 }}건 더 있음</p>
            </template>
          </div>

          <div v-if="esgItems.length > 0" class="ov-esg-footer">
            <span class="ov-esg-footer__text">달성 {{ esgAchievedCount }}건 / 전체 {{ esgItems.length }}건</span>
            <div class="ov-esg-footer__bar">
              <div
                class="ov-esg-footer__fill"
                :style="{ width: (esgItems.length > 0 ? Math.round(esgAchievedCount / esgItems.length * 100) : 0) + '%' }"
              ></div>
            </div>
          </div>
        </article>

      </div>
    </section>

    <section v-else-if="activeTab === '팀 보드 보기'" class="tab-surface">
      <div class="board-toolbar">
        <div class="segmented-control">
          <button type="button" :class="{ active: currentBoardView === 'part' }" @click="currentBoardView = 'part'">
            업무 파트
          </button>
          <button type="button" :class="{ active: currentBoardView === 'milestone' }" @click="currentBoardView = 'milestone'">
            마일스톤
          </button>
        </div>
        <div class="board-toolbar__actions">
          <input type="search" placeholder="업무 검색..." />
          <button type="button" class="btn btn--primary" @click="openTaskCreateModal">업무 추가</button>
          <button type="button" class="btn btn--secondary" @click="openPartCreateModal">업무 파트 / 마일스톤 생성하기</button>
        </div>
      </div>

      <div
        class="board-grid"
        :style="{ '--board-column-count': statusColumns.length }"
      >
        <div class="board-grid__head">
          <span>{{ currentBoardView === 'milestone' ? '마일스톤' : '업무 파트' }}</span>
          <span v-for="column in statusColumns" :key="column.id">
            {{ column.label }} <small v-if="column.sub">({{ column.sub }})</small>
          </span>
        </div>

        <div v-for="row in campaignBoardRows" :key="row.id" class="board-grid__row">
          <div class="team-cell">
            <strong>{{ row.label }}</strong>
            <span>{{ row.sub }}</span>
            <small v-if="row.meta?.reviewFlow">검수: {{ row.meta.reviewFlow }}</small>
          </div>

          <div
            v-for="column in statusColumns"
            :key="column.id"
            class="board-cell"
          >
            <article
              v-for="task in getTasksForBoardRow(row.id, column.id)"
              :key="task.id"
              class="task-card"
              role="button"
              tabindex="0"
              @click="openTaskDetail(task)"
              @keydown.enter="openTaskDetail(task)"
            >
              <div class="task-card__top">
                <span class="status-pill" :class="`status-pill--${getTaskTone(task)}`">
                  {{ statusLabelById(task.status) }}
                </span>
                <small>{{ task.part ?? task.type }}</small>
              </div>
              <strong>{{ task.title }}</strong>
              <div class="task-card__foot">
                <span>{{ task.dueDate }}</span>
                <small v-if="task.priority" class="task-priority">{{ priorityLabelById(task.priority) }}</small>
                <em>{{ createOwnerInitial(task) }}</em>
              </div>
            </article>
          </div>
        </div>
      </div>
    </section>

    <section v-else-if="activeTab === '검수/승인'" class="tab-surface">
      <ReviewApprovalView :key="campaignId" :campaign-id="campaignId" />
    </section>

    <section v-else-if="activeTab === '참여자 설정'" class="tab-surface">
      <!-- :key — 캠페인 전환 시 재마운트 강제 (stale 데이터 버그 수정) -->
      <CampaignMembersPanel :key="campaignId" :campaign-id="campaignId" />
    </section>

    <section v-else-if="activeTab === '캠페인 성과/KPI' && !isPartner" class="tab-surface">
      <div class="kpi-tab-legend">
        <span class="kpi-tab-legend__chip kpi-tab-legend__chip--cascade">🟣 cascade</span>
        <span class="kpi-tab-legend__text">상위 KPI(본사·계열사)에 기여하는 매핑</span>
        <span class="kpi-tab-legend__divider">·</span>
        <span class="kpi-tab-legend__chip kpi-tab-legend__chip--ops">🔵 운영</span>
        <span class="kpi-tab-legend__text">캠페인 자체 metric (CTR / 노출 등)</span>
      </div>
      <CampaignKpiCascadeView
        :contributions="kpiCascadeContributions"
        :loading="kpiCascadeLoading"
        style="margin-bottom: 16px;"
      />
      <CampaignKpiTab :key="campaignId" :campaign-id="campaignId" />
    </section>

    <section v-else-if="activeTab === '매칭 탭'" class="tab-surface">
      <MatchOverview :key="campaignId" :campaign-id="campaignId" />
    </section>

    <Teleport to="body">
      <div
        v-if="isTaskModalOpen"
        class="task-modal-backdrop"
        role="presentation"
        @click.self="closeTaskCreateModal"
        @keydown.esc="closeTaskCreateModal"
      >
        <section
          class="team-task-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="team-task-modal-title"
          tabindex="-1"
        >
          <header class="team-task-modal__header">
            <div>
              <span class="requirement-badge">TEAM_BOARD_TASK</span>
              <h2 id="team-task-modal-title">업무 추가</h2>
              <p>참여사 보드에 표시할 업무 속성을 입력합니다.</p>
            </div>
            <button
              type="button"
              class="team-task-modal__close"
              aria-label="업무 추가 모달 닫기"
              @click="closeTaskCreateModal"
            >
              X
            </button>
          </header>

          <form class="team-task-form" @submit.prevent="addTeamTask">
            <label class="team-task-form__wide">
              <span>업무명</span>
              <input v-model.trim="taskForm.title" type="text" placeholder="예: SNS 배너 1차 시안 검수" />
            </label>

            <div class="team-task-form__grid">
              <label>
                <span>상태</span>
                <select v-model="taskForm.status">
                  <option v-for="column in statusColumns" :key="column.id" :value="column.id">
                    {{ column.label }}
                  </option>
                </select>
              </label>

              <label>
                <span>업무 파트</span>
                <select v-model="taskForm.part">
                  <option v-for="part in taskPartOptions" :key="part" :value="part">
                    {{ part }}
                  </option>
                </select>
              </label>

              <label>
                <span>마일스톤</span>
                <select v-model="taskForm.milestone">
                  <option v-for="milestone in milestoneRows" :key="milestone.id" :value="milestone.id">
                    {{ milestone.label }}
                  </option>
                </select>
              </label>

              <label>
                <span>마감일</span>
                <input v-model="taskForm.dueDate" type="date" />
              </label>

              <label>
                <span>업무 유형</span>
                <select v-model="taskForm.type">
                  <option v-for="type in taskTypeOptions" :key="type" :value="type">
                    {{ type }}
                  </option>
                </select>
              </label>

              <label>
                <span>우선순위</span>
                <select v-model="taskForm.priority">
                  <option
                    v-for="option in taskPriorityOptions"
                    :key="option.id"
                    :value="option.id"
                  >
                    {{ option.label }}
                  </option>
                </select>
              </label>

              <label>
                <span>담당자</span>
                <select v-model="taskForm.assigneeId">
                  <option :value="null">— 미지정 —</option>
                  <option v-for="m in campaignMemberOptions" :key="m.userIdx" :value="m.userIdx">
                    {{ m.name }} ({{ m.companyName ?? '본사' }})
                  </option>
                </select>
              </label>

              <label>
                <span>담당 참여사</span>
                <select v-model="taskForm.participantId">
                  <option :value="null">— 미지정 —</option>
                  <option v-for="p in campaignParticipantOptions" :key="p.idx" :value="p.idx">
                    {{ p.organizationName }}
                  </option>
                </select>
              </label>
            </div>

            <label class="team-task-form__wide">
              <span>업무 메모</span>
              <textarea
                v-model.trim="taskForm.description"
                rows="3"
                placeholder="산출물 기준, 검수 포인트, 파트너 요청사항을 간단히 남겨주세요."
              />
            </label>

            <p v-if="taskFormError" class="team-task-form__error">{{ taskFormError }}</p>

            <footer class="team-task-modal__actions">
              <button type="button" class="btn btn--secondary" @click="closeTaskCreateModal">취소</button>
              <button type="submit" class="btn btn--primary">추가하기</button>
            </footer>
          </form>
        </section>
      </div>
    </Teleport>

    <Teleport to="body">
      <div
        v-if="isPartModalOpen"
        class="task-modal-backdrop task-modal-backdrop--anchor-top"
        role="presentation"
        @click.self="closePartCreateModal"
        @keydown.esc="closePartCreateModal"
      >
        <section
          class="team-task-modal"
          role="dialog"
          aria-modal="true"
          aria-labelledby="team-part-modal-title"
          tabindex="-1"
        >
          <header class="team-task-modal__header">
            <div>
              <span class="requirement-badge">{{ partCreateType === 'part' ? 'TEAM_BOARD_PART' : 'TEAM_BOARD_MILESTONE' }}</span>
              <h2 id="team-part-modal-title">{{ partCreateType === 'part' ? '업무 파트 생성하기' : '마일스톤 생성하기' }}</h2>
              <p>{{ partCreateType === 'part' ? '캠페인 내부 보드에서 새 행으로 관리할 업무 도메인과 검수 기준을 정의합니다.' : '캠페인의 주요 진행 단계와 기준 일정을 설정합니다.' }}</p>
            </div>
            <button
              type="button"
              class="team-task-modal__close"
              aria-label="모달 닫기"
              @click="closePartCreateModal"
            >
              X
            </button>
          </header>

          <div class="part-modal-tabs">
            <button
              type="button"
              :class="['part-modal-tab', { 'part-modal-tab--active': partCreateType === 'part' }]"
              @click="partCreateType = 'part'"
            >
              업무 파트
            </button>
            <button
              type="button"
              :class="['part-modal-tab', { 'part-modal-tab--active': partCreateType === 'milestone' }]"
              @click="partCreateType = 'milestone'"
            >
              마일스톤
            </button>
          </div>

          <!-- 업무 파트 폼 -->
          <form v-if="partCreateType === 'part'" class="team-task-form" @submit.prevent="addTaskPart">
            <label class="team-task-form__wide">
              <span>업무 파트명</span>
              <input v-model.trim="partForm.name" type="text" placeholder="예: 예약/결제 연동, 인플루언서 운영" />
            </label>

            <div class="team-task-form__grid">
              <label>
                <span>기준 마일스톤</span>
                <select v-model="partForm.milestone">
                  <option v-for="milestone in milestoneRows" :key="milestone.id" :value="milestone.id">
                    {{ milestone.label }} ({{ milestone.sub }})
                  </option>
                </select>
              </label>

              <label>
                <span>담당 참여사</span>
                <select v-model="partForm.participantId">
                  <option :value="null">— 미지정 —</option>
                  <option v-for="p in campaignParticipantOptions" :key="p.idx" :value="p.idx">
                    {{ p.organizationName }}
                  </option>
                </select>
              </label>

              <label>
                <span>기본 검수 흐름</span>
                <select v-model="partForm.reviewFlow">
                  <option value="검수 요청 → 본사 승인 대기 → 수정 중 → 완료">
                    검수 요청 → 본사 승인 대기 → 수정 중 → 완료
                  </option>
                  <option value="초안 작성 → 검수 요청 → 수정 중 → 완료">
                    초안 작성 → 검수 요청 → 수정 중 → 완료
                  </option>
                  <option value="QA → 본사 승인 대기 → 완료">QA → 본사 승인 대기 → 완료</option>
                  <option value="운영 확인 → 검수 요청 → 완료">운영 확인 → 검수 요청 → 완료</option>
                </select>
              </label>

              <label>
                <span>기본 우선순위</span>
                <select v-model="partForm.priority">
                  <option v-for="option in taskPriorityOptions" :key="option.id" :value="option.id">
                    {{ option.label }}
                  </option>
                </select>
              </label>

              <label>
                <span>선행 업무/의존성</span>
                <input
                  v-model.trim="partForm.dependency"
                  type="text"
                  placeholder="예: 브랜드 가이드 확정, 타겟 명단 확정"
                />
              </label>

              <label>
                <span>산출물 기준</span>
                <input
                  v-model.trim="partForm.deliverable"
                  type="text"
                  placeholder="예: 배너 3종, 운영 체크리스트, QA 결과표"
                />
              </label>
            </div>

            <label class="team-task-form__wide">
              <span>파트 설명</span>
              <textarea
                v-model.trim="partForm.description"
                rows="3"
                placeholder="이 파트에서 관리할 업무 범위, 검수 포인트, 참여사 역할을 간단히 적어주세요."
              />
            </label>

            <p v-if="partFormError" class="team-task-form__error">{{ partFormError }}</p>

            <footer class="team-task-modal__actions">
              <button type="button" class="btn btn--secondary" @click="closePartCreateModal">취소</button>
              <button type="submit" class="btn btn--primary">파트 생성하기</button>
            </footer>
          </form>

          <!-- 마일스톤 폼 -->
          <form v-else class="team-task-form" @submit.prevent="addMilestone">
            <label class="team-task-form__wide">
              <span>마일스톤명</span>
              <input v-model.trim="milestoneForm.label" type="text" placeholder="예: 기획 확정, 런칭 준비, 운영 완료" />
            </label>

            <div class="team-task-form__grid">
              <label>
                <span>시작일</span>
                <input v-model="milestoneForm.startDate" type="date" />
              </label>

              <label>
                <span>종료(목표)일</span>
                <input v-model="milestoneForm.endDate" type="date" />
              </label>
            </div>

            <label class="team-task-form__wide">
              <span>마일스톤 설명</span>
              <textarea
                v-model.trim="milestoneForm.description"
                rows="3"
                placeholder="이 마일스톤에서 달성해야 할 목표, 산출물 기준, 완료 조건을 적어주세요."
              />
            </label>

            <p v-if="milestoneFormError" class="team-task-form__error">{{ milestoneFormError }}</p>

            <footer class="team-task-modal__actions">
              <button type="button" class="btn btn--secondary" @click="closePartCreateModal">취소</button>
              <button type="submit" class="btn btn--primary">마일스톤 생성하기</button>
            </footer>
          </form>
        </section>
      </div>
    </Teleport>

    <Teleport to="body">
      <Transition name="cd-detail">
        <div
          v-if="taskDetailOpen && taskDetailItem"
          class="cd-detail-overlay"
          role="presentation"
          @click.self="closeTaskDetail"
          @keydown.esc="closeTaskDetail"
        >
          <aside
            class="cd-detail-panel"
            role="dialog"
            aria-modal="true"
            aria-labelledby="cd-detail-title"
            tabindex="-1"
          >
            <header class="cd-detail-header">
              <div class="cd-detail-header__top">
                <div class="cd-detail-badges">
                  <span class="cd-badge" :class="`cd-badge--${getTaskTone(taskDetailItem)}`">
                    {{ statusLabelById(taskDetailItem.status) }}
                  </span>
                </div>
                <button type="button" class="cd-detail-close" aria-label="닫기" @click="closeTaskDetail">✕</button>
              </div>
              <h2 id="cd-detail-title">{{ taskDetailItem.title }}</h2>
            </header>

            <div class="cd-detail-tracker">
              <div
                v-for="step in statusColumns"
                :key="step.id"
                class="cd-tracker-step"
                :class="{
                  'cd-tracker-step--active': step.id === taskDetailItem.status,
                  'cd-tracker-step--done': isStepDone(step.id, taskDetailItem.status),
                }"
              >
                <div class="cd-tracker-dot" />
                <span>{{ step.label }}</span>
              </div>
            </div>

            <div class="cd-detail-body">
              <dl class="cd-detail-grid">
                <div class="cd-detail-item">
                  <dt>업무 파트</dt>
                  <dd>{{ taskDetailItem.part ?? '-' }}</dd>
                </div>
                <div class="cd-detail-item">
                  <dt>마일스톤</dt>
                  <dd>{{ milestoneLabelById(taskDetailItem.milestone) ?? '-' }}</dd>
                </div>
                <div class="cd-detail-item">
                  <dt>업무 유형</dt>
                  <dd>{{ taskDetailItem.type ?? '-' }}</dd>
                </div>
                <div class="cd-detail-item">
                  <dt>우선순위</dt>
                  <dd class="cd-priority" :data-priority="taskDetailItem.priority">
                    {{ priorityLabelById(taskDetailItem.priority) ?? '-' }}
                  </dd>
                </div>
                <div class="cd-detail-item">
                  <dt>마감일</dt>
                  <dd>{{ taskDetailItem.dueDate ?? '미정' }}</dd>
                </div>
              </dl>

              <div class="cd-detail-section">
                <h3>담당자</h3>
                <div class="cd-assignee">
                  <span class="cd-assignee__avatar">{{ createOwnerInitial(taskDetailItem) }}</span>
                  <div>
                    <strong>{{ taskDetailItem.ownerName || createOwnerInitial(taskDetailItem) }}</strong>
                  </div>
                </div>
              </div>

              <div v-if="taskDetailItem.description" class="cd-detail-section">
                <h3>업무 메모</h3>
                <p class="cd-memo">{{ taskDetailItem.description }}</p>
              </div>
            </div>

            <footer class="cd-detail-footer">
              <button type="button" class="btn btn--secondary" @click="closeTaskDetail">닫기</button>
              <button type="button" class="btn btn--primary" @click="editFromDetail">업무 수정</button>
            </footer>
          </aside>
        </div>
      </Transition>
    </Teleport>
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
  flex-direction: column;
  gap: 18px;
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

.btn-back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md, 8px);
  border: 1px solid var(--border-color);
  background: var(--surface-1, transparent);
  color: var(--text-secondary);
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s, color 0.15s;
}
.btn-back:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
}

.edit-back-bar {
  margin-bottom: 16px;
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

.campaign-hero__sub-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.campaign-hero__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: stretch;
  min-width: 120px;
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

/* ─── Hero Toggle Button (iOS-style) ─────────────────────────── */
.campaign-tabs__toggle {
  margin-left: auto;
  flex: 0 0 auto;
  width: 36px;
  height: 36px;
  margin-right: 6px;
  align-self: center;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--border-color);
  border-radius: 10px;
  background: var(--panel-color);
  color: var(--text-secondary);
  cursor: pointer;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06), 0 1px 1px rgba(0, 0, 0, 0.04);
  transition:
    transform 0.15s ease,
    background 0.15s ease,
    border-color 0.15s ease,
    color 0.15s ease,
    box-shadow 0.15s ease;
  position: sticky;
  right: 6px;
  z-index: 2;
}
.campaign-tabs__toggle:hover {
  color: var(--color-primary-600);
  background: var(--color-primary-50);
  border-color: var(--color-primary-300);
  transform: translateY(-1px);
  box-shadow: 0 3px 8px rgba(124, 58, 237, 0.18);
}
.campaign-tabs__toggle:active {
  transform: translateY(0) scale(0.96);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}
.campaign-tabs__toggle:focus-visible {
  outline: 2px solid var(--color-primary-500);
  outline-offset: 2px;
}
.campaign-tabs__toggle-icon {
  display: block;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.campaign-tabs__toggle--collapsed .campaign-tabs__toggle-icon {
  transform: rotate(180deg);
}

/* ─── Hero Collapse (grid-template-rows 트릭으로 자연스러운 슬라이드) ─── */
.hero-collapse {
  display: grid;
  grid-template-rows: 1fr;
  transition: grid-template-rows 0.4s cubic-bezier(0.32, 0.72, 0, 1);
}
.hero-collapse--closed {
  grid-template-rows: 0fr;
}
.hero-collapse__inner {
  overflow: hidden;
  min-height: 0;
}
/* 헤더 내부 콘텐츠가 위로 빨려 올라가는 느낌 추가 */
.hero-collapse__inner .campaign-hero {
  transition: transform 0.4s cubic-bezier(0.32, 0.72, 0, 1);
  transform-origin: top center;
}
.hero-collapse--closed .campaign-hero {
  transform: translateY(-12px);
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  border-radius: var(--radius-md);
  cursor: pointer;
  font-size: 13px;
  font-weight: 750;
  padding: 0 14px;
  white-space: nowrap;
  text-decoration: none;
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

/* 캠페인 상태 칩 tone 변형 */
.status-chip--gray    { background: rgba(148,163,184,0.16); color: #475569; }
.status-chip--emerald { background: rgba(16,185,129,0.16);  color: #047857; }
.status-chip--blue    { background: rgba(59,130,246,0.16);  color: #1D4ED8; }
.status-chip--amber   { background: rgba(245,158,11,0.16);  color: #B45309; }
.status-chip--indigo  { background: rgba(99,102,241,0.16);  color: #4338CA; }
.status-chip--slate   { background: rgba(100,116,139,0.16); color: #334155; }
:root[data-theme='dark'] .status-chip--gray    { background: rgba(148,163,184,0.22); color: #CBD5E1; }
:root[data-theme='dark'] .status-chip--emerald { background: rgba(16,185,129,0.22);  color: #6EE7B7; }
:root[data-theme='dark'] .status-chip--blue    { background: rgba(59,130,246,0.22);  color: #93C5FD; }
:root[data-theme='dark'] .status-chip--amber   { background: rgba(245,158,11,0.22);  color: #FCD34D; }
:root[data-theme='dark'] .status-chip--indigo  { background: rgba(99,102,241,0.22);  color: #A5B4FC; }
:root[data-theme='dark'] .status-chip--slate   { background: rgba(148,163,184,0.22); color: #CBD5E1; }

/* ─── 캠페인 상태 변경 그리드 ─── */
.status-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 10px;
}
.status-grid__note {
  margin: 12px 0 0;
  font-size: 12px;
  color: var(--muted-text);
  font-weight: 600;
}
.status-card {
  text-align: left;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  cursor: pointer;
  font-family: inherit;
  color: var(--text-primary);
  transition: border-color 0.15s ease, background 0.15s ease, transform 0.15s ease;
  display: flex; flex-direction: column; gap: 4px;
}
.status-card:hover:not(.is-disabled):not(.is-active) {
  transform: translateY(-1px);
  border-color: color-mix(in srgb, var(--color-primary-500) 32%, var(--border-color));
  background: var(--panel-muted);
}
.status-card.is-active {
  cursor: default;
  border-width: 2px;
  padding: 11px 13px;
}
.status-card.is-disabled { cursor: not-allowed; opacity: 0.6; }
.status-card__head {
  display: flex; justify-content: space-between; align-items: center;
}
.status-card__label { font-size: 13px; font-weight: 800; }
.status-card__check {
  font-size: 14px; font-weight: 900;
  color: var(--color-primary-700);
}
.status-card__desc {
  margin: 0; font-size: 11px; line-height: 1.45;
  color: var(--muted-text);
}

/* tone별 색상 */
.status-card--gray.is-active     { border-color: #94A3B8; background: rgba(148,163,184,0.08); }
.status-card--amber.is-active    { border-color: #F59E0B; background: rgba(245,158,11,0.08); }
.status-card--emerald.is-active  { border-color: #10B981; background: rgba(16,185,129,0.10); }
.status-card--slate.is-active    { border-color: #64748B; background: rgba(100,116,139,0.08); }
.status-card--indigo.is-active   { border-color: #6366F1; background: rgba(99,102,241,0.10); }
.status-card--blue.is-active     { border-color: #3B82F6; background: rgba(59,130,246,0.10); }

/* status-pill tone variants — 패널 헤더 우측 표시 */
.status-pill--gray    { background: rgba(148,163,184,0.16); color: #475569; }
.status-pill--amber   { background: rgba(245,158,11,0.16);  color: #B45309; }
.status-pill--emerald { background: rgba(16,185,129,0.16);  color: #047857; }
.status-pill--slate   { background: rgba(100,116,139,0.16); color: #334155; }
.status-pill--indigo  { background: rgba(99,102,241,0.16);  color: #4338CA; }
.status-pill--blue    { background: rgba(59,130,246,0.16);  color: #1D4ED8; }
:root[data-theme='dark'] .status-pill--gray    { background: rgba(148,163,184,0.22); color: #CBD5E1; }
:root[data-theme='dark'] .status-pill--amber   { background: rgba(245,158,11,0.22);  color: #FCD34D; }
:root[data-theme='dark'] .status-pill--emerald { background: rgba(16,185,129,0.22);  color: #6EE7B7; }
:root[data-theme='dark'] .status-pill--slate   { background: rgba(148,163,184,0.22); color: #CBD5E1; }
:root[data-theme='dark'] .status-pill--indigo  { background: rgba(99,102,241,0.22);  color: #A5B4FC; }
:root[data-theme='dark'] .status-pill--blue    { background: rgba(59,130,246,0.22);  color: #93C5FD; }

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

.color-picker {
  display: grid;
  gap: 8px;
}

.color-picker__label {
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.color-picker__swatches {
  display: grid;
  grid-template-columns: repeat(10, minmax(0, 1fr));
  gap: 8px;
}

.color-swatch {
  position: relative;
  aspect-ratio: 1 / 1;
  width: 100%;
  border: 2px solid transparent;
  border-radius: 8px;
  padding: 0;
  cursor: pointer;
  transition: transform 0.12s ease, border-color 0.12s ease, box-shadow 0.12s ease;
}

.color-swatch:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.18);
}

.color-swatch:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.color-swatch--active {
  border-color: var(--text-primary, #111);
  box-shadow: 0 0 0 2px var(--panel-bg, #fff), 0 0 0 4px var(--text-primary, #111);
}

.color-swatch--active::after {
  content: '✓';
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 14px;
  font-weight: 900;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.35);
}

label {
  display: grid;
  gap: 7px;
  color: var(--text-secondary);
  font-size: 13px;
  font-weight: 700;
}

input,
textarea,
select {
  width: 100%;
  min-width: 0;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--control-color);
  color: var(--text-primary);
  font-size: 13px;
  padding: 10px 12px;
}

input,
select {
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

.activity-list,
.candidate-list,
.meta-list {
  display: grid;
  gap: 10px;
}

.activity-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

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

.person-cell strong,
.data-table__row strong,
.reference-card h3,
.task-card strong {
  display: block;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 800;
}

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

.task-card__foot .warning {
  color: var(--campaign-warning-text);
}

.link-button {
  color: var(--campaign-primary-text);
  cursor: pointer;
  font-size: 13px;
  font-weight: 800;
}

/* 최근 성과 요약 */
.kpi-overview-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 16px;
}

.kpi-overview-summary div {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 12px 14px;
}

.kpi-overview-summary span {
  display: block;
  color: var(--muted-text);
  font-size: 12px;
}

.kpi-overview-summary strong {
  display: block;
  margin-top: 4px;
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 850;
}

.performance-empty {
  padding: 32px 0;
  text-align: center;
  color: var(--muted-text);
  font-size: 13px;
}

.performance-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.perf-kpi-card {
  display: grid;
  gap: 6px;
  border: 1px solid var(--border-color);
  border-left: 4px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
  padding: 14px;
}

.perf-kpi-card--success {
  border-left-color: var(--color-success);
}

.perf-kpi-card--warning {
  border-left-color: var(--color-warning);
}

.perf-kpi-card__name {
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 700;
}

.perf-kpi-card__value {
  color: var(--text-primary);
  font-size: 22px;
  font-weight: 850;
  line-height: 1.2;
}

.perf-kpi-card__value small {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 600;
  margin-left: 3px;
}

.perf-kpi-card__badge {
  display: inline-flex;
  width: fit-content;
  border-radius: var(--radius-sm);
  font-size: 11px;
  font-weight: 800;
  padding: 2px 8px;
}

.badge--success {
  background: var(--campaign-success-surface);
  color: var(--campaign-success-text);
}

.badge--warning {
  background: color-mix(in srgb, var(--color-warning) 15%, transparent);
  color: var(--campaign-warning-text);
}

.perf-kpi-card__target {
  color: var(--muted-text);
  font-size: 12px;
}

/* ESG 점수 패널 */
.esg-panel {
  margin-top: 16px;
}

.esg-score-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.esg-score-display {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.esg-score-value {
  font-size: 40px;
  font-weight: 850;
  line-height: 1;
  color: var(--text-primary);
}

.esg-score-unit {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-secondary);
}

.esg-progress-track {
  height: 8px;
  border-radius: 999px;
  background: var(--border-color);
  overflow: hidden;
}

.esg-progress-fill {
  height: 100%;
  border-radius: 999px;
  background: var(--color-success);
  transition: width 0.4s ease;
}

.esg-score-meta {
  font-size: 13px;
  color: var(--muted-text);
  margin: 0;
}

.esg-score-empty {
  font-size: 13px;
  color: var(--muted-text);
  margin: 0;
  padding: 8px 0;
}

/* ── Campaign Overview (ov-*) ─────────────────────────── */

.ov-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.ov-stat {
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: var(--shadow-sm);
  padding: 20px 22px 18px;
  overflow: hidden;
  transition: box-shadow var(--transition-fast), border-color var(--transition-fast);
}

.ov-stat:hover {
  border-color: var(--border-strong);
  box-shadow: var(--shadow-md);
}

.ov-stat::before {
  content: '';
  position: absolute;
  inset: 0 auto 0 0;
  width: 4px;
  border-radius: var(--radius-lg) 0 0 var(--radius-lg);
}

.ov-stat--info::before    { background: var(--color-info); }
.ov-stat--success::before { background: var(--color-success); }
.ov-stat--primary::before { background: var(--color-primary-500); }
.ov-stat--warning::before { background: var(--color-warning); }

.ov-stat__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.ov-stat__label {
  font-size: 11px;
  font-weight: 700;
  color: var(--muted-text);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.ov-stat__badge {
  display: inline-flex;
  border-radius: var(--radius-sm);
  font-size: 10px;
  font-style: normal;
  font-weight: 800;
  padding: 2px 7px;
}

.ov-stat--primary .ov-stat__badge {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}

.ov-stat--warning .ov-stat__badge {
  background: color-mix(in srgb, var(--color-warning) 18%, transparent);
  color: var(--color-warning-dark);
}

.ov-stat__value {
  font-size: 38px;
  font-weight: 850;
  line-height: 1;
  color: var(--text-primary);
  letter-spacing: -1px;
}

.ov-stat__value small {
  font-size: 14px;
  font-weight: 700;
  color: var(--muted-text);
  margin-left: 3px;
  letter-spacing: 0;
}

.ov-stat__bar-track {
  height: 3px;
  border-radius: 999px;
  background: var(--border-color);
  overflow: hidden;
}

.ov-stat__bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.6s ease;
}

.ov-stat--info .ov-stat__bar-fill    { background: var(--color-info); }
.ov-stat--success .ov-stat__bar-fill { background: var(--color-success); }
.ov-stat--primary .ov-stat__bar-fill { background: var(--color-primary-500); }
.ov-stat--warning .ov-stat__bar-fill { background: var(--color-warning); }

/* Bento 2-col */
.ov-bento {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 16px;
  align-items: start;
}

/* Panel shared */
.ov-panel {
  gap: 20px;
}

.ov-panel__title {
  font-size: 15px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.2px;
}

/* KPI hero row */
.ov-kpi-hero {
  display: flex;
  align-items: center;
  gap: 28px;
}

/* Gauge */
.ov-gauge {
  position: relative;
  width: 114px;
  height: 114px;
  flex-shrink: 0;
}

.ov-gauge--esg {
  width: 136px;
  height: 136px;
  margin: 0 auto;
}

.ov-gauge__svg {
  width: 100%;
  height: 100%;
  transform: rotate(-90deg);
}

.ov-gauge__track {
  fill: none;
  stroke: var(--border-color);
  stroke-width: 9;
}

.ov-gauge__fill {
  fill: none;
  stroke-width: 9;
  stroke-linecap: round;
}

.ov-gauge__fill--kpi { stroke: var(--color-primary-500); }
.ov-gauge__fill--esg { stroke: var(--color-success); }

.ov-gauge__center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1px;
}

.ov-gauge__num {
  font-size: 26px;
  font-weight: 850;
  line-height: 1;
  color: var(--text-primary);
  letter-spacing: -1px;
}

.ov-gauge__num--esg {
  font-size: 30px;
}

.ov-gauge__unit {
  font-size: 11px;
  font-weight: 700;
  color: var(--muted-text);
}

.ov-gauge__sub {
  font-size: 9px;
  font-weight: 700;
  color: var(--muted-text);
  text-transform: uppercase;
  letter-spacing: 0.4px;
  margin-top: 2px;
}

/* KPI stats */
.ov-kpi-stats {
  display: flex;
  flex-direction: column;
  gap: 0;
  flex: 1;
}

.ov-kpi-stat {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 0;
  border-bottom: 1px solid var(--border-color);
}

.ov-kpi-stat:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.ov-kpi-stat:first-child {
  padding-top: 0;
}

.ov-kpi-stat__label {
  font-size: 11px;
  font-weight: 700;
  color: var(--muted-text);
  text-transform: uppercase;
  letter-spacing: 0.4px;
}

.ov-kpi-stat__val {
  font-size: 26px;
  font-weight: 850;
  color: var(--text-primary);
  line-height: 1;
  letter-spacing: -0.5px;
}

.ov-kpi-stat__val small {
  font-size: 13px;
  font-weight: 600;
  color: var(--muted-text);
  margin-left: 2px;
  letter-spacing: 0;
}

/* KPI / ESG row list */
.ov-kpi-list,
.ov-esg-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  border-top: 1px solid var(--border-color);
  padding-top: 18px;
}

.ov-kpi-row {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.ov-kpi-row__info {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.ov-kpi-row__name {
  font-size: 13px;
  font-weight: 700;
  color: var(--text-secondary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}

.ov-kpi-row__actual {
  font-size: 11px;
  color: var(--muted-text);
  white-space: nowrap;
  flex-shrink: 0;
}

.ov-kpi-row__bar-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.ov-kpi-row__bar {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: var(--border-color);
  overflow: hidden;
}

.ov-kpi-row__fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.5s ease;
}

.ov-fill--success { background: var(--color-success); }
.ov-fill--warn    { background: var(--color-warning); }
.ov-fill--esg     { background: var(--color-success); }

.ov-kpi-row__pct {
  min-width: 40px;
  text-align: right;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: -0.3px;
}

.ov-pct--success { color: var(--color-success-dark); }
.ov-pct--warn    { color: var(--color-warning-dark); }
.ov-pct--esg     { color: var(--color-success-dark); }

.ov-empty {
  padding: 16px 0 4px;
  text-align: center;
  font-size: 13px;
  color: var(--muted-text);
  margin: 0;
}

/* ESG hero */
.ov-esg-hero {
  display: flex;
  justify-content: center;
  padding: 4px 0;
}

.ov-esg-rating-badge {
  display: inline-flex;
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--color-success) 14%, transparent);
  color: var(--color-success-dark);
  font-size: 10px;
  font-weight: 800;
  padding: 2px 8px;
  margin-top: 5px;
  letter-spacing: 1.5px;
}

.ov-esg-more {
  font-size: 12px;
  color: var(--muted-text);
  text-align: center;
  margin: 0;
  padding-top: 4px;
}

/* ESG footer */
.ov-esg-footer {
  border-top: 1px solid var(--border-color);
  padding-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.ov-esg-footer__text {
  font-size: 12px;
  color: var(--muted-text);
  font-weight: 600;
}

.ov-esg-footer__bar {
  height: 5px;
  border-radius: 999px;
  background: var(--border-color);
  overflow: hidden;
}

.ov-esg-footer__fill {
  height: 100%;
  border-radius: 999px;
  background: var(--color-success);
  transition: width 0.5s ease;
}

.board-toolbar,
.reference-toolbar {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  padding: 12px;
}

.board-context-pill {
  display: inline-flex;
  min-height: 32px;
  align-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 800;
  padding: 0 10px;
  white-space: nowrap;
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
  min-width: 1420px;
  grid-template-columns: 190px repeat(var(--board-column-count, 4), minmax(205px, 1fr));
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

.team-cell small {
  display: block;
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 750;
  line-height: 1.35;
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

.task-card__meta {
  display: grid;
  gap: 3px;
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 700;
  line-height: 1.35;
}

.task-card__company {
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 800;
}

.task-card__foot {
  flex-wrap: wrap;
}

.task-priority {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--text-secondary);
  font-size: 11px;
  font-weight: 800;
  padding: 2px 7px;
}

.task-modal-backdrop {
  --campaign-primary-surface: var(--color-primary-100);
  --campaign-primary-text: var(--color-primary-700);
  --campaign-warning-surface: var(--color-warning-light);
  --campaign-warning-text: var(--color-warning-dark);
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: grid;
  place-items: center;
  overflow-y: auto;
  background: rgba(15, 23, 42, 0.46);
  padding: 24px;
}

.task-modal-backdrop--anchor-top {
  align-items: start;
  padding-top: 80px;
}

:global(:root[data-theme='dark']) .task-modal-backdrop {
  --campaign-primary-surface: rgba(139, 92, 246, 0.18);
  --campaign-primary-text: #ddd6fe;
  --campaign-warning-surface: rgba(245, 158, 11, 0.16);
  --campaign-warning-text: #fcd34d;
}

.team-task-modal {
  width: min(720px, 100%);
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-color);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.26);
}

.team-task-modal__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  border-bottom: 1px solid var(--border-color);
  padding: 22px 24px 18px;
}

.team-task-modal__header h2 {
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 850;
}

.team-task-modal__header p {
  margin-top: 5px;
  color: var(--muted-text);
  font-size: 13px;
  line-height: 1.5;
}

.team-task-modal__close {
  display: inline-grid;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: 13px;
  font-weight: 900;
}

.part-modal-tabs {
  display: flex;
  gap: 0;
  padding: 0 24px;
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-muted);
}

.part-modal-tab {
  position: relative;
  padding: 11px 20px;
  border: none;
  background: transparent;
  color: var(--muted-text);
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.01em;
  transition: color 0.15s;
}

.part-modal-tab::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 0;
  right: 0;
  height: 2px;
  background: transparent;
  transition: background 0.15s;
}

.part-modal-tab--active {
  color: var(--color-primary-500);
}

.part-modal-tab--active::after {
  background: var(--color-primary-500);
}

.team-task-form {
  display: grid;
  gap: 16px;
  padding: 22px 24px 24px;
}

.team-task-form__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.team-task-form__wide {
  min-width: 0;
}

.team-task-form__checkbox {
  display: flex;
  align-items: center;
  gap: 9px;
  width: fit-content;
  color: var(--text-secondary);
}

.team-task-form__checkbox input {
  width: 16px;
  height: 16px;
  min-height: 0;
  accent-color: var(--color-primary-500);
}

.team-task-form__error {
  border: 1px solid color-mix(in srgb, var(--color-warning) 34%, var(--border-color));
  border-radius: var(--radius-sm);
  background: var(--campaign-warning-surface);
  color: var(--campaign-warning-text);
  font-size: 13px;
  font-weight: 750;
  padding: 10px 12px;
}

.team-task-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid var(--border-color);
  padding-top: 16px;
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

  .metadata-layout {
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
  .form-grid,
  .team-task-form__grid {
    grid-template-columns: 1fr;
  }

  .task-modal-backdrop {
    align-items: start;
    padding: 14px;
  }

  .team-task-modal__header,
  .team-task-form {
    padding-right: 18px;
    padding-left: 18px;
  }
}

/* ── Task Detail Drawer ─────────────────────────── */
.task-card {
  cursor: pointer;
  transition: box-shadow var(--transition-fast), transform var(--transition-fast);
}

.task-card:hover {
  box-shadow: 0 4px 14px color-mix(in srgb, var(--color-primary-500) 18%, transparent);
  transform: translateY(-1px);
}

.task-card:focus-visible {
  outline: 2px solid var(--color-primary-500);
  outline-offset: 2px;
}

.cd-detail-overlay {
  --campaign-primary-surface: var(--color-primary-100);
  --campaign-primary-text: var(--color-primary-700);
  --campaign-success-surface: var(--color-success-light);
  --campaign-success-text: var(--color-success-dark);
  --campaign-warning-surface: var(--color-warning-light);
  --campaign-warning-text: var(--color-warning-dark);
  position: fixed;
  inset: 0;
  z-index: 800;
  background: rgba(15, 23, 42, 0.38);
  backdrop-filter: blur(2px);
}

:global(:root[data-theme='dark']) .cd-detail-overlay {
  --campaign-primary-surface: rgba(139, 92, 246, 0.18);
  --campaign-primary-text: #ddd6fe;
  --campaign-warning-surface: rgba(245, 158, 11, 0.16);
  --campaign-warning-text: #fcd34d;
}

.cd-detail-panel {
  position: absolute;
  top: 0;
  right: 0;
  width: min(460px, 100vw);
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--panel-color);
  border-left: 1px solid var(--border-color);
  box-shadow: -20px 0 60px rgba(15, 23, 42, 0.18);
  overflow: hidden;
}

.cd-detail-header {
  flex: 0 0 auto;
  border-bottom: 1px solid var(--border-color);
  padding: 22px 24px 18px;
  background: color-mix(in srgb, var(--panel-color) 95%, var(--color-primary-500));
}

.cd-detail-header__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.cd-detail-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cd-badge {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 750;
  padding: 0 9px;
}

.cd-badge--info,
.cd-badge--primary {
  background: var(--campaign-primary-surface);
  color: var(--campaign-primary-text);
}

.cd-badge--success {
  background: var(--campaign-success-surface);
  color: var(--campaign-success-text);
}

.cd-badge--warning {
  background: var(--campaign-warning-surface);
  color: var(--campaign-warning-text);
}

.cd-badge--risk {
  background: color-mix(in srgb, var(--color-danger) 13%, transparent);
  color: var(--danger-text-strong);
}

.cd-detail-close {
  display: inline-grid;
  width: 30px;
  height: 30px;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  background: var(--panel-muted);
  color: var(--muted-text);
  cursor: pointer;
  font-size: 13px;
  font-weight: 700;
  transition: background var(--transition-fast), color var(--transition-fast);
}

.cd-detail-close:hover {
  background: var(--border-color);
  color: var(--text-primary);
}

.cd-detail-header h2 {
  color: var(--text-primary);
  font-size: 18px;
  font-weight: 850;
  line-height: 1.3;
}

.cd-detail-sub {
  margin-top: 5px;
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 600;
}

/* Status tracker */
.cd-detail-tracker {
  flex: 0 0 auto;
  display: flex;
  align-items: flex-start;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-muted);
  overflow-x: auto;
  gap: 0;
}

.cd-tracker-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  flex: 1;
  position: relative;
  font-size: 10px;
  font-weight: 650;
  color: var(--muted-text);
  text-align: center;
  white-space: nowrap;
  min-width: 60px;
}

.cd-tracker-step:not(:last-child)::after {
  content: '';
  position: absolute;
  top: 7px;
  left: 50%;
  width: 100%;
  height: 2px;
  background: var(--border-color);
  z-index: 0;
}

.cd-tracker-step--done:not(:last-child)::after {
  background: var(--color-primary-500);
}

.cd-tracker-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 2px solid var(--border-color);
  background: var(--panel-color);
  position: relative;
  z-index: 1;
  transition: border-color var(--transition-fast), background var(--transition-fast);
}

.cd-tracker-step--done .cd-tracker-dot {
  border-color: var(--color-primary-500);
  background: var(--color-primary-500);
}

.cd-tracker-step--active {
  color: var(--campaign-primary-text);
  font-weight: 800;
}

.cd-tracker-step--active .cd-tracker-dot {
  border-color: var(--color-primary-500);
  background: var(--panel-color);
  box-shadow: 0 0 0 4px color-mix(in srgb, var(--color-primary-500) 20%, transparent);
}

/* Body */
.cd-detail-body {
  flex: 1;
  overflow-y: auto;
  padding: 22px 24px;
  display: grid;
  align-content: start;
  gap: 22px;
}

/* Property grid */
.cd-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin: 0;
  padding: 16px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--panel-muted);
}

.cd-detail-item {
  display: grid;
  gap: 5px;
}

.cd-detail-item dt {
  font-size: 10px;
  font-weight: 750;
  color: var(--muted-text);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.cd-detail-item dd {
  font-size: 13px;
  font-weight: 650;
  color: var(--text-primary);
  margin: 0;
}

/* Section */
.cd-detail-section {
  display: grid;
  gap: 8px;
}

.cd-detail-section h3 {
  font-size: 10px;
  font-weight: 750;
  color: var(--muted-text);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.cd-detail-section p {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
}

/* Assignee */
.cd-assignee {
  display: flex;
  align-items: center;
  gap: 10px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  padding: 10px 12px;
  background: var(--panel-muted);
}

.cd-assignee__avatar {
  display: inline-flex;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: var(--campaign-primary-surface);
  color: var(--campaign-primary-text);
  font-size: 13px;
  font-weight: 850;
}

.cd-assignee strong {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: var(--text-primary);
}

.cd-assignee small {
  display: block;
  font-size: 11px;
  color: var(--muted-text);
  margin-top: 2px;
}

/* Deliverable */
.cd-deliverable {
  font-size: 12px;
  background: var(--panel-muted);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 9px 11px;
  font-weight: 600;
}

/* Memo */
.cd-memo {
  background: color-mix(in srgb, var(--color-primary-500) 6%, var(--panel-muted));
  border-left: 3px solid var(--color-primary-500);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.6;
}

/* Priority */
.cd-priority[data-priority='critical'] { color: var(--danger-text-strong); font-weight: 750; }
.cd-priority[data-priority='high'] { color: var(--campaign-warning-text); font-weight: 750; }
.cd-priority[data-priority='medium'] { color: var(--campaign-primary-text); }
.cd-priority[data-priority='low'] { color: var(--muted-text); }

/* Footer */
.cd-detail-footer {
  flex: 0 0 auto;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  border-top: 1px solid var(--border-color);
  padding: 16px 24px;
  background: color-mix(in srgb, var(--panel-color) 96%, var(--panel-muted));
}

/* Transition */
.cd-detail-enter-active {
  transition: opacity 0.22s ease;
}

.cd-detail-leave-active {
  transition: opacity 0.2s ease;
}

.cd-detail-enter-from,
.cd-detail-leave-to {
  opacity: 0;
}

.cd-detail-enter-active .cd-detail-panel {
  animation: cd-panel-in 0.28s cubic-bezier(0.22, 1, 0.36, 1) forwards;
}

.cd-detail-leave-active .cd-detail-panel {
  animation: cd-panel-out 0.2s ease-in forwards;
}

@keyframes cd-panel-in {
  from { transform: translateX(100%); }
  to   { transform: translateX(0); }
}

@keyframes cd-panel-out {
  from { transform: translateX(0); }
  to   { transform: translateX(100%); }
}

/* ───── KPI 탭 분류 안내 (Q2 C) ───── */
.kpi-tab-legend {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  background: var(--panel-muted);
  border: 1px solid var(--border-color);
  border-radius: 999px;
  padding: 6px 14px;
  margin-bottom: 14px;
  font-size: 11px;
  color: var(--muted-text);
}
.kpi-tab-legend__chip {
  display: inline-flex;
  align-items: center;
  height: 18px;
  padding: 0 8px;
  font-size: 10px;
  font-weight: 800;
  border-radius: 999px;
}
.kpi-tab-legend__chip--cascade {
  background: var(--color-primary-100);
  color: var(--color-primary-700);
}
.kpi-tab-legend__chip--ops {
  background: #dbeafe;
  color: #2563eb;
}
.kpi-tab-legend__text { font-weight: 600; color: var(--text-secondary); }
.kpi-tab-legend__divider { color: var(--subtle-text); margin: 0 4px; }
</style>
