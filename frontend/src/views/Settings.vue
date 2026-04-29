<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getSettings, updateSettings } from '@/api/settings/index.js'
import { useAuthStore } from '@/stores/useAuthStore'
import { usePlannerStore } from '@/stores/planner'
import { useUserSettingsStore } from '@/stores/userSettings'

const route = useRoute()
const router = useRouter()
const plannerStore = usePlannerStore()
const authStore = useAuthStore()
const userSettingsStore = useUserSettingsStore()

const tabs = [
  {
    id: 'profile',
    label: '프로필',
    icon: 'badge',
    summary: '개인 정보와 프로필 이미지를 관리합니다.',
  },
  {
    id: 'notifications',
    label: '알림',
    icon: 'notifications',
    summary: '업무, QA, AI 분석 알림을 설정합니다.',
  },
  {
    id: 'theme',
    label: '테마/UI',
    icon: 'contrast',
    summary: '라이트/다크 모드와 화면 표시를 조정합니다.',
  },
  {
    id: 'security',
    label: '계정/보안',
    icon: 'lock',
    summary: '로그인 계정과 보안 진입 정보를 확인합니다.',
  },
]

const notificationItems = [
  {
    key: 'task',
    label: '업무 알림',
    description: '업무 생성, 배정, 상태 변경 알림',
  },
  {
    key: 'qa',
    label: 'QA 알림',
    description: '검수 결과, 수정 요청, 반려 알림',
  },
  {
    key: 'ai',
    label: 'AI 분석 알림',
    description: '리스크 감지, 가이드, 분석 결과 알림',
  },
  {
    key: 'critical',
    label: '중요 알림',
    description: '마감 임박, 업무 지연 등 필수 알림',
    locked: true,
  },
]

const densityOptions = [
  { value: 'comfortable', label: '기본' },
  { value: 'compact', label: '촘촘하게' },
]

const tabIds = new Set(tabs.map((tab) => tab.id))
const profileForm = reactive({
  name: '',
  company: '',
  department: '',
  role: '',
  phone: '',
  email: '',
  imageDataUrl: '',
})
const feedback = reactive({
  profile: '',
  profileError: '',
})
const isImageGenerationModalOpen = ref(false)
const imageGenerationPrompt = ref('')

const activeTab = computed(() => {
  const requestedTab = String(route.query.tab || 'profile')

  return tabIds.has(requestedTab) ? requestedTab : 'profile'
})

const currentTab = computed(() => tabs.find((tab) => tab.id === activeTab.value) ?? tabs[0])
const isDarkMode = computed(() => plannerStore.theme === 'dark')
const accountEmail = computed(() => profileForm.email || userSettingsStore.profile.email)
const userKey = computed(() => resolveUserKey(authStore.user))

function resolveUserKey(user) {
  return (
    user?.userId ??
    user?.idx ??
    user?.id ??
    user?.loginId ??
    user?.email ??
    user?.sub ??
    plannerStore.currentUserId ??
    'guest'
  )
}

function normalizeTheme(value) {
  return value === 'light' || value === 'dark' ? value : null
}

function resolveSettingsPayload(payload) {
  return payload?.result ?? payload?.data ?? payload ?? {}
}

function syncProfileForm() {
  Object.assign(profileForm, {
    name: userSettingsStore.profile.name,
    company: userSettingsStore.profile.company,
    department: userSettingsStore.profile.department,
    role: userSettingsStore.profile.role,
    phone: userSettingsStore.profile.phone,
    email: userSettingsStore.profile.email,
    imageDataUrl: userSettingsStore.profile.imageDataUrl,
  })
}

function selectTab(tabId) {
  router.replace({
    query: {
      ...route.query,
      tab: tabId,
    },
  })
}

function applyRemoteSettings(payload) {
  const source = resolveSettingsPayload(payload)
  const nextTheme =
    normalizeTheme(source.theme) ??
    (typeof source.darkMode === 'boolean' ? (source.darkMode ? 'dark' : 'light') : null)

  if (nextTheme) {
    plannerStore.setTheme(nextTheme)
    userSettingsStore.updateThemeUi({ theme: nextTheme })
  }

  notificationItems.forEach((item) => {
    const remoteValue = source.notifications?.[item.key] ?? source[item.key]

    if (typeof remoteValue === 'boolean') {
      userSettingsStore.updateNotification(item.key, remoteValue)
    }
  })
}

