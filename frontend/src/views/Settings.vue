<script setup>
import { computed, onMounted, reactive } from 'vue'
import { usePlannerStore } from '@/stores/planner'
import { getSettings, updateSettings } from '@/api/settings/index.js'

const store = usePlannerStore()

const notificationItems = {
  task: { label: '업무 알림', desc: '신규 업무 생성, 배정, 상태 변경 알림' },
  qa: { label: 'QA 알림', desc: '검수 결과 및 버그 수정 요청 알림' },
  ai: { label: 'AI 분석 알림', desc: '리스크 감지 및 업무 가이드 생성 알림' },
}

const settings = reactive({
  notifications: {
    task: true,
    qa: true,
    ai: true,
    critical: true,
  },
})

const isDarkMode = computed(() => store.theme === 'dark')

function normalizeTheme(value) {
  return value === 'light' || value === 'dark' ? value : null
}

function resolveSettingsPayload(payload) {
  return payload?.result ?? payload?.data ?? payload ?? {}
}

function applyRemoteSettings(payload) {
  const source = resolveSettingsPayload(payload)
  const nextTheme =
    normalizeTheme(source.theme) ??
    (typeof source.darkMode === 'boolean' ? (source.darkMode ? 'dark' : 'light') : null)

  if (nextTheme) {
    store.setTheme(nextTheme)
  }

  Object.keys(settings.notifications).forEach((key) => {
    const remoteValue = source.notifications?.[key] ?? source[key]

    if (typeof remoteValue === 'boolean') {
      settings.notifications[key] = remoteValue
    }
  })
}

async function syncSettingToServer(body) {
  try {
    await updateSettings(body)
  } catch (error) {
    console.warn('설정 저장 실패. 로컬 테마 설정은 유지됩니다.', error)
  }
}

function toggleDarkMode() {
  const nextTheme = isDarkMode.value ? 'light' : 'dark'

  store.setTheme(nextTheme)
  void syncSettingToServer({
    theme: nextTheme,
    darkMode: nextTheme === 'dark',
  })
}

function updateNotification(type) {
  if (type === 'critical') {
    return
  }

  const nextValue = !settings.notifications[type]
  settings.notifications[type] = nextValue

  void syncSettingToServer({
    [type]: nextValue,
    notifications: {
      [type]: nextValue,
    },
  })
}

onMounted(async () => {
  try {
    const res = await getSettings()
    applyRemoteSettings(res.data)
  } catch (error) {
    console.warn('설정 정보를 불러오지 못해 로컬 설정을 사용합니다.', error)
  }
})
</script>

<template>
  <section class="settings-page ui-page">
    <div class="settings-panel ui-card">
      <div class="settings-section ui-card-header">
        <div>
          <h3 class="settings-title">다크 모드 설정</h3>
          <p class="settings-description ui-muted">화면 테마를 어둡게 전환하여 눈의 피로를 줄입니다.</p>
        </div>

        <button
          type="button"
          class="ui-toggle"
          :class="{ 'is-active': isDarkMode }"
          :aria-pressed="isDarkMode"
          aria-label="다크 모드 설정"
          @click="toggleDarkMode"
        >
          <span class="ui-toggle-thumb" />
        </button>
      </div>

      <div class="settings-section ui-card-header">
        <div>
          <h3 class="settings-title">알림 수신 설정</h3>
          <p class="settings-description ui-muted">수신하고 싶은 알림 유형을 선택하세요.</p>
        </div>
      </div>

      <div class="settings-list">
        <div
          v-for="(info, key) in notificationItems"
          :key="key"
          class="settings-row"
        >
          <div>
            <strong class="settings-row__label">{{ info.label }}</strong>
            <p class="settings-row__description ui-muted">{{ info.desc }}</p>
          </div>

          <button
            type="button"
            class="ui-toggle"
            :class="{ 'is-active': settings.notifications[key] }"
            :aria-pressed="settings.notifications[key]"
            :aria-label="`${info.label} 수신 설정`"
            @click="updateNotification(key)"
          >
            <span class="ui-toggle-thumb" />
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.settings-page {
  display: grid;
  max-width: 56rem;
  gap: 1.5rem;
  margin: 0 auto;
  padding: 1.5rem;
  font-family: 'Pretendard', system-ui, sans-serif;
}

.settings-panel {
  border-radius: 24px;
}

.settings-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.5rem 2rem;
}

.settings-title {
  color: var(--text-heading);
  font-size: 1.125rem;
  font-weight: 700;
}

.settings-description {
  margin-top: 0.25rem;
  font-size: 0.875rem;
  font-weight: 500;
}

.settings-list {
  display: grid;
}

.settings-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  padding: 1.5rem 2rem;
  border-bottom: 1px solid var(--line-soft);
  transition: background var(--transition-fast);
}

.settings-row:last-child {
  border-bottom: 0;
}

.settings-row:hover {
  background: var(--surface-control-hover);
}

.settings-row__label {
  display: block;
  margin-bottom: 0.25rem;
  color: var(--text-heading);
  font-size: 0.94rem;
  font-weight: 700;
}

.settings-row__description {
  font-size: 0.875rem;
  font-weight: 500;
}

@media (max-width: 640px) {
  .settings-page {
    padding: 1rem;
  }

  .settings-section,
  .settings-row {
    padding-left: 1.25rem;
    padding-right: 1.25rem;
  }
}
</style>
