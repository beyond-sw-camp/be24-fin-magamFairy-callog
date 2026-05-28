<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { usePlannerStore } from '@/stores/planner'
import { useAuthStore } from '@/stores/useAuthStore'
import { useUserSettingsStore } from '@/stores/userSettings'
import { useNotificationsStore } from '@/stores/notifications'
import { getMyProfile } from '@/api/userProfiles/index.js'
import { formatRelativeTime } from '@/utils/datechange.js'
import { acceptCampaignInvitation, rejectCampaignInvitation } from '@/api/campaignMembers'

const route = useRoute()
const router = useRouter()
const store = usePlannerStore()
const authStore = useAuthStore()
const userSettingsStore = useUserSettingsStore()
const notificationStore = useNotificationsStore()

const notifications = computed(() => notificationStore.recentNotifications)
const unreadCount = computed(() => notificationStore.unreadCount)
const notificationsOpen = ref(false)
const notificationsButton = ref(null)
const notificationBubble = ref(null)
const appsMenuOpen = ref(false)

const inlineActionLoading = reactive({})
const inlineActionError = ref('')
let notificationBubbleTimer = null

function isCampaignInvitationActionable(item) {
  return (
    item?.referenceType === 'CAMPAIGN_INVITATION'
    && item?.referenceId
    && item?.referenceStatus === 'PENDING'
  )
}

function isGroupCampaignInvitation(item) {
  return isCampaignInvitationActionable(item) && Boolean(item?.groupPreview)
}

