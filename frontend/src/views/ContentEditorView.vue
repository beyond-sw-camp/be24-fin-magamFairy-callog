<script setup>
import { ref, computed, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import ChangeHistory from '@/components/content/ChangeHistory.vue'

const showReference = ref(false)
const showSummaryModal = ref(false)
const showHistoryModal = ref(false)
const showReviewModal = ref(false)
const showFilterModal = ref(false)

const panelWidth = ref(420)
const isResizing = ref(false)

// Content State
const contentTitle = ref('소셜 미디어 캠페인: 2024년 여름 컬렉션 디지털 마케팅 전략')
const sectionTitle = ref('캠페인 개요 및 시각적 방향성')
const contentBody = ref(
  '본 컨텐츠 카드는 2024년 여름 시즌을 겨냥한 통합 마케팅 전략의 핵심 자산입니다. MZ세대의 감성을 자극하는 고채도의 비주얼과 건축적인 미니멀리즘이 조화를 이루는 디자인을 지향합니다.',
)

const route = useRoute()
const router = useRouter()
const store = usePlannerStore()

const defaultPalette = {
  accent: '#2f80ed',
  surface: '#eaf3ff',
  text: '#1d4f99',
}

const contentStatus = ref('planned')
const contentPriority = ref('medium')
const contentAssigneeId = ref('')
const contentStartDate = ref('')
const contentDueDate = ref('')
const contentCustomer = ref('')
const contentTypeValue = ref('')
const contentTagsText = ref('')
const contentAttachments = ref([])
const contentPalette = ref({ ...defaultPalette })

function getRouteParam(value) {
  if (Array.isArray(value)) {
    return value[0] ?? ''
  }

  return typeof value === 'string' ? value : ''
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

const taskStatusLabel = computed(() => store.statusLabels[contentStatus.value] ?? contentStatus.value)
const taskPriorityLabel = computed(
  () => store.priorityLabels[contentPriority.value] ?? contentPriority.value,
)
const assigneeMember = computed(() => store.findMember(contentAssigneeId.value))
const contentSummaryChips = computed(() => [
  { label: '상태', value: taskStatusLabel.value },
  { label: '우선순위', value: taskPriorityLabel.value },
  { label: '담당자', value: assigneeMember.value?.name ?? '미지정' },
  { label: '마감일', value: contentDueDate.value || '미정' },
  { label: '유형', value: contentTypeValue.value || '미지정' },
])

const sectionVisual = computed(() => {
  const requirementId = activeTask.value?.requirementId ?? ''
  const isContentList = /^CONTENTLIST_\d+$/i.test(requirementId)

  return {
    icon: isCreateMode.value ? 'note_add' : isContentList ? 'view_list' : 'article',
    label: isCreateMode.value ? '새 콘텐츠 카드' : contentTypeValue.value || '콘텐츠 카드',
    hint: requirementId || 'EDITOR',
  }
})

function getAttachmentIconName(attachment) {
  const extension = String(attachment ?? '')
    .split('.')
    .pop()
    ?.toLowerCase() ?? ''

  if (['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'avif'].includes(extension)) {
    return 'image'
  }

  if (['pdf'].includes(extension)) {
    return 'picture_as_pdf'
  }

  if (['fig'].includes(extension)) {
    return 'design_services'
  }

  if (['xls', 'xlsx', 'csv'].includes(extension)) {
    return 'table_chart'
  }

  if (['mp4', 'mov', 'webm'].includes(extension)) {
    return 'movie'
  }

  if (['zip', 'rar', '7z'].includes(extension)) {
    return 'folder_zip'
  }

  return 'description'
}

const assetCards = computed(() => {
  const attachments =
    contentAttachments.value.length > 0
      ? contentAttachments.value
      : ['content-outline.md', 'reference-board.fig']

  const tones = [
    {
      background: 'linear-gradient(135deg, #eff6ff, #dbeafe)',
      accent: '#2563eb',
    },
    {
      background: 'linear-gradient(135deg, #f5f3ff, #e9d5ff)',
      accent: '#7c3aed',
    },
  ]

  return attachments.slice(0, 2).map((attachment, index) => {
    const ext = attachment.split('.').pop()?.toUpperCase() ?? 'FILE'
    const tone = tones[index % tones.length]

    return {
      id: `${attachment}-${index}`,
      attachment,
      ext,
      icon: getAttachmentIconName(attachment),
      tone,
      note: isCreateMode.value ? '새 카드 첨부' : `${contentTypeValue.value || '콘텐츠'} 첨부`,
    }
  })
})

function seedEditorState(task) {
  const seededDate = routeSeedDate.value

  if (!task) {
    contentTitle.value = '새 콘텐츠 카드'
    sectionTitle.value = '카드 개요'
    contentBody.value = ''
    contentStatus.value = 'planned'
    contentPriority.value = 'medium'
    contentAssigneeId.value = store.currentUserId
    contentStartDate.value = seededDate
    contentDueDate.value = seededDate
    contentCustomer.value = '미지정'
    contentTypeValue.value = '콘텐츠 카드'
    contentTagsText.value = ''
    contentAttachments.value = []
    contentPalette.value = { ...defaultPalette }
    return
  }

  contentTitle.value = task.title ?? '콘텐츠 카드'
  sectionTitle.value = task.title ?? task.requirementId ?? task.contentType ?? '카드 개요'
  contentBody.value = [task.summary, task.description].filter(Boolean).join('\n\n')
  contentStatus.value = task.status ?? 'planned'
  contentPriority.value = task.priority ?? 'medium'
  contentAssigneeId.value = task.assigneeId ?? store.currentUserId
  contentStartDate.value = task.startDate ?? seededDate
  contentDueDate.value = task.dueDate ?? seededDate
  contentCustomer.value = task.customer ?? '미지정'
  contentTypeValue.value = task.contentType ?? '콘텐츠'
  contentTagsText.value = (task.tags ?? []).join(', ')
  contentAttachments.value = [...(task.attachments ?? [])]
  contentPalette.value = task.palette ? { ...task.palette } : { ...defaultPalette }
}

function buildPayload() {
  const active = activeTask.value
  const tags = contentTagsText.value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)

  const body = contentBody.value.trim()
  const summary = body.split(/\n\s*\n/)[0]?.trim() || contentTitle.value.trim()
  const description = body || summary
  const baseId = active?.id ?? `CONTENT_${Date.now().toString(36).toUpperCase()}`

  return {
    id: baseId,
    requirementId: active?.requirementId ?? baseId,
    title: contentTitle.value.trim() || '새 콘텐츠 카드',
    category: active?.category ?? '콘텐츠 카드',
    summary,
    description,
    assigneeId: contentAssigneeId.value || active?.assigneeId || store.currentUserId,
    plannerId: active?.plannerId ?? store.currentUserId,
    designerId: active?.designerId ?? 'sumin',
    publisherId: active?.publisherId ?? 'minseo',
    supervisorId: active?.supervisorId ?? 'taeyoung',
    startDate: contentStartDate.value || active?.startDate || routeSeedDate.value,
    dueDate: contentDueDate.value || active?.dueDate || routeSeedDate.value,
    status: contentStatus.value,
    priority: contentPriority.value,
    customer: contentCustomer.value || active?.customer || '미지정',
    contentType: contentTypeValue.value || active?.contentType || '콘텐츠',
    visibility: active?.visibility ?? 'personal',
    timeRange: active?.timeRange ?? '10:00 - 13:00',
    tags,
    attachments: [...contentAttachments.value],
    history: [
      ...(active?.history ?? []),
      isCreateMode.value
        ? '콘텐츠 에디터에서 새 카드가 생성되었습니다.'
        : '콘텐츠 에디터에서 카드 정보가 수정되었습니다.',
    ],
    progress: active?.progress ?? 0,
    palette: { ...contentPalette.value },
  }
}

function saveContent() {
  const payload = buildPayload()

  if (isCreateMode.value) {
    store.createTask(payload)
    router.replace({ name: 'content-editor', params: { contentId: payload.id } })
    return
  }

  store.updateTask(payload.id, payload)
}

watch(
  [activeTask, routeSeedDate],
  ([task]) => {
    seedEditorState(task)
  },
  { immediate: true },
)

// Search and Filters
const searchQuery = ref('')
const appliedType = ref('전체')
const appliedTags = ref([])
const tempSelectedType = ref('전체')
const tempSelectedTags = ref([])

const contentTypes = ['전체', '이미지', '영상', '문서']
const availableTags = [
  'SocialMedia',
  'Marketing',
  'Minimalism',
  'Architecture',
  'Summer2024',
  'DigitalStrategy',
  'MZSense',
]

const referenceItems = ref([
  {
    id: 1,
    title: '2024 미니멀리즘 트렌드',
    desc: '핀터레스트 기반 무드보드',
    type: '이미지',
    tags: ['Minimalism', 'Architecture'],
    img: 'https://lh3.googleusercontent.com/aida-public/AB6AXuB2a3qWuCV1zIhXq9zW0WDOeDarmklCyD64ntkiaX6S6fuxwnD_xQVFh0eRdLDvdVLuoT2lJLzFkXG0t_61lSZPfJ78_LVYxyCSYHH9sCyVwVzOQaCGEuw6Ep5DFp3JcIC3GPvKqVEAYnszIfBUrQM4DoTzWs4Od9d_VmNG-45mKSS2m_bblFjNJtQNmHz5LK8lhN5VUlKi9PhSrNFBLvCHSWJeFxXEA2uyw4tA1HEF3Ucb37IeBDZuzB4MQ6jUKFGc2o-SzOGBtMjT',
  },
  {
    id: 2,
    title: '마케팅 성과 지표 가이드',
    desc: '내부 전략 레포트',
    type: '문서',
    tags: ['Marketing', 'DigitalStrategy'],
    img: 'https://lh3.googleusercontent.com/aida-public/AB6AXuDju0k1sHMSMK3KEmdJLJpQtUR0r92RbnKKNI0Ja1lK-JgUgc0mBZ2dyH6xgag1KcbTduurSvXraTEvjK5l2Ol3ZaBnj8Fshv_xv4o3R-zkq8cCoUOfEnSJH-tVdP28NKdHHwZDErjocAPI5-Ao4LZCFV_qBJ_KDbHZJAuNV4ehmPqC_qze69kCayP3Uf-JUmh1QmK9e9Ezmr-cU247bYPxIee3ByR9Tp7dQwPEbPdaIWQGs7cSsixxS2mgO3tVZuw4fcboxSpS4xnM',
  },
])

const filteredReferences = computed(() => {
  return referenceItems.value.filter((item) => {
    const matchesSearch =
      item.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      item.desc.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchesType = appliedType.value === '전체' || item.type === appliedType.value
    const matchesTags =
      appliedTags.value.length === 0 || appliedTags.value.every((tag) => item.tags.includes(tag))
    return matchesSearch && matchesType && matchesTags
  })
})

const historyData = ref([
  {
    version: 'v1.4',
    worker: '김민준',
    workerInitials: 'MJ',
    change: '이미지 레이아웃 수정 및 텍스트 보강',
    date: '2024-05-18 14:20',
    diff: {
      title: { previous: 'Tool Reference (v2.0.3)', current: 'Tool Reference (v2.0.4)' },
      details: {
        previous: { 마감일: '2024.06.12', 포맷: '포스트', 고객사: '네오-테크' },
        current: { 마감일: '2024.06.15', 포맷: '포스트 / 릴스', 고객사: '네오-테크' },
      },
      content: {
        lines: [
          {
            left: { number: 1, text: '## 이전 버전 본문 내용입니다.', kind: 'changed' },
            right: { number: 1, text: '## 새로운 버전 본문 내용입니다.', kind: 'changed' },
          },
          {
            left: { number: 2, text: '기존 이미지가 위치하던 곳입니다.', kind: 'removed' },
            right: null,
          },
          {
            left: null,
            right: { number: 2, text: '새로운 이미지가 추가되었습니다.', kind: 'added' },
          },
        ],
      },
    },
  },
  {
    version: 'v1.3',
    worker: '이유진',
    workerInitials: 'YJ',
    change: '캠페인 타겟팅 문구 업데이트',
    date: '2024-05-15 10:45',
    diff: {
      title: { previous: '...', current: '...' },
      details: { previous: {}, current: {} },
      content: { lines: [] },
    },
  },
  {
    version: 'v1.2',
    worker: '민경아',
    workerInitials: 'MK',
    change: '비주얼 레퍼런스 이미지 추가',
    date: '2024-05-12 16:30',
    diff: {
      title: { previous: '...', current: '...' },
      details: { previous: {}, current: {} },
      content: { lines: [] },
    },
  },
  {
    version: 'v1.1',
    worker: '현승원',
    workerInitials: 'HS',
    change: '초안 작성 및 구조 설계',
    date: '2024-05-10 09:15',
    diff: {
      title: { previous: '...', current: '...' },
      details: { previous: {}, current: {} },
      content: { lines: [] },
    },
  },
  {
    version: 'v1.0',
    worker: '박준서',
    workerInitials: 'JS',
    change: '프로젝트 생성',
    date: '2024-05-08 11:00',
    diff: {
      title: { previous: '...', current: '...' },
      details: { previous: {}, current: {} },
      content: { lines: [] },
    },
  },
])

function applyFilters() {
  appliedType.value = tempSelectedType.value
  appliedTags.value = [...tempSelectedTags.value]
  showFilterModal.value = false
}

function removeFilter(tag) {
  appliedTags.value = appliedTags.value.filter((t) => t !== tag)
  tempSelectedTags.value = tempSelectedTags.value.filter((t) => t !== tag)
}

function clearAllFilters() {
  appliedType.value = '전체'
  appliedTags.value = []
  tempSelectedType.value = '전체'
  tempSelectedTags.value = []
}

function startResizing(e) {
  isResizing.value = true
  document.addEventListener('mousemove', resizePanel)
  document.addEventListener('mouseup', stopResizing)
  document.body.style.cursor = 'col-resize'
  document.body.style.userSelect = 'none'
}

function stopResizing() {
  isResizing.value = false
  document.removeEventListener('mousemove', resizePanel)
  document.removeEventListener('mouseup', stopResizing)
  document.body.style.cursor = 'default'
  document.body.style.userSelect = 'auto'
}

function resizePanel(e) {
  if (!isResizing.value) return
  const newWidth = window.innerWidth - e.clientX
  if (newWidth > 360 && newWidth < 800) {
    panelWidth.value = newWidth
  }
}

onBeforeUnmount(() => {
  stopResizing()
})
</script>

<template>
  <main class="flex-1 flex flex-col min-h-screen transition-all duration-300">
    <!-- Editor Workspace -->
    <div class="flex-1 flex flex-col lg:flex-row overflow-hidden relative bg-slate-50/30">
      <!-- Content Editor -->
      <section class="flex-1 p-10 overflow-y-auto no-scrollbar">
        <div class="max-w-4xl mx-auto space-y-10">
          <div class="flex justify-between items-start">
            <div>
              <h1 class="text-3xl font-extrabold text-slate-900 mb-3 tracking-tight">
                컨텐츠 상세 및 편집
              </h1>
              <p class="text-slate-400 font-medium">{{ contentTitle }}</p>
            </div>
            <div
              class="flex items-center bg-white border border-slate-100 p-1.5 rounded-2xl white-shadow"
            >
              <button
                @click="saveContent"
                class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-bold text-emerald-600 hover:bg-emerald-50 transition-all"
              >
                <span class="material-symbols-outlined text-sm text-emerald-500">save</span>
                <span>저장</span>
              </button>
              <button
                @click="showSummaryModal = true"
                class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-50 transition-all"
              >
                <span class="material-symbols-outlined text-sm text-blue-500">auto_awesome</span>
                <span>AI 요약</span>
              </button>
              <button
                @click="showReviewModal = true"
                class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-50 transition-all"
              >
                <span class="material-symbols-outlined text-sm text-indigo-500">rate_review</span>
                <span>검토 요청</span>
              </button>
              <button
                @click="showHistoryModal = true"
                class="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-bold text-slate-600 hover:bg-slate-50 transition-all"
              >
                <span class="material-symbols-outlined text-sm">list_alt</span>
                <span>변경 이력</span>
              </button>
            </div>
          </div>

          <div class="grid gap-3 rounded-[1.5rem] border border-slate-100 bg-white p-4 white-shadow sm:grid-cols-2 xl:grid-cols-5">
            <div
              v-for="chip in contentSummaryChips"
              :key="chip.label"
              class="grid gap-1 rounded-2xl bg-slate-50 px-4 py-3 border border-slate-100"
            >
              <span class="text-[10px] font-black uppercase tracking-wider text-slate-400">
                {{ chip.label }}
              </span>
              <strong class="text-sm font-extrabold text-slate-900">{{ chip.value }}</strong>
            </div>
          </div>

          <!-- Title Input -->
          <div class="relative group">
            <div class="flex items-center gap-4 rounded-[2rem] border border-slate-100 bg-white px-5 py-4 white-shadow">
              <div class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-blue-50 text-blue-600">
                <span class="material-symbols-outlined text-[28px]">{{ sectionVisual.icon }}</span>
              </div>
              <div class="min-w-0 flex-1">
                <p class="mb-1 text-[10px] font-black uppercase tracking-[0.16em] text-slate-400">
                  {{ sectionVisual.label }}
                </p>
                <input
                  v-model="sectionTitle"
                  class="w-full bg-transparent text-2xl font-extrabold text-slate-900 placeholder:text-slate-300 focus:outline-none tracking-tight"
                  placeholder="컨텐츠 제목을 입력하세요"
                  type="text"
                />
              </div>
            </div>
            <div
              class="absolute right-6 top-1/2 -translate-y-1/2 flex items-center gap-2 opacity-0 group-focus-within:opacity-100 transition-opacity"
            >
              <span
                class="text-[10px] font-black text-blue-500 uppercase tracking-widest bg-blue-50 px-3 py-1.5 rounded-full"
                >Editing Title</span
              >
            </div>
          </div>

          <!-- Editor Canvas -->
          <div
            class="bg-white border border-slate-100 rounded-[2.5rem] white-shadow overflow-hidden flex flex-col"
          >
            <!-- Toolbar -->
            <div
              class="px-6 py-4 border-b border-slate-50 flex items-center justify-between bg-white/50 backdrop-blur-sm sticky top-0 z-10"
            >
              <div class="flex items-center gap-1">
                <button
                  type="button"
                  title="굵게"
                  aria-label="굵게"
                  class="p-2 hover:bg-slate-50 rounded-lg text-slate-400 hover:text-slate-900 transition-colors"
                >
                  <span class="material-symbols-outlined text-[20px]">format_bold</span>
                </button>
                <button
                  type="button"
                  title="기울임"
                  aria-label="기울임"
                  class="p-2 hover:bg-slate-50 rounded-lg text-slate-400 hover:text-slate-900 transition-colors"
                >
                  <span class="material-symbols-outlined text-[20px]">format_italic</span>
                </button>
                <button
                  type="button"
                  title="목록"
                  aria-label="목록"
                  class="p-2 hover:bg-slate-50 rounded-lg text-slate-400 hover:text-slate-900 transition-colors"
                >
                  <span class="material-symbols-outlined text-[20px]">format_list_bulleted</span>
                </button>
                <div class="w-px h-6 bg-slate-100 mx-2"></div>
                <button
                  type="button"
                  title="링크"
                  aria-label="링크"
                  class="p-2 hover:bg-slate-50 rounded-lg text-slate-400 hover:text-slate-900 transition-colors"
                >
                  <span class="material-symbols-outlined text-[20px]">link</span>
                </button>
              </div>
              <div class="flex items-center gap-2">
                <button
                  type="button"
                  title="파일 공유"
                  aria-label="파일 공유"
                  class="flex items-center gap-2 px-4 py-2 text-xs font-bold text-blue-600 hover:bg-blue-50 rounded-xl transition-all border border-blue-50"
                >
                  <span class="material-symbols-outlined text-lg">upload_file</span>
                </button>
                <button
                  type="button"
                  title="템플릿"
                  aria-label="템플릿"
                  class="flex items-center gap-2 px-4 py-2 text-xs font-bold text-slate-600 hover:bg-slate-50 rounded-xl transition-all border border-slate-100"
                >
                  <span class="material-symbols-outlined text-lg">dynamic_feed</span>
                </button>
              </div>
            </div>

            <!-- Editable Content Area -->
            <div class="p-12 min-h-[700px] outline-none space-y-8">
              <h2
                contenteditable="true"
                class="text-2xl font-extrabold text-slate-900 tracking-tight outline-none"
                @input="sectionTitle = $event.target.innerText"
              >
                {{ sectionTitle }}
              </h2>
              <div
                contenteditable="true"
                class="text-lg leading-relaxed text-slate-500 font-medium outline-none"
                @input="contentBody = $event.target.innerText"
              >
                {{ contentBody }}
              </div>

              <!-- Assets -->
              <div class="grid grid-cols-2 gap-6 my-10">
                <article
                  v-for="asset in assetCards"
                  :key="asset.id"
                  class="relative aspect-video overflow-hidden rounded-2xl border border-slate-100 shadow-[0_16px_30px_rgba(15,23,42,0.08)]"
                  :style="{ background: asset.tone.background }"
                  :title="asset.attachment"
                >
                  <div class="flex h-full flex-col justify-between p-6">
                    <div class="flex items-start justify-between gap-4">
                      <span
                        class="inline-flex w-fit items-center rounded-full bg-white/75 px-3 py-1 text-[0.7rem] font-black tracking-[0.14em] text-slate-700 shadow-sm backdrop-blur-sm"
                        :style="{ color: asset.tone.accent }"
                      >
                        {{ asset.ext }}
                      </span>
                      <div class="flex h-16 w-16 items-center justify-center rounded-2xl bg-white/75 shadow-sm backdrop-blur-sm">
                        <span class="material-symbols-outlined text-[30px]" :style="{ color: asset.tone.accent }">
                          {{ asset.icon }}
                        </span>
                      </div>
                    </div>
                    <div class="grid gap-1">
                      <p class="text-[0.72rem] font-bold uppercase tracking-[0.14em] text-slate-600/70">
                        {{ asset.note }}
                      </p>
                      <strong class="sr-only">{{ asset.attachment }}</strong>
                    </div>
                  </div>
                  <div
                    class="pointer-events-none absolute inset-0 border-2 border-white/60 opacity-70"
                    :style="{ boxShadow: `inset 0 0 0 1px ${asset.tone.accent}22` }"
                  />
                </article>
              </div>

              <p
                contenteditable="true"
                class="text-lg leading-relaxed text-slate-500 font-medium outline-none"
              >
                주요 소구 포인트는 '자연스러운 여유(Natural Ease)'입니다. 복잡한 텍스트보다는
                직관적인 이미지와 숏폼 영상을 활용하여 사용자들의 즉각적인 인터랙션을 유도하는 것이
                이번 캠페인의 주요 목표입니다.
              </p>
            </div>
          </div>
        </div>
      </section>

      <!-- Property Sidebar -->
      <aside
        class="w-full lg:w-96 bg-white border-l border-slate-100 p-8 overflow-y-auto no-scrollbar space-y-10"
      >
        <div>
          <h3 class="text-[11px] font-extrabold text-slate-400 tracking-[0.15em] uppercase mb-8">
            프로젝트 상세 속성
          </h3>
          <div class="space-y-8">
            <div
              class="flex justify-between items-center p-5 bg-slate-50 rounded-2xl border border-slate-100/50"
            >
              <div>
                <p
                  class="text-[10px] font-extrabold text-slate-400 uppercase tracking-wider mb-1.5"
                >
                  상태
                </p>
                <div class="flex items-center gap-2">
                  <span class="w-2 h-2 rounded-full bg-blue-500 animate-pulse"></span>
                  <span class="text-sm font-extrabold text-slate-900">편집 중</span>
                </div>
              </div>
              <div class="text-right">
                <p
                  class="text-[10px] font-extrabold text-slate-400 uppercase tracking-wider mb-1.5"
                >
                  관리자
                </p>
                <p class="text-sm font-extrabold text-slate-900">박준서 실장</p>
              </div>
            </div>

            <div class="space-y-6">
              <div class="group">
                <p class="text-[10px] font-extrabold text-slate-400 uppercase tracking-wider mb-3">
                  기획자 (Planners)
                </p>
                <div class="flex flex-wrap gap-2">
                  <div
                    class="flex items-center gap-2 bg-white px-3.5 py-1.5 rounded-full border border-slate-100 shadow-sm text-xs font-bold text-slate-700"
                  >
                    <div
                      class="w-5 h-5 rounded-full bg-blue-600 text-white flex items-center justify-center text-[8px] font-black"
                    >
                      HS
                    </div>
                    현승원
                  </div>
                  <div
                    class="flex items-center gap-2 bg-white px-3.5 py-1.5 rounded-full border border-slate-100 shadow-sm text-xs font-bold text-slate-700"
                  >
                    <div
                      class="w-5 h-5 rounded-full bg-slate-200 text-slate-700 flex items-center justify-center text-[8px] font-black"
                    >
                      YJ
                    </div>
                    이유진
                  </div>
                </div>
              </div>
              <div class="group">
                <p class="text-[10px] font-extrabold text-slate-400 uppercase tracking-wider mb-3">
                  디자이너 (Designers)
                </p>
                <div class="flex flex-wrap gap-2">
                  <div
                    class="flex items-center gap-2 bg-white px-3.5 py-1.5 rounded-full border border-slate-100 shadow-sm text-xs font-bold text-slate-700"
                  >
                    <div
                      class="w-5 h-5 rounded-full bg-slate-900 text-white flex items-center justify-center text-[8px] font-black"
                    >
                      MK
                    </div>
                    민경아
                  </div>
                </div>
              </div>
            </div>

            <div class="space-y-4 pt-6 border-t border-slate-50">
              <div class="flex justify-between items-center">
                <span class="text-sm font-medium text-slate-400">마감일</span
                ><span class="text-sm font-extrabold text-red-500">2024.06.15</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-sm font-medium text-slate-400">포맷</span
                ><span class="text-sm font-extrabold text-slate-900">포스트 / 릴스</span>
              </div>
              <div class="flex justify-between items-center">
                <span class="text-sm font-medium text-slate-400">고객사</span
                ><span class="text-sm font-extrabold text-slate-900">네오-테크</span>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- Reference Sidebar Toggle -->
      <button
        v-if="!showReference"
        @click="showReference = true"
        class="fixed right-0 top-15 z-[45] bg-white border border-slate-100 border-r-0 rounded-l-2xl p-2.5 shadow-[-10px_0_30px_rgba(0,0,0,0.03)] hover:bg-slate-50 transition-all flex items-center justify-center group animate-pulse hover:animate-none"
      >
        <span
          class="material-symbols-outlined text-slate-900 font-bold group-hover:scale-110 transition-transform"
          >chevron_left</span
        >
      </button>

      <!-- Reference Panel -->
      <Transition name="reference-panel">
        <aside
          v-if="showReference"
          class="fixed top-0 right-0 bottom-0 z-[50] bg-white border-l border-slate-100 shadow-2xl flex flex-col"
          :style="{ width: panelWidth + 'px' }"
        >
          <div
            class="absolute left-0 top-0 bottom-0 w-1 cursor-col-resize hover:bg-blue-500/20 transition-colors"
            @mousedown="startResizing"
          ></div>

          <header class="p-6 border-b border-slate-50 flex justify-between items-center">
            <h3 class="text-lg font-extrabold text-slate-900 tracking-tight">레퍼런스 탐색</h3>
            <button @click="showReference = false" class="p-2 hover:bg-slate-100 rounded-full">
              <span class="material-symbols-outlined">close</span>
            </button>
          </header>

          <div class="p-6 flex-1 overflow-y-auto no-scrollbar space-y-6">
            <div class="relative">
              <span
                class="material-symbols-outlined absolute left-4 top-1/2 -translate-y-1/2 text-slate-400 text-xl"
                >search</span
              >
              <input
                v-model="searchQuery"
                type="text"
                placeholder="키워드 검색..."
                class="w-full pl-11 pr-4 py-3 bg-slate-50 border border-slate-100 rounded-xl focus:ring-2 focus:ring-blue-100 focus:border-blue-200 outline-none transition-all"
              />
            </div>

            <div class="flex items-center justify-between">
              <button
                @click="showFilterModal = true"
                class="flex items-center gap-2 text-xs font-bold text-slate-600 bg-white border border-slate-100 px-4 py-2 rounded-lg hover:bg-slate-50 transition-all"
              >
                <span class="material-symbols-outlined text-sm">tune</span> 필터 및 태그
              </button>
              <button
                v-if="appliedTags.length || appliedType !== '전체'"
                @click="clearAllFilters"
                class="text-xs font-bold text-red-500 hover:underline"
              >
                초기화
              </button>
            </div>

            <div class="space-y-4">
              <div v-for="item in filteredReferences" :key="item.id" class="group cursor-pointer">
                <div
                  class="aspect-video bg-slate-100 rounded-2xl overflow-hidden border border-slate-100 mb-3"
                >
                  <img
                    :src="item.img"
                    class="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                  />
                </div>
                <h4
                  class="text-sm font-bold text-slate-900 group-hover:text-blue-600 transition-colors"
                >
                  {{ item.title }}
                </h4>
                <p class="text-xs font-medium text-slate-400 mt-1">{{ item.desc }}</p>
              </div>
            </div>
          </div>
        </aside>
      </Transition>
    </div>

    <!-- Modals -->
    <Transition name="fade">
      <div
        v-if="showHistoryModal"
        class="fixed inset-0 z-[100] flex items-center justify-center p-6 bg-white/60 backdrop-blur-md"
        @click.self="showHistoryModal = false"
      >
        <div class="w-full max-w-5xl h-[85vh]">
          <ChangeHistory :historyData="historyData" @close="showHistoryModal = false" />
        </div>
      </div>
    </Transition>

    <Transition name="fade">
      <div
        v-if="showSummaryModal"
        class="fixed inset-0 z-[100] flex items-center justify-center p-6 bg-white/60 backdrop-blur-md"
        @click.self="showSummaryModal = false"
      >
        <div class="w-full max-w-lg bg-white rounded-3xl p-8 border border-slate-100 shadow-2xl">
          <div class="flex items-center gap-4 mb-6">
            <div
              class="w-12 h-12 rounded-2xl bg-blue-50 flex items-center justify-center text-blue-500"
            >
              <span class="material-symbols-outlined text-2xl">auto_awesome</span>
            </div>
            <h3 class="text-xl font-extrabold text-slate-900 tracking-tight">AI 요약 생성</h3>
          </div>
          <p class="text-sm font-medium text-slate-500 mb-8 leading-relaxed">
            현재 컨텐츠의 핵심 내용을 분석하여 마케팅 요약을 생성합니다. 캠페인 목표와 비주얼
            방향성을 기반으로 최적화된 문구를 제안해 드립니다.
          </p>
          <div class="flex flex-col gap-3">
            <button
              class="w-full py-4 bg-blue-600 text-white font-bold rounded-2xl hover:bg-blue-700 transition-all shadow-lg shadow-blue-200"
            >
              생성 시작하기
            </button>
            <button
              @click="showSummaryModal = false"
              class="w-full py-4 bg-white text-slate-400 font-bold rounded-2xl hover:bg-slate-50 transition-all border border-slate-100"
            >
              닫기
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Filter Modal -->
    <Transition name="fade">
      <div
        v-if="showFilterModal"
        class="fixed inset-0 z-[110] flex items-center justify-center p-6 bg-white/60 backdrop-blur-md"
        @click.self="showFilterModal = false"
      >
        <div class="w-full max-w-md bg-white rounded-3xl p-8 border border-slate-100 shadow-2xl">
          <h3 class="text-xl font-extrabold text-slate-900 mb-6 tracking-tight">
            필터 및 태그 설정
          </h3>
          <div class="space-y-6">
            <div>
              <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-3">
                컨텐츠 유형
              </p>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="type in contentTypes"
                  :key="type"
                  @click="tempSelectedType = type"
                  :class="
                    tempSelectedType === type
                      ? 'bg-blue-600 text-white border-blue-600'
                      : 'bg-white text-slate-600 border-slate-100 hover:bg-slate-50'
                  "
                  class="px-4 py-2 rounded-xl text-xs font-bold border transition-all"
                >
                  {{ type }}
                </button>
              </div>
            </div>
            <div>
              <p class="text-[10px] font-black text-slate-400 uppercase tracking-widest mb-3">
                태그 선택
              </p>
              <div class="flex flex-wrap gap-2">
                <button
                  v-for="tag in availableTags"
                  :key="tag"
                  @click="
                    tempSelectedTags.includes(tag)
                      ? (tempSelectedTags = tempSelectedTags.filter((t) => t !== tag))
                      : tempSelectedTags.push(tag)
                  "
                  :class="
                    tempSelectedTags.includes(tag)
                      ? 'bg-slate-900 text-white border-slate-900'
                      : 'bg-white text-slate-600 border-slate-100 hover:bg-slate-50'
                  "
                  class="px-3 py-1.5 rounded-lg text-xs font-bold border transition-all"
                >
                  #{{ tag }}
                </button>
              </div>
            </div>
          </div>
          <div class="flex gap-3 mt-10">
            <button
              @click="applyFilters"
              class="flex-1 py-4 bg-slate-900 text-white font-bold rounded-2xl hover:bg-slate-800 transition-all"
            >
              적용하기
            </button>
            <button
              @click="showFilterModal = false"
              class="flex-1 py-4 bg-white text-slate-400 font-bold rounded-2xl hover:bg-slate-50 transition-all border border-slate-100"
            >
              취소
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </main>
</template>

<style scoped>
.material-symbols-outlined {
  font-family: 'Material Symbols Outlined';
  font-weight: normal;
  font-style: normal;
  line-height: 1;
  letter-spacing: normal;
  text-transform: none;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  direction: ltr;
  -webkit-font-smoothing: antialiased;
  font-feature-settings: 'liga';
  font-variation-settings:
    'FILL' 0,
    'wght' 400,
    'GRAD' 0,
    'opsz' 24;
}
.no-scrollbar::-webkit-scrollbar {
  display: none;
}
.no-scrollbar {
  -ms-overflow-style: none;
  scrollbar-width: none;
}
.white-shadow {
  box-shadow:
    0 4px 20px -2px rgba(0, 0, 0, 0.03),
    0 2px 8px -1px rgba(0, 0, 0, 0.02);
}

.reference-panel-enter-active,
.reference-panel-leave-active {
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}
.reference-panel-enter-from,
.reference-panel-leave-to {
  transform: translateX(100%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
