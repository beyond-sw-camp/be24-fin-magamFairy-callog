<template>
  <div class="notification-settings-panel" :class="{ 'is-compact': compact }">
    <section class="notification-settings-block notification-settings-block--split">
      <div>
        <strong>알림 사용</strong>
        <p>전체 알림 수신 여부를 관리합니다. 변경 사항은 브라우저에 즉시 저장됩니다.</p>
      </div>
      <button
        type="button"
        class="ui-toggle"
        :class="{ 'is-active': userSettingsStore.notifications.enabled }"
        :aria-pressed="userSettingsStore.notifications.enabled"
        aria-label="알림 사용 설정"
        @click="toggleNotificationEnabled"
      >
        <span class="ui-toggle-thumb" />
      </button>
    </section>

    <section class="notification-settings-block">
      <div class="notification-settings-heading">
        <div>
          <strong>표시 방식</strong>
          <p>화면에 보여줄 알림 UI와 외부 알림 채널을 선택합니다.</p>
        </div>
        <span>{{ notificationDisplaySummary }}</span>
      </div>
      <div class="notification-condition-list" aria-label="알림 표시 방식">
        <div
          v-for="option in notificationDisplayOptions"
          :key="option.key"
          class="notification-condition-row"
        >
          <div>
            <strong>{{ option.label }}</strong>
            <p>{{ option.description }}</p>
          </div>
          <button
            type="button"
            class="ui-toggle"
            :class="{ 'is-active': isDisplayOptionEnabled(option) }"
            :aria-pressed="isDisplayOptionEnabled(option)"
            :aria-label="`${option.label} 설정`"
            :disabled="!userSettingsStore.notifications.enabled || isSavingNotifications"
            @click="toggleNotificationDisplay(option)"
          >
            <span class="ui-toggle-thumb" />
          </button>
        </div>
      </div>
      <p v-if="browserPermissionMessage" class="notification-settings-note">
        {{ browserPermissionMessage }}
      </p>
    </section>

    <section class="notification-settings-block">
      <div class="notification-settings-heading">
        <div>
          <strong>받을 알림</strong>
          <p>알림 센터에 쌓을 업무 이벤트를 큰 범주로 관리합니다.</p>
        </div>
        <span>{{ notificationGroupCount }}개 활성</span>
      </div>
      <div class="notification-segmented notification-segmented--wide" role="group" aria-label="알림 정도 선택">
        <button
          v-for="option in notificationLevelOptions"
          :key="option.value"
          type="button"
          :title="option.description"
          :class="{ 'is-active': userSettingsStore.notifications.level === option.value }"
          :disabled="!userSettingsStore.notifications.enabled || isSavingNotifications"
          @click="setNotificationLevel(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
      <div class="notification-condition-list" aria-label="알림 조건">
        <div
          v-for="option in notificationGroupOptions"
          :key="option.key"
          class="notification-condition-row"
        >
          <div>
            <strong>{{ option.label }}</strong>
            <p>{{ option.description }}</p>
          </div>
          <button
            type="button"
            class="ui-toggle"
            :class="{ 'is-active': isConditionGroupEnabled(option) }"
            :aria-pressed="isConditionGroupEnabled(option)"
            :aria-label="`${option.label} 알림 설정`"
            :disabled="!userSettingsStore.notifications.enabled || isSavingNotifications"
            @click="toggleNotificationConditionGroup(option)"
          >
            <span class="ui-toggle-thumb" />
          </button>
        </div>
      </div>
    </section>

    <section v-if="isPolicyManager" class="notification-settings-block">
      <div class="notification-settings-heading">
        <div>
          <strong>조직 알림 정책</strong>
          <p>조직 구성원에게 허용할 알림 유형을 관리합니다.</p>
        </div>
        <span>{{ isLoadingPolicies ? '불러오는 중' : `${adminPolicies.length}개 설정됨` }}</span>
      </div>
      <div class="notification-condition-list" aria-label="조직 알림 정책">
        <div v-for="option in adminPolicyOptions" :key="option.type" class="notification-condition-row">
          <div>
            <strong>{{ option.label }}</strong>
            <p>{{ option.description }}</p>
          </div>
          <button
            type="button"
            class="ui-toggle"
            :class="{ 'is-active': isPolicyEnabled(option.type) }"
            :aria-pressed="isPolicyEnabled(option.type)"
            :aria-label="`${option.label} 조직 알림 정책 설정`"
            :disabled="isSavingPolicies"
            @click="toggleAdminPolicy(option.type)"
          >
            <span class="ui-toggle-thumb" />
          </button>
        </div>
      </div>
    </section>

    <p v-if="notificationServerMessage" class="notification-settings-note">
      {{ notificationServerMessage }}
    </p>

    <footer class="notification-settings-footer">
      <button type="button" class="notification-settings-reset" @click="resetNotifications">
        기본값으로 초기화
      </button>
      <RouterLink v-if="showCenterLink" :to="{ name: 'notifications' }" class="notification-center-link">
        알림 센터로 이동
      </RouterLink>
    </footer>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUserSettingsStore } from '@/stores/userSettings'