async function syncSettingToServer(body) {
  try {
    await updateSettings(body)
  } catch (error) {
    console.warn('설정 저장 API 호출에 실패했습니다. 로컬 설정은 유지됩니다.', error)
  }
}

function setTheme(nextTheme) {
  plannerStore.setTheme(nextTheme)
  userSettingsStore.updateThemeUi({ theme: nextTheme })

  void syncSettingToServer({
    theme: nextTheme,
    darkMode: nextTheme === 'dark',
  })
}

function toggleNotification(item) {
  if (item.locked) {
    return
  }

  const nextValue = !userSettingsStore.notifications[item.key]
  userSettingsStore.updateNotification(item.key, nextValue)

  void syncSettingToServer({
    [item.key]: nextValue,
    notifications: {
      [item.key]: nextValue,
    },
  })
}

function setDensity(value) {
  userSettingsStore.updateThemeUi({ density: value })
}

function toggleThemeUiValue(key) {
  userSettingsStore.updateThemeUi({
    [key]: !userSettingsStore.themeUi[key],
  })
}

function handleProfileImageUpload(event) {
  const file = event.target.files?.[0]

  if (!file) {
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    profileForm.imageDataUrl = String(reader.result || '')
  }
  reader.readAsDataURL(file)
  event.target.value = ''
}

function clearProfileImage() {
  profileForm.imageDataUrl = ''
}

function openImageGenerationModal() {
  imageGenerationPrompt.value = userSettingsStore.generatorPrompt
  isImageGenerationModalOpen.value = true
}

function closeImageGenerationModal() {
  isImageGenerationModalOpen.value = false
}

function requestImageGeneration() {
  const prompt = imageGenerationPrompt.value.trim()

  if (!prompt) {
    return
  }

  userSettingsStore.setGeneratorPrompt(prompt)
  userSettingsStore.markImageGenerationReady()
  feedback.profile = '프로필 이미지 생성 프롬프트가 저장되었습니다.'
  feedback.profileError = ''
  closeImageGenerationModal()
}

function saveProfile() {
  feedback.profile = ''
  feedback.profileError = ''

  const email = profileForm.email.trim()
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  if (email && !emailPattern.test(email)) {
    feedback.profileError = '올바른 이메일 형식으로 입력해 주세요.'
    return
  }

  userSettingsStore.updateProfile({
    ...profileForm,
    email,
    phone: profileForm.phone.trim(),
  })
  syncProfileForm()
  feedback.profile = '프로필 정보가 저장되었습니다.'
}

watch(
  () => [userKey.value, authStore.user],
  () => {
    userSettingsStore.loadUserSettings(userKey.value, authStore.user)
    syncProfileForm()
  },
  { immediate: true, deep: true },
)

watch(
  () => ({ ...userSettingsStore.profile }),
  () => {
    syncProfileForm()
  },
  { deep: true },
)

watch(
  () => plannerStore.theme,
  (nextTheme) => {
    userSettingsStore.updateThemeUi({ theme: nextTheme })
  },
  { immediate: true },
)

watch(
  () => authStore.isAuthenticated,
  async (isAuthenticated) => {
    if (!isAuthenticated) {
      return
    }

    try {
      const response = await getSettings()
      applyRemoteSettings(response.data)
    } catch (error) {
      console.warn('설정 정보를 불러오지 못해 로컬 설정을 사용합니다.', error)
    }
  },
  { immediate: true },
)
</script>