function getCampaignIdFromTargetUrl(item) {
  const [, campaignId = ''] = String(item?.targetUrl ?? '').match(/^\/campaigns\/([^/?#]+)/) ?? []
  return campaignId
}

function confirmInlineGroupAccept(item) {
  const preview = item?.groupPreview
  const orgName = preview?.organizationName ?? '협력사'
  const count = preview?.members?.length ?? 0
  return window.confirm(`${orgName} ${count}명이 캠페인에 합류합니다. 진행할까요?`)
}

function confirmInlineGroupReject() {
  return window.confirm('이 그룹 초대를 거절합니다. 같은 조직 인원도 합류하지 않습니다. 계속할까요?')
}

async function respondInlineCampaignInvitation(item, action) {
  if (!isCampaignInvitationActionable(item)) return

  const campaignId = getCampaignIdFromTargetUrl(item)
  if (!campaignId) {
    inlineActionError.value = '캠페인 정보를 찾지 못했습니다.'
    return
  }

  if (isGroupCampaignInvitation(item)) {
    if (action === 'accept' && !confirmInlineGroupAccept(item)) return
    if (action === 'reject' && !confirmInlineGroupReject()) return
  }

  inlineActionLoading[item.id] = action
  inlineActionError.value = ''

  try {
    if (action === 'accept') {
      const res = await acceptCampaignInvitation(campaignId, item.referenceId)
      const joined = res?.data?.data?.joinedCount ?? 1
      const isGroup = res?.data?.data?.type === 'GROUP'
      window.alert(isGroup ? `그룹 초대를 수락했습니다. (${joined}명 합류)` : '초대를 수락했습니다.')
    } else {
      await rejectCampaignInvitation(campaignId, item.referenceId)
    }
    await notificationStore.loadNotifications({ count: 100 })
  } catch (error) {
    console.warn('Header inline invitation action failed.', error)
    inlineActionError.value = '캠페인 초대 처리에 실패했습니다.'
  } finally {
    delete inlineActionLoading[item.id]
  }
}
const appsMenuButton = ref(null)
const profileCardOpen = ref(false)
const profileCardButton = ref(null)

const APP_MENU_MARGIN = 12
const NOTIFICATION_MENU_WIDTH = 360
const NOTIFICATION_MENU_HEIGHT_ESTIMATE = 390
const NOTIFICATION_BUBBLE_WIDTH = 340
const NOTIFICATION_BUBBLE_DURATION_MS = 5600
const APP_MENU_WIDTH = 220
const APP_MENU_HEIGHT_ESTIMATE = 260
const PROFILE_CARD_WIDTH = 320
const PROFILE_CARD_HEIGHT_ESTIMATE = 350

const notificationsPosition = reactive({
  top: 0,
  left: 0,
})
const notificationBubblePosition = reactive({
  top: 0,
  left: 0,
})
const appsMenuPosition = reactive({
  top: 0,
  left: 0,
})
const profileCardPosition = reactive({
  top: 0,
  left: 0,
})

const pageRoutes = [
  { id: 'dashboard', to: '/dashboard', label: '메인', section: '통합 대시보드' },
  { id: 'calendar', to: '/calendar', label: '캘린더', section: '운영 플래너' },
  { id: 'tasks', to: '/tasks', label: '업무 보드', section: '실행 보드' },
  {
    id: 'operations',
    to: '/operations',
    label: '운영 허브',
    section: '고객 및 업무 오케스트레이션',
  },
  { id: 'templates', to: '/templates', label: '템플릿', section: '콘텐츠 라이브러리' },
  { id: 'reports', to: '/reports', label: '리포트', section: '성과 리뷰' },
  { id: 'references', to: '/references', label: '레퍼런스', section: '캠페인 레퍼런스' },
  { id: 'resources', to: '/resources', label: '자료실', section: '캠페인 자료실' },
  { id: 'review-approval', to: '/review-approval', label: '검수/승인', section: '검수 대기 · 승인 요청' },
]

const activeRoute = computed(
  () =>
    pageRoutes.find((item) => route.path === item.to || route.path.startsWith(`${item.to}/`)) ??
    pageRoutes[0],
)
const pageTitle = computed(() => route.meta?.title ?? activeRoute.value.label)
const sectionTitle = computed(() => route.meta?.section ?? activeRoute.value.section)
const userSettingsKey = computed(() => resolveUserSettingsKey(authStore.user))
const profileCard = computed(() => userSettingsStore.profileCardData)
const notificationsStyle = computed(() => ({
  top: `${notificationsPosition.top}px`,
  left: `${notificationsPosition.left}px`,
}))
const notificationBubbleStyle = computed(() => ({
  top: `${notificationBubblePosition.top}px`,
  left: `${notificationBubblePosition.left}px`,
}))
const notificationBubbleReviewMeta = computed(() =>
  notificationBubble.value ? getReviewOutcomeMeta(notificationBubble.value) : null,
)
const appsMenuStyle = computed(() => ({
  top: `${appsMenuPosition.top}px`,
  left: `${appsMenuPosition.left}px`,
}))
const profileCardStyle = computed(() => ({
  top: `${profileCardPosition.top}px`,
  left: `${profileCardPosition.left}px`,
}))

const appMenuItems = computed(() => [
  {
    key: 'provisioning',
    label: '사용자관리',
    kind: 'route',
    to: { name: 'user-provisioning' },
    creatorOnly: true,
    icon: `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20a5 5 0 00-10 0m10 0H7m10 0h3a2 2 0 002-2v-1a4 4 0 00-4-4h-1m-6 7H4a2 2 0 01-2-2v-1a4 4 0 014-4h1m0 0a4 4 0 100-8 4 4 0 000 8zm10-4a3 3 0 11-6 0 3 3 0 016 0z"/></svg>`,
  },
  {
    key: 'theme',
    label: store.theme === 'dark' ? '라이트모드' : '다크모드',
    kind: 'action',
    action: 'theme',
    active: store.theme === 'dark',
    icon:
      store.theme === 'dark'
        ? `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><circle cx="12" cy="12" r="4" stroke-width="2"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M6.34 17.66l-1.41 1.41M19.07 4.93l-1.41 1.41"/></svg>`
        : `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 3a6 6 0 0 0 9 9 9 9 0 1 1-9-9Z"/></svg>`,
  },
  {
    key: 'settings',
    label: '환경설정',
    kind: 'route',
    to: { name: 'settings' },
    icon: `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/></svg>`,
  },
  {
    key: 'logout',
    label: '로그아웃',
    kind: 'route',
    to: { name: 'login' },
    danger: true,
    icon: `<svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/></svg>`,
  },
])

const visibleAppMenuItems = computed(() =>
  appMenuItems.value.filter((item) => !item.creatorOnly || authStore.canCreateUsers),
)

function resolveUserSettingsKey(user) {
  return (
    user?.userId ??
    user?.idx ??
    user?.id ??
    user?.loginId ??
    user?.email ??
    user?.sub ??
    store.currentUserId ??
    'guest'
  )
}

function resolveProfilePayload(payload) {
  return payload?.result ?? payload?.data ?? payload ?? {}
}

function applyRemoteProfile(payload) {
  const source = resolveProfilePayload(payload)
  const nextProfile = {
    name: source.name,
    email: source.email,
    phone: source.phone,
  }

  if (Object.prototype.hasOwnProperty.call(source, 'profileImageUrl')) {
    nextProfile.imageDataUrl = source.profileImageUrl || ''
  }

  Object.keys(nextProfile).forEach((key) => {
    if (nextProfile[key] === undefined || nextProfile[key] === null) {
      delete nextProfile[key]
    }
  })

  userSettingsStore.updateProfile(nextProfile)
}

async function refreshProfileCard() {
  if (!authStore.isAuthenticated) {
    return
  }

  try {
    const response = await getMyProfile()
    applyRemoteProfile(response.data)
  } catch (error) {
    console.warn('Profile image refresh failed.', error)
  }
}

function handleProfileImageError() {
  userSettingsStore.updateProfile({ imageDataUrl: '' })
}

function closeFloatingMenus() {
  notificationsOpen.value = false
  appsMenuOpen.value = false
  profileCardOpen.value = false
}

function clearNotificationBubbleTimer() {
  if (notificationBubbleTimer) {
    window.clearTimeout(notificationBubbleTimer)
    notificationBubbleTimer = null
  }
}

function dismissNotificationBubble() {
  clearNotificationBubbleTimer()
  notificationBubble.value = null
}

function showNotificationBubble(item) {
  if (!shouldShowNotificationBubble(item) || notificationsOpen.value) {
    return
  }

  notificationBubble.value = item
  positionNotificationBubble()
  void nextTick(positionNotificationBubble)

  clearNotificationBubbleTimer()
  notificationBubbleTimer = window.setTimeout(() => {
    dismissNotificationBubble()
  }, NOTIFICATION_BUBBLE_DURATION_MS)
}

async function openNotificationBubbleDetail() {
  const item = notificationBubble.value
  if (!item) return

  dismissNotificationBubble()
  await handleNotificationDetail(item)
}

async function openNotificationBubbleTarget() {
  const item = notificationBubble.value
  if (!item) return

  dismissNotificationBubble()
  await handleNotificationTarget(item)
}

function toggleNotifications() {
  appsMenuOpen.value = false
  profileCardOpen.value = false
  dismissNotificationBubble()

  if (notificationsOpen.value) {
    notificationsOpen.value = false
    return
  }

  positionNotifications()
  notificationsOpen.value = true
  void nextTick(positionNotifications)
}

function clamp(value, min, max) {
  if (max < min) {
    return min
  }

  return Math.min(max, Math.max(min, value))
}

function resolveNotificationConditionKey(item) {
  const type = String(item?.type ?? '').toUpperCase()
  const category = String(item?.category ?? '').toLowerCase()

  if (type === 'TASK_ASSIGNED') return 'taskAssigned'
  if (['TASK_STATUS_CHANGED', 'TASK_UPDATED'].includes(type)) return 'taskStatusChanged'
  if (
    ['REVIEW_REQUESTED', 'REVIEW_APPROVED', 'REVIEW_REJECTED'].includes(type) ||
    type.startsWith('AI_JUDGE_') ||
    category === 'qa'
  ) {
    return 'qaReview'
  }
  if (type.startsWith('DEADLINE_')) return 'deadline'
  if (type.startsWith('CAMPAIGN_') || category === 'campaign') return 'campaign'
  if (category === 'task') return 'taskStatusChanged'
  if (category === 'schedule') return 'schedule'

  return ''
}

function isNotificationAllowedByLevel(item) {
  const severity = String(item?.severity ?? 'normal').toLowerCase()

  if (severity === 'critical') {
    return true
  }

  const level = String(userSettingsStore.notifications.level || 'normal').toLowerCase()

  if (level === 'essential') {
    return severity === 'high'
  }

  if (level === 'normal') {
    return severity !== 'low'
  }

  return true
}

function shouldShowNotificationBubble(item) {
  if (!item || item.isRead) {
    return false
  }

  const notificationSettings = userSettingsStore.notifications

  if (!notificationSettings.enabled || !notificationSettings.methods.inApp) {
    return false
  }

  if (String(item.severity ?? '').toLowerCase() === 'critical') {
    return true
  }

  const conditionKey = resolveNotificationConditionKey(item)

  if (
    conditionKey &&
    Object.prototype.hasOwnProperty.call(notificationSettings.conditions, conditionKey) &&
    !notificationSettings.conditions[conditionKey]
  ) {
    return false
  }

  return isNotificationAllowedByLevel(item)
}

function positionNotifications() {
  const button = notificationsButton.value

  if (!(button instanceof HTMLElement)) {
    return
  }

  const rect = button.getBoundingClientRect()
  const maxLeft = window.innerWidth - NOTIFICATION_MENU_WIDTH - APP_MENU_MARGIN
  const maxTop = window.innerHeight - NOTIFICATION_MENU_HEIGHT_ESTIMATE - APP_MENU_MARGIN

  notificationsPosition.left = clamp(rect.right - NOTIFICATION_MENU_WIDTH, APP_MENU_MARGIN, maxLeft)
  notificationsPosition.top = clamp(rect.bottom + 8, APP_MENU_MARGIN, maxTop)
}

function positionNotificationBubble() {
  const button = notificationsButton.value
  const maxLeft = window.innerWidth - NOTIFICATION_BUBBLE_WIDTH - APP_MENU_MARGIN

  if (!(button instanceof HTMLElement)) {
    notificationBubblePosition.left = clamp(
      window.innerWidth - NOTIFICATION_BUBBLE_WIDTH - APP_MENU_MARGIN,
      APP_MENU_MARGIN,
      maxLeft,
    )
    notificationBubblePosition.top = APP_MENU_MARGIN
    return
  }

  const rect = button.getBoundingClientRect()

  notificationBubblePosition.left = clamp(rect.right - NOTIFICATION_BUBBLE_WIDTH, APP_MENU_MARGIN, maxLeft)
  notificationBubblePosition.top = rect.bottom + 10
}

function positionAppsMenu() {
  const button = appsMenuButton.value

  if (!(button instanceof HTMLElement)) {
    return
  }

  const rect = button.getBoundingClientRect()
  const maxLeft = window.innerWidth - APP_MENU_WIDTH - APP_MENU_MARGIN
  const maxTop = window.innerHeight - APP_MENU_HEIGHT_ESTIMATE - APP_MENU_MARGIN

  appsMenuPosition.left = clamp(rect.right - APP_MENU_WIDTH, APP_MENU_MARGIN, maxLeft)
  appsMenuPosition.top = clamp(rect.bottom + 8, APP_MENU_MARGIN, maxTop)
}

function positionProfileCard() {
  const button = profileCardButton.value

  if (!(button instanceof HTMLElement)) {
    return
  }

  const rect = button.getBoundingClientRect()
  const maxLeft = window.innerWidth - PROFILE_CARD_WIDTH - APP_MENU_MARGIN
  const maxTop = window.innerHeight - PROFILE_CARD_HEIGHT_ESTIMATE - APP_MENU_MARGIN

  profileCardPosition.left = clamp(rect.right - PROFILE_CARD_WIDTH, APP_MENU_MARGIN, maxLeft)
  profileCardPosition.top = clamp(rect.bottom + 8, APP_MENU_MARGIN, maxTop)
}

function toggleAppsMenu() {
  notificationsOpen.value = false
  profileCardOpen.value = false

  if (appsMenuOpen.value) {
    appsMenuOpen.value = false
    return
  }

  positionAppsMenu()
  appsMenuOpen.value = true
  void nextTick(positionAppsMenu)
}

function toggleProfileCard() {
  notificationsOpen.value = false
  appsMenuOpen.value = false

  if (profileCardOpen.value) {
    profileCardOpen.value = false
    return
  }

  positionProfileCard()
  profileCardOpen.value = true
  void nextTick(positionProfileCard)
}

function handleSearchInput(event) {
  store.setSearchQuery(event.target.value)
}

async function handleAppMenuItem(item) {
  closeFloatingMenus()

  if (item.key === 'logout') {
    await authStore.logout()
    router.push({ name: 'login' })
    return
  }

  if (item.kind === 'action') {
    if (item.action === 'theme') {
      store.toggleTheme()
      return
    }
    return
  }

  router.push(item.to)
}

function handleProfileEdit() {
  closeFloatingMenus()
  router.push({
    name: 'settings',
    query: {
      tab: 'profile',
    },
  })
}

function handleProfileDownload() {
  void userSettingsStore.downloadProfileCard()
}

async function handleNotificationDetail(item) {
  await notificationStore.markAsRead(item)
  closeFloatingMenus()
  router.push({
    name: 'notifications',
    query: {
      notificationId: item.id,
    },
  })
}

function getReviewOutcomeMeta(item) {
  return notificationStore.getReviewOutcomeMeta(item?.reviewOutcome)
}

async function handleNotificationTarget(item) {
  const targetUrl = await notificationStore.resolveNotificationTargetUrl(item)
  if (!targetUrl) {
    await handleNotificationDetail(item)
    return
  }

  await notificationStore.markAsRead(item)
  closeFloatingMenus()
  router.push(targetUrl)
}

function handleDocumentClick(event) {
  const path = typeof event.composedPath === 'function' ? event.composedPath() : []
  const inside = path.some(
    (node) =>
      node instanceof HTMLElement &&
      (node.dataset?.headerRoot === 'true' ||
        node.dataset?.notificationsMenuRoot === 'true' ||
        node.dataset?.appsMenuRoot === 'true' ||
        node.dataset?.profileCardRoot === 'true'),
  )
  if (!inside) {
    closeFloatingMenus()
  }
}

function handleViewportChange() {
  if (notificationsOpen.value) {
    positionNotifications()
  }

  if (notificationBubble.value) {
    positionNotificationBubble()
  }

  if (appsMenuOpen.value) {
    positionAppsMenu()
  }

  if (profileCardOpen.value) {
    positionProfileCard()
  }
}

watch(
  () => [userSettingsKey.value, authStore.user],
  () => {
    userSettingsStore.loadUserSettings(userSettingsKey.value, authStore.user)
    void refreshProfileCard()
  },
  { immediate: true, deep: true },
)

watch(
  () => [authStore.isAuthenticated, authStore.token],
  ([isAuthenticated, accessToken]) => {
    if (isAuthenticated && accessToken) {
      notificationStore.connect(accessToken)
      return
    }

    dismissNotificationBubble()
    notificationStore.disconnect()
  },
  { immediate: true },
)

watch(
  () => notificationStore.incomingNotificationSequence,
  () => {
    showNotificationBubble(notificationStore.lastIncomingNotification)
  },
)

watch(
  () => userSettingsStore.notifications,
  () => {
    if (notificationBubble.value && !shouldShowNotificationBubble(notificationBubble.value)) {
      dismissNotificationBubble()
    }
  },
  { deep: true },
)

onMounted(() => {
  window.addEventListener('click', handleDocumentClick)
  window.addEventListener('resize', handleViewportChange)
  window.addEventListener('scroll', handleViewportChange, true)
  void notificationStore.loadNotifications()
})

onBeforeUnmount(() => {
  window.removeEventListener('click', handleDocumentClick)
  window.removeEventListener('resize', handleViewportChange)
  window.removeEventListener('scroll', handleViewportChange, true)
  dismissNotificationBubble()
  notificationStore.disconnect()
})
</script>

<template>
  <header data-header-root="true" class="callog-header">
    <div class="callog-header__inner">
      <!-- 로고: 사이드바 너비 영역 좌상단 -->
      <RouterLink to="/dashboard" class="callog-header__logo" aria-label="대시보드로 이동">
        <img src="@/assets/callog-symbol.png" alt="callog" class="callog-header__logo-img" />
      </RouterLink>

      <!-- 왼쪽: 브레드크럼 -->
      <div class="callog-header__left">
        <div class="callog-header__breadcrumb">
          <p class="callog-header__section">{{ sectionTitle }}</p>
          <h1 class="callog-header__title">{{ pageTitle }}</h1>
        </div>
      </div>

      <!-- 오른쪽: 기능 버튼들 -->
      <div class="callog-header__right">
        <!-- 검색 -->
        <label class="callog-header__search" aria-label="검색">
          <svg
            class="callog-header__search-icon"
            xmlns="http://www.w3.org/2000/svg"
            width="16"
            height="16"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          >
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.3-4.3" />
          </svg>
          <input
            :value="store.searchQuery"
            type="search"
            placeholder="Search"
            class="callog-header__search-input"
            @input="handleSearchInput"
          />
        </label>

        <!-- 알림 -->
        <div class="callog-header__dropdown-wrap">
          <button
            ref="notificationsButton"
            type="button"
            class="callog-header__icon-btn callog-header__icon-btn--notif"
            aria-label="알림"
            :aria-expanded="notificationsOpen"
            @click.stop="toggleNotifications"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9" />
              <path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
            </svg>
            <span v-if="unreadCount" class="callog-header__notif-dot" />
          </button>

          <Transition name="callog-dropdown">
            <div
              v-if="false && notificationsOpen"
              class="callog-header__dropdown callog-header__dropdown--notif"
            >
              <div class="callog-dropdown__head">
                <strong class="callog-dropdown__title">최근 알림</strong>
                <RouterLink
                  to="/notifications"
                  class="callog-dropdown__more"
                  @click="closeFloatingMenus"
                >
                  알림 센터
                  <svg
                    xmlns="http://www.w3.org/2000/svg"
                    width="12"
                    height="12"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  >
                    <path d="m9 18 6-6-6-6" />
                  </svg>
                </RouterLink>
              </div>
              <div class="callog-dropdown__body">
                <div v-if="notifications.length" class="callog-notif-list">
                  <div
                    v-for="item in notifications"
                    :key="item.id"
                    class="callog-notif-item"
                    @click="handleNotificationDetail(item)"
                  >
                    <div class="callog-notif-item__top">
                      <p class="callog-notif-item__title">{{ item.title }}</p>
                      <button type="button" class="callog-notif-item__btn" @click.stop="handleNotificationDetail(item)">자세히 보기</button>
                    </div>
                    <p class="callog-notif-item__meta">
                      {{ formatRelativeTime(item.created_at) }} · {{ item.message }}
                    </p>
                    <div v-if="getReviewOutcomeMeta(item)" class="callog-notif-item__review">
                      <span
                        class="callog-review-chip"
                        :class="`callog-review-chip--${getReviewOutcomeMeta(item).tone}`"
                      >
                        <span class="material-symbols-outlined">{{ getReviewOutcomeMeta(item).icon }}</span>
                        {{ getReviewOutcomeMeta(item).label }}
                      </span>
                      <button
                        v-if="item.targetUrl"
                        type="button"
                        class="callog-notif-item__target"
                        @click.stop="handleNotificationTarget(item)"
                      >
                        검수 결과 보기
                      </button>
                    </div>
                    <div
                      v-if="isCampaignInvitationActionable(item)"
                      class="callog-notif-item__actions"
                      @click.stop
                    >
                      <button
                        type="button"
                        class="callog-notif-item__action callog-notif-item__action--accept"
                        :disabled="Boolean(inlineActionLoading[item.id])"
                        @click.stop="respondInlineCampaignInvitation(item, 'accept')"
                      >
                        {{ inlineActionLoading[item.id] === 'accept' ? '승인 중' : '승인' }}
                      </button>
                      <button
                        type="button"
                        class="callog-notif-item__action callog-notif-item__action--reject"
                        :disabled="Boolean(inlineActionLoading[item.id])"
                        @click.stop="respondInlineCampaignInvitation(item, 'reject')"
                      >
                        {{ inlineActionLoading[item.id] === 'reject' ? '반려 중' : '반려' }}
                      </button>
                    </div>
                  </div>
                </div>
                <div v-else class="callog-dropdown__empty">새로운 알림이 없습니다.</div>
              </div>
            </div>
          </Transition>
        </div>

        <!-- 프로필 -->
        <button
          ref="profileCardButton"
          type="button"
          class="callog-header__avatar"
          aria-label="프로필"
          :aria-expanded="profileCardOpen"
          @click.stop="toggleProfileCard"
        >
          <img
            v-if="profileCard.imageDataUrl"
            :src="profileCard.imageDataUrl"
            alt=""
            crossorigin="anonymous"
            referrerpolicy="strict-origin-when-cross-origin"
            @error="handleProfileImageError"
          />
          <span v-else>{{ profileCard.initials }}</span>
        </button>

        <!-- 전체 메뉴 -->
        <div class="callog-header__dropdown-wrap">
          <button
            ref="appsMenuButton"
            type="button"
            class="callog-header__icon-btn"
            aria-label="전체 메뉴"
            :aria-expanded="appsMenuOpen"
            @click.stop="toggleAppsMenu"
          >
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="18"
              height="18"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <circle cx="12" cy="5" r="1" />
              <circle cx="12" cy="12" r="1" />
              <circle cx="12" cy="19" r="1" />
            </svg>
          </button>
        </div>
      </div>
    </div>
  </header>

  <Teleport to="body">
    <Transition name="callog-notif-bubble">
      <article
        v-if="notificationBubble"
        data-notifications-menu-root="true"
        class="callog-notification-bubble"
        :class="`callog-notification-bubble--${notificationBubble.severity}`"
        :style="notificationBubbleStyle"
        role="status"
        aria-live="polite"
      >
        <button
          type="button"
          class="callog-notification-bubble__close"
          aria-label="알림 말풍선 닫기"
          @click.stop="dismissNotificationBubble"
        >
          <span class="material-symbols-outlined">close</span>
        </button>
        <button
          type="button"
          class="callog-notification-bubble__content"
          @click="openNotificationBubbleDetail"
        >
          <span class="callog-notification-bubble__icon material-symbols-outlined">
            {{ notificationBubbleReviewMeta?.icon || 'notifications_active' }}
          </span>
          <span class="callog-notification-bubble__body">
            <span class="callog-notification-bubble__eyebrow">
              {{ notificationBubble.source || 'Callog 알림' }}
            </span>
            <strong>{{ notificationBubble.title }}</strong>
            <span>{{ notificationBubble.message }}</span>
          </span>
        </button>
        <div class="callog-notification-bubble__actions">
          <span
            v-if="notificationBubbleReviewMeta"
            class="callog-review-chip"
            :class="`callog-review-chip--${notificationBubbleReviewMeta.tone}`"
          >
            <span class="material-symbols-outlined">{{ notificationBubbleReviewMeta.icon }}</span>
            {{ notificationBubbleReviewMeta.label }}
          </span>
          <button
            v-if="notificationBubble.targetUrl"
            type="button"
            class="callog-notification-bubble__target"
            @click.stop="openNotificationBubbleTarget"
          >
            {{ notificationBubbleReviewMeta ? '검수 결과 보기' : (notificationBubble.targetLabel || '관련 화면 보기') }}
          </button>
        </div>
      </article>
    </Transition>
  </Teleport>

  <Teleport to="body">
    <Transition name="callog-dropdown">
      <div
        v-if="notificationsOpen"
        data-notifications-menu-root="true"
        class="callog-header__dropdown callog-header__dropdown--notif callog-header__dropdown--floating"
        :style="notificationsStyle"
      >
        <div class="callog-dropdown__head">
          <strong class="callog-dropdown__title">최근 알림</strong>
          <RouterLink to="/notifications" class="callog-dropdown__more" @click="closeFloatingMenus">
            알림 센터
            <svg
              xmlns="http://www.w3.org/2000/svg"
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="m9 18 6-6-6-6" />
            </svg>
          </RouterLink>
        </div>
        <div class="callog-dropdown__body">
          <div v-if="notifications.length" class="callog-notif-list">
            <div
              v-for="item in notifications"
              :key="item.id"
              class="callog-notif-item"
              @click="handleNotificationDetail(item)"
            >
              <div class="callog-notif-item__top">
                <p class="callog-notif-item__title">{{ item.title }}</p>
                <button type="button" class="callog-notif-item__btn" @click.stop="handleNotificationDetail(item)">자세히 보기</button>
              </div>
              <p class="callog-notif-item__meta">
                {{ formatRelativeTime(item.created_at) }} · {{ item.message }}
              </p>
              <div v-if="getReviewOutcomeMeta(item)" class="callog-notif-item__review">
                <span
                  class="callog-review-chip"
                  :class="`callog-review-chip--${getReviewOutcomeMeta(item).tone}`"
                >
                  <span class="material-symbols-outlined">{{ getReviewOutcomeMeta(item).icon }}</span>
                  {{ getReviewOutcomeMeta(item).label }}
                </span>
                <button
                  v-if="item.targetUrl"
                  type="button"
                  class="callog-notif-item__target"
                  @click.stop="handleNotificationTarget(item)"
                >
                  검수 결과 보기
                </button>
              </div>
              <div
                v-if="isCampaignInvitationActionable(item)"
                class="callog-notif-item__actions"
                @click.stop
              >
                <button
                  type="button"
                  class="callog-notif-item__action callog-notif-item__action--accept"
                  :disabled="Boolean(inlineActionLoading[item.id])"
                  @click.stop="respondInlineCampaignInvitation(item, 'accept')"
                >
                  {{ inlineActionLoading[item.id] === 'accept' ? '승인 중' : '승인' }}
                </button>
                <button
                  type="button"
                  class="callog-notif-item__action callog-notif-item__action--reject"
                  :disabled="Boolean(inlineActionLoading[item.id])"
                  @click.stop="respondInlineCampaignInvitation(item, 'reject')"
                >
                  {{ inlineActionLoading[item.id] === 'reject' ? '반려 중' : '반려' }}
                </button>
              </div>
            </div>
          </div>
          <div v-else class="callog-dropdown__empty">새로운 알림이 없습니다.</div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <Teleport to="body">
    <Transition name="callog-dropdown">
      <div
        v-if="profileCardOpen"
        data-profile-card-root="true"
        class="callog-profile-card callog-header__dropdown--floating"
        :style="profileCardStyle"
      >
        <div class="callog-profile-card__hero">
          <div class="callog-profile-card__avatar">
            <img
              v-if="profileCard.imageDataUrl"
              :src="profileCard.imageDataUrl"
              alt=""
              crossorigin="anonymous"
              referrerpolicy="strict-origin-when-cross-origin"
              @error="handleProfileImageError"
            />
            <span v-else>{{ profileCard.initials }}</span>
          </div>
          <div>
            <strong>{{ profileCard.name }}</strong>
            <p>{{ profileCard.role }}</p>
          </div>
        </div>

        <div v-if="profileCard.companyLogoDataUrl" class="callog-profile-card__logo">
          <img :src="profileCard.companyLogoDataUrl" alt="" />
          <span>회사 로고</span>
        </div>

        <dl class="callog-profile-card__details">
          <div>
            <dt>회사</dt>
            <dd>{{ profileCard.company }}</dd>
          </div>
          <div>
            <dt>부서</dt>
            <dd>{{ profileCard.department }}</dd>
          </div>
          <div>
            <dt>전화번호</dt>
            <dd>{{ profileCard.phone }}</dd>
          </div>
          <div>
            <dt>이메일</dt>
            <dd>{{ profileCard.email }}</dd>
          </div>
        </dl>

        <div class="callog-profile-card__actions">
          <button type="button" class="callog-profile-card__button" @click="handleProfileDownload">
            다운로드
          </button>
          <button type="button" class="callog-profile-card__button" @click="handleProfileEdit">
            수정
          </button>
          <button
            type="button"
            class="callog-profile-card__button callog-profile-card__button--ghost"
            @click="closeFloatingMenus"
          >
            닫기
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>

  <Teleport to="body">
    <Transition name="callog-dropdown">
      <div
        v-if="appsMenuOpen"
        data-apps-menu-root="true"
        class="callog-header__dropdown callog-header__dropdown--menu callog-header__dropdown--floating"
        :style="appsMenuStyle"
      >
        <div class="callog-dropdown__head">
          <strong class="callog-dropdown__title">전체 메뉴</strong>
        </div>
        <div class="callog-appmenu-list">
          <button
            v-for="item in visibleAppMenuItems"
            :key="item.key"
            type="button"
            class="callog-appmenu-item"
            :class="{
              'callog-appmenu-item--active': item.active,
              'callog-appmenu-item--danger': item.danger,
            }"
            @click="handleAppMenuItem(item)"
          >
            <span class="callog-appmenu-item__icon" v-html="item.icon" />
            <span>{{ item.label }}</span>
          </button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.callog-header {
  height: var(--header-height);
  background: var(--header-color);
  border-bottom: 1px solid var(--border-color);
  color: var(--text-primary);
  position: sticky;
  top: 0;
  z-index: 10;
  flex-shrink: 0;
  transition:
    background var(--transition-normal),
    border-color var(--transition-normal),
    color var(--transition-normal);
}

.callog-header__inner {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px 0 0;
  gap: 12px;
}

.callog-header__logo {
  flex: 0 0 var(--sidebar-icon-width, 64px);
  width: var(--sidebar-icon-width, 64px);
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  text-decoration: none;
}

.callog-header__logo-img {
  width: 40px;
  height: 40px;
  object-fit: contain;
  display: block;
  padding: 4px;
  background: color-mix(in srgb, var(--color-primary-500) 10%, transparent);
  border-radius: var(--radius-lg);
  transition: background var(--transition-fast);
}

.callog-header__logo:hover .callog-header__logo-img {
  background: color-mix(in srgb, var(--color-primary-500) 18%, transparent);
}

.callog-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
  padding-left: 8px;
  margin-right: auto;
}

.callog-header__toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  color: var(--muted-text);
  background: transparent;
  border: none;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--transition-fast);
}

