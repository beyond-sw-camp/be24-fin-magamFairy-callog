<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import EditorJS from '@editorjs/editorjs'
import Header from '@editorjs/header'
import Paragraph from '@editorjs/paragraph'
import List from '@editorjs/list'
import Quote from '@editorjs/quote'
import Table from '@editorjs/table'
import CodeTool from '@editorjs/code'
import Embed from '@editorjs/embed'
import ImageTool from '@editorjs/image'
import InlineCode from '@editorjs/inline-code'
import Delimiter from '@editorjs/delimiter'
import Marker from '@editorjs/marker'
import Warning from '@editorjs/warning'
import RawTool from '@editorjs/raw'
import AlignmentTuneTool from 'editorjs-text-alignment-blocktune'
import { usePlannerStore } from '@/stores/planner'
import ChangeHistory from '@/components/content/ChangeHistory.vue'

const ENABLE_CONTENT_RAIL = true
const ENABLE_EDITOR_JS = true
const HEADER_OFFSET = 88
const RAIL_WIDTH = 64
const DEFAULT_PANEL_WIDTH = 384
const MIN_PANEL_WIDTH = 320
const MAX_PANEL_WIDTH = 560

const defaultPalette = {
  accent: '#2f80ed',
  surface: '#eaf3ff',
  text: '#1d4f99',
}

const referenceTypes = ['전체', '이미지', '영상', '문서']
const availableTags = ['SocialMedia', 'Marketing', 'Minimalism', 'Architecture', 'Summer2024', 'DigitalStrategy', 'MZSense']

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

function parseEditorData(value) {
  if (!value) {
    return null
  }

  if (typeof value === 'object' && Array.isArray(value.blocks)) {
    return value
  }

  if (typeof value === 'string') {
    try {
      const parsed = JSON.parse(value)
      return parsed && Array.isArray(parsed.blocks) ? parsed : null
    } catch {
      return null
    }
  }

  return null
}

function blocksToPlainText(blocks = []) {
  return blocks
    .map((block) => {
      if (!block) return ''

      if (block.type === 'header') {
        return htmlToPlainText(block.data?.text)
      }

      if (block.type === 'paragraph') {
        return htmlToPlainText(block.data?.text)
      }

      if (block.type === 'quote') {
        return [block.data?.text, block.data?.caption].filter(Boolean).map(htmlToPlainText).join(' ')
      }

      if (block.type === 'list') {
        return Array.isArray(block.data?.items)
          ? block.data.items.map(htmlToPlainText).join(' ')
          : ''
      }

      if (block.type === 'table') {
        return Array.isArray(block.data?.content)
          ? block.data.content.flat().map(htmlToPlainText).join(' ')
          : ''
      }

      if (block.type === 'warning') {
        return [block.data?.title, block.data?.message].filter(Boolean).map(htmlToPlainText).join(' ')
      }

      if (block.type === 'code') {
        return htmlToPlainText(block.data?.code)
      }

      return ''
    })
    .filter(Boolean)
    .join('\n\n')
    .trim()
}

function buildBodyEditorData(text) {
  const normalizedText = String(text ?? '').trim()
  const paragraphs = normalizedText
    ? normalizedText
        .split(/\n{2,}/)
        .map((part) => part.trim())
        .filter(Boolean)
    : ['본문을 입력하세요.']

  return {
    time: Date.now(),
    blocks: paragraphs.map((paragraph) => ({
      type: 'paragraph',
      data: { text: paragraph },
    })),
  }
}

function buildTitleEditorData(title) {
  return {
    time: Date.now(),
    blocks: [
      {
        type: 'header',
        data: {
          text: title || '새 콘텐츠 항목',
          level: 1,
        },
      },
    ],
  }
}

