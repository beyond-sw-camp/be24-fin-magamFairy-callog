<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/useAuthStore'
import { useNotificationsStore } from '@/stores/notifications'
import { formatRelativeTime } from '@/utils/datechange.js'
import NotificationSettingsPanel from '@/components/notifications/NotificationSettingsPanel.vue'
import { acceptCampaignInvitation, rejectCampaignInvitation } from '@/api/campaignMembers'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const notificationStore = useNotificationsStore()

const activeFilter = ref('all')
const activeDetailTab = ref('content')
const selectedNotificationId = ref('')
const isSettingsModalOpen = ref(false)
const invitationActionError = ref('')
const invitationActionLoading = ref('')

const filterOptions = [
  { key: 'all', label: '전체' },
  { key: 'unread', label: '미확인' },
  { key: 'task', label: '업무' },
  { key: 'qa', label: 'QA' },
  { key: 'campaign', label: '캠페인' },
  { key: 'schedule', label: '일정' },
  { key: 'system', label: '시스템' },
]

const detailTabs = [
  { key: 'content', label: '내용' },
  { key: 'metadata', label: '상세 정보' },
  { key: 'link', label: '연결 정보' },
]

const categoryMeta = {
  campaign: { label: '캠페인', icon: 'groups', tone: 'campaign' },
  qa: { label: 'QA', icon: 'fact_check', tone: 'qa' },
  schedule: { label: '일정', icon: 'event_note', tone: 'schedule' },
  system: { label: '시스템', icon: 'info', tone: 'system' },
  task: { label: '업무', icon: 'assignment', tone: 'task' },
}

const severityMeta = {
  critical: { label: '긴급' },
  high: { label: '중요' },
  low: { label: '낮음' },
  normal: { label: '기본' },
}

const notifications = computed(() => notificationStore.notifications)
const unreadCount = computed(() => notificationStore.unreadCount)
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
  { key: 'today', label: '오늘', value: todayCount.value },
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

function getCategoryMeta(category) {
  return categoryMeta[category] ?? categoryMeta.system
}

function getSeverityMeta(severity) {
  return severityMeta[severity] ?? severityMeta.normal
}

function getFilterCount(key) {
  if (key === 'all') {
    return notifications.value.length
  }

  if (key === 'unread') {
    return unreadCount.value
  }

  return notifications.value.filter((item) => item.category === key).length
}

function isToday(value) {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return false
  }

  return date.toDateString() === new Date().toDateString()
}

function formatDate(value) {
  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return '-'
  }

  return date.toLocaleString('ko-KR')
}

async function selectNotification(notification) {
  if (!notification) {
    return
  }

  selectedNotificationId.value = notification.id
  activeDetailTab.value = 'content'
  groupPreviewExpanded.value = false

  if (route.query.notificationId !== notification.id) {
    await router.replace({
      query: {
        ...route.query,
        notificationId: notification.id,
      },
    })
  }

  await notificationStore.markAsRead(notification)
}

async function markAllAsRead() {
  await notificationStore.markAllAsRead()
}

function openNotificationSettings() {
  isSettingsModalOpen.value = true
}

function closeNotificationSettings() {
  isSettingsModalOpen.value = false
}

function openTarget(notification) {
  if (!notification?.targetUrl) {
    return
  }

  router.push(notification.targetUrl)
}

function isCampaignInvitationActionable(notification) {
  return (
    notification?.referenceType === 'CAMPAIGN_INVITATION' &&
    notification?.referenceId &&
    notification?.referenceStatus === 'PENDING'
  )
}

