import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import { addDays, compareDateKeys, formatDateKey, todayKey } from '@/utils/calendar'
import {
  contentGeneratorBlueprints,
  customerSeed,
  customerSuggestionSeed,
  meetingNoteSeed,
  operationDomainTabs,
  operationRoleOptions,
  qaRequestSeed,
  taskNoteSeed,
} from '@/data/operationsSeed'
import { currentUserId, priorityLabels, statusLabels, teamMembers } from '@/data/scheduleSeed'
import { usePlannerStore } from '@/stores/planner'

function cloneValue(value) {
  return JSON.parse(JSON.stringify(value))
}

function parseDate(value) {
  const [year, month, day] = String(value).split('-').map(Number)
  return new Date(year, month - 1, day, 12)
}

function getWeekdayIndex(value) {
  return (parseDate(value).getDay() + 6) % 7
}

function getNextWeekday(baseDate, targetWeekday) {
  const cursor = parseDate(baseDate)

  while (getWeekdayIndex(formatDateKey(cursor)) !== targetWeekday) {
    cursor.setDate(cursor.getDate() + 1)
  }

  return formatDateKey(cursor)
}

function uniqueValues(items) {
  return [...new Set(items.filter(Boolean))]
}

function nextStatusFor(taskStatus) {
  const flow = {
    planned: 'in_progress',
    in_progress: 'review',
    review: 'done',
    done: 'done',
    at_risk: 'review',
  }

  return flow[taskStatus] ?? 'review'
}