function extractTitleFromEditorData(data) {
  const blocks = Array.isArray(data?.blocks) ? data.blocks : []
  const headerBlock = blocks.find((block) => block?.type === 'header')
  const firstBlock = blocks[0]
  const text = htmlToPlainText(headerBlock?.data?.text ?? firstBlock?.data?.text ?? '')
  return text || '새 콘텐츠 항목'
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

const draftTitleText = ref('새 콘텐츠 항목')
const draftBodyText = ref('본문을 입력하세요.')
const bodyEditorSeedData = ref(buildBodyEditorData(draftBodyText.value))
const contentStatus = ref('planned')
const contentPriority = ref('medium')
const contentAssigneeId = ref(store.currentUserId)
const contentDueDate = ref('')
const contentCustomer = ref('')
const contentTypeValue = ref('')
const contentTagsText = ref('')
const contentAttachments = ref([])
const contentPalette = ref({ ...defaultPalette })

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

const titleEditorHolder = ref(null)
const bodyEditorHolder = ref(null)
let titleEditor = null
let bodyEditor = null
let editorInitToken = 0
const editorReady = ref(false)
const isSaving = ref(false)
const isBlockPaletteOpen = ref(false)

const panelItems = [
  { id: 'properties', title: '상세 속성', icon: 'list_alt' },
  { id: 'gpt', title: 'GPT', icon: 'smart_toy' },
  { id: 'files', title: '파일&템플릿', icon: 'folder_open' },
  { id: 'review', title: '검토 요청', icon: 'rate_review' },
  { id: 'history', title: '변경 이력', icon: 'history' },
]

const bottomPanelItems = [{ id: 'references', title: '레퍼런스 탐색', icon: 'menu_book' }]
const allPanelItems = [...panelItems, ...bottomPanelItems]

const currentPlanner = computed(() =>
  store.findMember(activeTask.value?.plannerId ?? contentAssigneeId.value ?? store.currentUserId),
)
const currentDesigner = computed(() => store.findMember(activeTask.value?.designerId ?? 'sumin'))
const currentAssignee = computed(() =>
  store.findMember(contentAssigneeId.value || activeTask.value?.assigneeId || store.currentUserId),
)
const currentSupervisor = computed(() =>
  store.findMember(activeTask.value?.supervisorId ?? 'taeyoung'),
)
const propertyPlanners = computed(() => [currentPlanner.value, currentDesigner.value].filter(Boolean))

const taskStatusLabel = computed(() => store.statusLabels[contentStatus.value] ?? contentStatus.value)
const taskPriorityLabel = computed(
  () => store.priorityLabels[contentPriority.value] ?? contentPriority.value,
)

const contentTagsList = computed(() =>
  contentTagsText.value
    .split(',')
    .map((tag) => tag.trim())
    .filter(Boolean),
)

const workspaceRightInset = computed(() =>
  ENABLE_CONTENT_RAIL ? `${RAIL_WIDTH + (activePanel.value ? panelWidth.value : 0)}px` : '0px',
)

const taskStatusToneClass = computed(() => {
  const map = {
    planned: 'bg-blue-50 text-blue-600 ring-1 ring-blue-100',
    in_progress: 'bg-amber-50 text-amber-700 ring-1 ring-amber-100',
    review: 'bg-violet-50 text-violet-700 ring-1 ring-violet-100',
    done: 'bg-emerald-50 text-emerald-700 ring-1 ring-emerald-100',
    at_risk: 'bg-rose-50 text-rose-700 ring-1 ring-rose-100',
  }

  return map[contentStatus.value] ?? 'bg-blue-50 text-blue-600 ring-1 ring-blue-100'
})

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

function seedDraftFromTask(task) {
  const title = task?.editorTitle ?? task?.title ?? '새 콘텐츠 항목'
  const parsedBody = parseEditorData(task?.editorBody ?? task?.description)
  const fallbackText = [task?.summary, task?.description]
    .filter(Boolean)
    .map((part) => String(part).trim())
    .filter(Boolean)
    .join('\n\n')

  draftTitleText.value = title
  draftBodyText.value = parsedBody ? blocksToPlainText(parsedBody.blocks) || fallbackText || '본문을 입력하세요.' : fallbackText || '본문을 입력하세요.'
  bodyEditorSeedData.value = parsedBody ?? buildBodyEditorData(draftBodyText.value)
  contentStatus.value = task?.status ?? 'planned'
  contentPriority.value = task?.priority ?? 'medium'
  contentAssigneeId.value = task?.assigneeId ?? store.currentUserId
  contentDueDate.value = task?.dueDate ?? routeSeedDate.value ?? ''
  contentCustomer.value = task?.customer ?? '내부 보드'
  contentTypeValue.value = task?.contentType ?? '작업'
  contentTagsText.value = Array.isArray(task?.tags) ? task.tags.join(', ') : ''
  contentAttachments.value = Array.isArray(task?.attachments) ? [...task.attachments] : []
  contentPalette.value = task?.palette ? { ...task.palette } : { ...defaultPalette }
}

function destroyEditors() {
  if (titleEditor) {
    try {
      titleEditor.destroy()
    } catch (error) {
      console.warn('titleEditor destroy failed', error)
    }
    titleEditor = null
  }

  if (bodyEditor) {
    try {
      bodyEditor.destroy()
    } catch (error) {
      console.warn('bodyEditor destroy failed', error)
    }
    bodyEditor = null
  }

  editorReady.value = false
}

function createImageUploader() {
  return {
    uploader: {
      async uploadByFile(file) {
        const dataUrl = await new Promise((resolve, reject) => {
          const reader = new FileReader()
          reader.onload = () => resolve(String(reader.result ?? ''))
          reader.onerror = () => reject(reader.error ?? new Error('이미지 변환에 실패했습니다.'))
          reader.readAsDataURL(file)
        })

        return {
          success: 1,
          file: { url: dataUrl },
        }
      },
    },
  }
}

function createFallbackBlockData(toolName) {
  const fallbacks = {
    paragraph: { text: '' },
    header: { text: '새 제목', level: 2 },
    list: { style: 'unordered', items: [] },
    quote: { text: '', caption: '', alignment: 'left' },
    table: { content: [['', ''], ['', '']] },
    code: { code: '' },
    image: {
      file: { url: '' },
      caption: '',
      withBorder: false,
      withBackground: false,
      stretched: false,
    },
    embed: {
      service: 'youtube',
      source: '',
      embed: '',
      caption: '',
    },
    raw: { html: '' },
    delimiter: {},
    warning: { title: '주의', message: '' },
  }

  return fallbacks[toolName] ?? {}
}

function openBlockPalette() {
  isBlockPaletteOpen.value = !isBlockPaletteOpen.value
}

function closeBlockPalette() {
  isBlockPaletteOpen.value = false
}

function insertBodyBlock(toolName) {
  if (!bodyEditor || !toolName) {
    return
  }

  const blockData = bodyEditor.blocks.composeBlockData?.(toolName) ?? createFallbackBlockData(toolName)

  try {
    bodyEditor.blocks.insert(toolName, blockData, undefined, undefined, true)
  } catch (error) {
    console.warn('Failed to insert block', toolName, error)
  }

  closeBlockPalette()
}

function createBodyTools() {
  return {
    header: { class: Header, tunes: ['alignment'], config: { levels: [1, 2, 3, 4], defaultLevel: 2 } },
    list: { class: List, inlineToolbar: true, tunes: ['alignment'] },
    quote: { class: Quote, inlineToolbar: true, tunes: ['alignment'] },
    table: { class: Table, inlineToolbar: true },
    code: { class: CodeTool },
    embed: { class: Embed, inlineToolbar: false },
    image: { class: ImageTool, config: createImageUploader() },
    paragraph: { class: Paragraph, inlineToolbar: true },
    raw: { class: RawTool },
    inlineCode: { class: InlineCode },
    delimiter: Delimiter,
    marker: Marker,
    warning: Warning,
    alignment: { class: AlignmentTuneTool, config: { default: 'left' } },
  }
}

async function initializeEditors() {
  if (!ENABLE_EDITOR_JS) {
    return
  }

  const initToken = ++editorInitToken
  destroyEditors()
  await nextTick()

  if (initToken !== editorInitToken) {
    return
  }

  if (!titleEditorHolder.value || !bodyEditorHolder.value) {
    return
  }

  const titleInstance = new EditorJS({
    holder: titleEditorHolder.value,
    data: buildTitleEditorData(draftTitleText.value),
    placeholder: '제목을 입력하세요',
    minHeight: 0,
    autofocus: false,
    tools: {
      header: { class: Header, config: { levels: [1], defaultLevel: 1 } },
    },
    onChange: () => {
      // EditorJS 자체 저장 시점만 상태를 갱신한다.
    },
  })

  const bodyInstance = new EditorJS({
    holder: bodyEditorHolder.value,
    data: bodyEditorSeedData.value,
    placeholder: '본문을 입력하세요',
    minHeight: 0,
    autofocus: false,
    defaultBlock: 'paragraph',
    tools: createBodyTools(),
  })

  try {
    await Promise.all([titleInstance.isReady, bodyInstance.isReady])
  } catch (error) {
    console.error('EditorJS initialization failed', error)
  }

  if (initToken !== editorInitToken) {
    try {
      titleInstance.destroy()
      bodyInstance.destroy()
    } catch {
      // ignore cleanup failures for stale instances
    }
    return
  }

  titleEditor = titleInstance
  bodyEditor = bodyInstance
  editorReady.value = true
}

async function saveContent() {
  if (isSaving.value) {
    return
  }

  isSaving.value = true

  try {
    let nextTitle = draftTitleText.value
    let nextBodyText = draftBodyText.value
    let nextBodyData = bodyEditorSeedData.value

    if (ENABLE_EDITOR_JS) {
      const titleData = titleEditor ? await titleEditor.save() : buildTitleEditorData(draftTitleText.value)
      const bodyData = bodyEditor ? await bodyEditor.save() : bodyEditorSeedData.value

      nextTitle = extractTitleFromEditorData(titleData)
      nextBodyData = bodyData
      nextBodyText = blocksToPlainText(bodyData.blocks) || nextBodyText
    }

    draftTitleText.value = nextTitle
    draftBodyText.value = nextBodyText || '본문을 입력하세요.'
    bodyEditorSeedData.value = nextBodyData ?? buildBodyEditorData(draftBodyText.value)

    const patch = {
      title: nextTitle,
      summary: nextBodyText.slice(0, 120) || nextTitle,
      description: JSON.stringify(nextBodyData ?? buildBodyEditorData(nextBodyText)),
      editorTitle: nextTitle,
      editorBody: nextBodyData ?? buildBodyEditorData(nextBodyText),
      status: contentStatus.value,
      priority: contentPriority.value,
      assigneeId: contentAssigneeId.value,
      dueDate: contentDueDate.value || routeSeedDate.value,
      customer: contentCustomer.value,
      contentType: contentTypeValue.value,
      tags: contentTagsList.value,
      attachments: [...contentAttachments.value],
      palette: { ...contentPalette.value },
      plannerId: currentPlanner.value?.id ?? store.currentUserId,
      designerId: currentDesigner.value?.id ?? 'sumin',
      supervisorId: currentSupervisor.value?.id ?? 'taeyoung',
    }

    if (isCreateMode.value) {
      const nextIndex = String(store.tasks.length + 1).padStart(3, '0')
      const nextId = `CONTENT_CUSTOM_${nextIndex}`

      store.createTask({
        id: nextId,
        requirementId: nextId,
        title: nextTitle,
        category: '콘텐츠',
        summary: patch.summary,
        description: patch.description,
        editorTitle: nextTitle,
        editorBody: patch.editorBody,
        assigneeId: patch.assigneeId,
        plannerId: patch.plannerId,
        designerId: patch.designerId,
        supervisorId: patch.supervisorId,
        startDate: routeSeedDate.value,
        dueDate: patch.dueDate,
        status: patch.status,
        priority: patch.priority,
        customer: patch.customer,
        contentType: patch.contentType,
        visibility: 'personal',
        timeRange: '10:00 - 18:00',
        tags: patch.tags,
        attachments: patch.attachments,
        history: ['EditorJS로 새 콘텐츠가 생성되었습니다.'],
        progress: 0,
        palette: patch.palette,
      })

      await router.replace({ name: 'content-editor', params: { contentId: nextId } })
    } else if (activeTask.value) {
      store.updateTask(activeTask.value.id, patch)
    }
  } catch (error) {
    console.error('saveContent failed', error)
  } finally {
    isSaving.value = false
  }
}

function openPanel(id) {
  if (!ENABLE_CONTENT_RAIL) {
    return
  }

  activeModal.value = null
  activePanel.value = activePanel.value === id ? null : id
}

function closePanel() {
  activePanel.value = null
}

function openHistoryModal(version) {
  historyModalVersion.value = version
  activeModal.value = 'history'
}

function openReferenceFilterModal() {
  draftReferenceType.value = appliedReferenceType.value
  draftReferenceTags.value = [...appliedReferenceTags.value]
  activeModal.value = 'reference-filter'
}

function closeModal() {
  activeModal.value = null
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

  isPanelResizing.value = true
  document.body.style.userSelect = 'none'
  document.body.style.cursor = 'col-resize'

  const startX = event.clientX
  const startWidth = panelWidth.value

  const handleMove = (moveEvent) => {
    const delta = startX - moveEvent.clientX
    panelWidth.value = clamp(startWidth + delta, MIN_PANEL_WIDTH, MAX_PANEL_WIDTH)
  }

  const handleUp = () => {
    isPanelResizing.value = false
    document.body.style.userSelect = ''
    document.body.style.cursor = ''
    window.removeEventListener('pointermove', handleMove)
    window.removeEventListener('pointerup', handleUp)
  }

  window.addEventListener('pointermove', handleMove)
  window.addEventListener('pointerup', handleUp, { once: true })
}

watch(
  routeContentId,
  () => {
    seedDraftFromTask(activeTask.value)
    void initializeEditors()
  },
)

onMounted(() => {
  store.initialize?.()
  seedDraftFromTask(activeTask.value)
  void initializeEditors()
})

onBeforeUnmount(() => {
  destroyEditors()
  document.body.style.userSelect = ''
  document.body.style.cursor = ''
})
</script>

<template>
  <main class="relative min-h-screen bg-[#f8f9fa] text-slate-900">
    <div class="mx-auto max-w-[1600px] px-6 py-6 transition-all duration-300" :style="{ paddingRight: workspaceRightInset }">
      <div class="space-y-6">
        <header class="sticky top-4 z-20 rounded-[28px] border border-white/80 bg-white/90 px-6 py-5 shadow-[0_12px_30px_rgba(15,23,42,0.06)] backdrop-blur-md">
          <div class="flex items-start justify-between gap-6">
            <div class="flex min-w-0 items-start gap-4">
              <button
                type="button"
                class="flex h-11 w-11 shrink-0 items-center justify-center rounded-2xl border border-slate-100 bg-slate-50 text-slate-500 transition-colors hover:bg-slate-100 hover:text-slate-900"
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
                  class="editor-shell title-editor mt-3 min-h-[72px]"
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
                class="inline-flex items-center gap-2 rounded-2xl border border-emerald-100 bg-emerald-50 px-4 py-3 text-sm font-black text-emerald-600 transition-colors hover:bg-emerald-100"
                :disabled="isSaving"
                @click="saveContent"
              >
                <span class="material-symbols-outlined text-[18px]">save</span>
                {{ isSaving ? '저장 중' : '저장' }}
              </button>
            </div>
          </div>
        </header>

        <section class="overflow-visible rounded-[30px] border border-white/80 bg-gradient-to-br from-[#f8fbff] via-white to-[#edf5ff] shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
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
              class="editor-shell body-editor mx-auto min-h-[760px] max-w-[1220px]"
            ></div>
            <div v-else>
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
      class="fixed right-0 top-[88px] z-50 h-[calc(100vh-88px)] w-16 border-l border-slate-200 bg-white/95 shadow-[-4px_0_15px_rgba(15,23,42,0.03)] backdrop-blur-md"
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
      class="fixed z-40 h-[calc(100vh-88px)] border-l border-slate-200 bg-white/95 shadow-[-24px_0_40px_rgba(15,23,42,0.08)] backdrop-blur-md transition-all duration-300"
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
            <p class="text-[10px] font-black uppercase tracking-[0.24em] text-slate-400">현재 패널</p>
            <h2 class="mt-1 text-lg font-black text-slate-900">
              {{ allPanelItems.find((item) => item.id === activePanel)?.title ?? '상세 속성' }}
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
          <div v-if="activePanel === 'properties'" class="h-full overflow-y-auto p-5 no-scrollbar">
            <div class="space-y-4">
              <section class="rounded-[20px] border border-slate-100 bg-gradient-to-br from-blue-50/80 via-white to-slate-50 p-4 shadow-sm">
                <div class="flex items-start justify-between gap-4">
                  <div class="min-w-0">
                    <p class="text-[10px] font-black uppercase tracking-[0.18em] text-slate-400">
                      프로젝트 상세 속성
                    </p>
                    <h3 class="mt-2 text-[1.6rem] font-black leading-tight text-slate-900">상세 속성</h3>
                    <p class="mt-2 text-sm leading-6 text-slate-500">
                      현재 상태, 기획자, 담당자, 마감일, 포맷, 고객사를 한눈에 보여줍니다.
                    </p>
                  </div>

                  <div class="rounded-[18px] bg-white px-4 py-3 shadow-sm ring-1 ring-slate-100">
                    <p class="text-[10px] font-bold uppercase tracking-[0.16em] text-slate-400">상태</p>
                    <div class="mt-2 flex items-center gap-2">
                      <span class="h-2.5 w-2.5 rounded-full bg-blue-500"></span>
                      <span class="text-base font-black text-slate-900">{{ taskStatusLabel }}</span>
                    </div>
                  </div>
                </div>
              </section>

              <section class="rounded-[20px] border border-slate-100 bg-white p-4 shadow-sm">
                <div class="flex items-center justify-between gap-3">
                  <div>
                    <p class="text-[10px] font-black uppercase tracking-[0.18em] text-slate-400">기획자</p>
                    <h4 class="mt-1 text-base font-black text-slate-900">기획 및 담당 라인</h4>
                  </div>
                  <span class="rounded-full bg-slate-50 px-3 py-1 text-xs font-bold text-slate-500">활성 멤버</span>
                </div>

                <div class="mt-4 flex flex-wrap gap-2">
                  <div
                    v-for="member in propertyPlanners"
                    :key="member.id"
                    class="flex items-center gap-2 rounded-full border border-slate-100 bg-white px-3.5 py-2 text-xs font-bold text-slate-700 shadow-sm"
                  >
                    <div
                      class="flex h-6 w-6 items-center justify-center rounded-full text-[9px] font-black text-white"
                      :style="{ backgroundColor: member.accent }"
                    >
                      {{ member.initials }}
                    </div>
                    <span>{{ member.name }}</span>
                  </div>
                </div>

                <div class="mt-4 rounded-[18px] bg-slate-50 p-4">
                  <p class="text-[10px] font-black uppercase tracking-[0.16em] text-slate-400">담당자</p>
                  <div class="mt-3 flex items-center gap-3">
                    <div
                      class="flex h-10 w-10 items-center justify-center rounded-2xl text-sm font-black text-white"
                      :style="{ backgroundColor: currentAssignee?.accent ?? '#94a3b8' }"
                    >
                      {{ currentAssignee?.initials ?? 'U1' }}
                    </div>
                    <div class="min-w-0">
                      <p class="truncate text-sm font-black text-slate-900">{{ currentAssignee?.name ?? '미지정' }}</p>
                      <p class="truncate text-xs text-slate-500">{{ currentAssignee?.role ?? '담당자 정보 없음' }}</p>
                    </div>
                  </div>
                </div>

                <div class="mt-4 grid gap-3 sm:grid-cols-2">
                  <div class="rounded-[18px] bg-slate-50 p-4">
                    <p class="text-[10px] font-black uppercase tracking-[0.16em] text-slate-400">관리자</p>
                    <strong class="mt-2 block text-sm font-black text-slate-900">
                      {{ currentSupervisor?.name ?? '미정' }}
                    </strong>
                  </div>
                  <div class="rounded-[18px] bg-slate-50 p-4">
                    <p class="text-[10px] font-black uppercase tracking-[0.16em] text-slate-400">우선순위</p>
                    <strong class="mt-2 block text-sm font-black text-slate-900">
                      {{ taskPriorityLabel }}
                    </strong>
                  </div>
                </div>
              </section>

              <section class="grid gap-3 sm:grid-cols-2">
                <div class="rounded-[20px] border border-slate-100 bg-white p-4 shadow-sm">
                  <p class="text-[10px] font-black uppercase tracking-[0.16em] text-slate-400">마감일</p>
                  <strong class="mt-2 block text-lg font-black text-red-500">
                    {{ contentDueDate || '미정' }}
                  </strong>
                </div>
                <div class="rounded-[20px] border border-slate-100 bg-white p-4 shadow-sm">
                  <p class="text-[10px] font-black uppercase tracking-[0.16em] text-slate-400">포맷</p>
                  <strong class="mt-2 block text-lg font-black text-slate-900">
                    {{ contentTypeValue || '미정' }}
                  </strong>
                </div>
              </section>

              <section class="rounded-[20px] border border-slate-100 bg-white p-4 shadow-sm">
                <p class="text-[10px] font-black uppercase tracking-[0.16em] text-slate-400">고객사</p>
                <strong class="mt-2 block text-xl font-black text-slate-900">
                  {{ contentCustomer || '미정' }}
                </strong>
              </section>
            </div>
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
  </main>
</template>

<style scoped>
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
