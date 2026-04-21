<script setup>
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import ChangeHistory from '@/components/content/ChangeHistory.vue'
import { DEFAULT_EDITOR_TEXTS, ENABLE_EDITOR_JS, useContentEditor } from '@/components/content/editor'
import { addMonths, formatMonthLabel, getMonthWeeks } from '@/utils/calendar'

const ENABLE_CONTENT_RAIL = true
const HEADER_OFFSET = 88
const RAIL_WIDTH = 64
const DEFAULT_PANEL_WIDTH = 384
const MIN_PANEL_WIDTH = 320
const MAX_PANEL_WIDTH = 560

const referenceTypes = ['전체', '이미지', '영상', '문서']
const availableTags = ['SocialMedia', 'Marketing', 'Minimalism', 'Architecture', 'Summer2024', 'DigitalStrategy', 'MZSense']
const statusOptions = [
  { value: 'planned', label: '계획', toneClass: 'border-slate-200 bg-slate-100 text-slate-700' },
  { value: 'at_risk', label: '위험', toneClass: 'border-red-200 bg-red-50 text-red-600' },
  { value: 'in_progress', label: '진행중', toneClass: 'border-blue-200 bg-blue-50 text-blue-700' },
  { value: 'review', label: '검토', toneClass: 'border-orange-200 bg-orange-50 text-orange-700' },
  { value: 'done', label: '완료', toneClass: 'border-emerald-200 bg-emerald-50 text-emerald-700' },
]
const editActionClass = 'text-[10px] leading-none !font-bold !text-red-600 opacity-0 transition-opacity duration-200 group-hover:opacity-100 hover:!text-red-700 cursor-pointer select-none';
const referenceLibrary = [
  {
    id: 'ref-1',
    title: '2024 미니멀리즘 트렌드',
    desc: '핀터레스트 기반 무드보드',
    type: '이미지',
    tags: ['Marketing', 'Minimalism', 'Summer2024'],
    img: 'https://images.unsplash.com/photo-1513694203232-719a280e022f?auto=format&fit=crop&w=1200&q=80',
  },
  {
    id: 'ref-2',
    title: '디지털 전략 대시보드',
    desc: '성과 추적용 레퍼런스',
    type: '문서',
    tags: ['DigitalStrategy', 'MZSense'],
    img: 'https://images.unsplash.com/photo-1551288049-bebda4e38f71?auto=format&fit=crop&w=1200&q=80',
  },
  {
    id: 'ref-3',
    title: '콘텐츠 스타일 가이드',
    desc: '브랜드 톤앤매너 보드',
    type: '문서',
    tags: ['SocialMedia', 'Marketing'],
    img: 'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1200&q=80',
  },
  {
    id: 'ref-4',
    title: '모던 오피스 레이아웃',
    desc: '공간 레퍼런스 이미지',
    type: '이미지',
    tags: ['Architecture', 'Minimalism'],
    img: 'https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=1200&q=80',
  },
]

const route = useRoute()
const router = useRouter()
const store = usePlannerStore()