.callog-header__toggle:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
}

.callog-header__breadcrumb {
  min-width: 0;
}

.callog-header__section {
  font-size: 11px;
  color: var(--subtle-text);
  font-weight: 500;
  line-height: 1;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.callog-header__title {
  font-size: 15px;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.callog-header__right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.callog-header__icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-md);
  color: var(--muted-text);
  background: transparent;
  border: 1px solid var(--border-color);
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
}

.callog-header__icon-btn:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
  border-color: var(--border-strong);
}

.callog-header__icon-btn--notif {
  position: relative;
}

.callog-header__notif-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 6px;
  height: 6px;
  background: #ef4444;
  border-radius: 50%;
  border: 2px solid var(--header-color);
}

.callog-header__search {
  position: relative;
  display: flex;
  align-items: center;
}

.callog-header__search-icon {
  position: absolute;
  left: 10px;
  color: var(--subtle-text);
  pointer-events: none;
}

.callog-header__search-input {
  height: 36px;
  width: 180px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--control-color);
  padding: 0 12px 0 34px;
  font-size: 13px;
  color: var(--text-primary);
  outline: none;
  transition: all var(--transition-fast);
}

.callog-header__search-input::placeholder {
  color: var(--subtle-text);
}

.callog-header__search-input:focus {
  border-color: var(--color-primary-300);
  background: var(--control-focus-color);
  width: 220px;
}

