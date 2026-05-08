<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { confirm, getNoti } from '@/api/notifications/index.js'
import { formatRelativeTime } from '@/utils/datechange.js'

const fallbackNotifications = [
  {
    idx: 34897,
    type: 'qa',
    severity: 'high',
    created_at: new Date(Date.now() - 1000 * 60 * 12).toISOString(),
    title: '검수 요청이 도착했습니다',
    message: '캠페인 랜딩 페이지 초안에 대한 QA 검수가 요청되었습니다.',
    detail:
      '담당 매니저가 캠페인 랜딩 페이지 초안 검수를 요청했습니다. 승인 또는 수정 요청을 남기면 담당자에게 결과 알림이 전달됩니다.',
    source: '시스템',
    targetLabel: '검수 상세로 이동',
    targetUrl: '/team-board',
    isRead: false,
  },
  {
    idx: 78354,
    type: 'ai',
    severity: 'normal',
    created_at: new Date(Date.now() - 1000 * 60 * 44).toISOString(),
    title: 'AI 리스크 분석이 완료되었습니다',
    message: '마감 임박 업무 2건에서 일정 지연 가능성이 감지되었습니다.',
    detail:
      'AI 분석 결과, 콘텐츠 제작 일정과 검수 일정 사이의 여유 시간이 부족합니다. 담당자와 검수자를 확인하고 일정 조정 여부를 검토해 주세요.',
    source: 'AI 분석',
    targetLabel: '대시보드 확인',
    targetUrl: '/dashboard',
    isRead: false,
  },
  {
    idx: 54876,
    type: 'task',
    severity: 'normal',
    created_at: new Date(Date.now() - 1000 * 60 * 60 * 3).toISOString(),
    title: '새 업무가 배정되었습니다',
    message: '브랜드 가이드 초안 작성 업무가 담당자로 배정되었습니다.',
    detail:
      '업무 생성자가 브랜드 가이드 초안 작성 업무를 배정했습니다. 세부 요구사항과 마감일을 확인한 뒤 진행 상태를 업데이트해 주세요.',
    source: 'PM 매니저',
    targetLabel: '업무 보드로 이동',
    targetUrl: '/team-board',
    isRead: true,
  },
  {
    idx: 45453,
    type: 'campaign',
    severity: 'low',
    created_at: new Date(Date.now() - 1000 * 60 * 60 * 7).toISOString(),
    title: '캠페인 구성원이 추가되었습니다',
    message: 'CALL-LAUNCH 캠페인에 신규 협력사 매니저가 추가되었습니다.',
    detail:
      '캠페인 PM 매니저가 신규 협력사 매니저를 캠페인에 추가했습니다. 역할과 담당 범위를 확인해 주세요.',
    source: '캠페인 PM',
    targetLabel: '캠페인 보관함',
    targetUrl: '/campaign-folder',
    isRead: true,
  },
  {
    idx: 97531,
    type: 'deadline',
    severity: 'critical',
    created_at: new Date(Date.now() - 1000 * 60 * 60 * 26).toISOString(),
    title: '마감 24시간 전 알림',
    message: 'SNS 소재 검수 업무의 마감이 24시간 이내로 다가왔습니다.',
    detail:
      '마감 임박 업무입니다. 담당자와 관리자 모두에게 전달되는 중요 알림이며, 업무 상태와 산출물 업로드 여부를 확인해야 합니다.',
    source: '시스템',
    targetLabel: '캘린더 확인',
    targetUrl: '/calendar',
    isRead: false,
  },
]

const filterOptions = [
  { key: 'all', label: '전체' },
  { key: 'unread', label: '미확인' },
  { key: 'task', label: '업무' },
  { key: 'qa', label: 'QA' },
  { key: 'schedule', label: '캠페인/일정' },
  { key: 'ai', label: 'AI' },
]

const detailTabs = [
  { key: 'content', label: '내용' },
  { key: 'metadata', label: '상세 정보' },
  { key: 'link', label: '연결 정보' },
]

const categoryMeta = {
  ai: { label: 'AI', icon: 'auto_awesome' },
  qa: { label: 'QA', icon: 'verified' },
  schedule: { label: '캠페인/일정', icon: 'event_note' },
  system: { label: '시스템', icon: 'info' },
  task: { label: '업무', icon: 'assignment' },
}