import { useAuthStore } from '@/stores/useAuthStore'
import {
  getNotificationAdminPolicies,
  getNotificationSettings,
  updateNotificationAdminPolicies,
  updateNotificationSettings,
} from '@/api/notifications'

defineProps({
  compact: {
    type: Boolean,
    default: false,
  },
  showCenterLink: {
    type: Boolean,
    default: false,
  },
})

const userSettingsStore = useUserSettingsStore()
const authStore = useAuthStore()
const browserPermissionMessage = ref('')
const notificationServerMessage = ref('')
const adminPolicies = ref([])
const isLoadingPolicies = ref(false)
const isSavingPolicies = ref(false)
const isSavingNotifications = ref(false)

const isPolicyManager = computed(() => authStore.isAdmin || authStore.isGeneralManager)

const adminPolicyOptions = [
  {
    type: 'TASK_ASSIGNED',
    label: '업무 배정',
    description: '새 업무 생성 또는 담당자 배정 알림을 허용합니다.',
  },
  {
    type: 'TASK_STATUS_CHANGED',
    label: '업무 상태 변경',
    description: '진행 상태 변경과 업무 수정 알림을 허용합니다.',
  },
  {
    type: 'REVIEW_REQUESTED',
    label: 'QA 검수',
    description: '검수 요청, 승인, 반려, AI 검수 완료/확인 필요/실패 알림을 허용합니다.',
  },
  {
    type: 'DEADLINE_24H',
    label: '마감 임박',
    description: '마감 24시간/1시간 전과 지연 알림을 허용합니다.',
  },
  {
    type: 'CAMPAIGN_INVITED',
    label: '캠페인',
    description: '캠페인 초대, 승인/반려, 구성원 추가 알림을 허용합니다.',
  },
]

const adminPolicyTypeGroups = {
  TASK_ASSIGNED: ['TASK_ASSIGNED'],
  TASK_STATUS_CHANGED: ['TASK_STATUS_CHANGED', 'TASK_UPDATED'],
  REVIEW_REQUESTED: [
    'REVIEW_REQUESTED',
    'REVIEW_APPROVED',
    'REVIEW_REJECTED',
    'AI_JUDGE_COMPLETED',
    'AI_JUDGE_REVIEW_REQUIRED',
    'AI_JUDGE_FAILED',
  ],
  DEADLINE_24H: ['DEADLINE_24H', 'DEADLINE_1H', 'DEADLINE_OVERDUE'],
  CAMPAIGN_INVITED: [
    'CAMPAIGN_INVITED',
    'CAMPAIGN_INVITATION_ACCEPTED',
    'CAMPAIGN_INVITATION_REJECTED',
    'CAMPAIGN_MEMBER_ADDED',
  ],
}

const notificationDisplayOptions = [
  {
    key: 'inApp',
    type: 'method',
    label: '앱 내 알림',
    description: '헤더 알림, 최근 알림, 알림 센터에 표시합니다.',
  },
  {
    key: 'showAdCheckProgressPanel',
    type: 'local',
    label: '검수 진행 패널',
    description: 'AI 검수 중 다른 화면으로 이동해도 오른쪽 하단 진행도를 표시합니다.',
  },
  {
    key: 'browser',
    type: 'method',
    label: '브라우저 알림',
    description: '브라우저 권한이 허용된 경우 데스크톱 알림으로 표시합니다.',
  },
  {
    key: 'email',
    type: 'method',
    label: '이메일',
    description: '계정 이메일로 주요 알림을 전달할 수 있도록 준비합니다.',
  },
]

const notificationLevelOptions = [
  {
    value: 'essential',
    label: '중요만',
    description: '마감 임박, 지연, 검수 요청처럼 놓치면 안 되는 알림만 받습니다.',
  },
  {
    value: 'normal',
    label: '기본',
    description: '업무, QA, 캠페인 변경 등 작업에 필요한 알림을 받습니다.',
  },
  {
    value: 'all',
    label: '전체',
    description: '업무, QA, 캠페인, 일정 관련 알림을 넓게 받습니다.',
  },
]