.callog-header__avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--badge-bg);
  color: var(--badge-text);
  border: none;
  font-size: 13px;
  font-weight: 700;
  overflow: hidden;
  padding: 0;
  text-decoration: none;
  transition: all var(--transition-fast);
  flex-shrink: 0;
  cursor: pointer;
}

.callog-header__avatar:hover {
  background: var(--nav-icon-active-bg);
}

.callog-header__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.callog-header__dropdown-wrap {
  position: relative;
}

.callog-header__dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: var(--dropdown-color);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-elevated);
  z-index: 50;
  overflow: hidden;
  min-width: 280px;
}

.callog-header__dropdown--notif {
  width: 360px;
}

.callog-header__dropdown--notif.callog-header__dropdown--floating {
  z-index: 10000;
}

.callog-header__dropdown--menu {
  width: 220px;
}

.callog-header__dropdown--floating {
  position: fixed;
  right: auto;
  z-index: 9999;
}

.callog-profile-card {
  position: fixed;
  width: 320px;
  overflow: hidden;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-lg);
  background: var(--dropdown-color);
  box-shadow: var(--shadow-elevated);
  color: var(--text-primary);
  z-index: 10000;
}

.callog-profile-card__hero {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid var(--border-color);
  background: var(--panel-muted);
}

.callog-profile-card__avatar {
  display: inline-flex;
  width: 62px;
  height: 62px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid var(--border-strong);
  border-radius: var(--radius-md);
  background: var(--badge-bg);
  color: var(--badge-text);
  font-size: 20px;
  font-weight: 800;
}