const severityMeta = {
  critical: { label: '긴급', icon: 'priority_high' },
  high: { label: '중요', icon: 'error' },
  low: { label: '낮음', icon: 'low_priority' },
  normal: { label: '기본', icon: 'notifications' },
}

const notifications = ref([])
const activeFilter = ref('all')
const activeDetailTab = ref('content')
const selectedNotificationId = ref('')
const isLoading = ref(false)
const loadError = ref('')

const unreadCount = computed(() => notifications.value.filter((item) => !item.isRead).length)
const importantCount = computed(
  () => notifications.value.filter((item) => ['critical', 'high'].includes(item.severity)).length,
)
const todayCount = computed(
  () => notifications.value.filter((item) => isToday(item.createdAt)).length,
)

const statItems = computed(() => [
  { key: 'total', label: '전체 알림', value: notifications.value.length },
  { key: 'unread', label: '미확인', value: unreadCount.value },
  { key: 'important', label: '중요', value: importantCount.value },
  { key: 'today', label: '오늘 도착', value: todayCount.value },
])

const filteredNotifications = computed(() => {
  if (activeFilter.value === 'all') {
    return notifications.value
  }

  if (activeFilter.value === 'unread') {
    return notifications.value.filter((item) => !item.isRead)
  }

  return notifications.value.filter((item) => item.category === activeFilter.value)
})

const selectedNotification = computed(
  () =>
    notifications.value.find((item) => item.id === selectedNotificationId.value) ??
    filteredNotifications.value[0] ??
    notifications.value[0] ??
    null,
)

function getFilterCount(key) {
  if (key === 'all') {
    return notifications.value.length
  }

  if (key === 'unread') {
    return unreadCount.value
  }

  return notifications.value.filter((item) => item.category === key).length
}

function extractNotificationList(response) {
  const payload = response?.data ?? response
  const candidates = [
    payload?.data,
    payload?.result,
    payload?.notifications,
    payload?.items,
    payload,
  ]

  return candidates.find((candidate) => Array.isArray(candidate)) ?? []
}

function normalizeCategory(type) {
  const value = String(type || '').toLowerCase()

  if (value.includes('ai')) {
    return 'ai'
  }

  if (value.includes('qa') || value.includes('review') || value.includes('검수')) {
    return 'qa'
  }

  if (
    value.includes('campaign') ||
    value.includes('calendar') ||
    value.includes('schedule') ||
    value.includes('deadline') ||
    value.includes('캠페인') ||
    value.includes('일정') ||
    value.includes('마감')
  ) {
    return 'schedule'
  }

  if (value.includes('task') || value.includes('work') || value.includes('업무')) {
    return 'task'
  }

  return 'system'
}

function normalizeSeverity(value, category) {
  const normalizedValue = String(value || '').toLowerCase()

  if (['critical', 'urgent', '긴급'].includes(normalizedValue)) {
    return 'critical'
  }

  if (['high', 'important', 'warning', '중요'].includes(normalizedValue)) {
    return 'high'
  }

  if (['low', '낮음'].includes(normalizedValue)) {
    return 'low'
  }

  return category === 'schedule' ? 'high' : 'normal'
}

function normalizeDate(item) {
  const value = item.createdAt ?? item.created_at ?? item.createDate ?? item.time
  const date = new Date(value)

  return Number.isNaN(date.getTime()) ? new Date().toISOString() : date.toISOString()
}

function normalizeNotification(item, index) {
  const category = normalizeCategory(item.type ?? item.category)
  const severity = normalizeSeverity(item.severity ?? item.priority, category)
  const id = String(item.id ?? item.idx ?? `${category}-${index}`)
  const createdAt = normalizeDate(item)

  return {
    id,
    idx: item.idx ?? item.id ?? null,
    type: item.type ?? category,
    category,
    severity,
    title: item.title ?? '알림 제목 없음',
    message: item.message ?? item.summary ?? item.content ?? '알림 요약 정보가 없습니다.',
    detail:
      item.detail ??
      item.description ??
      item.content ??
      item.message ??
      '아직 상세 정보가 연결되지 않은 알림입니다.',
    createdAt,
    isRead: Boolean(item.isRead ?? item.read ?? item.confirmed),
    source: item.source ?? item.sender ?? '시스템',
    targetLabel: item.targetLabel ?? item.linkLabel ?? '연결 대상 없음',
    targetUrl: item.targetUrl ?? item.url ?? item.link ?? '',
  }
}

