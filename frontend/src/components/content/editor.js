import { computed, nextTick, onBeforeUnmount, onMounted, ref, unref, watch } from 'vue'
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
import editorApi from '@/api/editor/editorApi'

export const ENABLE_EDITOR_JS = true

export const DEFAULT_EDITOR_PALETTE = {
  accent: '#2f80ed',
  surface: '#eaf3ff',
  text: '#1d4f99',
}

export const DEFAULT_EDITOR_TEXTS = {
  emptyTitle: '새 콘텐츠 항목',
  emptyBody: '본문을 입력하세요.',
  titlePlaceholder: '제목을 입력하세요.',
  bodyPlaceholder: '본문을 입력하세요.',
  emptyCustomer: '내부 보드',
  emptyContentType: '작업',
  createTaskCategory: '콘텐츠',
  createHistoryMessage: 'EditorJS로 새 콘텐츠가 생성되었습니다.',
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

export function parseEditorData(value) {
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

export function blocksToPlainText(blocks = []) {
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

export function buildBodyEditorData(text, emptyBodyText = DEFAULT_EDITOR_TEXTS.emptyBody) {
  const normalizedText = String(text ?? '').trim()
  const paragraphs = normalizedText
    ? normalizedText
        .split(/\n{2,}/)
        .map((part) => part.trim())
        .filter(Boolean)
    : [emptyBodyText]

  return {
    time: Date.now(),
    blocks: paragraphs.map((paragraph) => ({
      type: 'paragraph',
      data: { text: paragraph },
    })),
  }
}

export function buildTitleEditorData(title, emptyTitleText = DEFAULT_EDITOR_TEXTS.emptyTitle) {
  return {
    time: Date.now(),
    blocks: [
      {
        type: 'header',
        data: {
          text: title || emptyTitleText,
          level: 1,
        },
      },
    ],
  }
}

export function extractTitleFromEditorData(data, emptyTitleText = DEFAULT_EDITOR_TEXTS.emptyTitle) {
  const blocks = Array.isArray(data?.blocks) ? data.blocks : []
  const headerBlock = blocks.find((block) => block?.type === 'header')
  const firstBlock = blocks[0]
  const text = htmlToPlainText(headerBlock?.data?.text ?? firstBlock?.data?.text ?? '')
  return text || emptyTitleText
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
    header: { text: DEFAULT_EDITOR_TEXTS.emptyTitle, level: 2 },
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

export function useContentEditor({ store, router, routeContentId, routeSeedDate, activeTask }) {
  const draftTitleText = ref(DEFAULT_EDITOR_TEXTS.emptyTitle)
  const draftBodyText = ref(DEFAULT_EDITOR_TEXTS.emptyBody)
  const bodyEditorSeedData = ref(buildBodyEditorData(draftBodyText.value))

  const contentStatus = ref('planned')
  const contentPriority = ref('medium')
  const contentPlannerId = ref(store.currentUserId)
  const contentPlannerSelected = ref(true)
  const contentAssigneeId = ref(store.currentUserId)
  const contentAssigneeSelected = ref(true)
  const contentDueDate = ref('')
  const contentCustomer = ref(DEFAULT_EDITOR_TEXTS.emptyCustomer)
  const contentTypeValue = ref(DEFAULT_EDITOR_TEXTS.emptyContentType)
  const contentTagsText = ref('')
  const contentAttachments = ref([])
  const contentPalette = ref({ ...DEFAULT_EDITOR_PALETTE })

  const titleEditorHolder = ref(null)
  const bodyEditorHolder = ref(null)
  let titleEditor = null
  let bodyEditor = null
  let editorInitToken = 0

  const editorReady = ref(false)
  const isSaving = ref(false)
  const isDeleting = ref(false)
  const isBlockPaletteOpen = ref(false)

  const currentPlanner = computed(() =>
    contentPlannerSelected.value ? store.findMember(contentPlannerId.value) : null,
  )
  const currentDesigner = computed(() => store.findMember(unref(activeTask)?.designerId ?? 'sumin'))
  const currentAssignee = computed(() =>
    contentAssigneeSelected.value ? store.findMember(contentAssigneeId.value) : null,
  )
  const currentSupervisor = computed(() =>
    store.findMember(unref(activeTask)?.supervisorId ?? 'taeyoung'),
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

  function seedDraftFromTask(task) {
    const title = task?.editorTitle ?? task?.title ?? DEFAULT_EDITOR_TEXTS.emptyTitle
    const parsedBody = parseEditorData(task?.editorBody ?? task?.description)
    const fallbackText = [task?.summary, task?.description]
      .filter(Boolean)
      .map((part) => String(part).trim())
      .filter(Boolean)
      .join('\n\n')

    draftTitleText.value = title
    draftBodyText.value = parsedBody
      ? blocksToPlainText(parsedBody.blocks) || fallbackText || DEFAULT_EDITOR_TEXTS.emptyBody
      : fallbackText || DEFAULT_EDITOR_TEXTS.emptyBody
    bodyEditorSeedData.value = parsedBody ?? buildBodyEditorData(draftBodyText.value)
    contentStatus.value = task?.status ?? 'planned'
    contentPriority.value = task?.priority ?? 'medium'
    contentPlannerId.value = task?.plannerId ?? store.currentUserId
    contentPlannerSelected.value = task?.plannerId != null || !task
    contentAssigneeId.value = task?.assigneeId ?? store.currentUserId
    contentAssigneeSelected.value = task?.assigneeId != null || !task
    contentDueDate.value = task?.dueDate ?? unref(routeSeedDate) ?? ''
    contentCustomer.value = task?.customer ?? DEFAULT_EDITOR_TEXTS.emptyCustomer
    contentTypeValue.value = task?.contentType ?? DEFAULT_EDITOR_TEXTS.emptyContentType
    contentTagsText.value = Array.isArray(task?.tags) ? task.tags.join(', ') : ''
    contentAttachments.value = Array.isArray(task?.attachments) ? [...task.attachments] : []
    contentPalette.value = task?.palette ? { ...task.palette } : { ...DEFAULT_EDITOR_PALETTE }
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
      placeholder: DEFAULT_EDITOR_TEXTS.titlePlaceholder,
      minHeight: 0,
      autofocus: false,
      tools: {
        header: { class: Header, config: { levels: [1], defaultLevel: 1 } },
      },
      onChange: () => {
        // Save is handled explicitly by the page-level controller.
      },
    })

    const bodyInstance = new EditorJS({
      holder: bodyEditorHolder.value,
      data: bodyEditorSeedData.value,
      placeholder: DEFAULT_EDITOR_TEXTS.bodyPlaceholder,
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
      draftBodyText.value = nextBodyText || DEFAULT_EDITOR_TEXTS.emptyBody
      bodyEditorSeedData.value = nextBodyData ?? buildBodyEditorData(draftBodyText.value)

      const isCreateMode = !unref(routeContentId) || unref(routeContentId) === 'new'

      const patch = {
        title: nextTitle,
        summary: String(nextBodyText ?? '').slice(0, 120) || nextTitle,
        description: JSON.stringify(nextBodyData ?? buildBodyEditorData(nextBodyText)),
        editorTitle: nextTitle,
        editorBody: nextBodyData ?? buildBodyEditorData(nextBodyText),
        status: contentStatus.value,
        priority: contentPriority.value,
        assigneeId: contentAssigneeSelected.value ? contentAssigneeId.value : null,
        dueDate: contentDueDate.value || unref(routeSeedDate),
        customer: contentCustomer.value,
        contentType: contentTypeValue.value,
        tags: contentTagsList.value,
        attachments: [...contentAttachments.value],
        palette: { ...contentPalette.value },
        plannerId: contentPlannerSelected.value ? contentPlannerId.value : null,
        designerId: currentDesigner.value?.id ?? 'sumin',
        supervisorId: currentSupervisor.value?.id ?? 'taeyoung',
      }
      const requestedContentId = isCreateMode
        ? `CONTENT_CUSTOM_${String(store.tasks.length + 1).padStart(3, '0')}`
        : unref(routeContentId) || null

      try {
        const savedContent = await editorApi.saveEditorContent({
          contentId: requestedContentId,
          mode: isCreateMode ? 'create' : 'update',
          ...patch,
        })

        const savedContentId =
          typeof savedContent === 'object' && savedContent
            ? savedContent.id ?? savedContent.contentId ?? savedContent.requirementId ?? null
            : null

        if (isCreateMode) {
          const nextId = savedContentId || requestedContentId

          store.createTask({
            id: nextId,
            requirementId: nextId,
            title: nextTitle,
            category: DEFAULT_EDITOR_TEXTS.createTaskCategory,
            summary: patch.summary,
            description: patch.description,
            editorTitle: nextTitle,
            editorBody: patch.editorBody,
            assigneeId: patch.assigneeId,
            plannerId: patch.plannerId,
            designerId: patch.designerId,
            supervisorId: patch.supervisorId,
            startDate: unref(routeSeedDate),
            dueDate: patch.dueDate,
            status: patch.status,
            priority: patch.priority,
            customer: patch.customer,
            contentType: patch.contentType,
            visibility: 'personal',
            timeRange: '10:00 - 18:00',
            tags: patch.tags,
            attachments: patch.attachments,
            history: [DEFAULT_EDITOR_TEXTS.createHistoryMessage],
            progress: 0,
            palette: patch.palette,
          })

          await router.replace({ name: 'content-editor', params: { contentId: nextId } })
        } else if (unref(activeTask)) {
          store.updateTask(unref(activeTask).id, patch)
        }
      } catch (apiError) {
        console.error('editor save api failed', apiError)
      }
    } catch (error) {
      console.error('saveContent failed', error)
    } finally {
      isSaving.value = false
    }
  }

  async function deleteContent() {
    if (isDeleting.value) {
      return false
    }

    const currentContentId =
      unref(activeTask)?.id ??
      (unref(routeContentId) && unref(routeContentId) !== 'new' ? unref(routeContentId) : null)

    if (!currentContentId) {
      return false
    }

    if (typeof window !== 'undefined' && !window.confirm('이 컨텐츠 카드를 삭제할까요?')) {
      return false
    }

    isDeleting.value = true

    try {
      await editorApi.deleteContent(currentContentId)
      store.deleteTask(currentContentId)

      if (typeof window !== 'undefined' && window.history.length > 1) {
        router.back()
      } else {
        await router.push({ name: 'calendar' })
      }

      return true
    } catch (error) {
      console.error('editor delete api failed', error)
      throw error
    } finally {
      isDeleting.value = false
    }
  }

  onMounted(async () => {
    await Promise.resolve(store.initialize?.())
    seedDraftFromTask(unref(activeTask))
    void initializeEditors()
  })

  watch(routeContentId, () => {
    seedDraftFromTask(unref(activeTask))
    void initializeEditors()
  })

  onBeforeUnmount(() => {
    destroyEditors()
  })

  return {
    draftTitleText,
    draftBodyText,
    bodyEditorSeedData,
    titleEditorHolder,
    bodyEditorHolder,
    contentStatus,
    contentPriority,
    contentPlannerId,
    contentPlannerSelected,
    contentAssigneeId,
    contentAssigneeSelected,
    contentDueDate,
    contentCustomer,
    contentTypeValue,
    contentTagsText,
    contentAttachments,
    contentPalette,
    currentPlanner,
    currentDesigner,
    currentAssignee,
    currentSupervisor,
    propertyPlanners,
    taskStatusLabel,
    taskPriorityLabel,
    editorReady,
    isSaving,
    isDeleting,
    isBlockPaletteOpen,
    openBlockPalette,
    closeBlockPalette,
    insertBodyBlock,
    initializeEditors,
    destroyEditors,
    saveContent,
    deleteContent,
    seedDraftFromTask,
  }
}