.callog-profile-card__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.callog-profile-card__hero strong {
  display: block;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 800;
}

.callog-profile-card__hero p {
  margin-top: 3px;
  color: var(--muted-text);
  font-size: 12px;
  font-weight: 600;
}

.callog-profile-card__logo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 16px;
  border-bottom: 1px solid var(--border-color);
}

.callog-profile-card__logo img {
  width: 92px;
  height: 34px;
  object-fit: contain;
}

.callog-profile-card__logo span {
  color: var(--subtle-text);
  font-size: 11px;
  font-weight: 700;
}

.callog-profile-card__details {
  display: grid;
  gap: 0;
  margin: 0;
  padding: 8px 16px;
}

.callog-profile-card__details div {
  display: grid;
  grid-template-columns: 68px minmax(0, 1fr);
  gap: 10px;
  min-height: 34px;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
}

.callog-profile-card__details div:last-child {
  border-bottom: 0;
}

.callog-profile-card__details dt,
.callog-profile-card__details dd {
  min-width: 0;
  margin: 0;
  font-size: 12px;
}

.callog-profile-card__details dt {
  color: var(--subtle-text);
  font-weight: 700;
}

.callog-profile-card__details dd {
  overflow: hidden;
  color: var(--text-secondary);
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.callog-profile-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  padding: 10px 12px 12px;
  border-top: 1px solid var(--border-color);
}