function applyNotifications(rawItems) {
  notifications.value = rawItems.map((item, index) => normalizeNotification(item, index))
  selectedNotificationId.value = notifications.value[0]?.id ?? ''
}

async function loadNotifications() {
  isLoading.value = true
  loadError.value = ''

  try {
    const response = await getNoti()
    applyNotifications(extractNotificationList(response))
  } catch (error) {
    console.warn('Notifications load failed. Fallback data will be used.', error)
    loadError.value = '알림 API 연결 전까지 예시 알림을 표시합니다.'
    applyNotifications(fallbackNotifications)
  } finally {
    isLoading.value = false
  }
}

async function markAsRead(notification) {
  if (!notification || notification.isRead) {
    return
  }

  notification.isRead = true

  if (!notification.idx) {
    return
  }

  try {
    await confirm(notification.idx)
  } catch (error) {
    console.warn('Notification confirm failed.', error)
  }
}

async function selectNotification(notification) {
  selectedNotificationId.value = notification.id
  activeDetailTab.value = 'content'
  await markAsRead(notification)
}

async function markAllAsRead() {
  const unreadItems = notifications.value.filter((item) => !item.isRead)

  unreadItems.forEach((item) => {
    item.isRead = true
  })

  await Promise.allSettled(
    unreadItems
      .filter((item) => item.idx)
      .map((item) =>
        confirm(item.idx).catch((error) => console.warn('Notification confirm failed.', error)),
      ),
  )
}

function isToday(value) {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return false
  }

  return date.toDateString() === new Date().toDateString()
}

function getCategoryMeta(category) {
  return categoryMeta[category] ?? categoryMeta.system
}

function getSeverityMeta(severity) {
  return severityMeta[severity] ?? severityMeta.normal
}

watch(filteredNotifications, (items) => {
  if (!items.length) {
    selectedNotificationId.value = ''
    return
  }

  if (!items.some((item) => item.id === selectedNotificationId.value)) {
    selectedNotificationId.value = items[0].id
  }
})

onMounted(() => {
  loadNotifications()
})
</script>

