import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import {
  addDays,
  addMonths,
  compareDateKeys,
  differenceInDays,
  formatDateKey,
  startOfWeek,
} from '@/utils/calendar'
import {
  currentUserId,
  priorityLabels,
  reportCards,
  seedTasks,
  statusLabels,
  teamMembers,
  templateLibrary,
} from '@/data/scheduleSeed'
import editorApi from '@/api/editor/editorApi'
import { ListCampaign } from '@/api/campaigns'
import { readStoredToken } from '@/authStorage'

const themeStorageKey = 'callog-theme'
const legacyThemeStorageKey = 'kellog-theme'
const tasksStorageKey = 'kellog-tasks'
const campaignsStorageKey = 'callog-campaigns'
const activeCampaignIdStorageKey = 'callog-active-campaign-id'
const campaignOrderStorageKey = 'callog-campaign-order'
const campaignFolderStorageKey = 'callog-campaign-folder'

const defaultCampaigns = []

function cloneValue(value) {
  return JSON.parse(JSON.stringify(value))
}

function normalizeTheme(value) {
  return value === 'light' || value === 'dark' ? value : null
}

function readStoredTheme() {
  if (typeof window === 'undefined') {
    return null
  }

  return (
    normalizeTheme(window.localStorage.getItem(themeStorageKey)) ??
    normalizeTheme(window.localStorage.getItem(legacyThemeStorageKey))
  )
}

function getPreferredTheme() {
  if (typeof window === 'undefined') {
    return 'light'
  }

  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function createDefaultTask(dateKey, nextIndex) {
  const number = String(nextIndex).padStart(3, '0')

  return {
    id: `CONTENT_CUSTOM_${number}`,
    requirementId: `CONTENT_CUSTOM_${number}`,
    title: '새 콘텐츠 항목',
    category: '마케팅 콘텐츠',
    summary: '새로 만든 카드에 대한 간단한 요약입니다.',
    description:
      '요청 내용, 산출물, 다음 액션을 정리해 두면 팀이 맥락을 잃지 않고 바로 이어서 작업할 수 있습니다.',
    assigneeId: currentUserId,
    plannerId: currentUserId,
    designerId: 'sumin',
    publisherId: 'minseo',
    supervisorId: 'taeyoung',
    startDate: dateKey,
    dueDate: dateKey,
    status: 'planned',
    priority: 'low',
    customer: '내부 보드',
    contentType: '작업',
    visibility: 'personal',
    timeRange: '10:00 - 13:00',
    tags: ['new'],
    attachments: [],
    history: ['새 작업이 생성되었습니다.'],
    progress: 0,
    palette: { accent: '#2f80ed', surface: '#eaf3ff', text: '#1d4f99' },
  }
}

function safeParseTasks(value) {
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : null
  } catch {
    return null
  }
}

function safeParseCampaigns(value) {
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : null
  } catch {
    return null
  }
}

function formatCampaignPeriod(startDate, endDate) {
  const normalize = (value) => String(value || '').replaceAll('-', '.')

  if (!startDate && !endDate) {
    return '기간 미정'
  }

  return `${normalize(startDate)} - ${normalize(endDate)}`
}

function createCampaignInitials(name) {
  const cleanedName = String(name || '').trim()

  if (!cleanedName) {
    return 'CP'
  }

  const words = cleanedName.split(/\s+/).filter(Boolean)
  const initials = words.length > 1
    ? words.slice(0, 2).map((word) => word[0]).join('')
    : cleanedName.slice(0, 2)

  return initials.toUpperCase()
}

function normalizeCampaignRecord(source, fallback = {}) {
  const merged = {
    ...fallback,
    ...(source ?? {}),
  }
  const name = merged.name?.trim?.() || fallback.name || ''
  const startDate = merged.startDate || ''
  const endDate = merged.endDate || ''
  const id = merged.id ?? merged.idx ?? fallback.id ?? `campaign-${Date.now()}`

  return {
    id: String(id),
    idx: merged.idx ?? null,
    name,
    purpose: merged.purpose?.trim?.() || '',
    tags: Array.isArray(merged.tags) ? merged.tags : [],
    startDate,
    endDate,
    period: merged.period || formatCampaignPeriod(startDate, endDate),
    partners: Array.isArray(merged.partners) ? merged.partners : [],
    goals: merged.goals?.trim?.() || '',
    mainMessage: merged.mainMessage?.trim?.() || '',
    status: merged.status || fallback.status || 'draft',
    initials: merged.initials || createCampaignInitials(name),
    color: merged.color || fallback.color || '#8B5CF6',
    createdAt: merged.createdAt ?? fallback.createdAt,
    updatedAt: merged.updatedAt ?? fallback.updatedAt,
  }
}