<template>
  <section class="settings-page ui-page">
    <header class="settings-header">
      <div>
        <p class="settings-eyebrow">SETTING_001</p>
        <h2 class="settings-heading">환경설정</h2>
        <p class="settings-subtitle">현재 적용된 개인 환경 설정을 확인하고 바로 변경합니다.</p>
      </div>
      <div class="settings-status" :class="{ 'is-dark': isDarkMode }">
        <span class="settings-status__dot" />
        <span>{{ isDarkMode ? '다크 모드' : '라이트 모드' }}</span>
      </div>
    </header>

    <div class="settings-shell">
      <nav class="settings-tabs" aria-label="환경설정 메뉴">
        <button
          v-for="tab in tabs"
          :key="tab.id"
          type="button"
          class="settings-tab"
          :class="{ 'is-active': activeTab === tab.id }"
          :aria-current="activeTab === tab.id ? 'page' : undefined"
          @click="selectTab(tab.id)"
        >
          <span class="material-symbols-outlined settings-tab__icon">{{ tab.icon }}</span>
          <span>
            <strong>{{ tab.label }}</strong>
            <small>{{ tab.summary }}</small>
          </span>
        </button>
      </nav>

      <article class="settings-panel ui-card">
        <div class="settings-panel__head ui-card-header">
          <div>
            <p class="settings-eyebrow">{{ currentTab.label }}</p>
            <h3>{{ currentTab.summary }}</h3>
          </div>
        </div>

        <form v-if="activeTab === 'profile'" class="settings-pane" @submit.prevent="saveProfile">
          <section class="settings-profile">
            <div class="profile-preview">
              <div class="profile-preview__image">
                <img v-if="profileForm.imageDataUrl" :src="profileForm.imageDataUrl" alt="" />
                <span v-else>{{ userSettingsStore.profileInitials }}</span>
              </div>
              <div>
                <strong>{{ profileForm.name || 'Callog User' }}</strong>
                <p>{{ profileForm.company }} · {{ profileForm.department }}</p>
              </div>
            </div>

            <div class="profile-actions">
              <button
                type="button"
                class="settings-button settings-button--ghost"
                @click="openImageGenerationModal"
              >
                이미지 생성
              </button>
              <label class="settings-button settings-button--ghost">
                이미지 선택
                <input
                  type="file"
                  accept="image/*"
                  class="settings-file"
                  @change="handleProfileImageUpload"
                />
              </label>
              <button
                type="button"
                class="settings-button settings-button--ghost"
                @click="clearProfileImage"
              >
                이미지 제거
              </button>
            </div>
          </section>

          <section class="settings-form-grid" aria-label="프로필 정보">
            <label class="settings-field">
              <span>이름</span>
              <input v-model.trim="profileForm.name" type="text" autocomplete="name" />
            </label>
            <label class="settings-field">
              <span>이메일</span>
              <input v-model.trim="profileForm.email" type="email" autocomplete="email" />
            </label>
            <label class="settings-field">
              <span>전화번호</span>
              <input v-model.trim="profileForm.phone" type="tel" autocomplete="tel" />
            </label>
            <label class="settings-field">
              <span>회사</span>
              <input v-model.trim="profileForm.company" type="text" />
            </label>
            <label class="settings-field">
              <span>부서</span>
              <input v-model.trim="profileForm.department" type="text" />
            </label>
            <label class="settings-field">
              <span>역할</span>
              <input v-model.trim="profileForm.role" type="text" />
            </label>
          </section>

          <footer class="settings-actions">
            <p v-if="feedback.profileError" class="settings-error">{{ feedback.profileError }}</p>
            <p v-else-if="feedback.profile" class="settings-success">{{ feedback.profile }}</p>
            <button type="submit" class="settings-button settings-button--primary">저장</button>
          </footer>
        </form>

        <div v-else-if="activeTab === 'notifications'" class="settings-pane">
          <div class="settings-list">
            <div v-for="item in notificationItems" :key="item.key" class="settings-row">
              <div>
                <strong>{{ item.label }}</strong>
                <p>{{ item.description }}</p>
              </div>
              <button
                type="button"
                class="ui-toggle"
                :class="{ 'is-active': userSettingsStore.notifications[item.key] }"
                :aria-pressed="userSettingsStore.notifications[item.key]"
                :aria-label="`${item.label} 수신 설정`"
                :disabled="item.locked"
                @click="toggleNotification(item)"
              >
                <span class="ui-toggle-thumb" />
              </button>
            </div>
          </div>
        </div>

        <div v-else-if="activeTab === 'theme'" class="settings-pane">
          <section class="settings-block">
            <div>
              <strong>테마</strong>
              <p>선택 즉시 전체 UI에 반영됩니다.</p>
            </div>
            <div class="settings-segmented" role="group" aria-label="테마 선택">
              <button
                type="button"
                :class="{ 'is-active': !isDarkMode }"
                @click="setTheme('light')"
              >
                라이트
              </button>
              <button type="button" :class="{ 'is-active': isDarkMode }" @click="setTheme('dark')">
                다크
              </button>
            </div>
          </section>

          <section class="settings-block">
            <div>
              <strong>화면 밀도</strong>
              <p>반복 작업에 맞춰 정보 간격을 조정합니다.</p>
            </div>
            <div class="settings-segmented" role="group" aria-label="화면 밀도 선택">
              <button
                v-for="option in densityOptions"
                :key="option.value"
                type="button"
                :class="{ 'is-active': userSettingsStore.themeUi.density === option.value }"
                @click="setDensity(option.value)"
              >
                {{ option.label }}
              </button>
            </div>
          </section>

          <section class="settings-list">
            <div class="settings-row">
              <div>
                <strong>모션 줄이기</strong>
                <p>전환 애니메이션을 더 차분하게 표시합니다.</p>
              </div>
              <button
                type="button"
                class="ui-toggle"
                :class="{ 'is-active': userSettingsStore.themeUi.reduceMotion }"
                :aria-pressed="userSettingsStore.themeUi.reduceMotion"
                aria-label="모션 줄이기 설정"
                @click="toggleThemeUiValue('reduceMotion')"
              >
                <span class="ui-toggle-thumb" />
              </button>
            </div>
            <div class="settings-row">
              <div>
                <strong>고대비 표시</strong>
                <p>텍스트와 경계 대비를 높이는 표시 옵션입니다.</p>
              </div>
              <button
                type="button"
                class="ui-toggle"
                :class="{ 'is-active': userSettingsStore.themeUi.highContrast }"
                :aria-pressed="userSettingsStore.themeUi.highContrast"
                aria-label="고대비 표시 설정"
                @click="toggleThemeUiValue('highContrast')"
              >
                <span class="ui-toggle-thumb" />
              </button>
            </div>
          </section>
        </div>

        <div v-else class="settings-pane">
          <section class="settings-account">
            <div class="settings-account__row">
              <span>로그인 계정</span>
              <strong>{{ accountEmail }}</strong>
            </div>
            <div class="settings-account__row">
              <span>권한</span>
              <strong>{{ userSettingsStore.profile.role }}</strong>
            </div>
            <div class="settings-account__row">
              <span>세션 상태</span>
              <strong>{{ userSettingsStore.security.sessionStatus }}</strong>
            </div>
            <div class="settings-account__row">
              <span>비밀번호</span>
              <strong>별도 기능에서 변경</strong>
            </div>
          </section>
        </div>
      </article>
    </div>

    <Teleport to="body">
      <Transition name="settings-modal">
        <div
          v-if="isImageGenerationModalOpen"
          class="settings-modal"
          role="presentation"
          @click.self="closeImageGenerationModal"
        >
          <form
            class="settings-modal__panel"
            aria-label="프로필 이미지 생성"
            @submit.prevent="requestImageGeneration"
          >
            <header class="settings-modal__header">
              <div>
                <p class="settings-eyebrow">240 x 240</p>
                <h3>프로필 이미지 생성</h3>
              </div>
              <button
                type="button"
                class="settings-modal__close"
                aria-label="닫기"
                @click="closeImageGenerationModal"
              >
                ×
              </button>
            </header>

            <div class="settings-modal__body">
              <div class="settings-modal__preview">
                <span class="material-symbols-outlined">auto_awesome</span>
                <strong>240</strong>
                <small>PNG</small>
              </div>
              <label class="settings-field">
                <span>간단한 프롬프트</span>
                <input
                  v-model.trim="imageGenerationPrompt"
                  type="text"
                  maxlength="80"
                  placeholder="예: 차분한 B2B 마케팅 리드"
                  autofocus
                />
              </label>
              <p class="settings-modal__note">
                현재는 이미지 생성 API 연동 전 준비 단계이며, 입력한 문구만 저장됩니다.
              </p>
            </div>

            <footer class="settings-modal__actions">
              <button
                type="button"
                class="settings-button settings-button--ghost"
                @click="closeImageGenerationModal"
              >
                취소
              </button>
              <button
                type="submit"
                class="settings-button settings-button--primary"
                :disabled="!imageGenerationPrompt.trim()"
              >
                생성 준비
              </button>
            </footer>
          </form>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<style scoped>