<template>
  <section class="notification-page ui-page">
    <header class="notification-hero ui-card">
      <div>
        <p class="notification-eyebrow">NOTI CENTER</p>
        <h2>알림 센터</h2>
        <p>시스템에서 발생한 업무, QA, 캠페인, AI 알림을 한곳에서 확인합니다.</p>
      </div>
      <button
        type="button"
        class="notification-button notification-button--primary"
        :disabled="!unreadCount"
        @click="markAllAsRead"
      >
        모두 읽음 처리
      </button>
    </header>

    <section class="notification-stats" aria-label="알림 현황">
      <article v-for="item in statItems" :key="item.key" class="notification-stat ui-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="notification-toolbar ui-card">
      <div class="notification-filters" role="tablist" aria-label="알림 필터">
        <button
          v-for="filter in filterOptions"
          :key="filter.key"
          type="button"
          :class="{ 'is-active': activeFilter === filter.key }"
          @click="activeFilter = filter.key"
        >
          {{ filter.label }}
          <span>{{ getFilterCount(filter.key) }}</span>
        </button>
      </div>
      <p v-if="loadError" class="notification-load-message">{{ loadError }}</p>
      <p v-else-if="isLoading" class="notification-load-message">알림을 불러오는 중입니다.</p>
    </section>

    <div class="notification-center">
      <section class="notification-list-panel ui-card" aria-label="알림 목록">
        <div class="notification-panel-head">
          <div>
            <strong>알림 요약</strong>
            <p>{{ filteredNotifications.length }}개의 알림이 표시됩니다.</p>
          </div>
        </div>

        <TransitionGroup name="notification-list" tag="div" class="notification-list">
          <article
            v-for="item in filteredNotifications"
            :key="item.id"
            class="notification-item"
            :class="{
              'is-selected': selectedNotification?.id === item.id,
              'is-read': item.isRead,
            }"
            :data-category="item.category"
            tabindex="0"
            @click="selectNotification(item)"
            @keyup.enter="selectNotification(item)"
          >
            <span class="notification-item__bar" />
            <div class="notification-item__icon">
              <span class="material-symbols-outlined">
                {{ getCategoryMeta(item.category).icon }}
              </span>
            </div>
            <div class="notification-item__body">
              <div class="notification-item__top">
                <span class="notification-chip">{{ getCategoryMeta(item.category).label }}</span>
                <span class="notification-time">{{ formatRelativeTime(item.createdAt) }}</span>
              </div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.message }}</p>
              <div class="notification-item__foot">
                <span
                  class="notification-severity"
                  :class="`notification-severity--${item.severity}`"
                >
                  {{ getSeverityMeta(item.severity).label }}
                </span>
                <button type="button" @click.stop="selectNotification(item)">자세히 보기</button>
              </div>
            </div>
            <span v-if="!item.isRead" class="notification-unread-dot" aria-label="미확인" />
          </article>
        </TransitionGroup>

        <div v-if="!filteredNotifications.length" class="notification-empty">
          <span class="material-symbols-outlined">notifications_off</span>
          <strong>표시할 알림이 없습니다.</strong>
          <p>다른 필터를 선택하거나 새 알림이 도착하면 이곳에 표시됩니다.</p>
        </div>
      </section>

      <aside class="notification-detail ui-card" aria-label="알림 상세 정보">
        <template v-if="selectedNotification">
          <div class="notification-detail__head">
            <span class="notification-detail__icon" :data-category="selectedNotification.category">
              <span class="material-symbols-outlined">
                {{ getCategoryMeta(selectedNotification.category).icon }}
              </span>
            </span>
            <div>
              <p class="notification-eyebrow">
                {{ getCategoryMeta(selectedNotification.category).label }}
              </p>
              <h3>{{ selectedNotification.title }}</h3>
              <span>{{ formatRelativeTime(selectedNotification.createdAt) }}</span>
            </div>
          </div>

          <div class="notification-detail-tabs" role="tablist" aria-label="알림 상세 탭">
            <button
              v-for="tab in detailTabs"
              :key="tab.key"
              type="button"
              :class="{ 'is-active': activeDetailTab === tab.key }"
              @click="activeDetailTab = tab.key"
            >
              {{ tab.label }}
            </button>
          </div>

          <section v-if="activeDetailTab === 'content'" class="notification-detail__section">
            <strong>알림 내용</strong>
            <p>{{ selectedNotification.message }}</p>
            <div class="notification-detail__box">
              {{ selectedNotification.detail }}
            </div>
          </section>

          <section v-else-if="activeDetailTab === 'metadata'" class="notification-detail__section">
            <strong>상세 정보</strong>
            <dl class="notification-meta-list">
              <div>
                <dt>유형</dt>
                <dd>{{ getCategoryMeta(selectedNotification.category).label }}</dd>
              </div>
              <div>
                <dt>중요도</dt>
                <dd>
                  <span
                    class="notification-severity"
                    :class="`notification-severity--${selectedNotification.severity}`"
                  >
                    {{ getSeverityMeta(selectedNotification.severity).label }}
                  </span>
                </dd>
              </div>
              <div>
                <dt>읽음 상태</dt>
                <dd>{{ selectedNotification.isRead ? '읽음' : '미확인' }}</dd>
              </div>
              <div>
                <dt>발생 시각</dt>
                <dd>{{ new Date(selectedNotification.createdAt).toLocaleString('ko-KR') }}</dd>
              </div>
              <div>
                <dt>발신 주체</dt>
                <dd>{{ selectedNotification.source }}</dd>
              </div>
            </dl>
          </section>

          <section v-else class="notification-detail__section">
            <strong>연결 정보</strong>
            <div v-if="selectedNotification.targetUrl" class="notification-detail__box">
              <p>{{ selectedNotification.targetLabel }}</p>
              <RouterLink :to="selectedNotification.targetUrl" class="notification-link-button">
                관련 화면으로 이동
              </RouterLink>
            </div>
            <div v-else class="notification-detail__box">
              연결된 업무, 캠페인, 검수 화면이 아직 없습니다.
            </div>
          </section>
        </template>

        <div v-else class="notification-empty notification-empty--detail">
          <span class="material-symbols-outlined">inbox</span>
          <strong>알림을 선택해 주세요.</strong>
          <p>왼쪽 목록에서 알림을 선택하면 상세 정보가 표시됩니다.</p>
        </div>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.notification-page {
  display: grid;
  gap: 16px;
  min-height: 100%;
}