export const usePlannerStore = defineStore('planner', () => {
  const sidebarCollapsed = ref(true)
  const campaigns = ref(cloneValue(defaultCampaigns))
  const activeCampaignId = ref(defaultCampaigns[0])
  const campaignUiOwnerKey = ref(currentUserId)
  const campaignOrder = ref(defaultCampaigns.map((campaign) => campaign.id))
  const campaignFolderIds = ref([])
  const campaignServerHydrated = ref(false)
  const campaignServerLoadPending = ref(false)
  const theme = ref(readStoredTheme() ?? getPreferredTheme())
  const activeMode = ref('personal')
  const calendarView = ref('month')
  const calendarTab = ref('calendar')
  const currentDate = ref('2026-04-15')
  const searchQuery = ref('')
  const statusFilter = ref('all')
  const sortMode = ref('due')
  const spanMode = ref(false)
  const selectedTaskId = ref(null)
  const taskOpenToken = ref(0)
  const createTaskToken = ref(0)
  const modalMode = ref('view')
  const createSeedDate = ref('2026-04-15')
  const tasks = ref(cloneValue(seedTasks))

  const activeCampaign = computed(() => {
    return campaigns.value.find((campaign) => campaign.id === activeCampaignId.value) ?? campaigns.value[0] ?? null
  })

  const orderedCampaigns = computed(() => {
    const orderMap = new Map(campaignOrder.value.map((campaignId, index) => [campaignId, index]))

    return [...campaigns.value].sort((left, right) => {
      const leftIndex = orderMap.get(left.id) ?? Number.MAX_SAFE_INTEGER
      const rightIndex = orderMap.get(right.id) ?? Number.MAX_SAFE_INTEGER

      return leftIndex - rightIndex
    })
  })

  const folderCampaignIdSet = computed(() => new Set(campaignFolderIds.value))

  const sidebarCampaigns = computed(() =>
    orderedCampaigns.value.filter((campaign) => !folderCampaignIdSet.value.has(campaign.id)),
  )

  const folderCampaigns = computed(() =>
    orderedCampaigns.value.filter((campaign) => folderCampaignIdSet.value.has(campaign.id)),
  )

  const campaignFolderCount = computed(() => folderCampaigns.value.length)

  function persistTasks() {
    if (typeof window === 'undefined') {
      return
    }

    window.localStorage.setItem(tasksStorageKey, JSON.stringify(tasks.value))
  }

  function persistCampaigns() {
    if (typeof window === 'undefined') {
      return
    }

    window.localStorage.setItem(campaignsStorageKey, JSON.stringify(campaigns.value))
  }

  function getCampaignUiStorageKey(baseKey) {
    return `${baseKey}:${campaignUiOwnerKey.value || 'default'}`
  }

  function persistCampaignOrder() {
    if (typeof window === 'undefined') {
      return
    }

    window.localStorage.setItem(getCampaignUiStorageKey(campaignOrderStorageKey), JSON.stringify(campaignOrder.value))
  }

  function persistCampaignFolderIds() {
    if (typeof window === 'undefined') {
      return
    }

    window.localStorage.setItem(
      getCampaignUiStorageKey(campaignFolderStorageKey),
      JSON.stringify(campaignFolderIds.value),
    )
  }

  function syncCampaignUiState() {
    const existingIds = campaigns.value.map((campaign) => campaign.id)
    const uniqueExistingIds = new Set(existingIds)

    campaignOrder.value = [
      ...campaignOrder.value.filter((campaignId, index, source) =>
        uniqueExistingIds.has(campaignId) && source.indexOf(campaignId) === index,
      ),
      ...existingIds.filter((campaignId) => !campaignOrder.value.includes(campaignId)),
    ]

    const completedIds = new Set(
      campaigns.value
        .filter((campaign) => campaign.status === 'completed')
        .map((campaign) => campaign.id),
    )

    campaignFolderIds.value = campaignFolderIds.value.filter((campaignId, index, source) =>
      completedIds.has(campaignId) && source.indexOf(campaignId) === index,
    )
  }

  function hydrateCampaignUiState() {
    if (typeof window === 'undefined') {
      return
    }

    const storedOrder = safeParseCampaigns(
      window.localStorage.getItem(getCampaignUiStorageKey(campaignOrderStorageKey)),
    )
    const storedFolderIds = safeParseCampaigns(
      window.localStorage.getItem(getCampaignUiStorageKey(campaignFolderStorageKey)),
    )

    campaignOrder.value = Array.isArray(storedOrder)
      ? storedOrder.filter((campaignId) => typeof campaignId === 'string')
      : campaigns.value.map((campaign) => campaign.id)

    campaignFolderIds.value = Array.isArray(storedFolderIds)
      ? storedFolderIds.filter((campaignId) => typeof campaignId === 'string')
      : []

    syncCampaignUiState()
  }

  function resetCampaigns() {
    campaigns.value = []
    campaignServerHydrated.value = false
    if (typeof window !== 'undefined') {
      window.localStorage.removeItem(campaignsStorageKey)
      window.localStorage.removeItem(activeCampaignIdStorageKey)
    }
  }

  async function loadCampaignsFromServer() {
    if (!readStoredToken() || campaignServerLoadPending.value) {
      return campaigns.value
    }

    campaignServerLoadPending.value = true

    try {
      const loadedCampaigns = await ListCampaign()

      if (Array.isArray(loadedCampaigns)) {
        const nextCampaigns = loadedCampaigns.map((campaign) => normalizeCampaignRecord(campaign))

        campaigns.value = nextCampaigns
        campaignOrder.value = nextCampaigns.map((campaign) => campaign.id)
        activeCampaignId.value = nextCampaigns.some((campaign) => campaign.id === activeCampaignId.value)
          ? activeCampaignId.value
          : nextCampaigns[0]?.id ?? null
        campaignServerHydrated.value = true
        syncCampaignUiState()
      }

      return campaigns.value
    } catch (error) {
      console.warn('loadCampaignsFromServer failed', error)
      return campaigns.value
    } finally {
      campaignServerLoadPending.value = false
    }
  }

  function applyTheme(nextTheme) {
    if (typeof document === 'undefined') {
      return
    }

    document.documentElement.dataset.theme = nextTheme
  }

  function initialize() {
    if (typeof window === 'undefined') {
      return
    }

    const storedTheme = readStoredTheme()
    const storedTasks = window.localStorage.getItem(tasksStorageKey)
    const storedCampaigns = window.localStorage.getItem(campaignsStorageKey)
    const storedActiveCampaignId = window.localStorage.getItem(activeCampaignIdStorageKey)

    if (storedTheme) {
      theme.value = storedTheme
    } else {
      theme.value = getPreferredTheme()
    }

    if (storedTasks) {
      const parsedTasks = safeParseTasks(storedTasks)
      if (parsedTasks) {
        tasks.value = parsedTasks
      }
    }

    if (storedCampaigns) {
      const parsedCampaigns = safeParseCampaigns(storedCampaigns)
      if (parsedCampaigns?.length) {
        campaigns.value = parsedCampaigns
      }
    }

    if (
      storedActiveCampaignId &&
      campaigns.value.some((campaign) => campaign.id === storedActiveCampaignId)
    ) {
      activeCampaignId.value = storedActiveCampaignId
    } else {
      activeCampaignId.value = campaigns.value[0]?.id ?? null
    }

    applyTheme(theme.value)
    hydrateCampaignUiState()

    if (!campaignServerHydrated.value && readStoredToken()) {
      void loadCampaignsFromServer()
    }
  }

  watch(
    theme,
    (nextTheme) => {
      if (typeof window !== 'undefined') {
        window.localStorage.setItem(themeStorageKey, nextTheme)
      }

      applyTheme(nextTheme)
    },
    { immediate: true },
  )

  watch(tasks, persistTasks, { deep: true })
  watch(
    campaigns,
    () => {
      persistCampaigns()
      syncCampaignUiState()
    },
    { deep: true },
  )
  watch(campaignOrder, persistCampaignOrder, { deep: true })
  watch(campaignFolderIds, persistCampaignFolderIds, { deep: true })
  watch(activeCampaignId, (nextCampaignId) => {
    if (typeof window !== 'undefined' && nextCampaignId) {
      window.localStorage.setItem(activeCampaignIdStorageKey, nextCampaignId)
    }
  })

  const members = computed(() => teamMembers)
  const templates = computed(() => templateLibrary)
  const reports = computed(() => reportCards)

  const memberMap = computed(() =>
    members.value.reduce((accumulator, member) => {
      accumulator[member.id] = member
      return accumulator
    }, {}),
  )

  function isPersonalTask(task) {
    return [
      task.assigneeId,
      task.plannerId,
      task.designerId,
      task.publisherId,
      task.supervisorId,
    ].includes(currentUserId)
  }

  const filteredTasks = computed(() => {
    let nextTasks = cloneValue(tasks.value)

    if (activeMode.value === 'personal') {
      nextTasks = nextTasks.filter((task) => isPersonalTask(task))
    }

    if (statusFilter.value !== 'all') {
      nextTasks = nextTasks.filter((task) => task.status === statusFilter.value)
    }

    if (searchQuery.value.trim()) {
      const keyword = searchQuery.value.trim().toLowerCase()

      nextTasks = nextTasks.filter((task) => {
        return [task.title, task.requirementId, task.customer, task.contentType, task.summary]
          .join(' ')
          .toLowerCase()
          .includes(keyword)
      })
    }

    const priorityWeight = {
      low: 0,
      medium: 1,
      high: 2,
      critical: 3,
    }

    nextTasks.sort((left, right) => {
      if (sortMode.value === 'priority') {
        return priorityWeight[right.priority] - priorityWeight[left.priority]
      }

      if (sortMode.value === 'assignee') {
        const leftMember = memberMap.value[left.assigneeId]?.name ?? ''
        const rightMember = memberMap.value[right.assigneeId]?.name ?? ''
        return leftMember.localeCompare(rightMember)
      }

      const dueCompare = compareDateKeys(left.dueDate, right.dueDate)
      if (dueCompare !== 0) {
        return dueCompare
      }

      return compareDateKeys(left.startDate, right.startDate)
    })

    return nextTasks
  })

  const selectedTask = computed(() => {
    return tasks.value.find((task) => task.id === selectedTaskId.value) ?? null
  })

  const modalTask = computed(() => {
    if (modalMode.value === 'create') {
      return createDefaultTask(createSeedDate.value, tasks.value.length + 1)
    }

    return selectedTask.value
  })

  const periodLabel = computed(() => {
    if (calendarView.value === 'week') {
      return startOfWeek(currentDate.value)
    }

    return currentDate.value
  })

  function findMember(memberId) {
    return memberMap.value[memberId] ?? null
  }

  function setSearchQuery(value) {
    searchQuery.value = value
  }

  function setActiveCampaign(campaignId) {
    if (!campaigns.value.some((campaign) => campaign.id === campaignId)) {
      return
    }

    activeCampaignId.value = campaignId
  }

  function setCampaignUiOwnerKey(nextOwnerKey) {
    const resolvedOwnerKey = String(nextOwnerKey || currentUserId)

    if (campaignUiOwnerKey.value === resolvedOwnerKey) {
      return
    }

    campaignUiOwnerKey.value = resolvedOwnerKey
    hydrateCampaignUiState()
  }

  function reorderCampaign(sourceCampaignId, targetCampaignId, placement = 'before') {
    if (!sourceCampaignId || !targetCampaignId || sourceCampaignId === targetCampaignId) {
      return false
    }

    const nextOrder = campaignOrder.value.filter((campaignId) => campaignId !== sourceCampaignId)
    const targetIndex = nextOrder.indexOf(targetCampaignId)

    if (targetIndex === -1) {
      return false
    }

    const insertIndex = placement === 'after' ? targetIndex + 1 : targetIndex
    nextOrder.splice(insertIndex, 0, sourceCampaignId)
    campaignOrder.value = nextOrder

    return true
  }

  function moveCampaignToFolder(campaignId) {
    const campaign = campaigns.value.find((item) => item.id === campaignId)

    if (!campaign || campaign.status !== 'completed' || campaignFolderIds.value.includes(campaignId)) {
      return false
    }

    campaignFolderIds.value = [...campaignFolderIds.value, campaignId]
    return true
  }

  function restoreCampaignFromFolder(campaignId) {
    if (!campaignFolderIds.value.includes(campaignId)) {
      return false
    }

    campaignFolderIds.value = campaignFolderIds.value.filter((item) => item !== campaignId)
    return true
  }

  function isCampaignInFolder(campaignId) {
    return campaignFolderIds.value.includes(campaignId)
  }

  function createCampaign(payload) {
    const nextIndex = campaigns.value.length + 1
    const name = payload.name?.trim() || `새 캠페인 ${nextIndex}`
    const startDate = payload.startDate || ''
    const endDate = payload.endDate || ''
    const nextCampaign = {
      id: String(payload.id ?? payload.idx ?? `campaign-${Date.now()}`),
      idx: payload.idx ?? null,
      name,
      purpose: payload.purpose?.trim() || '',
      tags: Array.isArray(payload.tags) ? payload.tags : [],
      startDate,
      endDate,
      period: formatCampaignPeriod(startDate, endDate),
      partners: Array.isArray(payload.partners) ? payload.partners : [],
      goals: payload.goals?.trim() || '',
      mainMessage: payload.mainMessage?.trim() || '',
      status: payload.status || 'draft',
      initials: payload.initials || createCampaignInitials(name),
      color: payload.color || '#8B5CF6',
      createdAt: payload.createdAt ?? new Date().toISOString(),
      updatedAt: payload.updatedAt,
    }

    campaigns.value.unshift(nextCampaign)
    campaignOrder.value = [nextCampaign.id, ...campaignOrder.value.filter((campaignId) => campaignId !== nextCampaign.id)]
    activeCampaignId.value = nextCampaign.id

    return nextCampaign
  }

  function updateCampaign(campaignId, payload) {
    const index = campaigns.value.findIndex((campaign) => campaign.id === campaignId)

    if (index === -1) {
      return null
    }

    const currentCampaign = campaigns.value[index]
    const name = payload.name?.trim() || currentCampaign.name
    const startDate = payload.startDate || currentCampaign.startDate || ''
    const endDate = payload.endDate || currentCampaign.endDate || ''

    const nextCampaign = {
      ...currentCampaign,
      idx: payload.idx ?? currentCampaign.idx ?? null,
      name,
      purpose: payload.purpose?.trim() || '',
      tags: Array.isArray(payload.tags) ? payload.tags : [],
      startDate,
      endDate,
      period: payload.period || formatCampaignPeriod(startDate, endDate),
      partners: Array.isArray(payload.partners) ? payload.partners : [],
      goals: payload.goals?.trim() || '',
      mainMessage: payload.mainMessage?.trim() || '',
      status: payload.status || currentCampaign.status,
      initials: payload.initials || createCampaignInitials(name),
      color: payload.color || currentCampaign.color,
      createdAt: payload.createdAt ?? currentCampaign.createdAt,
      updatedAt: payload.updatedAt ?? new Date().toISOString(),
    }

    campaigns.value[index] = nextCampaign

    return nextCampaign
  }

  function updateCampaignStatus(campaignId, nextStatus) {
    const index = campaigns.value.findIndex((campaign) => campaign.id === campaignId)

    if (index === -1) {
      return null
    }

    const nextCampaign = {
      ...campaigns.value[index],
      status: nextStatus,
      updatedAt: new Date().toISOString(),
    }

    campaigns.value[index] = nextCampaign

    if (nextStatus !== 'completed') {
      campaignFolderIds.value = campaignFolderIds.value.filter((item) => item !== campaignId)
    }

    return nextCampaign
  }

  function cycleStatusFilter() {
    const order = ['all', 'in_progress', 'review', 'at_risk', 'done', 'planned']
    const index = order.indexOf(statusFilter.value)
    statusFilter.value = order[(index + 1) % order.length]
  }

  function cycleSortMode() {
    const order = ['due', 'priority', 'assignee']
    const index = order.indexOf(sortMode.value)
    sortMode.value = order[(index + 1) % order.length]
  }

  function toggleSpanMode() {
    spanMode.value = !spanMode.value
  }

  function setActiveMode(mode) {
    activeMode.value = mode
  }

  function setCalendarView(mode) {
    calendarView.value = mode
  }

  function setCalendarTab(tab) {
    calendarTab.value = tab
  }

  function setSidebarCollapsed(value) {
    sidebarCollapsed.value = value
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setTheme(nextTheme) {
    const normalizedTheme = normalizeTheme(nextTheme)

    if (!normalizedTheme) {
      return
    }

    theme.value = normalizedTheme
  }

  function toggleTheme() {
    setTheme(theme.value === 'light' ? 'dark' : 'light')
  }

  function setToday() {
    currentDate.value = formatDateKey(new Date())
  }

  function shiftPeriod(step) {
    currentDate.value =
      calendarView.value === 'week'
        ? addDays(currentDate.value, step * 7)
        : addMonths(currentDate.value, step)
  }

  async function openTask(taskId) {
    if (!taskId) {
      return
    }

    selectedTaskId.value = taskId
    modalMode.value = 'view'

    try {
      const loadedTask = await editorApi.loadContent(taskId)

      if (loadedTask && typeof loadedTask === 'object' && !Array.isArray(loadedTask)) {
        updateTask(taskId, { ...loadedTask, id: taskId })
      }
    } catch (error) {
      console.warn('loadContent failed', error)
    } finally {
      taskOpenToken.value += 1
    }
  }

  function closeTask() {
    selectedTaskId.value = null
    modalMode.value = 'view'
  }

  function openCreateModal(dateKey = currentDate.value) {
    createSeedDate.value = dateKey
    selectedTaskId.value = null
    modalMode.value = 'create'
    createTaskToken.value += 1
  }

  function updateTask(taskId, patch) {
    const index = tasks.value.findIndex((task) => task.id === taskId)

    if (index === -1) {
      return
    }

    tasks.value[index] = {
      ...tasks.value[index],
      ...patch,
    }
  }

  function createTask(payload) {
    tasks.value.unshift(payload)
    selectedTaskId.value = payload.id
    modalMode.value = 'view'
  }

  function deleteTask(taskId) {
    tasks.value = tasks.value.filter((task) => task.id !== taskId)

    if (selectedTaskId.value === taskId) {
      closeTask()
    }
  }

  function duplicateTask(taskId) {
    const task = tasks.value.find((item) => item.id === taskId)

    if (!task) {
      return
    }

    const copies = tasks.value.filter((item) => item.title.startsWith(task.title)).length
    const clone = {
      ...cloneValue(task),
      id: `${task.id}_COPY_${copies}`,
      requirementId: `${task.requirementId}_COPY_${copies}`,
      title: `${task.title} (${copies})`,
      startDate: addDays(task.startDate, 1),
      dueDate: addDays(task.dueDate, 1),
      status: 'planned',
      history: [...task.history, '기존 작업을 복제해 새 카드가 생성되었습니다.'],
    }

    tasks.value.unshift(clone)
    selectedTaskId.value = clone.id
  }

  function moveTask(taskId, nextStartDate) {
    const task = tasks.value.find((item) => item.id === taskId)

    if (!task) {
      return
    }

    const duration =
      compareDateKeys(task.dueDate, task.startDate) >= 0
        ? differenceInDays(task.dueDate, task.startDate)
        : 0

    updateTask(taskId, {
      startDate: nextStartDate,
      dueDate: addDays(nextStartDate, duration),
      history: [...task.history, `일정 시작일이 ${nextStartDate}(으)로 변경되었습니다.`],
    })
  }

  return {
    activeCampaign,
    activeCampaignId,
    activeMode,
    calendarTab,
    calendarView,
    campaignFolderCount,
    campaignFolderIds,
    campaigns,
    folderCampaigns,
    closeTask,
    createCampaign,
    createTask,
    currentDate,
    currentUserId,
    cycleSortMode,
    cycleStatusFilter,
    deleteTask,
    duplicateTask,
    createSeedDate,
    createTaskToken,
    filteredTasks,
    findMember,
    initialize,
    isCampaignInFolder,
    loadCampaignsFromServer,
    resetCampaigns,
    members,
    modalMode,
    modalTask,
    moveCampaignToFolder,
    moveTask,
    openCreateModal,
    openTask,
    orderedCampaigns,
    periodLabel,
    priorityLabels,
    reports,
    reorderCampaign,
    restoreCampaignFromFolder,
    searchQuery,
    selectedTask,
    setActiveCampaign,
    setActiveMode,
    setCampaignUiOwnerKey,
    setCalendarTab,
    setCalendarView,
    setSearchQuery,
    setToday,
    setTheme,
    shiftPeriod,
    sidebarCampaigns,
    sidebarCollapsed,
    sortMode,
    spanMode,
    statusFilter,
    statusLabels,
    tasks,
    templates,
    taskOpenToken,
    theme,
    toggleSidebar,
    toggleSpanMode,
    toggleTheme,
    updateCampaign,
    updateCampaignStatus,
    updateTask,
  }
})