const notificationGroupOptions = [
  {
    key: 'task',
    conditionKeys: ['taskAssigned', 'taskStatusChanged'],
    label: '업무 알림',
    description: '업무 생성, 담당자 배정, 진행 상태 변경을 알림으로 받습니다.',
  },
  {
    key: 'qaReview',
    conditionKeys: ['qaReview'],
    label: 'QA 검수',
    description: '검수 요청, 승인, 반려, AI 검수 완료/확인 필요/실패 결과를 알림으로 받습니다.',
  },
  {
    key: 'schedule',
    conditionKeys: ['deadline', 'schedule'],
    label: '일정/마감',
    description: '마감 임박, 지연, 캘린더 일정 변경을 알림으로 받습니다.',
  },
  {
    key: 'campaign',
    conditionKeys: ['campaign'],
    label: '캠페인 변경',
    description: '캠페인 초대, 승인/반려, 구성원 변경 알림을 받습니다.',
  },
]

const notificationDisplaySummary = computed(() => {
  const selectedDisplays = notificationDisplayOptions
    .filter((option) => isDisplayOptionEnabled(option))
    .map((option) => option.label)

  return selectedDisplays.length ? selectedDisplays.join(', ') : '선택 없음'
})

const notificationGroupCount = computed(
  () => notificationGroupOptions.filter((option) => isConditionGroupEnabled(option)).length,
)

function resolvePayload(response) {
  return response?.data?.data ?? response?.data ?? response ?? {}
}

function toClientSettings(payload) {
  return {
    enabled: Boolean(payload.enabled ?? true),
    level: String(payload.level ?? 'NORMAL').toLowerCase(),
    methods: {
      inApp: Boolean(payload.methods?.inApp ?? true),
      email: Boolean(payload.methods?.email ?? false),
      browser: Boolean(payload.methods?.browser ?? false),
    },
    conditions: {
      taskAssigned: Boolean(payload.conditions?.taskAssigned ?? true),
      taskStatusChanged: Boolean(payload.conditions?.taskStatusChanged ?? true),
      qaReview: Boolean(payload.conditions?.qaReview ?? true),
      deadline: Boolean(payload.conditions?.deadline ?? true),
      campaign: Boolean(payload.conditions?.campaign ?? true),
      schedule: Boolean(payload.conditions?.schedule ?? true),
    },
  }
}

function toServerSettings() {
  return {
    enabled: userSettingsStore.notifications.enabled,
    level: String(userSettingsStore.notifications.level || 'normal').toUpperCase(),
    methods: {
      ...userSettingsStore.notifications.methods,
    },
    conditions: {
      ...userSettingsStore.notifications.conditions,
    },
  }
}

function isDisplayOptionEnabled(option) {
  if (option.type === 'method') {
    return Boolean(userSettingsStore.notifications.methods[option.key])
  }

  return userSettingsStore.notifications[option.key] !== false
}

function isConditionGroupEnabled(option) {
  return option.conditionKeys.every((key) => userSettingsStore.notifications.conditions[key])
}

async function loadRemoteNotificationSettings() {
  try {
    const response = await getNotificationSettings()
    userSettingsStore.updateNotifications(toClientSettings(resolvePayload(response)))
  } catch (error) {
    console.warn('Notification settings request failed.', error)
    notificationServerMessage.value = '알림 설정을 서버에서 불러오지 못해 로컬 설정을 표시합니다.'
  }
}

async function saveNotificationSettings() {
  isSavingNotifications.value = true
  notificationServerMessage.value = ''

  try {
    const response = await updateNotificationSettings(toServerSettings())
    userSettingsStore.updateNotifications(toClientSettings(resolvePayload(response)))
  } catch (error) {
    console.warn('Notification settings save failed.', error)
    notificationServerMessage.value = '알림 설정 저장에 실패했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isSavingNotifications.value = false
  }
}

async function loadAdminPolicies() {
  if (!isPolicyManager.value) {
    return
  }

  isLoadingPolicies.value = true
  try {
    const response = await getNotificationAdminPolicies()
    adminPolicies.value = resolvePayload(response).policies ?? []
  } catch (error) {
    console.warn('Notification policy request failed.', error)
  } finally {
    isLoadingPolicies.value = false
  }
}

function findPolicy(type) {
  return adminPolicies.value.find((policy) => policy.notificationType === type && policy.roleName === 'ALL')
}

function isPolicyEnabled(type) {
  const types = adminPolicyTypeGroups[type] ?? [type]
  return types.every((item) => findPolicy(item)?.enabled ?? true)
}

async function toggleAdminPolicy(type) {
  isSavingPolicies.value = true
  const nextEnabled = !isPolicyEnabled(type)
  const types = adminPolicyTypeGroups[type] ?? [type]

  try {
    const response = await updateNotificationAdminPolicies({
      policies: types.map((item) => ({
          roleName: 'ALL',
          notificationType: item,
          enabled: nextEnabled,
        })),
    })
    adminPolicies.value = resolvePayload(response).policies ?? []
  } catch (error) {
    console.warn('Notification policy save failed.', error)
    notificationServerMessage.value = '조직 알림 정책 저장에 실패했습니다.'
  } finally {
    isSavingPolicies.value = false
  }
}