.notification-hero,
.notification-toolbar,
.notification-list-panel,
.notification-detail,
.notification-stat {
  border: 1px solid var(--line-soft);
  background: var(--surface-card);
}

.notification-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px;
}

.notification-eyebrow {
  margin: 0;
  color: var(--accent-color);
  font-size: 12px;
  font-weight: 800;
}

.notification-hero h2,
.notification-detail__head h3 {
  margin: 4px 0 0;
  color: var(--text-heading);
  font-weight: 900;
}

.notification-hero h2 {
  font-size: 24px;
}

.notification-hero p:not(.notification-eyebrow),
.notification-panel-head p,
.notification-load-message,
.notification-empty p,
.notification-detail__head span,
.notification-detail__section p {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13px;
}

.notification-button,
.notification-link-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  border-radius: var(--radius-sm);
  padding: 0 14px;
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
  cursor: pointer;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.notification-button--primary,
.notification-link-button {
  border: 1px solid var(--accent-strong);
  background: var(--accent-strong);
  color: #ffffff;
}

.notification-button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.notification-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.notification-stat {
  display: grid;
  gap: 8px;
  min-height: 86px;
  padding: 16px;
}

.notification-stat span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.notification-stat strong {
  color: var(--text-heading);
  font-size: 26px;
  font-weight: 900;
}

.notification-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
}

.notification-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.notification-filters button {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  min-height: 34px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-sm);
  background: var(--surface-control);
  padding: 0 12px;
  color: var(--text-body);
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.notification-filters button.is-active {
  border-color: var(--accent-strong);
  background: var(--accent-strong);
  color: #ffffff;
}

.notification-filters span {
  opacity: 0.72;
}

.notification-center {
  display: grid;
  grid-template-columns: minmax(360px, 0.95fr) minmax(360px, 1.05fr);
  gap: 16px;
  align-items: start;
}

.notification-list-panel,
.notification-detail {
  overflow: hidden;
}

.notification-panel-head {
  padding: 16px 18px;
  border-bottom: 1px solid var(--line-soft);
}

.notification-panel-head strong,
.notification-detail__section strong,
.notification-empty strong {
  color: var(--text-heading);
  font-size: 15px;
  font-weight: 900;
}

.notification-list {
  display: grid;
  gap: 10px;
  padding: 12px;
}

.notification-item {
  position: relative;
  display: grid;
  grid-template-columns: 6px 38px minmax(0, 1fr) auto;
  gap: 12px;
  align-items: start;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card-muted);
  padding: 12px;
  cursor: pointer;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast),
    opacity var(--transition-fast);
}

.notification-item:hover,
.notification-item.is-selected {
  border-color: color-mix(in srgb, var(--noti-tone, var(--accent-color)) 45%, var(--line-soft));
  background: color-mix(in srgb, var(--noti-tone, var(--accent-color)) 9%, var(--surface-card));
}

.notification-item.is-selected {
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--noti-tone, var(--accent-color)) 30%, transparent);
}

.notification-item.is-read {
  opacity: 0.72;
}

.notification-item[data-category='ai'],
.notification-detail__icon[data-category='ai'] {
  --noti-tone: #8b5cf6;
}

.notification-item[data-category='qa'],
.notification-detail__icon[data-category='qa'] {
  --noti-tone: #0ea5e9;
}

.notification-item[data-category='schedule'],
.notification-detail__icon[data-category='schedule'] {
  --noti-tone: #f59e0b;
}

.notification-item[data-category='task'],
.notification-detail__icon[data-category='task'] {
  --noti-tone: #22c55e;
}

.notification-item[data-category='system'],
.notification-detail__icon[data-category='system'] {
  --noti-tone: var(--accent-color);
}

.notification-item__bar {
  width: 6px;
  height: 100%;
  min-height: 92px;
  border-radius: 999px;
  background: var(--noti-tone, var(--accent-color));
}

.notification-item__icon,
.notification-detail__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid color-mix(in srgb, var(--noti-tone, var(--accent-color)) 28%, var(--line-soft));
  background: color-mix(in srgb, var(--noti-tone, var(--accent-color)) 12%, var(--surface-control));
  color: var(--noti-tone, var(--accent-color));
}