.callog-profile-card__button {
  min-height: 30px;
  border: 1px solid var(--accent-strong);
  border-radius: var(--radius-sm);
  background: var(--accent-strong);
  color: #ffffff;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 800;
  cursor: pointer;
}

.callog-profile-card__button--ghost {
  border-color: var(--border-color);
  background: var(--panel-color);
  color: var(--text-secondary);
}

.callog-profile-card__button:hover {
  filter: brightness(1.04);
}

.callog-dropdown__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px 10px;
  border-bottom: 1px solid var(--border-color);
}

.callog-dropdown__title {
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
}

.callog-dropdown__more {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  color: var(--muted-text);
  text-decoration: none;
  transition: color var(--transition-fast);
}

.callog-dropdown__more:hover {
  color: var(--color-primary-600);
}

.callog-dropdown__body {
  max-height: 360px;
  overflow-y: auto;
  padding: 8px;
}

.callog-dropdown__empty {
  padding: 24px;
  text-align: center;
  font-size: 13px;
  color: var(--subtle-text);
}

.callog-notification-bubble {
  position: fixed;
  z-index: 1300;
  width: min(340px, calc(100vw - 24px));
  border: 1px solid color-mix(in srgb, var(--color-primary-500) 22%, var(--border-color));
  border-radius: var(--radius-lg);
  background: var(--panel-color);
  box-shadow: 0 18px 46px rgba(15, 23, 42, 0.2);
  padding: 10px;
}