.settings-page {
  display: grid;
  width: min(1180px, 100%);
  max-width: 1180px;
  gap: 16px;
  margin: 0 auto;
  padding: 24px;
  scrollbar-gutter: stable;
}

.settings-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 2px;
}

.settings-eyebrow {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0;
}

.settings-heading {
  margin-top: 4px;
  color: var(--text-heading);
  font-size: 24px;
  font-weight: 800;
}

.settings-subtitle {
  margin-top: 6px;
  color: var(--text-muted);
  font-size: 14px;
}

.settings-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  color: var(--text-body);
  font-size: 13px;
  font-weight: 700;
}

.settings-status__dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--warning-color);
}

.settings-status.is-dark .settings-status__dot {
  background: var(--accent-color);
}

.settings-shell {
  display: grid;
  grid-template-columns: 252px minmax(0, 1fr);
  align-items: start;
  gap: 16px;
}

.settings-tabs {
  display: grid;
  align-self: start;
  gap: 4px;
  padding: 8px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  box-shadow: var(--shadow-soft);
}

.settings-tab {
  display: grid;
  position: relative;
  grid-template-columns: 28px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 76px;
  padding: 10px;
  border-radius: var(--radius-sm);
  color: var(--text-body);
  text-align: left;
  cursor: pointer;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.settings-tab::before {
  position: absolute;
  top: 10px;
  bottom: 10px;
  left: 0;
  width: 3px;
  border-radius: 0 999px 999px 0;
  background: transparent;
  content: '';
}

.settings-tab:hover,
.settings-tab.is-active {
  background: var(--surface-control-hover);
  color: var(--text-heading);
}

.settings-tab.is-active {
  box-shadow: none;
}

.settings-tab.is-active::before {
  background: var(--accent-color);
}

.settings-tab__icon {
  width: 28px;
  height: 28px;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
  font-size: 20px;
}

.settings-tab strong,
.settings-tab small {
  display: block;
}

.settings-tab > span:last-child {
  min-width: 0;
}

.settings-tab strong {
  font-size: 14px;
  font-weight: 800;
}

.settings-tab small {
  margin-top: 2px;
  color: var(--text-muted);
  font-size: 12px;
  line-height: 1.35;
}

.settings-panel {
  min-width: 0;
  min-height: 560px;
  border-radius: var(--radius-md);
}

.settings-panel__head {
  display: flex;
  height: 88px;
  align-items: center;
  overflow: hidden;
  padding: 18px 20px;
}

.settings-panel__head > div {
  display: grid;
  width: 100%;
  min-width: 0;
  grid-template-rows: 16px 25px;
  align-content: center;
}

.settings-panel__head .settings-eyebrow {
  overflow: hidden;
  line-height: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.settings-panel__head h3 {
  margin-top: 4px;
  overflow: hidden;
  color: var(--text-heading);
  font-size: 18px;
  font-weight: 800;
  line-height: 25px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.settings-pane {
  display: grid;
  gap: 18px;
  padding: 20px;
}

.settings-profile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line-soft);
}

.profile-preview {
  display: flex;
  align-items: center;
  min-width: 0;
  gap: 14px;
}

.profile-preview__image {
  display: inline-flex;
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--badge-bg);
  color: var(--badge-text);
  font-size: 22px;
  font-weight: 800;
}