.notification-item__icon {
  width: 38px;
  height: 38px;
  border-radius: var(--radius-sm);
}

.notification-item__body {
  display: grid;
  min-width: 0;
  gap: 8px;
}

.notification-item__top,
.notification-item__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.notification-chip,
.notification-severity {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  border-radius: 999px;
  padding: 0 8px;
  font-size: 11px;
  font-weight: 900;
}

.notification-chip {
  background: color-mix(in srgb, var(--noti-tone, var(--accent-color)) 13%, var(--surface-control));
  color: var(--text-heading);
}

.notification-time {
  color: var(--text-muted);
  font-size: 11px;
  font-weight: 800;
}

.notification-item strong {
  overflow: hidden;
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notification-item p {
  display: -webkit-box;
  overflow: hidden;
  margin: 0;
  color: var(--text-muted);
  font-size: 13px;
  line-height: 1.5;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.notification-item__foot button {
  border: 0;
  background: transparent;
  color: var(--accent-color);
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

.notification-severity {
  border: 1px solid var(--line-soft);
  background: var(--surface-control);
  color: var(--text-muted);
}

.notification-severity--critical,
.notification-severity--high {
  border-color: color-mix(in srgb, var(--danger-color) 42%, var(--line-soft));
  background: var(--danger-surface);
  color: var(--danger-text-strong);
}

.notification-severity--low {
  color: var(--text-muted);
}

.notification-unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  background: var(--accent-color);
}

.notification-detail {
  position: sticky;
  top: 86px;
}

.notification-detail__head {
  display: flex;
  gap: 14px;
  padding: 18px;
  border-bottom: 1px solid var(--line-soft);
}

.notification-detail__icon {
  width: 46px;
  height: 46px;
  flex: 0 0 auto;
  border-radius: var(--radius-md);
}

.notification-detail__head h3 {
  font-size: 19px;
}

.notification-detail-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-bottom: 1px solid var(--line-soft);
  background: var(--surface-control);
}

.notification-detail-tabs button {
  min-height: 42px;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 900;
  cursor: pointer;
}

.notification-detail-tabs button.is-active {
  background: var(--surface-card);
  color: var(--text-heading);
  box-shadow: inset 0 -2px 0 var(--accent-color);
}

.notification-detail__section {
  display: grid;
  gap: 14px;
  padding: 18px;
}

.notification-detail__box {
  display: grid;
  gap: 12px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card-muted);
  padding: 14px;
  color: var(--text-body);
  font-size: 13px;
  line-height: 1.6;
}

.notification-meta-list {
  display: grid;
  overflow: hidden;
  margin: 0;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
}

.notification-meta-list div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  min-height: 48px;
  padding: 0 14px;
  border-bottom: 1px solid var(--line-soft);
}

.notification-meta-list div:last-child {
  border-bottom: 0;
}

.notification-meta-list dt {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.notification-meta-list dd {
  margin: 0;
  color: var(--text-heading);
  font-size: 13px;
  font-weight: 800;
  text-align: right;
}

.notification-empty {
  display: grid;
  justify-items: center;
  gap: 8px;
  padding: 46px 20px;
  color: var(--text-muted);
  text-align: center;
}

.notification-empty .material-symbols-outlined {
  color: var(--text-muted);
  font-size: 34px;
}

.notification-empty--detail {
  min-height: 360px;
  align-content: center;
}

.notification-list-move,
.notification-list-enter-active,
.notification-list-leave-active {
  transition:
    opacity var(--transition-fast),
    transform var(--transition-fast);
}

.notification-list-enter-from,
.notification-list-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (max-width: 1100px) {
  .notification-center {
    grid-template-columns: 1fr;
  }

  .notification-detail {
    position: static;
  }
}

@media (max-width: 760px) {
  .notification-hero,
  .notification-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .notification-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .notification-item {
    grid-template-columns: 5px 34px minmax(0, 1fr);
  }

  .notification-unread-dot {
    position: absolute;
    top: 14px;
    right: 14px;
  }

  .notification-item__top,
  .notification-item__foot,
  .notification-meta-list div {
    align-items: flex-start;
    flex-direction: column;
  }

  .notification-detail-tabs {
    grid-template-columns: 1fr;
  }
}
</style>
