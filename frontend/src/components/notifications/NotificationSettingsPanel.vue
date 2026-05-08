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
          <strong>알림 방법</strong>
          <p>알림을 받을 채널을 선택합니다.</p>
        </div>
        <span>{{ notificationMethodSummary }}</span>
      </div>
      <div class="notification-methods" role="group" aria-label="알림 방법 선택">
        <button
          v-for="option in notificationMethodOptions"
          :key="option.key"
          type="button"
          class="notification-method"
          :class="{ 'is-active': userSettingsStore.notifications.methods[option.key] }"
          :disabled="!userSettingsStore.notifications.enabled"
          @click="toggleNotificationMethod(option.key)"
        >
          <span class="notification-method__check material-symbols-outlined">
            {{
              userSettingsStore.notifications.methods[option.key]
                ? 'check_circle'
                : 'radio_button_unchecked'
            }}
          </span>
          <span>
            <strong>{{ option.label }}</strong>
            <small>{{ option.description }}</small>
          </span>
        </button>
      </div>
      <p v-if="browserPermissionMessage" class="notification-settings-note">
        {{ browserPermissionMessage }}
      </p>
    </section>

    <section class="notification-settings-block notification-settings-block--split">
      <div>
        <strong>알림 정도</strong>
        <p>알림을 어느 범위까지 받을지 선택합니다.</p>
      </div>
      <div class="notification-segmented" role="group" aria-label="알림 정도 선택">
        <button
          v-for="option in notificationLevelOptions"
          :key="option.value"
          type="button"
          :title="option.description"
          :class="{ 'is-active': userSettingsStore.notifications.level === option.value }"
          :disabled="!userSettingsStore.notifications.enabled"
          @click="setNotificationLevel(option.value)"
        >
          {{ option.label }}
        </button>
      </div>
    </section>

    <section class="notification-settings-block">
      <div class="notification-settings-heading">
        <div>
          <strong>알림 조건</strong>
          <p>알림 센터에 쌓을 이벤트 조건을 선택합니다.</p>
        </div>
        <span>{{ notificationConditionCount }}개 활성</span>
      </div>
      <div class="notification-condition-list" aria-label="알림 조건">
        <div
          v-for="option in notificationConditionOptions"
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
            :class="{ 'is-active': userSettingsStore.notifications.conditions[option.key] }"
            :aria-pressed="userSettingsStore.notifications.conditions[option.key]"
            :aria-label="`${option.label} 알림 조건 설정`"
            :disabled="!userSettingsStore.notifications.enabled"
            @click="toggleNotificationCondition(option.key)"
          >
            <span class="ui-toggle-thumb" />
          </button>
        </div>
      </div>
    </section>

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
import { computed, ref } from 'vue'
import { useUserSettingsStore } from '@/stores/userSettings'

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
const browserPermissionMessage = ref('')

const notificationMethodOptions = [
  {
    key: 'inApp',
    label: '앱 내 알림',
    description: '헤더 알림 패널과 알림 센터에 표시합니다.',
  },
  {
    key: 'email',
    label: '이메일',
    description: '계정 이메일로 주요 알림을 전달할 수 있도록 준비합니다.',
  },
  {
    key: 'browser',
    label: '브라우저 알림',
    description: '브라우저 권한이 허용된 경우 데스크톱 알림으로 표시합니다.',
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

const notificationConditionOptions = [
  {
    key: 'taskAssigned',
    label: '업무 생성/배정',
    description: '새 업무가 생성되거나 담당자로 배정될 때 알림을 받습니다.',
  },
  {
    key: 'taskStatusChanged',
    label: '업무 상태 변경',
    description: '진행중, 검수, 완료 등 주요 상태가 바뀔 때 알림을 받습니다.',
  },
  {
    key: 'qaReview',
    label: 'QA 검수',
    description: '검수 요청, 승인, 반려, 수정 요청 결과를 알림으로 받습니다.',
  },
  {
    key: 'deadline',
    label: '마감 임박/지연',
    description: '마감 24시간 전, 1시간 전, 지연 상태 알림을 받습니다.',
  },
  {
    key: 'campaign',
    label: '캠페인 변경',
    description: '캠페인 초대, 승인/반려, 구성원 변경 알림을 받습니다.',
  },
  {
    key: 'schedule',
    label: '일정 알림',
    description: '캘린더 일정 변경과 주요 일정 안내를 알림으로 받습니다.',
  },
]

const notificationMethodSummary = computed(() => {
  const selectedMethods = notificationMethodOptions
    .filter((option) => userSettingsStore.notifications.methods[option.key])
    .map((option) => option.label)

  return selectedMethods.length ? selectedMethods.join(', ') : '선택 없음'
})

const notificationConditionCount = computed(
  () =>
    notificationConditionOptions.filter(
      (option) => userSettingsStore.notifications.conditions[option.key],
    ).length,
)

function toggleNotificationEnabled() {
  userSettingsStore.updateNotifications({
    enabled: !userSettingsStore.notifications.enabled,
  })
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
}

function setNotificationLevel(value) {
  userSettingsStore.updateNotifications({ level: value })
}

function toggleNotificationCondition(key) {
  userSettingsStore.updateNotificationCondition(
    key,
    !userSettingsStore.notifications.conditions[key],
  )
}

function resetNotifications() {
  browserPermissionMessage.value = ''
  userSettingsStore.resetNotifications()
}
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