.callog-notification-bubble::before {
  content: '';
  position: absolute;
  top: -7px;
  right: 22px;
  width: 13px;
  height: 13px;
  border-top: 1px solid color-mix(in srgb, var(--color-primary-500) 22%, var(--border-color));
  border-left: 1px solid color-mix(in srgb, var(--color-primary-500) 22%, var(--border-color));
  background: var(--panel-color);
  transform: rotate(45deg);
}

.callog-notification-bubble__content {
  display: grid;
  width: 100%;
  grid-template-columns: 36px minmax(0, 1fr);
  gap: 10px;
  align-items: flex-start;
  border: 0;
  background: transparent;
  cursor: pointer;
  padding: 0 28px 0 0;
  text-align: left;
}

.callog-notification-bubble__icon {
  display: inline-flex;
  width: 36px;
  height: 36px;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-md);
  background: color-mix(in srgb, var(--color-primary-500) 14%, transparent);
  color: var(--color-primary-600);
  font-size: 21px;
}

.callog-notification-bubble__body {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.callog-notification-bubble__eyebrow {
  overflow: hidden;
  color: var(--muted-text);
  font-size: 11px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.callog-notification-bubble__body strong {
  overflow: hidden;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.callog-notification-bubble__body > span:last-child {
  display: -webkit-box;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.45;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.callog-notification-bubble__close {
  position: absolute;
  top: 8px;
  right: 8px;
  display: inline-flex;
  width: 26px;
  height: 26px;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--muted-text);
  cursor: pointer;
}

.callog-notification-bubble__close:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
}

.callog-notification-bubble__close .material-symbols-outlined {
  font-size: 16px;
}

.callog-notification-bubble__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 10px;
  padding-left: 46px;
}

.callog-notification-bubble__target {
  border: 0;
  background: transparent;
  color: var(--color-primary-600);
  cursor: pointer;
  font-size: 12px;
  font-weight: 900;
  white-space: nowrap;
}

.callog-notification-bubble__target:hover {
  text-decoration: underline;
}

.callog-notif-bubble-enter-active,
.callog-notif-bubble-leave-active {
  transition: opacity 180ms ease, transform 180ms ease;
  transform-origin: top right;
}

.callog-notif-bubble-enter-from,
.callog-notif-bubble-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.98);
}

