import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { confirm, confirmAll, getNoti } from '@/api/notifications/index.js'

const SSE_RETRY_DELAY_MS = 5000

const REVIEW_OUTCOME_META = {
  completed: { label: '검수 완료', tone: 'completed', icon: 'task_alt' },
  review_required: { label: '확인 필요', tone: 'review-required', icon: 'policy_alert' },
  failed: { label: '검수 실패', tone: 'failed', icon: 'error' },
}

function extractListPayload(response) {
  const payload = response?.data?.data ?? response?.data ?? response ?? {}

  if (Array.isArray(payload)) {
    return {
      notifications: payload,
      unreadCount: payload.filter((item) => !Boolean(item.isRead ?? item.read ?? item.confirmed)).length,
    }
  }

  return {
    notifications: payload.notifications ?? payload.items ?? payload.data ?? [],
    unreadCount: Number(payload.unreadCount ?? 0),
  }
}

function normalizeCategory(type, category) {
  const value = String(category || type || '').toLowerCase()

  if (
    value.includes('review') ||
    value.includes('qa') ||
    value.includes('judge') ||
    value.includes('ad_check')
  ) {
    return 'qa'
  }

  if (value.includes('campaign')) {
    return 'campaign'
  }

  if (value.includes('deadline') || value.includes('schedule')) {
    return 'schedule'
  }

  if (value.includes('task')) {
    return 'task'
  }

  return 'system'
}

function normalizeSeverity(value, category, type) {
  const normalized = String(value || '').toLowerCase()
  const normalizedType = String(type || '').toUpperCase()

  if (['critical', 'urgent'].includes(normalized)) {
    return 'critical'
  }

  if (['high', 'important', 'warning'].includes(normalized)) {
    return 'high'
  }

  if (['low'].includes(normalized)) {
    return 'low'
  }

  if (['AI_JUDGE_REVIEW_REQUIRED', 'AI_JUDGE_FAILED'].includes(normalizedType)) {
    return 'high'
  }

  return category === 'schedule' ? 'high' : 'normal'
}

function resolveReviewOutcome(type) {
  const normalizedType = String(type || '').toUpperCase()

  if (normalizedType === 'AI_JUDGE_COMPLETED') {
    return 'completed'
  }

  if (normalizedType === 'AI_JUDGE_REVIEW_REQUIRED') {
    return 'review_required'
  }

  if (normalizedType === 'AI_JUDGE_FAILED') {
    return 'failed'
  }

  return ''
}

function normalizeDate(value) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? new Date().toISOString() : date.toISOString()
}

function normalizeNotification(item, index = 0) {
  const category = normalizeCategory(item.type, item.category)
  const severity = normalizeSeverity(item.severity ?? item.priority, category, item.type)
  const idx = item.idx ?? item.id ?? null
  const createdAt = normalizeDate(item.createdAt ?? item.created_at ?? item.createDate ?? item.time)
  const reviewOutcome = resolveReviewOutcome(item.type)

  return {
    id: String(idx ?? `${category}-${index}`),
    idx,
    type: item.type ?? category,
    category,
    severity,
    title: item.title ?? '알림',
    message: item.message ?? item.summary ?? item.content ?? '',
    detail: item.detail ?? item.description ?? item.message ?? '',
    createdAt,
    created_at: createdAt,
    isRead: Boolean(item.isRead ?? item.read ?? item.confirmed),
    source: item.source ?? item.sender ?? 'System',
    targetLabel: item.targetLabel ?? item.linkLabel ?? '연결 정보 없음',
    targetUrl: item.targetUrl ?? item.url ?? item.link ?? '',
    referenceType: item.referenceType ?? '',
    referenceId: item.referenceId ?? null,
    referenceStatus: item.referenceStatus ?? '',
    groupPreview: item.groupPreview ?? null,
    reviewOutcome,
  }
}