function getRouteParam(value) {
  if (Array.isArray(value)) {
    return value[0] ?? ''
  }

  return typeof value === 'string' ? value : ''
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

function htmlToPlainText(value) {
  const text = String(value ?? '')

  if (typeof document === 'undefined') {
    return text.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim()
  }

  const node = document.createElement('div')
  node.innerHTML = text
  return (node.textContent ?? node.innerText ?? '').replace(/\s+/g, ' ').trim()
}

function createHistoryEntry({
  version,
  worker,
  workerInitials,
  change,
  date,
  previousTitle,
  currentTitle,
  previousDetails,
  currentDetails,
  previousLines,
  currentLines,
}) {
  const maxLength = Math.max(previousLines.length, currentLines.length)
  const lines = []

  for (let index = 0; index < maxLength; index += 1) {
    const previousLine = previousLines[index]
    const currentLine = currentLines[index]

    if (!previousLine && !currentLine) {
      continue
    }

    const isDifferent = Boolean(previousLine && currentLine && previousLine !== currentLine)

    lines.push({
      left: previousLine
        ? {
            number: index + 1,
            text: previousLine,
            kind: currentLine && isDifferent ? 'changed' : 'removed',
          }
        : null,
      right: currentLine
        ? {
            number: index + 1,
            text: currentLine,
            kind: previousLine && isDifferent ? 'changed' : 'added',
          }
        : null,
    })
  }

  return {
    version,
    worker,
    workerInitials,
    change,
    date,
    diff: {
      title: { previous: previousTitle, current: currentTitle },
      details: { previous: previousDetails, current: currentDetails },
      content: { lines },
    },
  }
}

const routeContentId = computed(() => getRouteParam(route.params.contentId))
const routeSeedDate = computed(() => getRouteParam(route.query.date) || store.currentDate)
const isCreateMode = computed(() => !routeContentId.value || routeContentId.value === 'new')
const activeTask = computed(() => {
  if (isCreateMode.value) {
    return null
  }

  return store.tasks.find((task) => task.id === routeContentId.value) ?? null
})

const activePanel = ref(ENABLE_CONTENT_RAIL ? 'properties' : null)
const activeModal = ref(null)
const historyModalVersion = ref('')
const panelWidth = ref(DEFAULT_PANEL_WIDTH)
const isPanelResizing = ref(false)

const {
  draftTitleText,
  draftBodyText,
  titleEditorHolder,
  bodyEditorHolder,
  contentStatus,
  contentPlannerId,
  contentPlannerSelected,
  contentAssigneeId,
  contentAssigneeSelected,
  contentDueDate,
  contentTypeValue,
  contentCustomer,
  currentPlanner,
  currentDesigner,
  currentAssignee,
  currentSupervisor,
  propertyPlanners,
  taskStatusLabel,
  taskPriorityLabel,
  isSaving,
  saveContent,
} = useContentEditor({
  store,
  router,
  routeContentId,
  routeSeedDate,
  activeTask,
})

const panelItems = [
  { id: 'properties', title: '상세 속성', icon: 'list_alt' },
  { id: 'gpt', title: 'GPT', icon: 'smart_toy' },
  { id: 'files', title: '파일&템플릿', icon: 'folder_open' },
  { id: 'review', title: '검토 요청', icon: 'rate_review' },
  { id: 'history', title: '변경 이력', icon: 'history' },
]

const bottomPanelItems = [{ id: 'references', title: '레퍼런스 탐색', icon: 'menu_book' }]
const allPanelItems = [...panelItems, ...bottomPanelItems]

const workspaceRightInset = computed(() =>
  ENABLE_CONTENT_RAIL ? `${RAIL_WIDTH + (activePanel.value ? panelWidth.value : 0)}px` : '0px',
)

const gptInput = ref('')
const gptMessages = ref([])
const reviewMessage = ref('')
const reviewRequests = ref([])
const sharedFiles = ref(['brand-guidelines.pdf', 'meeting-notes-0415.docx'])
const importedTemplates = ref([
  {
    id: 'template-1',
    type: '브랜딩',
    name: '캠페인 브리프',
    owner: 'Marketing',
    usage: '캠페인 방향과 핵심 메시지를 정리합니다.',
    sections: ['개요', '목표', '메시지'],
  },
  {
    id: 'template-2',
    type: '리포트',
    name: '월간 리포트',
    owner: 'Operations',
    usage: '성과와 개선 포인트를 빠르게 정리합니다.',
    sections: ['성과', '인사이트', '다음 액션'],
  },
])

const referenceSearchQuery = ref('')
const appliedReferenceType = ref('전체')
const appliedReferenceTags = ref([])
const draftReferenceType = ref('전체')
const draftReferenceTags = ref([])
const detailSearchQuery = ref('')
const isDueDateEditorOpen = ref(false)
const isFormatEditing = ref(false)
const isStatusListOpen = ref(false)
const dueDateCalendarMonth = ref(contentDueDate.value || routeSeedDate.value || store.currentDate)

const detailModalType = computed(() =>
  typeof activeModal.value === 'string' && activeModal.value.startsWith('detail-')
    ? activeModal.value.slice('detail-'.length)
    : '',
)

const dueDateCalendarLabel = computed(() => formatMonthLabel(dueDateCalendarMonth.value))
const dueDateCalendarDays = computed(() => getMonthWeeks(dueDateCalendarMonth.value).flat())

const filteredDetailMembers = computed(() => {
  if (!['planner', 'assignee'].includes(detailModalType.value)) {
    return []
  }

  const keyword = detailSearchQuery.value.trim().toLowerCase()
  const selectedId =
    detailModalType.value === 'planner'
      ? (contentPlannerSelected.value ? contentPlannerId.value : null)
      : contentAssigneeSelected.value
        ? contentAssigneeId.value
        : null

  return [...store.members]
    .filter((member) => {
      if (!keyword) {
        return true
      }

      return [member.name, member.role, member.team, member.initials]
        .join(' ')
        .toLowerCase()
        .includes(keyword)
    })
    .sort((left, right) => {
      const leftSelected = left.id === selectedId ? 1 : 0
      const rightSelected = right.id === selectedId ? 1 : 0

      if (leftSelected !== rightSelected) {
        return rightSelected - leftSelected
      }

      return left.name.localeCompare(right.name)
    })
})

const customerDirectory = computed(() => {
  const customers = new Map()

  ;[contentCustomer.value, ...store.tasks.map((task) => task.customer)].forEach((customer) => {
    const name = String(customer ?? '').trim()

    if (!name || name === DEFAULT_EDITOR_TEXTS.emptyCustomer) {
      return
    }

    const current = customers.get(name) ?? { name, count: 0 }
    current.count += 1
    customers.set(name, current)
  })

  return [...customers.values()].sort((left, right) => left.name.localeCompare(right.name))
})

const filteredDetailCustomers = computed(() => {
  if (detailModalType.value !== 'customer') {
    return []
  }

  const keyword = detailSearchQuery.value.trim().toLowerCase()
  const currentCustomer = String(contentCustomer.value ?? '').trim()

  return customerDirectory.value
    .filter((customer) => {
      if (!keyword) {
        return true
      }

      return customer.name.toLowerCase().includes(keyword)
    })
    .sort((left, right) => {
      const leftSelected = left.name === currentCustomer ? 1 : 0
      const rightSelected = right.name === currentCustomer ? 1 : 0

      if (leftSelected !== rightSelected) {
        return rightSelected - leftSelected
      }

      if (right.count !== left.count) {
        return right.count - left.count
      }

      return left.name.localeCompare(right.name)
    })
})

const detailModalTitle = computed(() => {
  const titles = {
    status: '상태 수정',
    planner: '기획자 수정',
    assignee: '담당자 수정',
    customer: '고객사 수정',
  }

  return titles[detailModalType.value] ?? ''
})

const detailModalDescription = computed(() => {
  const descriptions = {
    status: '상태는 버튼으로 빠르게 바꿀 수 있습니다.',
    planner: '기획자 닉네임을 검색해서 선택합니다.',
    assignee: '담당자 닉네임을 검색해서 선택합니다.',
    customer: '고객사를 검색해서 바로 변경합니다.',
  }

  return descriptions[detailModalType.value] ?? ''
})

const currentStatusOption = computed(
  () => statusOptions.find((option) => option.value === contentStatus.value) ?? statusOptions[0],
)

const currentStatusDotColor = computed(() => {
  const colors = {
    planned: '#94a3b8',
    at_risk: '#ef4444',
    in_progress: '#3b82f6',
    review: '#f97316',
    done: '#16a34a',
  }

  return colors[contentStatus.value] ?? '#94a3b8'
})

function currentStatusToneColor(status) {
  const colors = {
    planned: '#94a3b8',
    at_risk: '#ef4444',
    in_progress: '#3b82f6',
    review: '#f97316',
    done: '#16a34a',
  }

  return colors[status] ?? '#94a3b8'
}

const selectedDetailMember = computed(() => {
  if (detailModalType.value === 'planner' && contentPlannerSelected.value) {
    return currentPlanner.value
  }

  if (detailModalType.value === 'assignee' && contentAssigneeSelected.value) {
    return currentAssignee.value
  }

  return null
})

let panelResizeCleanup = null

const filteredReferences = computed(() => {
  const keyword = referenceSearchQuery.value.trim().toLowerCase()

  return referenceLibrary.filter((item) => {
    const matchesKeyword =
      !keyword ||
      [item.title, item.desc, item.type, ...(item.tags ?? [])]
        .join(' ')
        .toLowerCase()
        .includes(keyword)

    const matchesType = appliedReferenceType.value === '전체' || item.type === appliedReferenceType.value
    const matchesTags =
      appliedReferenceTags.value.length === 0 ||
      appliedReferenceTags.value.every((tag) => item.tags.includes(tag))

    return matchesKeyword && matchesType && matchesTags
  })
})

const historyData = computed(() => {
  const currentTitleValue = draftTitleText.value || activeTask.value?.title || '새 콘텐츠 항목'
  const bodyPreview = draftBodyText.value || '본문을 입력하세요.'
  const currentDetails = {
    상태: taskStatusLabel.value,
    기획자: propertyPlanners.value.map((member) => member?.name).filter(Boolean).join(', ') || '미정',
    담당자: currentAssignee.value?.name ?? '미정',
    마감일: contentDueDate.value || routeSeedDate.value || '미정',
    포맷: contentTypeValue.value || '미정',
    고객사: contentCustomer.value || '미정',
  }
  const previousDetails = {
    상태: '초안',
    기획자: currentPlanner.value?.name ?? '미정',
    담당자: '미지정',
    마감일: '2024-06-12',
    포맷: '작업',
    고객사: contentCustomer.value || '미정',
  }

  return [
    createHistoryEntry({
      version: 'v1.4',
      worker: currentPlanner.value?.name ?? 'Jaewon Kim',
      workerInitials: currentPlanner.value?.initials ?? 'JK',
      change: '제목과 본문 최신화',
      date: '2024-05-18 14:20',
      previousTitle: `${currentTitleValue} Draft`,
      currentTitle: currentTitleValue,
      previousDetails,
      currentDetails,
      previousLines: [
        '## 이전 버전',
        '본문과 상세 속성이 아직 정리되지 않은 상태였습니다.',
        '제목과 본문을 편집기 기반으로 다시 정리합니다.',
      ],
      currentLines: [
        `## ${currentTitleValue}`,
        bodyPreview,
        '제목과 본문이 EditorJS에서 관리됩니다.',
      ],
    }),
    createHistoryEntry({
      version: 'v1.3',
      worker: currentDesigner.value?.name ?? 'Sumin Lee',
      workerInitials: currentDesigner.value?.initials ?? 'SL',
      change: '상세 속성 카드 재배치',
      date: '2024-05-15 10:45',
      previousTitle: '상세 속성 초안',
      currentTitle: currentTitleValue,
      previousDetails: {
        상태: '검토',
        기획자: currentDesigner.value?.name ?? '미정',
        담당자: currentAssignee.value?.name ?? '미정',
        마감일: '2024-06-10',
        포맷: '포스트',
        고객사: contentCustomer.value || '미정',
      },
      currentDetails,
      previousLines: ['상세 속성 카드가 완전히 분리되지 않았습니다.'],
      currentLines: ['상세 속성 카드가 더 촘촘하게 배치되었습니다.'],
    }),
    createHistoryEntry({
      version: 'v1.2',
      worker: currentSupervisor.value?.name ?? 'Taeyoung Lim',
      workerInitials: currentSupervisor.value?.initials ?? 'TL',
      change: '레퍼런스와 변경 이력 연결',
      date: '2024-05-08 16:20',
      previousTitle: '레퍼런스 모듈',
      currentTitle: currentTitleValue,
      previousDetails: {
        상태: '계획',
        기획자: currentPlanner.value?.name ?? '미정',
        담당자: '미지정',
        마감일: '2024-06-08',
        포맷: '작업',
        고객사: contentCustomer.value || '미정',
      },
      currentDetails,
      previousLines: ['레퍼런스 탐색과 변경 이력이 하나의 화면에 섞여 있었습니다.'],
      currentLines: ['레퍼런스 탐색과 변경 이력을 분리하고, 각각 모달을 사용합니다.'],
    }),
  ]
})

function stopPanelResize() {
  if (!panelResizeCleanup) {
    return
  }

  panelResizeCleanup()
  panelResizeCleanup = null
}

function closeInlineEditors() {
  isDueDateEditorOpen.value = false
  isFormatEditing.value = false
  isStatusListOpen.value = false
}

function openPanel(id) {
  if (!ENABLE_CONTENT_RAIL) {
    return
  }

  closeInlineEditors()
  activeModal.value = null
  activePanel.value = activePanel.value === id ? null : id
}

function closePanel() {
  closeInlineEditors()
  activeModal.value = null
  activePanel.value = null
}

function openDetailModal(type) {
  if (type === 'status') {
    const nextStatusListOpen = !isStatusListOpen.value
    closeInlineEditors()
    isStatusListOpen.value = nextStatusListOpen
    return
  }

  closeInlineEditors()
  activeModal.value = `detail-${type}`
  detailSearchQuery.value =
    type === 'customer' && contentCustomer.value ? contentCustomer.value : ''
}

function openHistoryModal(version) {
  closeInlineEditors()
  historyModalVersion.value = version
  activeModal.value = 'history'
}

function openReferenceFilterModal() {
  closeInlineEditors()
  draftReferenceType.value = appliedReferenceType.value
  draftReferenceTags.value = [...appliedReferenceTags.value]
  activeModal.value = 'reference-filter'
}

function closeModal() {
  activeModal.value = null
  detailSearchQuery.value = ''
  closeInlineEditors()
}

function selectDetailStatus(status) {
  contentStatus.value = status
  isStatusListOpen.value = false
}

function selectDetailMember(member) {
  if (detailModalType.value === 'planner') {
    contentPlannerSelected.value = true
    contentPlannerId.value = member.id
  }

  if (detailModalType.value === 'assignee') {
    contentAssigneeSelected.value = true
    contentAssigneeId.value = member.id
  }

  closeModal()
}

function clearDetailSelection() {
  if (detailModalType.value === 'planner') {
    contentPlannerSelected.value = false
    contentPlannerId.value = null
  }

  if (detailModalType.value === 'assignee') {
    contentAssigneeSelected.value = false
    contentAssigneeId.value = null
  }
}

function selectDetailCustomer(customerName) {
  contentCustomer.value = customerName
  closeModal()
}

function toggleDueDateEditor() {
  if (activeModal.value) {
    return
  }

  if (!isDueDateEditorOpen.value) {
    dueDateCalendarMonth.value = contentDueDate.value || routeSeedDate.value || store.currentDate
  }

  isFormatEditing.value = false
  isDueDateEditorOpen.value = !isDueDateEditorOpen.value
}

function moveDueDateMonth(delta) {
  dueDateCalendarMonth.value = addMonths(dueDateCalendarMonth.value, delta)
}

function selectDueDate(dateKey) {
  contentDueDate.value = dateKey
  isDueDateEditorOpen.value = false
}

function toggleDraftTag(tag) {
  if (draftReferenceTags.value.includes(tag)) {
    draftReferenceTags.value = draftReferenceTags.value.filter((item) => item !== tag)
    return
  }

  draftReferenceTags.value = [...draftReferenceTags.value, tag]
}

function applyReferenceFilters() {
  appliedReferenceType.value = draftReferenceType.value
  appliedReferenceTags.value = [...draftReferenceTags.value]
  activeModal.value = null
}

function clearReferenceFilters() {
  referenceSearchQuery.value = ''
  appliedReferenceType.value = '전체'
  appliedReferenceTags.value = []
  draftReferenceType.value = '전체'
  draftReferenceTags.value = []
}

function removeAppliedTag(tag) {
  appliedReferenceTags.value = appliedReferenceTags.value.filter((item) => item !== tag)
}

function sendGptMessage() {
  const text = gptInput.value.trim()
  if (!text) {
    return
  }

  gptMessages.value = [
    ...gptMessages.value,
    {
      id: `gpt-user-${Date.now()}`,
      role: 'user',
      text,
    },
    {
      id: `gpt-assistant-${Date.now()}`,
      role: 'assistant',
      text: `좋습니다. "${draftTitleText.value}" 문서를 기준으로 ${taskStatusLabel.value} 상태와 ${taskPriorityLabel.value} 우선순위를 함께 고려해 답변드릴게요.`,
    },
  ]
  gptInput.value = ''
}

function sendReviewRequest() {
  const text = reviewMessage.value.trim()
  if (!text) {
    return
  }

  reviewRequests.value = [
    {
      id: `review-${Date.now()}`,
      sender: store.findMember(store.currentUserId)?.name ?? '현재 사용자',
      target: currentAssignee.value?.name ?? '담당자',
      message: text,
      time: new Date().toLocaleString('ko-KR', { hour12: false }),
    },
    ...reviewRequests.value,
  ]
  reviewMessage.value = ''
}

function shareFile() {
  const nextIndex = sharedFiles.value.length + 1
  sharedFiles.value = [...sharedFiles.value, `shared-document-${nextIndex}.pdf`]
}

function importTemplate() {
  const nextIndex = importedTemplates.value.length + 1
  importedTemplates.value = [
    {
      id: `template-${nextIndex}`,
      type: '활용',
      name: `새 템플릿 ${nextIndex}`,
      owner: 'Workspace',
      usage: '새로운 콘텐츠를 빠르게 시작하는 기본 템플릿입니다.',
      sections: ['제목', '본문', '요약'],
    },
    ...importedTemplates.value,
  ]
}

function formatFileName(fileName) {
  return String(fileName).replace(/\.[^.]+$/, '')
}

function getAttachmentIconName(fileName) {
  const lower = String(fileName).toLowerCase()

  if (lower.endsWith('.pdf')) return 'picture_as_pdf'
  if (lower.endsWith('.png') || lower.endsWith('.jpg') || lower.endsWith('.jpeg')) return 'image'
  if (lower.endsWith('.ppt') || lower.endsWith('.pptx')) return 'slideshow'
  if (lower.endsWith('.doc') || lower.endsWith('.docx')) return 'description'
  if (lower.endsWith('.xls') || lower.endsWith('.xlsx')) return 'table_chart'

  return 'insert_drive_file'
}

function routerBack() {
  router.back()
}

function startPanelResize(event) {
  if (!activePanel.value) {
    return
  }

  stopPanelResize()

  isPanelResizing.value = true
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'col-resize'

  const startX = event.clientX
  const startWidth = panelWidth.value

  const handleMove = (moveEvent) => {
    const delta = startX - moveEvent.clientX
    panelWidth.value = clamp(startWidth + delta, MIN_PANEL_WIDTH, MAX_PANEL_WIDTH)
  }

  const cleanup = () => {
    isPanelResizing.value = false
    document.body.style.userSelect = ''
    document.body.style.cursor = ''
    window.removeEventListener('pointermove', handleMove)
    window.removeEventListener('pointerup', cleanup)
  }

  panelResizeCleanup = cleanup

  window.addEventListener('pointermove', handleMove)
  window.addEventListener('pointerup', cleanup, { once: true })
}

onBeforeUnmount(() => {
  stopPanelResize()
})

</script>

<template>
  <main class="content-editor-shell relative min-h-screen bg-[#f7f9fc] text-slate-900">
    <div class="mx-auto max-w-[1600px] px-6 py-6 transition-all duration-300" :style="{ paddingRight: workspaceRightInset }">
      <div class="space-y-6">
        <header class="sticky top-4 z-20 border border-slate-200 bg-white px-6 py-5 shadow-none">
          <div class="flex items-start justify-between gap-6">
            <div class="flex min-w-0 items-start gap-4">
                <button
                  type="button"
                  class="flex h-11 w-11 shrink-0 items-center justify-center border border-slate-200 bg-white text-slate-500 transition-colors hover:bg-slate-50 hover:text-slate-900"
                  @click="routerBack"
                >
                  <span class="material-symbols-outlined text-[22px]">arrow_back</span>
                </button>

              <div class="min-w-0">
                <p class="text-[10px] font-black uppercase tracking-[0.22em] text-slate-400">
                  콘텐츠 워크스페이스
                </p>

                <div
                  v-if="ENABLE_EDITOR_JS"
                  ref="titleEditorHolder"
                  class="editor-shell title-editor mt-3 min-h-[32px]"
                ></div>
                <div v-else class="mt-3">
                  <input
                    v-model="draftTitleText"
                    type="text"
                    class="w-full rounded-[20px] border border-slate-100 bg-slate-50 px-4 py-4 text-2xl font-black text-slate-900 outline-none transition-colors placeholder:text-slate-300 focus:border-blue-200 focus:bg-white"
                    placeholder="제목을 입력하세요"
                  />
                </div>
              </div>
            </div>

            <div class="flex shrink-0 items-center gap-3">
              <button
                type="button"
                class="inline-flex items-center gap-2 border border-slate-200 bg-slate-950 px-4 py-3 text-sm font-black text-white transition-colors hover:bg-slate-800"
                :disabled="isSaving"
                @click="saveContent"
              >
                <span class="material-symbols-outlined text-[18px]">save</span>
                {{ isSaving ? '저장 중' : '저장' }}
              </button>
            </div>
          </div>
        </header>

        <section class="overflow-visible border border-slate-200 bg-white shadow-none">
          <div class="flex items-center justify-between border-b border-slate-200 px-6 py-4">
            <div>
              <p class="text-[10px] font-black uppercase tracking-[0.22em] text-slate-400">본문</p>
              <h2 class="mt-1 text-base font-black text-slate-900">EditorJS 본문</h2>
            </div>

            <span class="rounded-full bg-slate-100 px-3 py-1 text-[11px] font-bold text-slate-500">
              {{ (activeTask?.requirementId ?? routeContentId) || 'NEW' }}
            </span>
          </div>

            <div
              v-if="ENABLE_EDITOR_JS"
              ref="bodyEditorHolder"
              class="editor-shell body-editor mx-auto min-h-[760px] w-full max-w-[1220px] pl-16 pr-8 py-6"
            ></div>
            <div v-else class="pl-16 pr-8 py-6">
              <textarea
                v-model="draftBodyText"
                rows="24"
                class="mx-auto min-h-[760px] w-full max-w-[1220px] rounded-[24px] border border-slate-100 bg-white px-5 py-5 text-[15px] leading-7 text-slate-700 outline-none transition-colors placeholder:text-slate-300 focus:border-blue-200 focus:ring-4 focus:ring-blue-50"
                placeholder="본문을 입력하세요"
              />
            </div>
        </section>
      </div>
    </div>

    <nav
      v-if="ENABLE_CONTENT_RAIL"
      class="fixed right-0 top-[88px] z-50 h-[calc(100vh-88px)] w-16 border-l border-slate-200 bg-white shadow-none"
    >
      <div class="flex h-full flex-col items-center gap-2 py-6">
        <button
          v-for="item in panelItems"
          :key="item.id"
          type="button"
          class="group relative flex h-12 w-12 items-center justify-center rounded-[14px] transition-all duration-200"
          :class="
            activePanel === item.id
              ? 'bg-blue-50 text-blue-600 shadow-sm ring-1 ring-blue-100'
              : 'text-slate-500 hover:bg-slate-100 hover:text-slate-900'
          "
          :aria-pressed="activePanel === item.id"
          :title="item.title"
          @click="openPanel(item.id)"
        >
          <span
            class="material-symbols-outlined text-[22px] transition-transform"
            :class="{ 'scale-110': activePanel === item.id }"
          >
            {{ item.icon }}
          </span>
        </button>

        <div class="my-2 h-px w-8 bg-slate-200"></div>

        <button
          v-for="item in bottomPanelItems"
          :key="item.id"
          type="button"
          class="group relative flex h-12 w-12 items-center justify-center rounded-[14px] transition-all duration-200"
          :class="
            activePanel === item.id
              ? 'bg-blue-50 text-blue-600 shadow-sm ring-1 ring-blue-100'
              : 'text-slate-500 hover:bg-slate-100 hover:text-slate-900'
          "
          :aria-pressed="activePanel === item.id"
          :title="item.title"
          @click="openPanel(item.id)"
        >
          <span
            class="material-symbols-outlined text-[22px] transition-transform"
            :class="{ 'scale-110': activePanel === item.id }"
          >
            {{ item.icon }}
          </span>
        </button>
      </div>
    </nav>

    <aside
      v-if="ENABLE_CONTENT_RAIL"
      class="fixed z-40 h-[calc(100vh-88px)] border-l border-slate-200 bg-white shadow-none transition-all duration-300"
      :class="activePanel ? 'translate-x-0 opacity-100' : 'translate-x-full opacity-0 pointer-events-none'"
      :style="{ top: `${HEADER_OFFSET}px`, right: `${RAIL_WIDTH}px`, width: `${panelWidth}px` }"
    >
      <div
        class="absolute left-0 top-0 h-full w-2 -translate-x-1/2 cursor-col-resize"
        @pointerdown.prevent="startPanelResize"
      >
        <div class="mx-auto h-full w-[2px] rounded-full bg-slate-200/90 transition-colors hover:bg-blue-300"></div>
      </div>

      <div class="flex h-full min-h-0 flex-col">
        <div class="flex items-center justify-between border-b border-slate-100 px-6 py-5">
          <div>
            <p class="text-[10px] font-black uppercase tracking-[0.24em] text-slate-400">
              {{ activePanel === 'properties' ? 'PROJECT DETAILS' : '현재 패널' }}
            </p>
            <h2 class="mt-1 text-lg font-black text-slate-900">
              {{ activePanel === 'properties' ? '상세 속성' : (allPanelItems.find((item) => item.id === activePanel)?.title ?? '상세 속성') }}
            </h2>
          </div>

          <button
            type="button"
            class="flex h-10 w-10 items-center justify-center rounded-full text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-900"
            @click="closePanel"
          >
            <span class="material-symbols-outlined text-[20px]">close</span>
          </button>
        </div>

        <div class="min-h-0 flex-1">
          <div v-if="activePanel === 'properties'" class="h-full overflow-y-auto p-3 no-scrollbar">
            <section class="overflow-visible border border-slate-200 bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
              <div class="divide-y divide-slate-100">
                <div class="group relative px-2.5 py-2.5 transition-colors hover:bg-slate-50">
                  <div class="flex items-start justify-between gap-2">
                    <div class="flex min-w-0 items-start gap-2">
                      <span class="w-14 shrink-0 pt-0.5 text-[11px] font-black uppercase tracking-[0.18em] text-slate-400">
                      상태
                      </span>
                      <div class="flex items-center gap-2 pt-0.5">
                        <span class="h-2.5 w-2.5 border border-white" :style="{ backgroundColor: currentStatusDotColor }"></span>
                        <span class="text-sm font-black text-slate-900">{{ taskStatusLabel }}</span>
                      </div>
                    </div>
                    <button
                      type="button"
                      class="text-[10px] leading-none"
                      :class="editActionClass"
                      @click="openDetailModal('status')"
                    >
                      수정
                    </button>
                  </div>

                  <Transition name="fade-scale">
                    <div
                      v-if="isStatusListOpen"
                      class="absolute left-2 right-2 top-full z-20 mt-2 overflow-hidden rounded-[12px] border border-slate-200 bg-white shadow-[0_18px_48px_rgba(15,23,42,0.14)]"
                    >
                      <div class="flex items-center justify-between border-b border-slate-100 bg-slate-50/80 px-3 py-2.5">
                        <div>
                          <p class="text-[10px] font-black uppercase tracking-[0.22em] text-slate-400">STATUS LIST</p>
                          <p class="mt-0.5 text-[11px] font-medium text-slate-500">상태를 선택하세요</p>
                        </div>
                        <span class="rounded-full bg-white px-2.5 py-1 text-[10px] font-black uppercase tracking-[0.16em] text-slate-400">
                          총 5개
                        </span>
                      </div>

                      <div class="space-y-2 p-2.5">
                        <button
                          v-for="option in statusOptions"
                          :key="option.value"
                          type="button"
                          class="flex min-h-[44px] w-full items-center justify-between rounded-[10px] border px-3 py-2.5 text-left transition-all duration-150 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-slate-200"
                          :class="[
                            option.value === contentStatus
                              ? 'border-slate-300 bg-slate-50 shadow-sm hover:-translate-y-px hover:border-slate-300 hover:bg-slate-50 hover:shadow-[0_4px_12px_rgba(15,23,42,0.08)]'
                              : 'border-slate-200 bg-white hover:-translate-y-px hover:border-slate-300 hover:bg-slate-50 hover:shadow-[0_4px_12px_rgba(15,23,42,0.06)]',
                          ]"
                          @click="selectDetailStatus(option.value)"
                        >
                          <span class="flex min-w-0 items-center gap-2.5">
                            <span
                              class="h-2.5 w-2.5 shrink-0 rounded-[2px] border border-white shadow-[0_1px_1px_rgba(15,23,42,0.08)]"
                              :style="{ backgroundColor: currentStatusToneColor(option.value) }"
                            ></span>
                            <span
                              class="truncate text-[12px] font-bold leading-none tracking-tight"
                              :style="{ color: currentStatusToneColor(option.value) }"
                            >
                              {{ option.label }}
                            </span>
                          </span>
                          <span
                            v-if="option.value === contentStatus"
                            class="rounded-full border border-blue-100 bg-blue-50 px-2 py-0.5 text-[10px] font-black tracking-[0.08em] text-blue-600"
                          >
                            선택됨
                          </span>
                        </button>
                      </div>
                    </div>
                  </Transition>
                </div>

                <div class="group flex items-start justify-between gap-2 px-2.5 py-2.5 transition-colors hover:bg-slate-50">
                  <div class="flex min-w-0 items-start gap-2">
                    <span class="w-14 shrink-0 pt-0.5 text-[11px] font-black uppercase tracking-[0.18em] text-slate-400">
                      기획자
                    </span>
                    <div class="min-w-0 space-y-2">
                      <p class="text-[11px] font-medium leading-5 text-slate-400">기획 및 담당 라인 (활성 멤버)</p>
                      <div class="space-y-2">
                        <div
                          v-for="member in propertyPlanners"
                          :key="member.id"
                          class="flex items-center gap-3 border border-slate-200 bg-white px-3 py-2"
                        >
                          <div
                            class="flex h-8 w-8 shrink-0 items-center justify-center rounded-[4px] text-[10px] font-black text-white"
                            :style="{ backgroundColor: member.accent }"
                          >
                            {{ member.initials }}
                          </div>
                          <div class="min-w-0">
                            <p class="truncate text-sm font-black text-slate-950">{{ member.name }}</p>
                            <p class="truncate text-[11px] leading-5 text-slate-500">{{ member.role }}</p>
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <button
                    type="button"
                    class="text-[10px] leading-none"
                    :class="editActionClass"
                    @click="openDetailModal('planner')"
                  >
                    수정
                  </button>
                </div>

                <div class="group flex items-start justify-between gap-2 px-2.5 py-2.5 transition-colors hover:bg-slate-50">
                  <div class="flex min-w-0 items-start gap-2">
                    <span class="w-14 shrink-0 pt-0.5 text-[11px] font-black uppercase tracking-[0.18em] text-slate-400">
                      담당자
                    </span>
                    <div class="flex min-w-0 items-center gap-3 border border-slate-200 bg-white px-3 py-2">
                      <div
                        class="flex h-10 w-10 shrink-0 items-center justify-center rounded-[4px] text-sm font-black text-white"
                        :style="{ backgroundColor: currentAssignee?.accent ?? '#475569' }"
                      >
                        {{ currentAssignee?.initials ?? 'U1' }}
                      </div>
                      <div class="min-w-0">
                        <p class="truncate text-sm font-black text-slate-950">{{ currentAssignee?.name ?? '미지정' }}</p>
                        <p class="truncate text-[11px] leading-5 text-slate-500">{{ currentAssignee?.role ?? '담당자 정보 없음' }}</p>
                      </div>
                    </div>
                  </div>
                  <button
                    type="button"
                    class="text-[10px] leading-none"
                    :class="editActionClass"
                    @click="openDetailModal('assignee')"
                  >
                    수정
                  </button>
                </div>

                <div class="group relative flex items-start justify-between gap-2 px-2.5 py-2.5 transition-colors hover:bg-slate-50">
                  <div class="flex min-w-0 items-center gap-2">
                    <span class="w-14 shrink-0 text-[11px] font-black uppercase tracking-[0.18em] text-slate-400">
                      마감일
                    </span>
                    <span class="text-[15px] font-black tracking-tight text-red-600">
                      {{ contentDueDate || '미정' }}
                    </span>
                  </div>
                  <button
                    type="button"
                    class="text-[10px] leading-none"
                    :class="editActionClass"
                    @click="toggleDueDateEditor"
                  >
                    수정
                  </button>

                  <div
                    v-if="isDueDateEditorOpen"
                    class="absolute left-4 right-4 top-full z-20 mt-3 overflow-hidden border border-slate-200 bg-white shadow-[0_16px_40px_rgba(15,23,42,0.12)]"
                  >
                    <div class="flex items-center justify-between border-b border-slate-100 px-3 py-3">
                      <button
                        type="button"
                        class="flex h-8 w-8 items-center justify-center border border-slate-200 bg-white text-slate-500 transition-colors hover:bg-slate-50 hover:text-slate-900"
                        @click="moveDueDateMonth(-1)"
                      >
                        <span class="material-symbols-outlined text-[18px]">chevron_left</span>
                      </button>

                      <p class="text-[13px] font-black text-slate-900">{{ dueDateCalendarLabel }}</p>

                      <button
                        type="button"
                        class="flex h-8 w-8 items-center justify-center border border-slate-200 bg-white text-slate-500 transition-colors hover:bg-slate-50 hover:text-slate-900"
                        @click="moveDueDateMonth(1)"
                      >
                        <span class="material-symbols-outlined text-[18px]">chevron_right</span>
                      </button>
                    </div>

                    <div class="grid grid-cols-7 border-b border-slate-100 bg-slate-50 px-2 py-2 text-center text-[10px] font-black uppercase tracking-[0.14em] text-slate-400">
                      <span>월</span>
                      <span>화</span>
                      <span>수</span>
                      <span>목</span>
                      <span>금</span>
                      <span>토</span>
                      <span>일</span>
                    </div>

                    <div class="grid grid-cols-7">
                      <button
                        v-for="day in dueDateCalendarDays"
                        :key="day.key"
                        type="button"
                        class="flex h-10 items-center justify-center border-r border-b border-slate-100 text-center text-[12px] font-semibold transition-colors last:border-r-0"
                        :class="[
                          day.isCurrentMonth ? 'bg-white text-slate-900 hover:bg-slate-50' : 'bg-slate-50 text-slate-300',
                          contentDueDate === day.key ? 'bg-slate-950 text-white hover:bg-slate-950' : '',
                        ]"
                        @click="selectDueDate(day.key)"
                      >
                        {{ day.dayNumber }}
                      </button>
                    </div>
                  </div>
                </div>

                <div class="group flex items-center justify-between gap-2 px-2.5 py-2.5 transition-colors hover:bg-slate-50">
                  <div class="flex min-w-0 items-center gap-2">
                    <span class="w-14 shrink-0 text-[11px] font-black uppercase tracking-[0.18em] text-slate-400">
                      포맷
                    </span>
                    <div v-if="isFormatEditing" class="relative w-full max-w-[280px]">
                      <input
                        v-model="contentTypeValue"
                        type="text"
                        class="w-full border border-slate-200 bg-white px-3 py-2 pr-16 text-sm font-semibold text-slate-900 outline-none transition-colors placeholder:text-slate-300 focus:border-slate-400"
                        placeholder="포맷을 입력하세요"
                      />
                      <button
                        type="button"
                        class="absolute right-2 top-1/2 flex h-7 -translate-y-1/2 items-center border border-slate-200 bg-slate-50 px-2.5 text-[11px] font-black text-slate-700 transition-colors hover:bg-slate-100"
                        @click="isFormatEditing = false"
                      >
                        완료
                      </button>
                    </div>
                    <span v-else class="text-sm font-semibold text-slate-900">{{ contentTypeValue || '미정' }}</span>
                  </div>
                  <button
                    v-if="!isFormatEditing"
                    type="button"
                    class="text-[10px] leading-none"
                    :class="editActionClass"
                    @click="isFormatEditing = true"
                  >
                    수정
                  </button>
                </div>

                <div class="group flex items-start justify-between gap-2 px-2.5 py-2.5 transition-colors hover:bg-slate-50">
                  <div class="flex min-w-0 items-center gap-2">
                    <span class="w-14 shrink-0 text-[11px] font-black uppercase tracking-[0.18em] text-slate-400">
                      고객사
                    </span>
                    <span class="text-sm font-black text-slate-950">{{ contentCustomer || '미정' }}</span>
                  </div>
                  <button
                    type="button"
                    class="text-[10px] leading-none"
                    :class="editActionClass"
                    @click="openDetailModal('customer')"
                  >
                    수정
                  </button>
                </div>
              </div>
            </section>
          </div>

          <div v-else-if="activePanel === 'gpt'" class="flex h-full flex-col">
            <div class="border-b border-slate-100 px-5 py-5">
              <p class="text-[10px] font-black uppercase tracking-[0.24em] text-slate-400">GPT</p>
              <h3 class="mt-1 text-lg font-black text-slate-900">메시지로 질문하기</h3>
              <p class="mt-2 text-sm leading-6 text-slate-500">
                {{ draftTitleText }} 문서를 기준으로 GPT에게 질문할 수 있습니다.
              </p>
            </div>

            <div class="flex-1 min-h-0 overflow-y-auto bg-slate-50/50 px-5 py-5 no-scrollbar">
              <div class="space-y-3">
                <div
                  v-for="message in gptMessages"
                  :key="message.id"
                  class="flex"
                  :class="message.role === 'user' ? 'justify-end' : 'justify-start'"
                >
                  <div
                    class="max-w-[84%] rounded-[20px] px-4 py-3 text-sm leading-6 shadow-sm"
                    :class="message.role === 'user' ? 'bg-blue-600 text-white' : 'border border-slate-100 bg-white text-slate-700'"
                  >
                    {{ message.text }}
                  </div>
                </div>
              </div>
            </div>

            <div class="border-t border-slate-100 bg-white px-5 py-4">
              <textarea
                v-model="gptInput"
                rows="4"
                class="w-full rounded-[20px] border border-slate-100 bg-slate-50 px-4 py-4 text-sm leading-6 text-slate-700 outline-none transition-colors placeholder:text-slate-300 focus:border-blue-200 focus:bg-white focus:ring-4 focus:ring-blue-50"
                placeholder="GPT에게 질문을 입력하세요"
              />
              <button
                type="button"
                class="mt-3 inline-flex w-full items-center justify-center gap-2 rounded-[18px] bg-blue-600 px-4 py-3 text-sm font-black text-white transition-colors hover:bg-blue-700"
                @click="sendGptMessage"
              >
                <span class="material-symbols-outlined text-[18px]">send</span>
                메시지 보내기
              </button>
            </div>
          </div>

          <div v-else-if="activePanel === 'files'" class="flex h-full flex-col">
            <div class="border-b border-slate-100 px-5 py-5">
              <p class="text-[10px] font-black uppercase tracking-[0.24em] text-slate-400">Files & Templates</p>
              <h3 class="mt-1 text-lg font-black text-slate-900">파일&템플릿</h3>
              <p class="mt-2 text-sm leading-6 text-slate-500">
                파일 공유와 템플릿 가져오기를 통해 관련 자료를 관리합니다.
              </p>
            </div>

            <div class="flex-1 min-h-0 overflow-y-auto p-5 no-scrollbar space-y-5">
              <div class="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  class="rounded-[18px] border border-blue-100 bg-blue-50 px-4 py-4 text-sm font-black text-blue-600 transition-colors hover:bg-blue-100"
                  @click="shareFile"
                >
                  파일 공유
                </button>
                <button
                  type="button"
                  class="rounded-[18px] border border-slate-100 bg-slate-50 px-4 py-4 text-sm font-black text-slate-700 transition-colors hover:bg-slate-100"
                  @click="importTemplate"
                >
                  템플릿 가져오기
                </button>
              </div>

              <section class="rounded-[20px] border border-slate-100 bg-white p-4 shadow-sm">
                <div class="flex items-center justify-between">
                  <h4 class="text-sm font-black text-slate-900">공유한 파일</h4>
                  <span class="text-[11px] font-bold text-slate-400">{{ sharedFiles.length }}개</span>
                </div>

                <div class="mt-4 space-y-3">
                  <div
                    v-for="file in sharedFiles"
                    :key="file"
                    class="flex items-center gap-3 rounded-[16px] bg-slate-50 px-4 py-3"
                  >
                    <div class="flex h-10 w-10 items-center justify-center rounded-2xl bg-white text-slate-500 shadow-sm">
                      <span class="material-symbols-outlined text-[20px]">{{ getAttachmentIconName(file) }}</span>
                    </div>
                    <div class="min-w-0">
                      <p class="truncate text-sm font-black text-slate-900">{{ file }}</p>
                      <p class="text-xs text-slate-400">{{ formatFileName(file) }}</p>
                    </div>
                  </div>
                </div>
              </section>

              <section class="rounded-[20px] border border-slate-100 bg-white p-4 shadow-sm">
                <div class="flex items-center justify-between">
                  <h4 class="text-sm font-black text-slate-900">가져온 템플릿</h4>
                  <span class="text-[11px] font-bold text-slate-400">{{ importedTemplates.length }}개</span>
                </div>

                <div class="mt-4 space-y-3">
                  <article
                    v-for="template in importedTemplates"
                    :key="template.id"
                    class="rounded-[18px] border border-slate-100 bg-slate-50 p-4"
                  >
                    <div class="flex items-start justify-between gap-3">
                      <div>
                        <p class="text-[11px] font-black uppercase tracking-[0.18em] text-slate-400">
                          {{ template.type }}
                        </p>
                        <h5 class="mt-1 text-base font-black text-slate-900">{{ template.name }}</h5>
                      </div>
                      <span class="rounded-full bg-white px-3 py-1 text-[11px] font-bold text-slate-500">
                        {{ template.owner }}
                      </span>
                    </div>
                    <p class="mt-3 text-sm leading-6 text-slate-500">{{ template.usage }}</p>
                    <div class="mt-3 flex flex-wrap gap-2">
                      <span
                        v-for="section in template.sections"
                        :key="section"
                        class="rounded-full bg-white px-3 py-1 text-[11px] font-bold text-slate-600 ring-1 ring-slate-100"
                      >
                        {{ section }}
                      </span>
                    </div>
                  </article>
                </div>
              </section>
            </div>
          </div>

          <div v-else-if="activePanel === 'review'" class="flex h-full flex-col">
            <div class="border-b border-slate-100 px-5 py-5">
              <p class="text-[10px] font-black uppercase tracking-[0.24em] text-slate-400">Review Request</p>
              <h3 class="mt-1 text-lg font-black text-slate-900">검토 요청</h3>
              <p class="mt-2 text-sm leading-6 text-slate-500">
                담당자에게 검토 메시지를 보내고 요청 기록을 남깁니다.
              </p>
            </div>

            <div class="flex-1 min-h-0 overflow-y-auto p-5 no-scrollbar space-y-4">
              <div class="rounded-[20px] border border-slate-100 bg-slate-50 p-4">
                <p class="text-[10px] font-black uppercase tracking-[0.18em] text-slate-400">요청 대상</p>
                <div class="mt-3 flex items-center gap-3">
                  <div
                    class="flex h-12 w-12 items-center justify-center rounded-2xl text-sm font-black text-white"
                    :style="{ backgroundColor: currentAssignee?.accent ?? '#94a3b8' }"
                  >
                    {{ currentAssignee?.initials ?? 'U1' }}
                  </div>
                  <div>
                    <p class="text-base font-black text-slate-900">{{ currentAssignee?.name ?? '미지정' }}</p>
                    <p class="text-sm text-slate-500">{{ currentAssignee?.role ?? '담당자 정보 없음' }}</p>
                  </div>
                </div>
              </div>

              <textarea
                v-model="reviewMessage"
                rows="5"
                class="w-full rounded-[20px] border border-slate-100 bg-white px-4 py-4 text-sm leading-6 text-slate-700 outline-none transition-colors placeholder:text-slate-300 focus:border-blue-200 focus:ring-4 focus:ring-blue-50"
                placeholder="검토 요청 메시지를 입력하세요"
              />

              <button
                type="button"
                class="inline-flex w-full items-center justify-center gap-2 rounded-[18px] bg-indigo-600 px-4 py-3 text-sm font-black text-white transition-colors hover:bg-indigo-700"
                @click="sendReviewRequest"
              >
                <span class="material-symbols-outlined text-[18px]">send</span>
                요청 보내기
              </button>

              <section class="rounded-[20px] border border-slate-100 bg-white p-4 shadow-sm">
                <div class="flex items-center justify-between">
                  <h4 class="text-sm font-black text-slate-900">최근 요청</h4>
                  <span class="text-[11px] font-bold text-slate-400">{{ reviewRequests.length }}개</span>
                </div>

                <div class="mt-4 space-y-3">
                  <article
                    v-for="request in reviewRequests"
                    :key="request.id"
                    class="rounded-[18px] bg-slate-50 p-4"
                  >
                    <div class="flex items-start justify-between gap-3">
                      <div>
                        <p class="text-[11px] font-black uppercase tracking-[0.16em] text-slate-400">
                          {{ request.sender }} → {{ request.target }}
                        </p>
                        <p class="mt-2 text-sm leading-6 text-slate-700">{{ request.message }}</p>
                      </div>
                      <span class="text-[11px] font-bold text-slate-400">{{ request.time }}</span>
                    </div>
                  </article>
                </div>
              </section>
            </div>
          </div>

          <div v-else-if="activePanel === 'history'" class="flex h-full flex-col">
            <div class="border-b border-slate-100 px-5 py-5">
              <p class="text-[10px] font-black uppercase tracking-[0.24em] text-slate-400">History</p>
              <h3 class="mt-1 text-lg font-black text-slate-900">변경 이력</h3>
              <p class="mt-2 text-sm leading-6 text-slate-500">
                행을 누르면 가운데 모달로 변경 상세를 확인할 수 있습니다.
              </p>
            </div>

            <div class="flex-1 min-h-0 overflow-y-auto p-5 no-scrollbar">
              <div class="space-y-3">
                <button
                  v-for="log in historyData"
                  :key="log.version"
                  type="button"
                  class="group w-full rounded-[18px] border border-slate-100 bg-white p-4 text-left shadow-sm transition-all hover:border-blue-200 hover:shadow-md"
                  @click="openHistoryModal(log.version)"
                >
                  <div class="flex items-center justify-between gap-3">
                    <div class="flex min-w-0 items-center gap-3">
                      <span class="rounded-full bg-slate-100 px-3 py-1 text-[11px] font-black text-slate-600 transition-colors group-hover:bg-blue-50 group-hover:text-blue-600">
                        {{ log.version }}
                      </span>
                      <div class="min-w-0">
                        <p class="truncate text-sm font-black text-slate-900">{{ log.worker }}</p>
                        <p class="truncate text-xs text-slate-400">{{ log.date }}</p>
                      </div>
                    </div>

                    <span class="material-symbols-outlined text-slate-300 transition-all group-hover:translate-x-1 group-hover:text-blue-500">
                      arrow_forward_ios
                    </span>
                  </div>
                  <p class="mt-3 text-sm font-semibold text-slate-600 transition-colors group-hover:text-slate-900">
                    {{ log.change }}
                  </p>
                </button>
              </div>
            </div>
          </div>

          <div v-else-if="activePanel === 'references'" class="flex h-full flex-col">
            <div class="border-b border-slate-100 px-5 py-5">
              <p class="text-[10px] font-black uppercase tracking-[0.24em] text-slate-400">References</p>
              <h3 class="mt-1 text-lg font-black text-slate-900">레퍼런스 탐색</h3>
              <p class="mt-2 text-sm leading-6 text-slate-500">
                검색, 필터, 태그를 활용해 레퍼런스를 빠르게 찾아보세요.
              </p>
            </div>

            <div class="flex-1 min-h-0 overflow-y-auto p-5 no-scrollbar space-y-5">
              <div class="relative">
                <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 text-xl">search</span>
                <input
                  v-model="referenceSearchQuery"
                  type="text"
                  placeholder="키워드 검색..."
                  class="w-full rounded-[20px] border border-slate-100 bg-slate-50 py-4 pl-12 pr-4 text-sm text-slate-700 outline-none transition-colors placeholder:text-slate-300 focus:border-blue-200 focus:bg-white focus:ring-4 focus:ring-blue-50"
                />
              </div>

              <div class="flex items-center justify-between gap-3">
                <button
                  type="button"
                  class="inline-flex items-center gap-2 rounded-full bg-slate-900 px-4 py-2 text-xs font-black text-white transition-colors hover:bg-slate-800"
                  @click="openReferenceFilterModal"
                >
                  <span class="material-symbols-outlined text-[16px]">tune</span>
                  필터 및 태그
                </button>

                <button
                  type="button"
                  class="text-xs font-bold text-slate-400 transition-colors hover:text-slate-600"
                  @click="clearReferenceFilters"
                >
                  초기화
                </button>
              </div>

              <div class="flex flex-wrap gap-2">
                <span
                  v-if="appliedReferenceType !== '전체'"
                  class="inline-flex items-center gap-1 rounded-full bg-blue-50 px-3 py-1 text-[11px] font-bold text-blue-600 ring-1 ring-blue-100"
                >
                  {{ appliedReferenceType }}
                </span>
                <button
                  v-for="tag in appliedReferenceTags"
                  :key="tag"
                  type="button"
                  class="inline-flex items-center gap-1 rounded-full bg-slate-50 px-3 py-1 text-[11px] font-bold text-slate-600 ring-1 ring-slate-100 transition-colors hover:bg-slate-100"
                  @click="removeAppliedTag(tag)"
                >
                  {{ tag }}
                  <span class="material-symbols-outlined text-[14px]">close</span>
                </button>
              </div>

              <div class="space-y-4">
                <article
                  v-for="item in filteredReferences"
                  :key="item.id"
                  class="overflow-hidden rounded-[22px] border border-slate-100 bg-white shadow-sm"
                >
                  <div class="h-44 bg-slate-100">
                    <img :src="item.img" :alt="item.title" class="h-full w-full object-cover" />
                  </div>
                  <div class="p-4">
                    <div class="flex flex-wrap gap-2">
                      <span
                        v-for="tag in item.tags"
                        :key="tag"
                        class="rounded-full bg-slate-100 px-3 py-1 text-[11px] font-black uppercase tracking-[0.14em] text-slate-500"
                      >
                        {{ tag }}
                      </span>
                    </div>
                    <h4 class="mt-4 text-lg font-black text-blue-600">{{ item.title }}</h4>
                    <p class="mt-1 text-sm text-slate-500">{{ item.desc }}</p>
                  </div>
                </article>

                <p v-if="filteredReferences.length === 0" class="rounded-[20px] bg-slate-50 p-5 text-sm text-slate-500">
                  조건에 맞는 레퍼런스가 없습니다.
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </aside>

    <Transition name="fade-scale">
      <div
        v-if="activeModal === 'history'"
        class="fixed inset-0 z-[80] flex items-center justify-center bg-slate-950/40 px-4 py-6"
        @click.self="closeModal"
      >
        <div class="h-[88vh] max-h-[960px] w-full max-w-[1120px] overflow-hidden rounded-[24px] bg-white shadow-2xl">
          <ChangeHistory
            :historyData="historyData"
            :initialVersion="historyModalVersion"
            :startInDiff="true"
            class="h-full"
            @close="closeModal"
          />
        </div>
      </div>
    </Transition>

    <Transition name="fade-scale">
      <div
        v-if="activeModal === 'reference-filter'"
        class="fixed inset-0 z-[80] flex items-center justify-center bg-slate-950/40 px-4 py-6"
        @click.self="closeModal"
      >
        <div class="w-full max-w-[720px] overflow-hidden rounded-[24px] bg-white shadow-2xl">
          <div class="border-b border-slate-100 px-6 py-5">
            <p class="text-[10px] font-black uppercase tracking-[0.24em] text-slate-400">Reference Filter</p>
            <h3 class="mt-1 text-lg font-black text-slate-900">필터 및 태그</h3>
            <p class="mt-2 text-sm leading-6 text-slate-500">
              검색 결과를 좁히기 위해 유형과 태그를 선택하세요.
            </p>
          </div>

          <div class="space-y-5 p-6">
            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 text-xl">search</span>
              <input
                v-model="referenceSearchQuery"
                type="text"
                placeholder="키워드 검색..."
                class="w-full rounded-[20px] border border-slate-100 bg-slate-50 py-4 pl-12 pr-4 text-sm text-slate-700 outline-none transition-colors placeholder:text-slate-300 focus:border-blue-200 focus:bg-white focus:ring-4 focus:ring-blue-50"
              />
            </div>

            <div class="space-y-3">
              <p class="text-sm font-black text-slate-900">유형</p>
              <div class="grid grid-cols-2 gap-2">
                <button
                  v-for="type in referenceTypes"
                  :key="type"
                  type="button"
                  class="rounded-full px-4 py-3 text-sm font-black transition-all"
                  :class="
                    draftReferenceType === type
                      ? 'bg-blue-50 text-blue-600 ring-1 ring-blue-100'
                      : 'bg-slate-50 text-slate-500 hover:bg-slate-100'
                  "
                  @click="draftReferenceType = type"
                >
                  {{ type }}
                </button>
              </div>
            </div>

            <div class="space-y-3">
              <p class="text-sm font-black text-slate-900">태그</p>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="tag in availableTags"
                  :key="tag"
                  type="button"
                  class="rounded-full px-3 py-2 text-[11px] font-bold transition-all"
                  :class="
                    draftReferenceTags.includes(tag)
                      ? 'bg-blue-50 text-blue-600 ring-1 ring-blue-100'
                      : 'bg-slate-50 text-slate-500 hover:bg-slate-100'
                  "
                  @click="toggleDraftTag(tag)"
                >
                  {{ tag }}
                </button>
              </div>
            </div>

            <div class="flex items-center justify-between gap-3 pt-2">
              <button
                type="button"
                class="rounded-full bg-slate-100 px-4 py-2 text-xs font-black text-slate-600 transition-colors hover:bg-slate-200"
                @click="clearReferenceFilters"
              >
                초기화
              </button>

              <div class="flex items-center gap-2">
                <button
                  type="button"
                  class="rounded-full bg-slate-50 px-4 py-2 text-xs font-black text-slate-600 transition-colors hover:bg-slate-100"
                  @click="closeModal"
                >
                  닫기
                </button>
                <button
                  type="button"
                  class="rounded-full bg-slate-900 px-4 py-2 text-xs font-black text-white transition-colors hover:bg-slate-800"
                  @click="applyReferenceFilters"
                >
                  적용
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="fade-scale">
      <div
        v-if="detailModalType"
        class="fixed inset-0 z-[90] flex items-center justify-center bg-slate-950/40 px-4 py-6"
        @click.self="closeModal"
      >
        <div class="w-full max-w-[760px] overflow-hidden border border-slate-200 bg-white shadow-[0_24px_60px_rgba(15,23,42,0.18)]">
          <div class="flex items-start justify-between gap-4 border-b border-slate-200 px-6 py-5">
            <div class="min-w-0">
              <p class="text-[10px] font-black uppercase tracking-[0.24em] text-slate-400">DETAIL EDIT</p>
              <h3 class="mt-1 text-lg font-black text-slate-900">{{ detailModalTitle }}</h3>
              <p class="mt-2 text-sm leading-6 text-slate-500">{{ detailModalDescription }}</p>
            </div>

            <button
              type="button"
              class="flex h-10 w-10 items-center justify-center border border-slate-200 bg-white text-slate-400 transition-colors hover:bg-slate-50 hover:text-slate-900"
              @click="closeModal"
            >
              <span class="material-symbols-outlined text-[20px]">close</span>
            </button>
          </div>

          <div v-if="detailModalType === 'status'" class="space-y-4 p-6">
            <div class="grid grid-cols-2 gap-2 md:grid-cols-5">
              <button
                v-for="option in statusOptions"
                :key="option.value"
                type="button"
                class="border px-4 py-3 text-sm font-black transition-all"
                :class="[
                  option.toneClass,
                  contentStatus === option.value ? 'ring-2 ring-slate-900/10' : 'hover:brightness-[0.98]',
                ]"
                @click="selectDetailStatus(option.value)"
              >
                {{ option.label }}
              </button>
            </div>

            <div class="border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
              현재 상태: <strong class="font-black text-slate-900">{{ currentStatusOption.label }}</strong>
            </div>
          </div>

          <div v-else class="space-y-5 p-6">
            <div
              v-if="selectedDetailMember"
              class="flex items-center justify-between gap-3 border border-slate-200 bg-slate-50 px-3 py-3"
            >
              <div class="flex min-w-0 items-center gap-3">
                <div
                  class="flex h-10 w-10 shrink-0 items-center justify-center border border-slate-200 text-xs font-black text-white"
                  :style="{ backgroundColor: selectedDetailMember.accent }"
                >
                  {{ selectedDetailMember.initials }}
                </div>
                <div class="min-w-0">
                  <p class="truncate text-sm font-black text-slate-950">{{ selectedDetailMember.name }}</p>
                  <p class="truncate text-[11px] leading-5 text-slate-500">{{ selectedDetailMember.role }}</p>
                </div>
              </div>

              <button
                type="button"
                class="flex h-8 w-8 items-center justify-center border border-slate-200 bg-white text-slate-400 transition-colors hover:bg-slate-100 hover:text-slate-900"
                @click="clearDetailSelection"
              >
                <span class="material-symbols-outlined text-[18px]">close</span>
              </button>
            </div>

            <div class="relative">
              <span class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 text-xl">search</span>
              <input
                v-model="detailSearchQuery"
                type="text"
                :placeholder="detailModalType === 'customer' ? '고객사 이름 검색' : '닉네임 또는 이름 검색'"
                class="w-full border border-slate-200 bg-white py-3.5 pl-12 pr-4 text-sm text-slate-700 outline-none transition-colors placeholder:text-slate-300 focus:border-slate-400"
              />
            </div>

            <div class="max-h-[56vh] space-y-2 overflow-y-auto pr-1 no-scrollbar">
              <template v-if="detailModalType === 'customer'">
                <button
                  v-for="customer in filteredDetailCustomers"
                  :key="customer.name"
                  type="button"
                  class="flex w-full items-center justify-between gap-4 border border-slate-200 bg-white px-4 py-3 text-left transition-colors hover:border-slate-300 hover:bg-slate-50"
                  :class="contentCustomer === customer.name ? 'border-slate-400 bg-slate-50' : ''"
                  @click="selectDetailCustomer(customer.name)"
                >
                  <div class="flex min-w-0 items-center gap-3">
                    <div class="flex h-10 w-10 shrink-0 items-center justify-center border border-slate-200 bg-slate-900 text-xs font-black text-white">
                      {{ customer.name.slice(0, 2) }}
                    </div>
                    <div class="min-w-0">
                      <p class="truncate text-sm font-black text-slate-950">{{ customer.name }}</p>
                      <p class="truncate text-[11px] leading-5 text-slate-500">{{ customer.count }}건 사용</p>
                    </div>
                  </div>
                  <span
                    v-if="contentCustomer === customer.name"
                    class="text-[11px] font-black text-blue-600"
                  >
                    선택됨
                  </span>
                </button>
              </template>

              <template v-else>
                <button
                  v-for="member in filteredDetailMembers"
                  :key="member.id"
                  type="button"
                  class="flex w-full items-center justify-between gap-4 border border-slate-200 bg-white px-4 py-3 text-left transition-colors hover:border-slate-300 hover:bg-slate-50"
                  :class="
                    (detailModalType === 'planner' && contentPlannerId === member.id) ||
                    (detailModalType === 'assignee' && contentAssigneeId === member.id)
                      ? 'border-slate-400 bg-slate-50'
                      : ''
                  "
                  @click="selectDetailMember(member)"
                >
                  <div class="flex min-w-0 items-center gap-3">
                    <div
                      class="flex h-10 w-10 shrink-0 items-center justify-center border border-slate-200 text-xs font-black text-white"
                      :style="{ backgroundColor: member.accent }"
                    >
                      {{ member.initials }}
                    </div>
                    <div class="min-w-0">
                      <p class="truncate text-sm font-black text-slate-950">{{ member.name }}</p>
                      <p class="truncate text-[11px] leading-5 text-slate-500">{{ member.role }}</p>
                    </div>
                  </div>
                  <span
                    v-if="
                      (detailModalType === 'planner' && contentPlannerId === member.id) ||
                      (detailModalType === 'assignee' && contentAssigneeId === member.id)
                    "
                    class="text-[11px] font-black text-blue-600"
                  >
                    선택됨
                  </span>
                </button>
              </template>

              <p
                v-if="
                  (detailModalType === 'customer' && filteredDetailCustomers.length === 0) ||
                  ((detailModalType === 'planner' || detailModalType === 'assignee') && filteredDetailMembers.length === 0)
                "
                class="border border-dashed border-slate-200 bg-slate-50 px-4 py-6 text-sm text-slate-500"
              >
                검색 결과가 없습니다.
              </p>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </main>