/* 알림 목록 */
.callog-notif-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.callog-notif-item {
  background: var(--panel-muted);
  border-radius: var(--radius-md);
  padding: 10px 12px;
}

.callog-notif-item__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 4px;
}

.callog-notif-item__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.callog-notif-item__btn {
  font-size: 11px;
  color: var(--muted-text);
  background: var(--panel-color);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-sm);
  padding: 2px 8px;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: all var(--transition-fast);
}

.callog-notif-item__btn:hover {
  border-color: var(--color-primary-300);
  color: var(--color-primary-600);
}

.callog-notif-item__meta {
  font-size: 12px;
  color: var(--subtle-text);
}

.callog-notif-item__review {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-top: 8px;
}

.callog-review-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 24px;
  padding: 3px 8px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-color);
  font-size: 11px;
  font-weight: 800;
}

.callog-review-chip .material-symbols-outlined {
  font-size: 14px;
}

.callog-review-chip--completed {
  border-color: rgba(16, 185, 129, 0.32);
  background: rgba(16, 185, 129, 0.1);
  color: #047857;
}

.callog-review-chip--review-required {
  border-color: rgba(217, 119, 6, 0.32);
  background: rgba(245, 158, 11, 0.12);
  color: #92400e;
}

.callog-review-chip--failed {
  border-color: rgba(220, 38, 38, 0.3);
  background: rgba(239, 68, 68, 0.1);
  color: #b91c1c;
}

.callog-notif-item__target {
  border: 0;
  background: transparent;
  color: var(--color-primary-600);
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
  cursor: pointer;
}

.callog-notif-item__target:hover {
  text-decoration: underline;
}

.callog-notif-item__actions { display: flex; gap: 6px; margin-top: 6px; }
.callog-notif-item__action { padding: 4px 10px; font-size: 11px; font-weight: 700; border-radius: var(--radius-sm); cursor: pointer; border: 1px solid var(--border-color); }
.callog-notif-item__action:disabled { opacity: 0.5; cursor: not-allowed; }
.callog-notif-item__action--accept { background: var(--color-primary-500); color: #fff; border-color: var(--color-primary-500); }
.callog-notif-item__action--accept:hover:not(:disabled) { background: var(--color-primary-600); }
.callog-notif-item__action--reject { background: var(--panel-color); color: var(--text-primary); }
.callog-notif-item__action--reject:hover:not(:disabled) { background: var(--panel-muted); }

.callog-appmenu-list {
  display: flex;
  flex-direction: column;
  padding: 8px;
  gap: 2px;
}

.callog-appmenu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 12px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  background: transparent;
  border: none;
  cursor: pointer;
  text-align: left;
  width: 100%;
  transition: all var(--transition-fast);
}

.callog-appmenu-item:hover {
  background: var(--panel-muted);
  color: var(--text-primary);
}

.callog-appmenu-item--active {
  background: var(--badge-bg);
  color: var(--badge-text);
}

.callog-appmenu-item--active .callog-appmenu-item__icon {
  color: var(--badge-text);
}

.callog-appmenu-item--danger {
  color: #ef4444;
}

.callog-appmenu-item--danger:hover {
  background: var(--danger-surface);
  color: var(--danger-text-strong);
}

.callog-appmenu-item__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  color: var(--subtle-text);
  flex-shrink: 0;
}

.callog-appmenu-item--danger .callog-appmenu-item__icon {
  color: #fca5a5;
}

.callog-dropdown-enter-active,
.callog-dropdown-leave-active {
  transition: all var(--transition-fast);
  transform-origin: top right;
}

.callog-dropdown-enter-from,
.callog-dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}
</style>