function getCampaignIdFromTargetUrl(notification) {
  const [, campaignId = ''] = String(notification?.targetUrl ?? '').match(/^\/campaigns\/([^/?#]+)/) ?? []
  return campaignId
}

const groupPreviewExpanded = ref(false)

function isGroupCampaignInvitation(notification) {
  return (
    isCampaignInvitationActionable(notification)
    && Boolean(notification?.groupPreview)
  )
}

function groupPreviewSummary(notification) {
  const preview = notification?.groupPreview
  if (!preview) return ''
  return `그룹 초대 — 수락 시 ${preview.organizationName} 활성 인원 ${preview.members?.length ?? 0}명이 함께 캠페인에 합류합니다.`
}

function confirmGroupAccept(notification) {
  const preview = notification?.groupPreview
  const orgName = preview?.organizationName ?? '협력사'
  const count = preview?.members?.length ?? 0
  return window.confirm(`${orgName} ${count}명이 캠페인에 합류합니다. 진행할까요?`)
}

function confirmGroupReject() {
  return window.confirm('이 그룹 초대를 거절합니다. 같은 조직 인원도 합류하지 않습니다. 계속할까요?')
}

async function respondCampaignInvitation(notification, action) {
  if (!isCampaignInvitationActionable(notification)) {
    return
  }

  const campaignId = getCampaignIdFromTargetUrl(notification)
  if (!campaignId) {
    invitationActionError.value = '캠페인 정보를 찾지 못했습니다.'
    return
  }

  if (isGroupCampaignInvitation(notification)) {
    if (action === 'accept' && !confirmGroupAccept(notification)) return
    if (action === 'reject' && !confirmGroupReject()) return
  }

  invitationActionLoading.value = action
  invitationActionError.value = ''

  try {
    if (action === 'accept') {
      const res = await acceptCampaignInvitation(campaignId, notification.referenceId)
      const joined = res?.data?.data?.joinedCount ?? 1
      const isGroup = res?.data?.data?.type === 'GROUP'
      window.alert(isGroup ? `그룹 초대를 수락했습니다. (${joined}명 합류)` : '초대를 수락했습니다.')
    } else {
      await rejectCampaignInvitation(campaignId, notification.referenceId)
    }
    await notificationStore.loadNotifications({ count: 100 })
  } catch (error) {
    console.warn('Campaign invitation action failed.', error)
    invitationActionError.value = '캠페인 초대 처리에 실패했습니다.'
  } finally {
    invitationActionLoading.value = ''
  }
}

watch(
  () => route.query.notificationId,
  (notificationId) => {
    if (typeof notificationId === 'string') {
      selectedNotificationId.value = notificationId
    }
  },
  { immediate: true },
)

watch(filteredNotifications, (items) => {
  if (!items.length) {
    selectedNotificationId.value = ''
    return
  }

  if (!items.some((item) => item.id === selectedNotificationId.value)) {
    selectedNotificationId.value = items[0].id
  }
})

watch(
  () => [authStore.isAuthenticated, authStore.token],
  ([isAuthenticated, accessToken]) => {
    if (isAuthenticated && accessToken) {
      notificationStore.connect(accessToken)
      return
    }

    notificationStore.disconnect()
  },
  { immediate: true },
)

onMounted(() => {
  void notificationStore.loadNotifications({ count: 100 })
})

</script>

<template>
  <section class="notification-page ui-page">
    <header class="notification-hero">
      <div>
        <p class="notification-eyebrow">NOTIFICATION CENTER</p>
        <h2>알림 센터</h2>
        <p>업무, QA, 캠페인, 일정 알림을 한 곳에서 확인하고 읽음 상태를 관리합니다.</p>
      </div>
      <div class="notification-hero__actions">
        <button
          type="button"
          class="notification-button notification-button--secondary"
          @click="openNotificationSettings"
        >
          <span class="material-symbols-outlined">tune</span>
          알림 설정
        </button>
        <button
          type="button"
          class="notification-button notification-button--primary"
          :disabled="!unreadCount"
          @click="markAllAsRead"
        >
          모두 읽음 처리
        </button>
      </div>
    </header>

    <section class="notification-stats" aria-label="알림 현황">
      <article v-for="item in statItems" :key="item.key" class="notification-stat">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </section>

    <section class="notification-toolbar">
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
      <p v-if="notificationStore.loadError" class="notification-load-message">
        {{ notificationStore.loadError }}
      </p>
      <p v-else-if="notificationStore.isLoading" class="notification-load-message">
        알림을 불러오는 중입니다.
      </p>
    </section>

    <div class="notification-center">
      <section class="notification-list-panel" aria-label="알림 목록">
        <div class="notification-panel-head">
          <div>
            <strong>알림 요약</strong>
            <p>{{ filteredNotifications.length }}개의 알림을 표시합니다.</p>
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

      <aside class="notification-detail" aria-label="알림 상세 정보">
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
            <div
              v-if="isGroupCampaignInvitation(selectedNotification)"
              class="notification-group-banner"
            >
              <p>{{ groupPreviewSummary(selectedNotification) }}</p>
              <button type="button" class="notification-group-banner__toggle" @click="groupPreviewExpanded = !groupPreviewExpanded">
                {{ groupPreviewExpanded ? '미리보기 접기' : '합류 예정 인원 미리보기' }}
              </button>
              <div v-if="groupPreviewExpanded" class="notification-group-preview" role="table">
                <div class="notification-group-preview__head" role="row">
                  <span role="columnheader">이름</span>
                  <span role="columnheader">이메일</span>
                  <span role="columnheader">조직 내 역할</span>
                </div>
                <div
                  v-for="m in (selectedNotification.groupPreview?.members ?? [])"
                  :key="`${m.email}-${m.name}`"
                  class="notification-group-preview__row"
                  role="row"
                >
                  <span>{{ m.name }}</span>
                  <span>{{ m.email }}</span>
                  <span>{{ m.role }}</span>
                </div>
              </div>
            </div>
            <div
              v-if="isCampaignInvitationActionable(selectedNotification)"
              class="notification-invitation-actions"
            >
              <button
                type="button"
                class="notification-button notification-button--primary"
                :disabled="Boolean(invitationActionLoading)"
                @click="respondCampaignInvitation(selectedNotification, 'accept')"
              >
                {{ invitationActionLoading === 'accept' ? '승인 중' : '승인' }}
              </button>
              <button
                type="button"
                class="notification-button notification-button--secondary"
                :disabled="Boolean(invitationActionLoading)"
                @click="respondCampaignInvitation(selectedNotification, 'reject')"
              >
                {{ invitationActionLoading === 'reject' ? '반려 중' : '반려' }}
              </button>
            </div>
            <p v-if="invitationActionError" class="notification-load-message">
              {{ invitationActionError }}
            </p>
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
                <dd>{{ formatDate(selectedNotification.createdAt) }}</dd>
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
              <button type="button" class="notification-link-button" @click="openTarget(selectedNotification)">
                {{ selectedNotification.targetLabel || '관련 화면으로 이동' }}
              </button>
            </div>
            <div v-else class="notification-detail__box">
              연결된 업무, 캠페인, 검수 화면이 없습니다.
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

  <Teleport to="body">
    <Transition name="notification-settings-modal">
      <div
        v-if="isSettingsModalOpen"
        class="notification-settings-modal"
        role="presentation"
        @click.self="closeNotificationSettings"
      >
        <section
          class="notification-settings-dialog"
          role="dialog"
          aria-modal="true"
          aria-labelledby="notification-settings-title"
        >
          <header class="notification-settings-dialog__header">
            <div>
              <p class="notification-eyebrow">NOTIFICATION SETTINGS</p>
              <h3 id="notification-settings-title">알림 설정</h3>
              <span>알림 방법, 중요도, 수신 조건을 바로 조정합니다.</span>
            </div>
            <button
              type="button"
              class="notification-settings-dialog__close"
              aria-label="닫기"
              @click="closeNotificationSettings"
            >
              <span class="material-symbols-outlined">close</span>
            </button>
          </header>

          <div class="notification-settings-dialog__body">
            <NotificationSettingsPanel compact />
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
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
  border-radius: var(--radius-md);
  background: var(--surface-card);
}

.notification-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 22px;
}

.notification-hero__actions {
  display: flex;
  align-items: center;
  gap: 10px;
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

.notification-live {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-sm);
  background: var(--surface-control);
  padding: 0 12px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.notification-live span {
  width: 7px;
  height: 7px;
  border-radius: 999px;
  background: var(--text-muted);
}

.notification-live.is-connected span {
  background: var(--success-color, #16a34a);
}

.notification-button,
.notification-link-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
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

.notification-button .material-symbols-outlined {
  font-size: 18px;
}

.notification-button--primary,
.notification-link-button {
  border: 1px solid var(--accent-strong);
  background: var(--accent-strong);
  color: #ffffff;
}

.notification-button--secondary {
  border: 1px solid var(--line-soft);
  background: var(--surface-control);
  color: var(--text-body);
}

.notification-button:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.notification-invitation-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
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

.notification-item[data-category='campaign'],
.notification-detail__icon[data-category='campaign'] {
  --noti-tone: #6366f1;
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

.notification-group-banner { margin-top: 12px; padding: 12px 14px; border: 1px solid var(--border-color); border-radius: var(--radius-md); background: var(--panel-muted); display: flex; flex-direction: column; gap: 8px; }
.notification-group-banner p { margin: 0; font-size: 13px; color: var(--text-primary); }
.notification-group-banner__toggle { align-self: flex-start; padding: 4px 10px; font-size: 12px; border: 1px solid var(--border-color); border-radius: var(--radius-sm); background: var(--panel-color); cursor: pointer; }
.notification-group-preview { display: grid; gap: 4px; padding-top: 4px; }
.notification-group-preview__head, .notification-group-preview__row { display: grid; grid-template-columns: 1fr 1.4fr 0.8fr; gap: 8px; padding: 6px 0; font-size: 12px; }
.notification-group-preview__head { color: var(--muted-text); font-weight: 700; border-bottom: 1px dashed var(--border-color); }

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
  border: 0;
  background: transparent;
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

.notification-settings-modal {
  position: fixed;
  inset: 0;
  z-index: 2600;
  display: grid;
  place-items: center;
  overflow-y: auto;
  background: rgba(15, 23, 42, 0.58);
  padding: 24px;
}

.notification-settings-dialog {
  width: min(940px, 100%);
  max-height: min(860px, calc(100vh - 48px));
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-lg, 16px);
  background: var(--surface-page);
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.32);
}

.notification-settings-dialog__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid var(--line-soft);
  background: var(--surface-card);
  padding: 20px 22px;
}

.notification-settings-dialog__header h3 {
  margin: 4px 0 0;
  color: var(--text-heading);
  font-size: 20px;
  font-weight: 900;
}

.notification-settings-dialog__header span {
  display: block;
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
}

.notification-settings-dialog__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  flex: 0 0 auto;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-sm);
  background: var(--surface-control);
  color: var(--text-body);
  cursor: pointer;
}

.notification-settings-dialog__close .material-symbols-outlined {
  font-size: 19px;
}

.notification-settings-dialog__body {
  overflow-y: auto;
  padding: 18px;
}

.notification-settings-modal-enter-active,
.notification-settings-modal-leave-active {
  transition: opacity var(--transition-fast);
}

.notification-settings-modal-enter-active .notification-settings-dialog,
.notification-settings-modal-leave-active .notification-settings-dialog {
  transition: transform var(--transition-fast);
}

.notification-settings-modal-enter-from,
.notification-settings-modal-leave-to {
  opacity: 0;
}

.notification-settings-modal-enter-from .notification-settings-dialog,
.notification-settings-modal-leave-to .notification-settings-dialog {
  transform: translateY(10px);
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
  .notification-hero__actions,
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

  .notification-settings-modal {
    padding: 12px;
  }

  .notification-settings-dialog {
    max-height: calc(100vh - 24px);
  }

  .notification-settings-dialog__header {
    padding: 16px;
  }

  .notification-settings-dialog__body {
    padding: 14px;
  }
}
</style>