</template>

<style scoped>
.content-editor-shell {
  background:
    radial-gradient(circle at top left, rgba(255, 255, 255, 0.92), transparent 38%),
    linear-gradient(180deg, #f7f9fc 0%, #eef3f8 100%);
}

.content-editor-shell :deep([class*='rounded-']) {
  border-radius: 4px !important;
}

.content-editor-shell :deep(.shadow-2xl) {
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12) !important;
}

.content-editor-shell :deep(.shadow-xl) {
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.1) !important;
}

.content-editor-shell :deep(.shadow-lg) {
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.08) !important;
}

.content-editor-shell :deep(.shadow-md) {
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.06) !important;
}

.editor-shell :deep(.ce-block__content),
.editor-shell :deep(.ce-toolbar__content) {
  max-width: none;
}

.editor-shell :deep(.ce-toolbar__settings-btn),
.editor-shell :deep(.ce-toolbar__plus) {
  color: rgb(100 116 139);
}

.title-editor :deep(.codex-editor__redactor) {
  padding-bottom: 0;
}

.title-editor :deep(.ce-toolbar) {
  display: none;
}

.title-editor :deep(.ce-block) {
  margin: 0;
}

.title-editor :deep(.ce-header) {
  margin: 0;
  padding: 0;
  font-size: clamp(2rem, 3vw, 3.1rem);
  line-height: 1.08;
  font-weight: 800;
  color: rgb(15 23 42);
}

.body-editor :deep(.codex-editor__redactor) {
  padding-bottom: 32px;
}

.body-editor :deep(.ce-block) {
  margin: 0;
}

.body-editor :deep(.ce-paragraph) {
  font-size: 15px;
  line-height: 1.9;
  color: rgb(51 65 85);
}

.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.no-scrollbar::-webkit-scrollbar {
  display: none;
}

.fade-scale-enter-active,
.fade-scale-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.fade-scale-enter-from,
.fade-scale-leave-to {
  opacity: 0;
  transform: translateY(8px) scale(0.98);
}
</style>
