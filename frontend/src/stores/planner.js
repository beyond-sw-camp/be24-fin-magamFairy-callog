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

const themeStorageKey = 'kellog-theme'
const tasksStorageKey = 'kellog-tasks'

function cloneValue(value) {
  return JSON.parse(JSON.stringify(value))
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

export const usePlannerStore = defineStore('planner', () => {
  const sidebarCollapsed = ref(true)
  const theme = ref('light')
  const activeMode = ref('personal')
  const calendarView = ref('month')
  const calendarTab = ref('calendar')
  const currentDate = ref('2026-04-15')
  const searchQuery = ref('')
  const statusFilter = ref('all')
  const sortMode = ref('due')
  const spanMode = ref(false)
  const selectedTaskId = ref(null)
  const modalMode = ref('view')
  const createSeedDate = ref('2026-04-15')
  const tasks = ref(cloneValue(seedTasks))

  function persistTasks() {
    if (typeof window === 'undefined') {
      return
    }

    window.localStorage.setItem(tasksStorageKey, JSON.stringify(tasks.value))
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

    const storedTheme = window.localStorage.getItem(themeStorageKey)
    const storedTasks = window.localStorage.getItem(tasksStorageKey)

    if (storedTheme === 'light' || storedTheme === 'dark') {
      theme.value = storedTheme
    } else {
      theme.value = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
    }

    if (storedTasks) {
      const parsedTasks = safeParseTasks(storedTasks)
      if (parsedTasks) {
        tasks.value = parsedTasks
      }
    }

    applyTheme(theme.value)
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

  function toggleTheme() {
    theme.value = theme.value === 'light' ? 'dark' : 'light'
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

  function openTask(taskId) {
    selectedTaskId.value = taskId
    modalMode.value = 'view'
  }

  function closeTask() {
    selectedTaskId.value = null
    modalMode.value = 'view'
  }

  function openCreateModal(dateKey = currentDate.value) {
    createSeedDate.value = dateKey
    selectedTaskId.value = null
    modalMode.value = 'create'
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
    activeMode,
    calendarTab,
    calendarView,
    closeTask,
    createTask,
    currentDate,
    currentUserId,
    cycleSortMode,
    cycleStatusFilter,
    deleteTask,
    duplicateTask,
    filteredTasks,
    findMember,
    initialize,
    members,
    modalMode,
    modalTask,
    moveTask,
    openCreateModal,
    openTask,
    periodLabel,
    priorityLabels,
    reports,
    searchQuery,
    selectedTask,
    setActiveMode,
    setCalendarTab,
    setCalendarView,
    setSearchQuery,
    setSidebarCollapsed,
    setToday,
    shiftPeriod,
    sidebarCollapsed,
    sortMode,
    spanMode,
    statusFilter,
    statusLabels,
    tasks,
    templates,
    theme,
    toggleSidebar,
    toggleSpanMode,
    toggleTheme,
    updateTask,
  }
})
