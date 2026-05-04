<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { changePasswordRequest } from '@/authApi'
import { getMyProfile, updateMyProfile } from '@/api/userProfiles/index.js'
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
    id: 'theme',
    label: '테마/UI',
    icon: 'contrast',
    summary: '라이트/다크 모드와 화면 표시를 조정합니다.',
  },
  {
    id: 'security',
    label: '계정/보안',
    icon: 'lock',
    summary: '세션과 비밀번호를 직접 관리합니다.',
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
  companyLogoDataUrl: '',
})
const feedback = reactive({
  profile: '',
  profileError: '',
})
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})
const securityFeedback = reactive({
  success: '',
  error: '',
})
const isImageGenerationModalOpen = ref(false)
const imageGenerationPrompt = ref('')
const isRefreshingSession = ref(false)
const isLoggingOut = ref(false)
const isChangingPassword = ref(false)

const activeTab = computed(() => {
  const requestedTab = String(route.query.tab || 'profile')

  return tabIds.has(requestedTab) ? requestedTab : 'profile'
})
const currentTab = computed(() => tabs.find((tab) => tab.id === activeTab.value) ?? tabs[0])
const isDarkMode = computed(() => plannerStore.theme === 'dark')
const accountEmail = computed(() => profileForm.email || userSettingsStore.profile.email)
const userKey = computed(() => resolveUserKey(authStore.user))
const accountId = computed(
  () => readFirstString(authStore.user, ['id', 'loginId', 'userId', 'sub']) || '-',
)
const accountName = computed(
  () => profileForm.name || readFirstString(authStore.user, ['name', 'username']) || 'Callog User',
)
const accountRole = computed(
  () =>
    profileForm.role || readFirstString(authStore.user, ['roleName', 'role', 'authority']) || '-',
)
const sessionStatus = computed(() => (authStore.hasFreshAccessToken() ? '활성' : '갱신 필요'))
const themePreviewTitle = computed(() =>
  isDarkMode.value ? '다크 모드 미리보기' : '라이트 모드 미리보기',
)
const isPasswordPolicyValid = computed(() => passwordPolicyChecks.value.every((item) => item.valid))

const passwordPolicyChecks = computed(() => {
  const value = passwordForm.newPassword

  return [
    { id: 'length', label: '8~20자', valid: value.length >= 8 && value.length <= 20 },
    { id: 'upper', label: '대문자 포함', valid: /[A-Z]/.test(value) },
    { id: 'lower', label: '소문자 포함', valid: /[a-z]/.test(value) },
    { id: 'digit', label: '숫자 포함', valid: /\d/.test(value) },
    { id: 'special', label: '특수문자 포함', valid: /[^A-Za-z0-9]/.test(value) },
    { id: 'space', label: '공백 없음', valid: value.length > 0 && !/\s/.test(value) },
  ]
})

function readFirstString(source, keys) {
  if (!source || typeof source !== 'object') {
    return ''
  }

  return (
    keys
      .map((key) => source[key])
      .find((value) => typeof value === 'string' && value.trim())
      ?.trim() ?? ''
  )
}

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