function toggleNotificationEnabled() {
  userSettingsStore.updateNotifications({
    enabled: !userSettingsStore.notifications.enabled,
  })
  void saveNotificationSettings()
}

async function toggleNotificationMethod(key) {
  const nextValue = !userSettingsStore.notifications.methods[key]

  browserPermissionMessage.value = ''

  if (key === 'browser' && nextValue && typeof window !== 'undefined') {
    if (!('Notification' in window)) {
      browserPermissionMessage.value = '현재 브라우저는 데스크톱 알림 권한을 지원하지 않습니다.'
      return
    }

    if (Notification.permission !== 'granted') {
      const permission = await Notification.requestPermission()

      if (permission !== 'granted') {
        browserPermissionMessage.value = '브라우저 알림 권한이 허용되지 않아 설정을 켜지 않았습니다.'
        return
      }
    }
  }

  userSettingsStore.updateNotificationMethod(key, nextValue)
  void saveNotificationSettings()
}

function toggleNotificationDisplay(option) {
  if (option.type === 'method') {
    void toggleNotificationMethod(option.key)
    return
  }

  userSettingsStore.updateNotifications({
    [option.key]: !isDisplayOptionEnabled(option),
  })
}

function setNotificationLevel(value) {
  userSettingsStore.updateNotifications({ level: value })
  void saveNotificationSettings()
}

function toggleNotificationConditionGroup(option) {
  const nextValue = !isConditionGroupEnabled(option)
  const nextConditions = Object.fromEntries(
    option.conditionKeys.map((key) => [key, nextValue]),
  )

  userSettingsStore.updateNotifications({
    conditions: nextConditions,
  })
  void saveNotificationSettings()
}

function resetNotifications() {
  browserPermissionMessage.value = ''
  userSettingsStore.resetNotifications()
  void saveNotificationSettings()
}

onMounted(() => {
  void loadRemoteNotificationSettings()
  void loadAdminPolicies()
})
</script>

<style scoped>
.notification-settings-panel {
  display: grid;
  gap: 16px;
}

.notification-settings-block {
  display: grid;
  gap: 14px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  padding: 18px;
}

.notification-settings-block--split,
.notification-settings-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.notification-settings-block strong,
.notification-condition-row strong,
.notification-method strong {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 900;
}

.notification-settings-block p,
.notification-condition-row p,
.notification-method small {
  margin: 5px 0 0;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.5;
}

.notification-settings-heading > span,
.notification-settings-note {
  color: var(--accent-color);
  font-size: 12px;
  font-weight: 900;
}

.notification-methods {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.notification-method {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-height: 98px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card-muted);
  padding: 14px;
  color: var(--text-body);
  text-align: left;
  cursor: pointer;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast),
    opacity var(--transition-fast);
}

.notification-method.is-active {
  border-color: var(--accent-color);
  background: color-mix(in srgb, var(--accent-color) 11%, var(--surface-card));
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--accent-color) 28%, transparent);
}

.notification-method:disabled {
  cursor: not-allowed;
  opacity: 0.58;
}

.notification-method__check {
  color: var(--accent-color);
  font-size: 20px;
  line-height: 1;
}

.notification-segmented {
  display: inline-flex;
  gap: 4px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-sm);
  background: var(--surface-control);
  padding: 4px;
}

.notification-segmented--wide {
  width: fit-content;
}

.notification-segmented button {
  min-height: 32px;
  border: 0;
  border-radius: calc(var(--radius-sm) - 2px);
  background: transparent;
  padding: 0 12px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 900;
  cursor: pointer;
}

.notification-segmented button.is-active {
  background: var(--accent-strong);
  color: #ffffff;
}

.notification-segmented button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.notification-condition-list {
  display: grid;
  gap: 10px;
}

.notification-condition-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 70px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card-muted);
  padding: 14px;
}

.notification-settings-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.notification-settings-reset,
.notification-center-link {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  border-radius: var(--radius-sm);
  padding: 0 14px;
  font-size: 13px;
  font-weight: 900;
  text-decoration: none;
}

.notification-settings-reset {
  border: 1px solid var(--line-soft);
  background: var(--surface-control);
  color: var(--text-body);
  cursor: pointer;
}

.notification-center-link {
  border: 1px solid var(--accent-strong);
  background: var(--accent-strong);
  color: #ffffff;
}

.notification-settings-panel.is-compact .notification-settings-block {
  padding: 16px;
}

@media (max-width: 820px) {
  .notification-settings-block--split,
  .notification-settings-heading,
  .notification-condition-row,
  .notification-settings-footer {
    align-items: flex-start;
    flex-direction: column;
  }

  .notification-methods {
    grid-template-columns: 1fr;
  }

  .notification-segmented {
    width: 100%;
  }

  .notification-segmented button {
    flex: 1;
  }
}
</style>
