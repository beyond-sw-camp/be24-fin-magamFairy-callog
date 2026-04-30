<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import { GetCampaignDetails } from '@/api/campaigns'
import {
  ListMilestones,
  ListTaskParts,
  ListTasksByCampaign,
  CreateMilestone,
  CreateTaskPart,
  CreateTask,
  UpdateTask,
} from '@/api/teamboard'

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
const store = usePlannerStore()

const activeTab = ref('캠페인 오버뷰')
const currentBoardView = ref('part')
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
  { id: 'backlog', label: '백로그', sub: 'Backlog' },
  { id: 'in_progress', label: '진행 중', sub: 'WIP' },
  { id: 'review', label: '검수 요청', sub: 'QA' },
  { id: 'approval_wait', label: '본사 승인 대기', sub: 'Approval' },
  { id: 'revision', label: '수정 중', sub: 'Revision' },
  { id: 'done', label: '완료', sub: 'Done' },
]

const milestoneRows = ref([])

const teams = []

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

const campaignTeams = computed(() => [
  ...(teams[0] ? [teams[0]] : []),
  ...partnerNames.value.map((partnerName, index) => ({
    id: `partner-${index}`,
    name: partnerName,
    role: '캠페인 협력 업무',
    progress: 50,
    color: 'blue',
  })),
])


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
    ownerName: '',
    ownerInitial: '',
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
  teams.length ? Math.round(teams.reduce((sum, team) => sum + team.progress, 0) / teams.length) : 0,
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
    ownerName: t.ownerName ?? '',
    ownerInitial: t.ownerInitial ?? '',
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
    participantId: null,
    dueDate: toIsoDateTime(taskForm.value.dueDate),
    taskType: TYPE_TO_BACKEND[taskForm.value.type] ?? 'OTHER',
    status: STATUS_TO_BACKEND[taskForm.value.status] ?? 'BACKLOG',
    taskPartId,
    milestoneId,
    assigneeId: null,
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
      ownerName: result.assigneeName ?? taskForm.value.ownerName.trim(),
      ownerInitial:
        result.assigneeName?.charAt(0) ||
        taskForm.value.ownerInitial.trim() ||
        taskForm.value.ownerName.trim().slice(0, 1) ||
        'N',
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

async function loadCampaignTeamboard(campaignId) {
  if (!campaignId) return

  try {
    const [milestonesData, taskPartsData, tasksData] = await Promise.all([
      ListMilestones(campaignId),
      ListTaskParts(campaignId),
      ListTasksByCampaign(campaignId),
    ])

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
  }
})

watch(
  () => route.params.campaignId,
  (campaignId) => {
    if (campaignId) {
      loadCampaignTeamboard(String(campaignId))
    }
  },
)
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
          <button type="button" :class="{ active: currentBoardView === 'part' }" @click="currentBoardView = 'part'">
            업무 파트
          </button>
          <button type="button" :class="{ active: currentBoardView === 'milestone' }" @click="currentBoardView = 'milestone'">
            마일스톤
          </button>
        </div>
        <div class="board-toolbar__actions">
          <span class="board-context-pill">본사 + 캠페인 참여사 {{ campaignTeams.length }}곳</span>
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
                <input
                  v-model.trim="taskForm.ownerName"
                  type="text"
                  placeholder="예: 김본사"
                />
              </label>

              <label>
                <span>담당자 이니셜</span>
                <input
                  v-model.trim="taskForm.ownerInitial"
                  type="text"
                  maxlength="2"
                  placeholder="김"
                />
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
        class="task-modal-backdrop"
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
</style>