function resolveProfilePayload(payload) {
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
    companyLogoDataUrl: userSettingsStore.profile.companyLogoDataUrl,
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

function applyRemoteProfile(payload) {
  const source = resolveProfilePayload(payload)
  const nextProfile = {
    name: source.name,
    email: source.email,
    phone: source.phone,
    imageDataUrl: source.profileImageUrl,
  }

  Object.keys(nextProfile).forEach((key) => {
    if (nextProfile[key] === undefined || nextProfile[key] === null) {
      delete nextProfile[key]
    }
  })

  userSettingsStore.updateProfile(nextProfile)
}

function setTheme(nextTheme) {
  plannerStore.setTheme(nextTheme)
}

function setDensity(value) {
  userSettingsStore.updateThemeUi({ density: value })
}

function toggleThemeUiValue(key) {
  userSettingsStore.updateThemeUi({
    [key]: !userSettingsStore.themeUi[key],
  })
}

function resetDisplaySettings() {
  plannerStore.setTheme('light')
  userSettingsStore.resetThemeUi()
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

function handleCompanyLogoUpload(event) {
  const file = event.target.files?.[0]

  if (!file) {
    return
  }

  const reader = new FileReader()
  reader.onload = () => {
    profileForm.companyLogoDataUrl = String(reader.result || '')
  }
  reader.readAsDataURL(file)
  event.target.value = ''
}

function clearCompanyLogo() {
  profileForm.companyLogoDataUrl = ''
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

function resolveProfileImagePayload() {
  if (!profileForm.imageDataUrl || profileForm.imageDataUrl.startsWith('data:')) {
    return {}
  }

  return {
    profileImageKey: null,
    profileImageUrl: profileForm.imageDataUrl,
  }
}

async function saveProfile() {
  feedback.profile = ''
  feedback.profileError = ''

  const email = profileForm.email.trim()
  const phone = profileForm.phone.trim()
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

  if (email && !emailPattern.test(email)) {
    feedback.profileError = '올바른 이메일 형식으로 입력해 주세요.'
    return
  }

  try {
    const response = await updateMyProfile({
      email,
      phone,
      ...resolveProfileImagePayload(),
    })

    applyRemoteProfile(response.data)
    userSettingsStore.updateProfile({
      ...profileForm,
      email,
      phone,
    })
    syncProfileForm()
    feedback.profile = '프로필 정보가 저장되었습니다.'
  } catch (error) {
    console.error('프로필 저장 API 호출에 실패했습니다.', error)
    feedback.profileError = '프로필 정보를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  }
}

async function loadRemoteProfile() {
  try {
    const response = await getMyProfile()
    applyRemoteProfile(response.data)
    syncProfileForm()
  } catch (error) {
    console.warn('프로필 정보를 불러오지 못해 로컬 설정을 사용합니다.', error)
  }
}

async function refreshSession() {
  securityFeedback.success = ''
  securityFeedback.error = ''
  isRefreshingSession.value = true

  try {
    const refreshed = await authStore.refreshSession()

    if (!refreshed) {
      securityFeedback.error = '세션을 갱신하지 못했습니다. 다시 로그인해 주세요.'
      await router.replace({ name: 'login' })
      return
    }

    securityFeedback.success = '세션이 갱신되었습니다.'
  } finally {
    isRefreshingSession.value = false
  }
}

async function logout() {
  securityFeedback.success = ''
  securityFeedback.error = ''
  isLoggingOut.value = true

  try {
    await authStore.logout()
    await router.replace({ name: 'login' })
  } finally {
    isLoggingOut.value = false
  }
}

function validatePasswordForm() {
  if (!passwordForm.currentPassword || !passwordForm.newPassword || !passwordForm.confirmPassword) {
    return '현재 비밀번호와 새 비밀번호를 모두 입력해 주세요.'
  }

  if (!isPasswordPolicyValid.value) {
    return '새 비밀번호 정책을 모두 만족해야 합니다.'
  }

  if (passwordForm.currentPassword === passwordForm.newPassword) {
    return '새 비밀번호는 현재 비밀번호와 달라야 합니다.'
  }

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    return '새 비밀번호 확인이 일치하지 않습니다.'
  }

  return ''
}

async function changePassword() {
  securityFeedback.success = ''
  securityFeedback.error = ''

  const validationMessage = validatePasswordForm()

  if (validationMessage) {
    securityFeedback.error = validationMessage
    return
  }

  isChangingPassword.value = true

  try {
    await changePasswordRequest({
      currentPassword: passwordForm.currentPassword,
      newPassword: passwordForm.newPassword,
    })

    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    securityFeedback.success = '비밀번호가 변경되었습니다. 다시 로그인해 주세요.'
    await authStore.logout()
    await router.replace({ name: 'login' })
  } catch (error) {
    console.error('비밀번호 변경 API 호출에 실패했습니다.', error)
    securityFeedback.error = error?.message || '비밀번호를 변경하지 못했습니다.'
  } finally {
    isChangingPassword.value = false
  }
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
  () => authStore.isAuthenticated,
  async (isAuthenticated) => {
    if (isAuthenticated) {
      await loadRemoteProfile()
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

          <section class="settings-logo-panel">
            <div class="company-logo-preview">
              <div class="company-logo-preview__image">
                <img
                  v-if="profileForm.companyLogoDataUrl"
                  :src="profileForm.companyLogoDataUrl"
                  alt=""
                />
                <span v-else class="material-symbols-outlined">business</span>
              </div>
              <div>
                <strong>회사 로고</strong>
                <p>명함 다운로드 이미지에 함께 표시됩니다.</p>
              </div>
            </div>

            <div class="profile-actions">
              <label class="settings-button settings-button--ghost">
                로고 선택
                <input
                  type="file"
                  accept="image/*"
                  class="settings-file"
                  @change="handleCompanyLogoUpload"
                />
              </label>
              <button
                type="button"
                class="settings-button settings-button--ghost"
                @click="clearCompanyLogo"
              >
                로고 제거
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

        <div v-else-if="activeTab === 'theme'" class="settings-pane">
          <section class="settings-block settings-block--inline">
            <div>
              <strong>테마</strong>
              <p>선택 즉시 전체 UI에 반영되며 브라우저 로컬 저장소에 유지됩니다.</p>
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

          <section class="settings-block settings-block--inline">
            <div>
              <strong>화면 밀도</strong>
              <p>대시보드와 설정 카드의 여백, 행 높이, 버튼 높이를 조정합니다.</p>
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
                <p>전환 애니메이션을 최소화해 반복 작업 중 움직임 부담을 줄입니다.</p>
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
                <p>텍스트와 경계 대비를 높여 카드와 입력 영역을 더 또렷하게 표시합니다.</p>
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

          <section class="settings-preview">
            <div class="settings-preview__head">
              <div>
                <strong>{{ themePreviewTitle }}</strong>
                <p>현재 화면 설정이 카드, 행, 버튼에 어떻게 반영되는지 확인합니다.</p>
              </div>
              <button
                type="button"
                class="settings-button settings-button--ghost"
                @click="resetDisplaySettings"
              >
                화면 설정 초기화
              </button>
            </div>
            <div class="settings-preview__body">
              <div class="settings-preview__card">
                <span class="settings-chip">작업 카드</span>
                <strong>콘텐츠 검수 요청</strong>
                <p>밀도, 고대비, 모션 설정은 새로고침 후에도 유지됩니다.</p>
              </div>
              <div class="settings-preview__list">
                <span>상태</span>
                <strong>{{
                  userSettingsStore.themeUi.density === 'compact' ? '촘촘하게' : '기본'
                }}</strong>
              </div>
              <div class="settings-preview__list">
                <span>표시 옵션</span>
                <strong>
                  {{ userSettingsStore.themeUi.highContrast ? '고대비' : '표준 대비' }} ·
                  {{ userSettingsStore.themeUi.reduceMotion ? '모션 최소화' : '표준 모션' }}
                </strong>
              </div>
            </div>
          </section>
        </div>

        <div v-else class="settings-pane">
          <section class="settings-account">
            <div class="settings-account__row">
              <span>로그인 ID</span>
              <strong>{{ accountId }}</strong>
            </div>
            <div class="settings-account__row">
              <span>이름</span>
              <strong>{{ accountName }}</strong>
            </div>
            <div class="settings-account__row">
              <span>이메일</span>
              <strong>{{ accountEmail }}</strong>
            </div>
            <div class="settings-account__row">
              <span>권한</span>
              <strong>{{ accountRole }}</strong>
            </div>
            <div class="settings-account__row">
              <span>세션 상태</span>
              <strong>{{ sessionStatus }}</strong>
            </div>
          </section>

          <section class="settings-security-actions">
            <button
              type="button"
              class="settings-button settings-button--ghost"
              :disabled="isRefreshingSession"
              @click="refreshSession"
            >
              {{ isRefreshingSession ? '갱신 중' : '세션 갱신' }}
            </button>
            <button
              type="button"
              class="settings-button settings-button--danger"
              :disabled="isLoggingOut"
              @click="logout"
            >
              {{ isLoggingOut ? '로그아웃 중' : '로그아웃' }}
            </button>
          </section>

          <form class="settings-password" @submit.prevent="changePassword">
            <div>
              <strong>내 비밀번호 변경</strong>
              <p>변경이 완료되면 보안을 위해 다시 로그인해야 합니다.</p>
            </div>

            <section class="settings-form-grid">
              <label class="settings-field">
                <span>현재 비밀번호</span>
                <input
                  v-model="passwordForm.currentPassword"
                  type="password"
                  autocomplete="current-password"
                />
              </label>
              <label class="settings-field">
                <span>새 비밀번호</span>
                <input
                  v-model="passwordForm.newPassword"
                  type="password"
                  autocomplete="new-password"
                />
              </label>
              <label class="settings-field">
                <span>새 비밀번호 확인</span>
                <input
                  v-model="passwordForm.confirmPassword"
                  type="password"
                  autocomplete="new-password"
                />
              </label>
            </section>

            <ul class="settings-policy" aria-label="비밀번호 정책">
              <li
                v-for="item in passwordPolicyChecks"
                :key="item.id"
                :class="{ 'is-valid': item.valid }"
              >
                <span class="material-symbols-outlined">{{
                  item.valid ? 'check_circle' : 'radio_button_unchecked'
                }}</span>
                {{ item.label }}
              </li>
            </ul>

            <footer class="settings-actions">
              <p v-if="securityFeedback.error" class="settings-error">
                {{ securityFeedback.error }}
              </p>
              <p v-else-if="securityFeedback.success" class="settings-success">
                {{ securityFeedback.success }}
              </p>
              <button
                type="submit"
                class="settings-button settings-button--primary"
                :disabled="isChangingPassword"
              >
                {{ isChangingPassword ? '변경 중' : '비밀번호 변경' }}
              </button>
            </footer>
          </form>
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
  gap: var(--ui-section-gap);
  margin: 0 auto;
  padding: var(--ui-page-padding);
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
  padding: var(--ui-card-padding);
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

.settings-logo-panel,
.settings-preview,
.settings-password {
  display: grid;
  gap: 16px;
  padding: 16px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card-muted);
}

.settings-logo-panel {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.company-logo-preview {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 14px;
}

.company-logo-preview__image {
  display: inline-flex;
  width: 96px;
  height: 54px;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius-md);
  background: var(--surface-control);
  color: var(--text-muted);
}

.company-logo-preview__image img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 8px;
}

.company-logo-preview__image .material-symbols-outlined {
  font-size: 24px;
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

.profile-preview strong,
.settings-preview strong,
.settings-password strong {
  display: block;
  color: var(--text-heading);
  font-size: 18px;
  font-weight: 800;
}

.profile-preview p,
.company-logo-preview p,
.settings-row p,
.settings-block p,
.settings-preview p,
.settings-password p {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 13px;
}

.company-logo-preview strong {
  display: block;
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 800;
}

.profile-actions,
.settings-actions,
.settings-security-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.settings-security-actions {
  justify-content: flex-end;
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
  min-height: var(--ui-control-height);
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

.settings-block--inline {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
}

.settings-block strong,
.settings-row strong {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 800;
}

.settings-success,
.settings-error {
  font-size: 13px;
  font-weight: 700;
}

.settings-success {
  color: var(--success-color);
}

.settings-error {
  color: var(--danger-text-strong);
}

.settings-actions {
  justify-content: flex-end;
  min-height: var(--ui-control-height);
}

.settings-actions p {
  margin-right: auto;
}

.settings-button {
  display: inline-flex;
  min-height: var(--ui-control-height);
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

.settings-button--danger {
  border: 1px solid color-mix(in srgb, var(--danger-color) 42%, var(--line-soft));
  background: var(--danger-surface);
  color: var(--danger-text-strong);
}

.settings-list,
.settings-account {
  display: grid;
  overflow: hidden;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
}

.settings-row,
.settings-account__row {
  display: flex;
  min-height: var(--ui-row-height);
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line-soft);
  background: var(--surface-card);
}

.settings-row:last-child,
.settings-account__row:last-child {
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
  min-height: var(--ui-control-height);
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

.settings-preview__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.settings-preview__body {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr) minmax(0, 1fr);
  gap: 10px;
}

.settings-preview__card,
.settings-preview__list {
  display: grid;
  gap: 5px;
  min-height: var(--ui-row-height);
  align-content: center;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card);
}

.settings-preview__card strong,
.settings-preview__list strong {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 800;
}

.settings-preview__list span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.settings-chip {
  width: fit-content;
  border-radius: var(--radius-full);
  background: var(--accent-soft);
  color: var(--accent-strong);
  padding: 2px 8px;
  font-size: 12px;
  font-weight: 800;
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

.settings-policy {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.settings-policy li {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-height: 28px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-full);
  background: var(--surface-control);
  color: var(--text-muted);
  padding: 0 10px;
  font-size: 12px;
  font-weight: 800;
}

.settings-policy li.is-valid {
  border-color: color-mix(in srgb, var(--success-color) 58%, var(--line-soft));
  color: var(--success-color);
}

.settings-policy .material-symbols-outlined {
  font-size: 16px;
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

.settings-modal__preview small,
.settings-modal__note {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.settings-modal__note {
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

@media (max-width: 960px) {
  .settings-shell {
    grid-template-columns: 1fr;
  }

  .settings-tabs {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .settings-preview__body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .settings-header,
  .settings-profile,
  .settings-logo-panel,
  .settings-row,
  .settings-account__row,
  .settings-preview__head,
  .settings-block--inline {
    align-items: stretch;
    flex-direction: column;
    grid-template-columns: 1fr;
  }

  .settings-status,
  .settings-segmented,
  .settings-actions,
  .settings-security-actions,
  .profile-actions {
    width: 100%;
  }

  .settings-tabs,
  .settings-form-grid {
    grid-template-columns: 1fr;
  }

  .settings-actions,
  .settings-security-actions {
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