function nowStamp() {
  return new Intl.DateTimeFormat('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
    .format(new Date())
    .replace(/\./g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

export const useOperationsStore = defineStore('operations', () => {
  const planner = usePlannerStore()

  const activeTab = ref('workload')
  const activeRole = ref('admin')
  const activeMetricKey = ref(null)
  const hoverMetricKey = ref(null)
  const focusTarget = ref(null)
  const tasknoteMode = ref('personal')
  const tasknoteListFilter = ref('all')
  const selectedCustomerId = ref(customerSeed[0]?.id ?? null)
  const selectedTeamTaskId = ref(null)
  const selectedWorkItem = ref(null)
  const generatedBatchSize = ref(5)
  const customerSearchQuery = ref('')
  const customerHealthFilter = ref('all')
  const workInboxFilter = ref('all')
  const customers = ref(
    cloneValue(customerSeed).map((customer) => ({
      ...customer,
      lastSavedAt: `${customer.lastQaUpdate} 09:00`,
    })),
  )
  const customerSuggestions = ref(cloneValue(customerSuggestionSeed))
  const qaRequests = ref(cloneValue(qaRequestSeed))
  const meetingNotes = ref(cloneValue(meetingNoteSeed))
  const completedTaskIds = ref([...taskNoteSeed.completedMarks])
  const statusRequests = ref(cloneValue(taskNoteSeed.statusRequests))
  const generatedSequence = ref(1)

  function currentStamp(dateKey = planner.currentDate || todayKey()) {
    return `${dateKey} ${nowStamp().split(' ').pop()}`
  }

  const members = computed(() => teamMembers)
  const memberMap = computed(() =>
    members.value.reduce((accumulator, member) => {
      accumulator[member.id] = member
      return accumulator
    }, {}),
  )

  const selectedCustomer = computed(() => {
    return customers.value.find((customer) => customer.id === selectedCustomerId.value) ?? null
  })

  const customerSuggestionsForSelected = computed(() => {
    return customerSuggestions.value
      .filter((suggestion) => suggestion.customerId === selectedCustomerId.value)
      .sort((left, right) => right.createdAt.localeCompare(left.createdAt))
  })

  const selectedCustomerHistory = computed(() => {
    return [...(selectedCustomer.value?.history ?? [])].sort((left, right) =>
      right.createdAt.localeCompare(left.createdAt),
    )
  })

  const pendingCustomerSuggestions = computed(() => {
    return customerSuggestions.value.filter((suggestion) => suggestion.status === 'pending')
  })

  const filteredCustomers = computed(() => {
    const keyword = customerSearchQuery.value.trim().toLowerCase()

    return customers.value.filter((customer) => {
      const matchesHealth =
        customerHealthFilter.value === 'all' || customer.health === customerHealthFilter.value
      const matchesKeyword =
        !keyword ||
        [
          customer.name,
          customer.segment,
          customer.email,
          customer.memo,
          customer.tone,
          ...customer.tags,
          ...customer.traits,
        ]
          .join(' ')
          .toLowerCase()
          .includes(keyword)

      return matchesHealth && matchesKeyword
    })
  })

  const generatorPreview = computed(() => {
    const weekdayPriority = [0, 2, 4, 1, 3]
    const weekdayCounts = [0, 0, 0, 0, 0]
    const weekdayUsage = [0, 0, 0, 0, 0]
    const previewBaseDate = planner.currentDate || todayKey()

    planner.tasks.forEach((task) => {
      const weekday = getWeekdayIndex(task.dueDate)

      if (weekday >= 0 && weekday <= 4) {
        weekdayCounts[weekday] += 1
      }
    })

    const nextSlots = weekdayPriority.reduce((accumulator, weekday) => {
      accumulator[weekday] = getNextWeekday(previewBaseDate, weekday)
      return accumulator
    }, {})

    const items = Array.from({ length: generatedBatchSize.value }, (_, index) => {
      const minCount = Math.min(...weekdayCounts)
      const targetWeekday =
        weekdayPriority.find((weekday) => weekdayCounts[weekday] <= minCount + 1) ??
        weekdayPriority[0]
      const blueprint = contentGeneratorBlueprints[index % contentGeneratorBlueprints.length]
      const dueDate = addDays(nextSlots[targetWeekday], weekdayUsage[targetWeekday] * 7)

      weekdayCounts[targetWeekday] += 1
      weekdayUsage[targetWeekday] += 1

      return {
        ...blueprint,
        previewId: `generator-${index + 1}`,
        dueDate,
        weekdayIndex: targetWeekday,
        weekdayLabel: ['월', '화', '수', '목', '금'][targetWeekday],
      }
    })

    return {
      items,
      counts: weekdayCounts,
      spread: Math.max(...weekdayCounts) - Math.min(...weekdayCounts),
    }
  })

  const qaQueue = computed(() => {
    return qaRequests.value
      .map((request) => ({
        ...request,
        customer: customers.value.find((customer) => customer.id === request.customerId) ?? null,
        recommendedAssignee: memberMap.value[request.recommendedAssigneeId] ?? null,
      }))
      .sort((left, right) => compareDateKeys(left.dueDate, right.dueDate))
  })

  const meetingQueue = computed(() => {
    return meetingNotes.value
      .map((note) => ({
        ...note,
        approvalsCompleted: note.attendees.every((attendee) => attendee.approved),
        approvalCount: note.attendees.filter((attendee) => attendee.approved).length,
      }))
      .sort((left, right) => compareDateKeys(left.meetingDate, right.meetingDate))
  })

  const workInboxItems = computed(() => {
    const generatorItem = {
      id: 'generator-preview',
      type: 'generator',
      section: '기본 생성',
      title: `기본 콘텐츠 ${generatedBatchSize.value}건 생성`,
      subtitle: '월, 수, 금 우선 규칙으로 다음 배치를 생성합니다.',
      dueDate: generatorPreview.value.items[0]?.dueDate ?? planner.currentDate ?? todayKey(),
      status: generatorPreview.value.spread <= 2 ? 'balanced' : 'watch',
      count: generatorPreview.value.items.length,
      needsApproval: false,
      sortWeight: 3,
    }

    const qaItems = qaQueue.value
      .filter((request) => request.status !== 'approved')
      .map((request) => ({
        id: request.id,
        type: 'qa',
        section: 'QA 요청',
        title: request.title,
        subtitle: request.aiSummary,
        dueDate: request.dueDate,
        customer: request.customer?.name ?? '고객 미지정',
        status: request.status,
        needsApproval: ['saved', 'submitted'].includes(request.status),
        sortWeight:
          request.status === 'submitted'
            ? 0
            : request.status === 'saved'
              ? 1
              : request.status === 'draft'
                ? 2
                : 3,
      }))

    const meetingItems = meetingQueue.value
      .filter((note) => !note.linkedTaskId)
      .map((note) => ({
        id: note.id,
        type: 'meeting',
        section: '내부 회의',
        title: note.title,
        subtitle: note.aiSummary,
        dueDate: note.meetingDate,
        customer: note.customer,
        status: note.approvalsCompleted ? 'ready' : 'waiting',
        needsApproval: !note.approvalsCompleted,
        approvalCount: note.approvalCount,
        approvalTotal: note.attendees.length,
        sortWeight: note.approvalsCompleted ? 2 : 1,
      }))

    return [...qaItems, ...meetingItems, generatorItem]
      .filter((item) => {
        if (workInboxFilter.value === 'approvals') {
          return item.needsApproval
        }

        if (workInboxFilter.value === 'drafts') {
          return item.type === 'qa' && ['draft', 'saved'].includes(item.status)
        }

        if (workInboxFilter.value === 'generator') {
          return item.type === 'generator'
        }

        return true
      })
      .sort((left, right) => {
        if (left.sortWeight !== right.sortWeight) {
          return left.sortWeight - right.sortWeight
        }

        return compareDateKeys(left.dueDate, right.dueDate)
      })
  })

  const selectedWorkItemDetail = computed(() => {
    if (!selectedWorkItem.value) {
      return null
    }

    if (selectedWorkItem.value.type === 'qa') {
      const request = qaQueue.value.find((item) => item.id === selectedWorkItem.value.id)
      return request ? { ...request, type: 'qa' } : null
    }

    if (selectedWorkItem.value.type === 'meeting') {
      const note = meetingQueue.value.find((item) => item.id === selectedWorkItem.value.id)
      return note ? { ...note, type: 'meeting' } : null
    }

    if (selectedWorkItem.value.type === 'generator') {
      return {
        ...generatorPreview.value,
        type: 'generator',
      }
    }

    return null
  })

  const workloadMetrics = computed(() => {
    const pendingQa = qaRequests.value.filter((request) =>
      ['draft', 'saved', 'submitted'].includes(request.status),
    ).length
    const pendingMeetingApprovals = meetingNotes.value.filter((note) =>
      note.attendees.some((attendee) => !attendee.approved),
    ).length

    return [
      {
        id: 'metric-approvals',
        label: '대기 승인',
        value: String(pendingQa + pendingMeetingApprovals),
        detail: 'QA와 내부 회의에서 승인을 기다리는 항목입니다.',
        accent: '#2f80ed',
      },
      {
        id: 'metric-generated',
        label: '오늘 생성 예정',
        value: String(generatorPreview.value.items.length),
        detail: '기본 생성 규칙으로 바로 캘린더에 넣을 수 있는 배치입니다.',
        accent: '#59c36d',
      },
      {
        id: 'metric-balance',
        label: '분배 균형',
        value: generatorPreview.value.spread <= 2 ? '양호' : '조정 필요',
        detail: `요일별 최대 편차 ${generatorPreview.value.spread}`,
        accent: '#f5b64e',
      },
      {
        id: 'metric-meeting',
        label: '회의 승인 대기',
        value: String(pendingMeetingApprovals),
        detail: '참석자 승인 완료 후 카드 추가 요청으로 이어집니다.',
        accent: '#df5f75',
      },
    ]
  })

  const personalTaskNotes = computed(() => {
    return [...planner.tasks]
      .filter((task) => task.assigneeId === currentUserId)
      .sort((left, right) => compareDateKeys(left.dueDate, right.dueDate))
      .map((task) => ({
        ...task,
        isMarkedComplete: completedTaskIds.value.includes(task.id),
        nextStatus: nextStatusFor(task.status),
      }))
  })

  const filteredPersonalTaskNotes = computed(() => {
    if (tasknoteListFilter.value === 'completed') {
      return personalTaskNotes.value.filter((task) => task.isMarkedComplete)
    }

    return personalTaskNotes.value
  })

  const pendingStatusRequests = computed(() => {
    return statusRequests.value
      .filter((request) => request.state === 'pending')
      .map((request) => ({
        ...request,
        task: planner.tasks.find((task) => task.id === request.taskId) ?? null,
      }))
  })

  const teamTaskGroups = computed(() => {
    return members.value
      .map((member) => ({
        ...member,
        tasks: [...planner.tasks]
          .filter((task) => task.assigneeId === member.id)
          .sort((left, right) => compareDateKeys(left.dueDate, right.dueDate)),
      }))
      .filter((group) => group.tasks.length)
  })

  const selectedTeamTask = computed(() => {
    const flattenedTasks = teamTaskGroups.value.flatMap((group) => group.tasks)

    return (
      flattenedTasks.find((task) => task.id === selectedTeamTaskId.value) ??
      flattenedTasks[0] ??
      null
    )
  })

  watch(
    filteredCustomers,
    (items) => {
      if (!items.length) {
        selectedCustomerId.value = null
        return
      }

      if (!items.some((customer) => customer.id === selectedCustomerId.value)) {
        selectedCustomerId.value = items[0].id
      }
    },
    { immediate: true },
  )

  watch(
    workInboxItems,
    (items) => {
      if (!items.length) {
        selectedWorkItem.value = null
        return
      }

      const hasCurrent = items.some(
        (item) => item.type === selectedWorkItem.value?.type && item.id === selectedWorkItem.value?.id,
      )

      if (!hasCurrent) {
        selectedWorkItem.value = {
          type: items[0].type,
          id: items[0].id,
        }
      }
    },
    { immediate: true },
  )

  function nextGeneratedId(prefix) {
    const value = `${prefix}_${String(generatedSequence.value).padStart(3, '0')}`
    generatedSequence.value += 1
    return value
  }

  function buildPlannerTaskPayload({
    prefix,
    title,
    dueDate,
    assigneeId,
    customer,
    contentType,
    summary,
    description,
    priority = 'medium',
    status = 'planned',
    tags = [],
    palette,
    history = [],
  }) {
    const identifier = nextGeneratedId(prefix)

    return {
      id: identifier,
      requirementId: identifier,
      title,
      category: '운영 허브',
      summary,
      description,
      assigneeId,
      plannerId: currentUserId,
      designerId: 'sumin',
      publisherId: 'minseo',
      supervisorId: 'taeyoung',
      startDate: dueDate,
      dueDate,
      status,
      priority,
      customer,
      contentType,
      visibility: assigneeId === currentUserId ? 'personal' : 'team',
      timeRange: '10:00 - 13:00',
      tags,
      attachments: [],
      history,
      progress: status === 'done' ? 100 : status === 'in_progress' ? 55 : 22,
      palette: palette ?? { accent: '#2f80ed', surface: '#e8f2ff', text: '#1f5cb7' },
    }
  }

  function pushCustomerHistory(customerId, entry) {
    const customer = customers.value.find((item) => item.id === customerId)

    if (!customer) {
      return
    }

    customer.history.unshift({
      id: `history-${customerId}-${Date.now()}`,
      ...entry,
    })
  }

  function enqueueSuggestionFromQa(request) {
    const suggestionId = `qa-suggestion-${request.id}`

    if (customerSuggestions.value.some((item) => item.id === suggestionId)) {
      return
    }

    customerSuggestions.value.unshift({
      id: suggestionId,
      customerId: request.customerId,
      source: request.source,
      title: `${request.title} 기반 자동 고객 메모 제안`,
      summary:
        'QA 요청에서 추출한 수정 포인트와 추천 담당자 정보를 고객 프로필에 반영하도록 제안합니다.',
      suggestedTags: ['QA 자동화', '추천 담당자'],
      suggestedTraits: ['수정 요청 히스토리 추적'],
      suggestedMemo: `최근 QA 요청 "${request.title}"의 패턴을 반영해 고객 메모에 추천 담당자 규칙과 요청 우선순위를 추가합니다.`,
      createdAt: currentStamp(),
      status: 'pending',
    })
  }

  function setActiveTab(tab) {
    activeTab.value = tab
    activeMetricKey.value = null
    hoverMetricKey.value = null
    focusTarget.value = null

    if (tab === 'customers') {
      customerHealthFilter.value = 'all'
    }

    if (tab === 'workload') {
      workInboxFilter.value = 'all'
    }

    if (tab === 'tasknotes') {
      tasknoteListFilter.value = 'all'
    }
  }

  function setActiveRole(role) {
    activeRole.value = role
  }

  function setHoverMetricKey(metricId) {
    hoverMetricKey.value = metricId
  }

  function setTasknoteListFilter(value) {
    tasknoteListFilter.value = value
  }

  function setTasknoteMode(mode) {
    tasknoteMode.value = mode
  }

  function setCustomerSearchQuery(value) {
    customerSearchQuery.value = value
  }

  function setCustomerHealthFilter(value) {
    customerHealthFilter.value = value
  }

  function setWorkInboxFilter(value) {
    workInboxFilter.value = value
  }

  function setSelectedCustomer(customerId) {
    selectedCustomerId.value = customerId
  }

  function activateMetric(metricId) {
    hoverMetricKey.value = null

    if (activeMetricKey.value === metricId) {
      activeMetricKey.value = null
      focusTarget.value = null
      customerHealthFilter.value = 'all'
      workInboxFilter.value = 'all'
      tasknoteListFilter.value = 'all'
      return
    }

    activeMetricKey.value = metricId
    focusTarget.value = null

    if (activeTab.value === 'workload') {
      if (metricId === 'metric-approvals') {
        workInboxFilter.value = 'approvals'
        const firstQa = qaQueue.value.find((request) =>
          ['saved', 'submitted'].includes(request.status),
        )
        const firstMeeting = meetingQueue.value.find(
          (note) => !note.approvalsCompleted && !note.linkedTaskId,
        )

        if (firstQa) {
          setSelectedWorkItem({ type: 'qa', id: firstQa.id })
        } else if (firstMeeting) {
          setSelectedWorkItem({ type: 'meeting', id: firstMeeting.id })
        }

        focusTarget.value = 'workload-inbox'
        return
      }

      if (metricId === 'metric-generated') {
        workInboxFilter.value = 'generator'
        setSelectedWorkItem({ type: 'generator', id: 'generator-preview' })
        focusTarget.value = 'workload-generator'
        return
      }

      if (metricId === 'metric-balance') {
        workInboxFilter.value = 'generator'
        setSelectedWorkItem({ type: 'generator', id: 'generator-preview' })
        focusTarget.value = 'workload-balance'
        return
      }

      if (metricId === 'metric-meeting') {
        workInboxFilter.value = 'all'
        const meeting = meetingQueue.value.find(
          (note) => !note.approvalsCompleted && !note.linkedTaskId,
        )

        if (meeting) {
          setSelectedWorkItem({ type: 'meeting', id: meeting.id })
        }

        focusTarget.value = 'workload-meeting'
        return
      }
    }

    if (activeTab.value === 'customers') {
      if (metricId === 'customer-total') {
        customerHealthFilter.value = 'all'
        focusTarget.value = 'customers-list'
        return
      }

      if (metricId === 'customer-ai') {
        customerHealthFilter.value = 'all'
        const suggestion = pendingCustomerSuggestions.value[0]

        if (suggestion) {
          setSelectedCustomer(suggestion.customerId)
        }

        focusTarget.value = 'customers-suggestions'
        return
      }

      if (metricId === 'customer-stable') {
        customerHealthFilter.value = 'stable'
        focusTarget.value = 'customers-list'
        return
      }

      if (metricId === 'customer-risk') {
        customerHealthFilter.value = 'risk'
        focusTarget.value = 'customers-list'
      }

      return
    }

    if (metricId === 'tasknote-today') {
      tasknoteMode.value = 'personal'
      tasknoteListFilter.value = 'all'
      focusTarget.value = 'tasknotes-list'
      return
    }

    if (metricId === 'tasknote-complete') {
      tasknoteMode.value = 'personal'
      tasknoteListFilter.value = 'completed'
      focusTarget.value = 'tasknotes-list'
      return
    }

    if (metricId === 'tasknote-requests') {
      tasknoteMode.value = 'personal'
      focusTarget.value = 'tasknotes-requests'
      return
    }

    if (metricId === 'tasknote-team') {
      tasknoteMode.value = 'team'
      focusTarget.value = 'tasknotes-team'
    }
  }

  function getMetricPreview(metricId) {
    if (metricId === 'metric-approvals') {
      return [
        ...qaQueue.value
          .filter((request) => ['saved', 'submitted'].includes(request.status))
          .slice(0, 2)
          .map((request) => ({
            title: request.title,
            meta: `${request.customer?.name ?? '고객'} · ${request.status === 'submitted' ? '승인 요청' : '초안 저장'}`,
          })),
        ...meetingQueue.value
          .filter((note) => !note.approvalsCompleted)
          .slice(0, 1)
          .map((note) => ({
            title: note.title,
            meta: `${note.approvalCount}/${note.attendees.length} 승인`,
          })),
      ].slice(0, 3)
    }

    if (metricId === 'metric-generated') {
      return generatorPreview.value.items.slice(0, 3).map((item) => ({
        title: item.title,
        meta: `${item.customer} · ${item.weekdayLabel} ${item.dueDate}`,
      }))
    }

    if (metricId === 'metric-balance') {
      return generatorPreview.value.counts.map((count, index) => ({
        title: ['월', '화', '수', '목', '금'][index],
        meta: `${count}건`,
      }))
    }

    if (metricId === 'metric-meeting') {
      return meetingQueue.value
        .filter((note) => !note.approvalsCompleted)
        .slice(0, 3)
        .map((note) => ({
          title: note.title,
          meta: `${note.approvalCount}/${note.attendees.length} 승인`,
        }))
    }

    if (metricId === 'customer-total') {
      return customers.value.slice(0, 3).map((customer) => ({
        title: customer.name,
        meta: customerHealthFilter.value === 'all' ? customer.segment : customer.health,
      }))
    }

    if (metricId === 'customer-ai') {
      return pendingCustomerSuggestions.value.slice(0, 3).map((suggestion) => ({
        title: suggestion.title,
        meta: customers.value.find((customer) => customer.id === suggestion.customerId)?.name ?? '고객',
      }))
    }

    if (metricId === 'customer-stable' || metricId === 'customer-risk') {
      const targetHealth = metricId === 'customer-stable' ? 'stable' : 'risk'
      return customers.value
        .filter((customer) => customer.health === targetHealth)
        .slice(0, 3)
        .map((customer) => ({
          title: customer.name,
          meta: customer.segment,
        }))
    }

    if (metricId === 'tasknote-today') {
      return personalTaskNotes.value.slice(0, 3).map((task) => ({
        title: task.title,
        meta: `${task.customer} · ${task.dueDate}`,
      }))
    }

    if (metricId === 'tasknote-complete') {
      return personalTaskNotes.value
        .filter((task) => task.isMarkedComplete)
        .slice(0, 3)
        .map((task) => ({
          title: task.title,
          meta: '완료 표시',
        }))
    }

    if (metricId === 'tasknote-requests') {
      return pendingStatusRequests.value.slice(0, 3).map((request) => ({
        title: request.task?.title ?? '연결 작업 없음',
        meta: statusLabels[request.nextStatus] ?? request.nextStatus,
      }))
    }

    if (metricId === 'tasknote-team') {
      return teamTaskGroups.value.slice(0, 3).map((group) => ({
        title: group.name,
        meta: `${group.tasks.length}건`,
      }))
    }

    return []
  }

  function setSelectedWorkItem(payload) {
    selectedWorkItem.value = payload
      ? {
          type: payload.type,
          id: payload.id,
        }
      : null
  }

  function updateSelectedCustomerField(field, value) {
    if (!selectedCustomer.value) {
      return
    }

    selectedCustomer.value[field] = value
    selectedCustomer.value.lastSavedAt = currentStamp()
  }

  function updateSelectedCustomerListField(field, value) {
    if (!selectedCustomer.value) {
      return
    }

    selectedCustomer.value[field] = uniqueValues(
      String(value)
        .split(',')
        .map((item) => item.trim()),
    )
    selectedCustomer.value.lastSavedAt = currentStamp()
  }

  function createCustomer() {
    const identifier = nextGeneratedId('CUSTOMER')
    const createdCustomer = {
      id: `customer-${identifier.toLowerCase()}`,
      name: '신규 고객',
      ownerId: currentUserId,
      segment: '신규 분류',
      email: 'new-customer@example.com',
      tone: '선호 톤 정리 필요',
      health: 'stable',
      lastQaUpdate: planner.currentDate || todayKey(),
      lastSavedAt: currentStamp(),
      tags: ['신규'],
      traits: ['특성 정리 필요'],
      memo: '운영 허브에서 신규 고객 초안을 만들었습니다.',
      history: [
        {
          id: `history-${identifier.toLowerCase()}-create`,
          type: 'create',
          title: '신규 고객 초안 생성',
          detail: '고객 세부 정보를 입력해 즉시 운영 허브에 반영할 수 있습니다.',
          createdAt: currentStamp(),
        },
      ],
    }

    customerSearchQuery.value = ''
    customerHealthFilter.value = 'all'
    customers.value.unshift(createdCustomer)
    selectedCustomerId.value = createdCustomer.id
  }

  function deleteCustomer(customerId) {
    customers.value = customers.value.filter((customer) => customer.id !== customerId)
    customerSuggestions.value = customerSuggestions.value.filter(
      (suggestion) => suggestion.customerId !== customerId,
    )

    if (selectedCustomerId.value === customerId) {
      selectedCustomerId.value = filteredCustomers.value[0]?.id ?? customers.value[0]?.id ?? null
    }
  }

  function approveSuggestion(suggestionId) {
    const suggestion = customerSuggestions.value.find((item) => item.id === suggestionId)
    const customer = customers.value.find((item) => item.id === suggestion?.customerId)

    if (!suggestion || !customer || suggestion.status !== 'pending') {
      return
    }

    customer.tags = uniqueValues([...customer.tags, ...suggestion.suggestedTags])
    customer.traits = uniqueValues([...customer.traits, ...suggestion.suggestedTraits])
    customer.memo = `${customer.memo}\n\n${suggestion.suggestedMemo}`.trim()
    customer.lastQaUpdate = planner.currentDate || todayKey()
    customer.lastSavedAt = currentStamp()
    suggestion.status = 'approved'

    pushCustomerHistory(customer.id, {
      type: 'ai',
      title: suggestion.title,
      detail: suggestion.suggestedMemo,
      createdAt: currentStamp(),
    })
  }

  function rejectSuggestion(suggestionId) {
    const suggestion = customerSuggestions.value.find((item) => item.id === suggestionId)

    if (!suggestion || suggestion.status !== 'pending') {
      return
    }

    suggestion.status = 'rejected'
    pushCustomerHistory(suggestion.customerId, {
      type: 'note',
      title: `${suggestion.title} 보류`,
      detail: 'AI 제안은 보류되었고 기존 고객 메모는 유지되었습니다.',
      createdAt: currentStamp(),
    })
  }

  function setGeneratedBatchSize(value) {
    generatedBatchSize.value = Math.max(1, Math.min(8, value))
  }

  function createGeneratedContentBatch() {
    generatorPreview.value.items.forEach((item, index) => {
      const payload = buildPlannerTaskPayload({
        prefix: 'WORKLOAD',
        title: item.title,
        dueDate: item.dueDate,
        assigneeId: item.assigneeId,
        customer: item.customer,
        contentType: item.contentType,
        summary: item.summary,
        description: item.description,
        priority: item.priority,
        palette: item.palette,
        tags: ['기본 생성', item.weekdayLabel],
        history: [
          `${index + 1}번째 자동 분배 패널로 생성되었습니다.`,
          `${item.weekdayLabel} 우선 규칙에 따라 일정이 배정되었습니다.`,
        ],
      })

      planner.createTask(payload)
    })

    planner.closeTask()
  }

  function saveQaDraft(requestId) {
    const request = qaRequests.value.find((item) => item.id === requestId)

    if (!request) {
      return
    }

    if (!request.linkedTaskId) {
      const payload = buildPlannerTaskPayload({
        prefix: 'QA',
        title: request.title,
        dueDate: request.dueDate,
        assigneeId: request.recommendedAssigneeId,
        customer: customers.value.find((item) => item.id === request.customerId)?.name ?? '고객',
        contentType: 'QA 요청',
        summary: request.aiSummary,
        description: request.requestedChanges.join('\n'),
        priority: 'high',
        status: 'review',
        tags: ['QA', '승인 대기'],
        palette: { accent: '#ff9f43', surface: '#fff3e5', text: '#a76200' },
        history: ['사용자 뷰에서 QA 요청 초안이 저장되었습니다.'],
      })

      request.linkedTaskId = payload.id
      planner.createTask(payload)
      planner.closeTask()
    }

    request.status = 'saved'
    enqueueSuggestionFromQa(request)
    pushCustomerHistory(request.customerId, {
      type: 'qa',
      title: `${request.title} 초안 저장`,
      detail: 'QA 요청이 초안 카드로 저장되었고 관리자 승인 대기열과 연결되었습니다.',
      createdAt: currentStamp(),
    })
  }

  function approveQaRequest(requestId) {
    const request = qaRequests.value.find((item) => item.id === requestId)

    if (!request) {
      return
    }

    if (request.linkedTaskId) {
      const linkedTask = planner.tasks.find((task) => task.id === request.linkedTaskId)

      if (linkedTask) {
        planner.updateTask(linkedTask.id, {
          status: 'planned',
          assigneeId: request.recommendedAssigneeId,
          history: [...linkedTask.history, '운영 허브에서 QA 요청이 승인되어 실행 단계로 전환되었습니다.'],
        })
      }
    } else {
      const payload = buildPlannerTaskPayload({
        prefix: 'QA',
        title: request.title,
        dueDate: request.dueDate,
        assigneeId: request.recommendedAssigneeId,
        customer: customers.value.find((item) => item.id === request.customerId)?.name ?? '고객',
        contentType: 'QA 요청',
        summary: request.aiSummary,
        description: request.requestedChanges.join('\n'),
        priority: 'critical',
        status: 'planned',
        tags: ['QA', '승인 완료'],
        palette: { accent: '#df5f75', surface: '#ffe9ee', text: '#b8455b' },
        history: ['관리자 뷰에서 QA 요청을 승인하고 콘텐츠 카드를 생성했습니다.'],
      })

      request.linkedTaskId = payload.id
      planner.createTask(payload)
      planner.closeTask()
    }

    request.status = 'approved'
    enqueueSuggestionFromQa(request)
    pushCustomerHistory(request.customerId, {
      type: 'qa',
      title: `${request.title} 승인`,
      detail: 'QA 요청이 승인되어 캘린더와 운영 보드 카드로 연결되었습니다.',
      createdAt: currentStamp(),
    })
  }

  function toggleMeetingApproval(noteId, memberId) {
    const note = meetingNotes.value.find((item) => item.id === noteId)
    const attendee = note?.attendees.find((item) => item.memberId === memberId)

    if (!note || !attendee) {
      return
    }

    attendee.approved = !attendee.approved
  }

  function createMeetingTask(noteId) {
    const note = meetingNotes.value.find((item) => item.id === noteId)

    if (!note || note.attendees.some((attendee) => !attendee.approved)) {
      return
    }

    if (!note.linkedTaskId) {
      const payload = buildPlannerTaskPayload({
        prefix: 'MEETING',
        title: note.suggestedTaskTitle,
        dueDate: addDays(note.meetingDate, 2),
        assigneeId: note.organizerId,
        customer: note.customer,
        contentType: '내부 업무',
        summary: note.aiSummary,
        description: note.suggestedDescription,
        priority: 'medium',
        tags: ['회의록', '자동 정리'],
        palette: { accent: '#7c4dff', surface: '#f2edff', text: '#5632c9' },
        history: ['회의 참석자 전원 승인 후 후속 작업 카드가 생성되었습니다.'],
      })

      note.linkedTaskId = payload.id
      planner.createTask(payload)
      planner.closeTask()
    }
  }

  function setSelectedTeamTask(taskId) {
    selectedTeamTaskId.value = taskId
  }

  function toggleTaskCompletion(taskId) {
    if (completedTaskIds.value.includes(taskId)) {
      completedTaskIds.value = completedTaskIds.value.filter((item) => item !== taskId)
      return
    }

    completedTaskIds.value = [...completedTaskIds.value, taskId]
  }

  function requestNextStatus(taskId) {
    const task = planner.tasks.find((item) => item.id === taskId)

    if (!task) {
      return
    }

    const existingRequest = statusRequests.value.find(
      (request) => request.taskId === taskId && request.state === 'pending',
    )

    if (existingRequest) {
      existingRequest.createdAt = currentStamp()
      return
    }

    statusRequests.value.unshift({
      id: `status-request-${Date.now()}`,
      taskId,
      nextStatus: nextStatusFor(task.status),
      reason: `${statusLabels[task.status] ?? task.status} 단계가 마무리되어 다음 단계 전환 요청을 만들었습니다.`,
      state: 'pending',
      createdAt: currentStamp(),
    })
  }

  function applyStatusRequest(requestId) {
    const request = statusRequests.value.find((item) => item.id === requestId)
    const task = planner.tasks.find((item) => item.id === request?.taskId)

    if (!request || !task || request.state !== 'pending') {
      return
    }

    planner.updateTask(task.id, {
      status: request.nextStatus,
      history: [
        ...task.history,
        `${priorityLabels[task.priority]} 우선순위 카드가 ${statusLabels[request.nextStatus]} 단계로 전환되었습니다.`,
      ],
    })

    request.state = 'applied'
  }

  function shiftTeamTask(taskId, amount) {
    const task = planner.tasks.find((item) => item.id === taskId)

    if (!task) {
      return
    }

    planner.moveTask(taskId, addDays(task.startDate, amount))
  }

  function reassignTeamTask(taskId, memberId) {
    const task = planner.tasks.find((item) => item.id === taskId)

    if (!task) {
      return
    }

    planner.updateTask(taskId, {
      assigneeId: memberId,
      history: [...task.history, `${memberMap.value[memberId]?.name ?? memberId}에게 담당이 재배정되었습니다.`],
    })
  }

  function findMember(memberId) {
    return memberMap.value[memberId] ?? null
  }

  return {
    activeMetricKey,
    activeRole,
    activeTab,
    activateMetric,
    approveQaRequest,
    approveSuggestion,
    applyStatusRequest,
    completedTaskIds,
    createCustomer,
    createGeneratedContentBatch,
    createMeetingTask,
    customerHealthFilter,
    customerSearchQuery,
    customerSuggestions,
    customerSuggestionsForSelected,
    customers,
    deleteCustomer,
    filteredCustomers,
    filteredPersonalTaskNotes,
    findMember,
    focusTarget,
    generatedBatchSize,
    getMetricPreview,
    generatorPreview,
    hoverMetricKey,
    meetingNotes,
    meetingQueue,
    members,
    operationDomainTabs,
    operationRoleOptions,
    pendingCustomerSuggestions,
    pendingStatusRequests,
    personalTaskNotes,
    qaQueue,
    qaRequests,
    rejectSuggestion,
    requestNextStatus,
    reassignTeamTask,
    saveQaDraft,
    selectedCustomer,
    selectedCustomerHistory,
    selectedCustomerId,
    selectedTeamTask,
    selectedTeamTaskId,
    selectedWorkItem,
    selectedWorkItemDetail,
    setActiveRole,
    setActiveTab,
    setCustomerHealthFilter,
    setCustomerSearchQuery,
    setGeneratedBatchSize,
    setHoverMetricKey,
    setSelectedCustomer,
    setSelectedTeamTask,
    setSelectedWorkItem,
    setTasknoteMode,
    setTasknoteListFilter,
    setWorkInboxFilter,
    shiftTeamTask,
    statusRequests,
    tasknoteMode,
    tasknoteListFilter,
    teamTaskGroups,
    toggleMeetingApproval,
    toggleTaskCompletion,
    updateSelectedCustomerField,
    updateSelectedCustomerListField,
    workInboxFilter,
    workInboxItems,
    workloadMetrics,
  }
})