export const useNotificationsStore = defineStore('notifications', () => {
  const notifications = ref([])
  const unreadCount = ref(0)
  const isLoading = ref(false)
  const loadError = ref('')
  const isSseConnected = ref(false)
  const sseError = ref('')
  const eventSource = ref(null)
  const currentToken = ref('')

  // SSE — 캘린더/내 캠페인 실시간 갱신 신호 (구독자가 watch로 수신)
  const lastCalendarRefresh = ref(0)
  const lastMyCampaignsRefresh = ref(0)
  let reconnectTimer = null

  const recentNotifications = computed(() => notifications.value.slice(0, 3))

  function getReviewOutcomeMeta(outcome) {
    return REVIEW_OUTCOME_META[outcome] ?? null
  }

  function setNotifications(items, nextUnreadCount = null) {
    notifications.value = items.map((item, index) => normalizeNotification(item, index))
    unreadCount.value = Number.isFinite(nextUnreadCount)
      ? nextUnreadCount
      : notifications.value.filter((item) => !item.isRead).length
  }

  function upsertNotification(rawNotification) {
    const notification = normalizeNotification(rawNotification)
    const existingIndex = notifications.value.findIndex((item) => item.id === notification.id)

    if (existingIndex >= 0) {
      const wasUnread = !notifications.value[existingIndex].isRead
      notifications.value.splice(existingIndex, 1, notification)
      if (wasUnread && notification.isRead) {
        unreadCount.value = Math.max(0, unreadCount.value - 1)
      }
      return
    }

    notifications.value.unshift(notification)
    if (!notification.isRead) {
      unreadCount.value += 1
    }
  }

  async function loadNotifications(options = {}) {
    isLoading.value = true
    loadError.value = ''

    try {
      const response = await getNoti(options.count)
      const payload = extractListPayload(response)
      setNotifications(payload.notifications, payload.unreadCount)
    } catch (error) {
      console.warn('Notification list request failed.', error)
      loadError.value = '알림을 불러오지 못했습니다.'
    } finally {
      isLoading.value = false
    }
  }

  async function markAsRead(notification) {
    if (!notification || notification.isRead) {
      return
    }

    notification.isRead = true
    unreadCount.value = Math.max(0, unreadCount.value - 1)

    if (!notification.idx) {
      return
    }

    try {
      await confirm(notification.idx)
    } catch (error) {
      console.warn('Notification confirm request failed.', error)
      notification.isRead = false
      unreadCount.value += 1
    }
  }

  async function markAllAsRead() {
    const unreadItems = notifications.value.filter((item) => !item.isRead)
    unreadItems.forEach((item) => {
      item.isRead = true
    })
    unreadCount.value = 0

    try {
      const response = await confirmAll()
      const payload = extractListPayload(response)
      setNotifications(payload.notifications, payload.unreadCount)
    } catch (error) {
      console.warn('Notification confirm all request failed.', error)
      await loadNotifications()
    }
  }

  function disconnect() {
    if (reconnectTimer) {
      window.clearTimeout(reconnectTimer)
      reconnectTimer = null
    }

    if (eventSource.value) {
      eventSource.value.close()
      eventSource.value = null
    }

    currentToken.value = ''
    isSseConnected.value = false
  }

  function scheduleReconnect() {
    if (!currentToken.value || reconnectTimer) {
      return
    }

    reconnectTimer = window.setTimeout(() => {
      reconnectTimer = null
      connect(currentToken.value, { force: true })
    }, SSE_RETRY_DELAY_MS)
  }

  function connect(accessToken, options = {}) {
    if (!accessToken) {
      disconnect()
      return
    }

    if (!options.force && eventSource.value && currentToken.value === accessToken) {
      return
    }

    disconnect()
    currentToken.value = accessToken

    const source = new EventSource(`/api/notifications/subscribe?token=${encodeURIComponent(accessToken)}`)
    eventSource.value = source

    source.onopen = () => {
      isSseConnected.value = true
      sseError.value = ''
      void loadNotifications()
    }

    source.addEventListener('notification.created', (event) => {
      try {
        upsertNotification(JSON.parse(event.data))
      } catch (error) {
        console.warn('Notification SSE payload parsing failed.', error)
      }
    })

    source.addEventListener('heartbeat', () => {
      isSseConnected.value = true
    })

    // 캘린더 데이터 변경 → 구독자(OverView 등)가 watch로 감지해서 재로드
    source.addEventListener('calendar.refresh', () => {
      lastCalendarRefresh.value = Date.now()
    })

    // 내 캠페인 멤버십 변경 (초대 수락 / 추방 / 직접 추가) → Sidebar2 + dashboardStore 재로드 트리거
    source.addEventListener('my-campaigns.refresh', () => {
      lastMyCampaignsRefresh.value = Date.now()
    })

    source.onerror = () => {
      isSseConnected.value = false
      sseError.value = '알림 실시간 연결을 재시도하고 있습니다.'
      if (eventSource.value) {
        eventSource.value.close()
        eventSource.value = null
      }
      scheduleReconnect()
    }
  }

  return {
    notifications,
    unreadCount,
    recentNotifications,
    isLoading,
    loadError,
    isSseConnected,
    sseError,
    lastCalendarRefresh,
    lastMyCampaignsRefresh,
    connect,
    disconnect,
    loadNotifications,
    markAsRead,
    markAllAsRead,
    upsertNotification,
    getReviewOutcomeMeta,
  }
})