.profile-preview__image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-preview strong {
  display: block;
  color: var(--text-heading);
  font-size: 18px;
  font-weight: 800;
}

.profile-preview p,
.settings-row p,
.settings-block p {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 13px;
}

.profile-actions,
.settings-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.settings-file {
  display: none;
}

.settings-form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.settings-field {
  display: grid;
  gap: 7px;
  color: var(--text-body);
  font-size: 13px;
  font-weight: 700;
}

.settings-field input {
  width: 100%;
  min-height: 40px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-control);
  padding: 0 12px;
  color: var(--text-heading);
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast);
}

.settings-field input:focus {
  border-color: var(--accent-color);
  background: var(--control-focus-color);
  outline: none;
}

.settings-block {
  display: grid;
  gap: 12px;
  padding: 16px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card-muted);
}

.settings-block strong,
.settings-row strong {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 800;
}

.settings-message,
.settings-success,
.settings-error {
  font-size: 13px;
  font-weight: 700;
}

.settings-message {
  color: var(--text-muted);
}

.settings-success {
  color: var(--success-color);
}

.settings-error {
  color: var(--danger-text-strong);
}

.settings-actions {
  justify-content: flex-end;
  min-height: 40px;
}

.settings-actions p {
  margin-right: auto;
}

.settings-button {
  display: inline-flex;
  min-height: 38px;
  align-items: center;
  justify-content: center;
  gap: 6px;
  border-radius: var(--radius-md);
  padding: 0 14px;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    color var(--transition-fast);
}

.settings-button--primary {
  border: 1px solid var(--accent-strong);
  background: var(--accent-strong);
  color: #ffffff;
}

.settings-button--ghost {
  border: 1px solid var(--line-soft);
  background: var(--surface-control);
  color: var(--text-heading);
}

.settings-button--ghost:hover {
  border-color: var(--line-strong);
  background: var(--surface-control-hover);
}

.settings-modal {
  position: fixed;
  z-index: 10020;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(8, 13, 22, 0.58);
  backdrop-filter: blur(8px);
}

.settings-modal__panel {
  width: min(440px, 100%);
  overflow: hidden;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  box-shadow: var(--shadow-elevated);
  color: var(--text-body);
}

.settings-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--line-soft);
  background: var(--surface-card-muted);
}

.settings-modal__header h3 {
  margin-top: 4px;
  color: var(--text-heading);
  font-size: 18px;
  font-weight: 800;
}

.settings-modal__close {
  display: inline-flex;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-control);
  color: var(--text-heading);
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
}

.settings-modal__body {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.settings-modal__preview {
  display: grid;
  width: 240px;
  height: 240px;
  place-items: center;
  align-content: center;
  justify-self: center;
  gap: 4px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-md);
  background: var(--surface-card-muted);
  color: var(--text-muted);
}

.settings-modal__preview .material-symbols-outlined {
  color: var(--accent-color);
  font-size: 34px;
}

.settings-modal__preview strong {
  color: var(--text-heading);
  font-size: 28px;
  font-weight: 800;
}

.settings-modal__preview small {
  font-size: 12px;
  font-weight: 800;
}

.settings-modal__note {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 600;
}

.settings-modal__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 20px 18px;
  border-top: 1px solid var(--line-soft);
}

.settings-modal-enter-active,
.settings-modal-leave-active {
  transition: opacity var(--transition-fast);
}

.settings-modal-enter-from,
.settings-modal-leave-to {
  opacity: 0;
}

.settings-modal-enter-active .settings-modal__panel,
.settings-modal-leave-active .settings-modal__panel {
  transition: transform var(--transition-fast);
}

.settings-modal-enter-from .settings-modal__panel,
.settings-modal-leave-to .settings-modal__panel {
  transform: translateY(8px) scale(0.98);
}

.settings-list {
  display: grid;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.settings-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 74px;
  padding: 14px 16px;
  border-bottom: 1px solid var(--line-soft);
  background: var(--surface-card);
}

.settings-row:last-child {
  border-bottom: 0;
}

.settings-segmented {
  display: inline-grid;
  grid-auto-flow: column;
  align-self: start;
  overflow: hidden;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-control);
}

.settings-segmented button {
  min-width: 96px;
  min-height: 38px;
  padding: 0 14px;
  color: var(--text-body);
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
}

.settings-segmented button.is-active {
  background: var(--accent-strong);
  color: #ffffff;
}

.settings-account {
  display: grid;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.settings-account__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 58px;
  padding: 0 16px;
  border-bottom: 1px solid var(--line-soft);
}

.settings-account__row:last-child {
  border-bottom: 0;
}

.settings-account__row span {
  color: var(--text-muted);
  font-size: 13px;
  font-weight: 700;
}

.settings-account__row strong {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 800;
  text-align: right;
}

@media (max-width: 960px) {
  .settings-shell {
    grid-template-columns: 1fr;
  }

  .settings-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .settings-page {
    padding: 16px;
  }

  .settings-header,
  .settings-profile,
  .settings-row,
  .settings-account__row {
    align-items: stretch;
    flex-direction: column;
  }

  .settings-status,
  .settings-segmented,
  .settings-actions,
  .profile-actions {
    width: 100%;
  }

  .settings-tabs,
  .settings-form-grid {
    grid-template-columns: 1fr;
  }

  .settings-actions {
    justify-content: stretch;
  }

  .settings-actions p {
    margin-right: 0;
  }

  .settings-button,
  .settings-segmented button {
    width: 100%;
  }

  .settings-modal {
    align-items: flex-start;
    padding: 16px;
  }

  .settings-modal__preview {
    width: min(240px, 100%);
    height: auto;
    aspect-ratio: 1;
  }

  .settings-modal__actions {
    flex-direction: column-reverse;
  }
}
</style>
