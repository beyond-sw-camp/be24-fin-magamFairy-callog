<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { changePasswordRequest } from '@/authApi'
import {
  createProfileImageUploadUrl,
  deleteMyProfileImage,
  generateMyProfileImage,
  getMyProfile,
  getMyProfileImageHistories,
  selectMyProfileImageHistory,
  updateMyProfile,
  updateMyProfileImage,
  uploadProfileImageToS3,
} from '@/api/userProfiles/index.js'
import { useAuthStore } from '@/stores/useAuthStore'
import { usePlannerStore } from '@/stores/planner'
import { useUserSettingsStore } from '@/stores/userSettings'
import NotificationSettingsPanel from '@/components/notifications/NotificationSettingsPanel.vue'

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
    summary: '알림 수신 방식과 조건을 관리합니다.',
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
    summary: '로그인 계정과 비밀번호를 관리합니다.',
  },
]

const densityOptions = [
  { value: 'comfortable', label: '기본', description: '정보 간격을 여유 있게 표시합니다.' },
  { value: 'compact', label: '컴팩트', description: '반복 작업에 맞춰 간격을 줄입니다.' },
]

const ALLOWED_PROFILE_IMAGE_TYPES = new Set(['image/png', 'image/jpeg', 'image/webp'])
const MAX_PROFILE_IMAGE_SIZE = 5 * 1024 * 1024
const PROFILE_IMAGE_GENERATION_SIZE = 1024
const tabIds = new Set(tabs.map((tab) => tab.id))

const profileForm = reactive({
  name: '',
  company: '',
  department: '',
  role: '',
  phone: '',
  email: '',
  imageDataUrl: '',
  profileImageKey: '',
  companyLogoDataUrl: '',
})

const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const feedback = reactive({
  profile: '',
  profileError: '',
  security: '',
  securityError: '',
})

const isProfileImageModalOpen = ref(false)
const isImageGenerationModalOpen = ref(false)
const isPasswordModalOpen = ref(false)
const imageGenerationPrompt = ref('')
const isSavingProfile = ref(false)
const isGeneratingImage = ref(false)
const isLoadingImageHistories = ref(false)
const isChangingPassword = ref(false)
const selectingHistoryId = ref(null)
const pendingProfileImageFile = ref(null)
const shouldRemoveProfileImage = ref(false)
const profileImageHistories = reactive({
  appliedImages: [],
  generatedImages: [],
})

const activeTab = computed(() => {
  const requestedTab = String(route.query.tab || 'profile')

  return tabIds.has(requestedTab) ? requestedTab : 'profile'
})
const currentTab = computed(() => tabs.find((tab) => tab.id === activeTab.value) ?? tabs[0])
const isDarkMode = computed(() => plannerStore.theme === 'dark')
const userKey = computed(() => resolveUserKey(authStore.user))
const accountEmail = computed(() => profileForm.email || userSettingsStore.profile.email)
const accountRows = computed(() => [
  { label: '로그인 ID', value: readUserValue(['id', 'loginId', 'sub']) || userKey.value },
  { label: '이름', value: profileForm.name },
  { label: '회사', value: profileForm.company },
  { label: '부서', value: profileForm.department },
  { label: '권한', value: profileForm.role },
  { label: '이메일', value: accountEmail.value },
])
const passwordPolicyItems = computed(() => {
  const value = passwordForm.newPassword

  return [
    { key: 'length', label: '8~20자', valid: value.length >= 8 && value.length <= 20 },
    { key: 'upper', label: '대문자 포함', valid: /[A-Z]/.test(value) },
    { key: 'lower', label: '소문자 포함', valid: /[a-z]/.test(value) },
    { key: 'number', label: '숫자 포함', valid: /\d/.test(value) },
    { key: 'special', label: '특수문자 포함', valid: /[^A-Za-z0-9]/.test(value) },
    { key: 'space', label: '공백 없음', valid: value.length > 0 && !/\s/.test(value) },
    {
      key: 'different',
      label: '현재 비밀번호와 다름',
      valid: Boolean(value) && value !== passwordForm.currentPassword,
    },
  ]
})
const isPasswordPolicyValid = computed(() => passwordPolicyItems.value.every((item) => item.valid))
const isPasswordFormReady = computed(
  () =>
    Boolean(passwordForm.currentPassword) &&
    Boolean(passwordForm.newPassword) &&
    passwordForm.newPassword === passwordForm.confirmPassword &&
    isPasswordPolicyValid.value,
)
const hasPendingProfileImageChange = computed(
  () => Boolean(pendingProfileImageFile.value) || shouldRemoveProfileImage.value,
)

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

function readUserValue(keys) {
  const user = authStore.user

  if (!user || typeof user !== 'object') {
    return ''
  }

  return (
    keys.map((key) => user[key]).find((value) => typeof value === 'string' && value.trim()) ?? ''
  )
}

function resolvePayload(payload) {
  return payload?.result ?? payload?.data ?? payload ?? {}
}

function resolveProfileImageGenerationError(error) {
  if (error?.code === 'ECONNABORTED') {
    return '이미지 생성 시간이 너무 오래 걸리고 있습니다. 잠시 후 다시 시도해 주세요.'
  }

  if (error?.response?.status === 503) {
    return 'OpenAI 이미지 API 키 설정을 확인해 주세요. 서버 실행 환경에 OPEN_API_IMAGE가 필요합니다.'
  }

  if (error?.response?.status === 502) {
    return 'OpenAI 이미지 생성 요청이 실패했습니다. 프롬프트, 모델 권한, 결제/쿼터 상태를 확인해 주세요.'
  }

  if (error?.response?.status === 500) {
    return '서버에서 프로필 이미지 생성 처리 중 오류가 발생했습니다. 잠시 후 다시 시도하거나 백엔드 로그를 확인해 주세요.'
  }

  const responseData = error?.response?.data

  return (
    responseData?.message ??
    responseData?.error?.message ??
    responseData?.data ??
    error?.message ??
    '프로필 이미지를 생성하지 못했습니다. 프롬프트나 API 설정을 확인해 주세요.'
  )
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
    profileImageKey: userSettingsStore.profile.profileImageKey || '',
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
  const source = resolvePayload(payload)
  const nextProfile = {
    name: source.name,
    email: source.email,
    phone: source.phone,
  }

  if (Object.prototype.hasOwnProperty.call(source, 'profileImageUrl')) {
    nextProfile.imageDataUrl = source.profileImageUrl || ''
  }

  if (Object.prototype.hasOwnProperty.call(source, 'profileImageKey')) {
    nextProfile.profileImageKey = source.profileImageKey || ''
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

function readFileAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()

    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = reject
    reader.readAsDataURL(file)
  })
}

function validateProfileImageFile(file) {
  if (!ALLOWED_PROFILE_IMAGE_TYPES.has(file.type)) {
    return 'PNG, JPG, WEBP 형식의 이미지만 업로드할 수 있습니다.'
  }

  if (file.size <= 0 || file.size > MAX_PROFILE_IMAGE_SIZE) {
    return '프로필 이미지는 5MB 이하로 선택해 주세요.'
  }

  return ''
}

async function handleProfileImageUpload(event) {
  const file = event.target.files?.[0]

  if (!file) {
    return
  }

  const validationMessage = validateProfileImageFile(file)

  if (validationMessage) {
    feedback.profile = ''
    feedback.profileError = validationMessage
    event.target.value = ''
    return
  }

  try {
    profileForm.imageDataUrl = await readFileAsDataUrl(file)
    pendingProfileImageFile.value = file
    shouldRemoveProfileImage.value = false
    feedback.profile = ''
    feedback.profileError = ''
  } catch {
    feedback.profile = ''
    feedback.profileError = '프로필 이미지를 읽지 못했습니다. 다른 파일을 선택해 주세요.'
  }

  event.target.value = ''
}

function clearProfileImage() {
  profileForm.imageDataUrl = ''
  pendingProfileImageFile.value = null
  shouldRemoveProfileImage.value = true
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

async function openProfileImageModal() {
  isProfileImageModalOpen.value = true
  await loadProfileImageHistories()
}

function closeProfileImageModal() {
  isProfileImageModalOpen.value = false
}

function openImageGenerationModal() {
  imageGenerationPrompt.value = userSettingsStore.generatorPrompt
  isImageGenerationModalOpen.value = true
}

function closeImageGenerationModal() {
  isImageGenerationModalOpen.value = false
}

function openPasswordModal() {
  feedback.security = ''
  feedback.securityError = ''
  isPasswordModalOpen.value = true
}

function closePasswordModal() {
  if (isChangingPassword.value) {
    return
  }

  isPasswordModalOpen.value = false
}

function applyProfileImageHistories(payload) {
  const source = resolvePayload(payload)

  profileImageHistories.appliedImages = Array.isArray(source.appliedImages)
    ? source.appliedImages
    : []
  profileImageHistories.generatedImages = Array.isArray(source.generatedImages)
    ? source.generatedImages
    : []
}

async function loadProfileImageHistories() {
  if (!authStore.isAuthenticated) {
    applyProfileImageHistories({})
    return
  }

  isLoadingImageHistories.value = true

  try {
    const response = await getMyProfileImageHistories()
    applyProfileImageHistories(response.data)
  } catch (error) {
    console.warn('Profile image histories load failed.', error)
  } finally {
    isLoadingImageHistories.value = false
  }
}

async function requestImageGeneration() {
  const prompt = imageGenerationPrompt.value.trim()

  if (!prompt || isGeneratingImage.value) {
    return
  }

  feedback.profileError = ''
  feedback.profile = ''
  isGeneratingImage.value = true

  try {
    userSettingsStore.setGeneratorPrompt(prompt)
    const response = await generateMyProfileImage({
      prompt,
      size: PROFILE_IMAGE_GENERATION_SIZE,
    })
    const payload = resolvePayload(response.data)

    applyProfileImageHistories(payload.histories)
    feedback.profile =
      '생성 이미지가 기록에 추가되었습니다. 미리보기에서 적용할 이미지를 선택해 주세요.'
    closeImageGenerationModal()
  } catch (error) {
    console.error('Profile image generation failed.', error)
    feedback.profileError = resolveProfileImageGenerationError(error)
  } finally {
    isGeneratingImage.value = false
  }
}

async function applyProfileImageHistory(history) {
  if (!history?.id || selectingHistoryId.value) {
    return
  }

  feedback.profile = ''
  feedback.profileError = ''
  selectingHistoryId.value = history.id

  try {
    const response = await selectMyProfileImageHistory({
      historyId: history.id,
    })
    const payload = resolvePayload(response.data)

    applyRemoteProfile(payload.profile)
    applyProfileImageHistories(payload.histories)
    syncProfileForm()
    pendingProfileImageFile.value = null
    shouldRemoveProfileImage.value = false
    feedback.profile = '선택한 프로필 이미지가 적용되었습니다.'
  } catch (error) {
    console.error('Profile image history select failed.', error)
    feedback.profileError =
      '선택한 프로필 이미지를 적용하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    selectingHistoryId.value = null
  }
}

async function saveProfileImageChanges() {
  if (!hasPendingProfileImageChange.value || isSavingProfile.value) {
    return
  }

  feedback.profile = ''
  feedback.profileError = ''
  isSavingProfile.value = true

  try {
    const response = await syncProfileImageToServer()

    if (response) {
      applyRemoteProfile(response.data)
    }

    pendingProfileImageFile.value = null
    shouldRemoveProfileImage.value = false
    syncProfileForm()
    await loadProfileImageHistories()
    closeProfileImageModal()
    feedback.profile = '프로필 이미지가 저장되었습니다.'
  } catch (error) {
    console.error('Profile image save failed.', error)
    feedback.profileError = '프로필 이미지를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isSavingProfile.value = false
  }
}

async function syncProfileImageToServer() {
  if (pendingProfileImageFile.value) {
    const file = pendingProfileImageFile.value
    const presignedResponse = await createProfileImageUploadUrl({
      fileName: file.name,
      contentType: file.type,
      fileSize: file.size,
    })
    const presignedPayload = resolvePayload(presignedResponse.data)

    await uploadProfileImageToS3({
      uploadUrl: presignedPayload.uploadUrl,
      file,
      contentType: presignedPayload.contentType || file.type,
    })

    return updateMyProfileImage({
      objectKey: presignedPayload.objectKey,
    })
  }

  if (shouldRemoveProfileImage.value) {
    return deleteMyProfileImage()
  }

  return null
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

  isSavingProfile.value = true

  try {
    await syncProfileImageToServer()
    const response = await updateMyProfile({
      email,
      phone,
    })

    applyRemoteProfile(response.data)
    userSettingsStore.updateProfile({
      companyLogoDataUrl: profileForm.companyLogoDataUrl,
    })
    pendingProfileImageFile.value = null
    shouldRemoveProfileImage.value = false
    syncProfileForm()
    await loadProfileImageHistories()
    feedback.profile = '프로필 정보가 저장되었습니다.'
  } catch (error) {
    console.error('Profile save failed.', error)
    feedback.profileError = '프로필 정보를 저장하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isSavingProfile.value = false
  }
}

async function loadRemoteProfile() {
  try {
    const response = await getMyProfile()
    applyRemoteProfile(response.data)
    syncProfileForm()
    await loadProfileImageHistories()
  } catch (error) {
    console.warn('Profile load failed. Local settings will be used.', error)
  }
}

async function changePassword() {
  feedback.security = ''
  feedback.securityError = ''

  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    feedback.securityError = '새 비밀번호 확인이 일치하지 않습니다.'
    return
  }

  if (!isPasswordPolicyValid.value) {
    feedback.securityError = '비밀번호 정책을 모두 만족해야 합니다.'
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
    feedback.security = '비밀번호가 변경되었습니다. 다시 로그인해 주세요.'
    await authStore.logout()
    await router.replace('/user/login')
  } catch (error) {
    feedback.securityError =
      error?.response?.data?.message ??
      error?.message ??
      '비밀번호를 변경하지 못했습니다. 입력값을 확인해 주세요.'
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
    if (!isAuthenticated) {
      return
    }

    await loadRemoteProfile()
  },
  { immediate: true },
)
</script>

<template>
  <section class="settings-page ui-page">
    <header class="settings-header">
      <div>
        <h2 class="settings-heading">환경설정</h2>
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
                <img
                  v-if="profileForm.imageDataUrl"
                  :src="profileForm.imageDataUrl"
                  alt=""
                  crossorigin="anonymous"
                  referrerpolicy="strict-origin-when-cross-origin"
                />
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
                @click="openProfileImageModal"
              >
                프로필 이미지 변경
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
              <input v-model.trim="profileForm.name" type="text" autocomplete="name" readonly />
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
              <input v-model.trim="profileForm.company" type="text" readonly />
            </label>
            <label class="settings-field">
              <span>부서</span>
              <input v-model.trim="profileForm.department" type="text" readonly />
            </label>
            <label class="settings-field">
              <span>역할</span>
              <input v-model.trim="profileForm.role" type="text" readonly />
            </label>
          </section>

          <footer class="settings-actions">
            <p v-if="feedback.profileError" class="settings-error">{{ feedback.profileError }}</p>
            <p v-else-if="feedback.profile" class="settings-success">{{ feedback.profile }}</p>
            <button
              type="submit"
              class="settings-button settings-button--primary"
              :disabled="isSavingProfile"
            >
              {{ isSavingProfile ? '저장 중' : '저장' }}
            </button>
          </footer>
        </form>

        <div v-else-if="activeTab === 'notifications'" class="settings-pane">
          <NotificationSettingsPanel show-center-link />
        </div>

        <div v-else-if="activeTab === 'theme'" class="settings-pane">
          <section class="settings-block settings-block--split">
            <div>
              <strong>테마</strong>
              <p>선택 즉시 전체 UI에 반영되며 브라우저 localStorage에 저장됩니다.</p>
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

          <section class="settings-block settings-block--split">
            <div>
              <strong>화면 밀도</strong>
              <p>업무 화면의 여백과 컨트롤 높이를 사용 방식에 맞게 조정합니다.</p>
            </div>
            <div class="settings-segmented" role="group" aria-label="화면 밀도 선택">
              <button
                v-for="option in densityOptions"
                :key="option.value"
                type="button"
                :title="option.description"
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
                <p>전환 애니메이션과 움직임을 최소화합니다.</p>
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
                <p>텍스트와 경계 대비를 높여 화면 요소를 더 뚜렷하게 표시합니다.</p>
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

          <section class="settings-preview" aria-label="화면 설정 미리보기">
            <div class="settings-preview__sample">
              <div class="settings-preview__line">
                <strong>업무 카드 미리보기</strong>
                <span>오늘</span>
              </div>
              <p>현재 테마, 밀도, 고대비 설정이 적용된 공통 카드 예시입니다.</p>
              <div class="settings-preview__actions">
                <button type="button">검토</button>
                <button type="button">완료</button>
              </div>
            </div>
            <button
              type="button"
              class="settings-button settings-button--ghost"
              @click="resetDisplaySettings"
            >
              화면 설정 초기화
            </button>
          </section>
        </div>

        <div v-else class="settings-pane">
          <section class="settings-account" aria-label="계정 정보">
            <div v-for="row in accountRows" :key="row.label" class="settings-account__row">
              <span>{{ row.label }}</span>
              <strong>{{ row.value || '-' }}</strong>
            </div>
          </section>

          <section class="settings-security-actions">
            <button
              type="button"
              class="settings-button settings-button--primary"
              @click="openPasswordModal"
            >
              비밀번호 변경
            </button>
          </section>
          <p v-if="feedback.security" class="settings-success">{{ feedback.security }}</p>
        </div>
      </article>
    </div>

    <Teleport to="body">
      <Transition name="settings-modal">
        <div
          v-if="isPasswordModalOpen"
          class="settings-modal"
          role="presentation"
          @click.self="closePasswordModal"
        >
          <form
            class="settings-modal__panel password-modal"
            aria-label="비밀번호 변경"
            @submit.prevent="changePassword"
          >
            <header class="settings-modal__header">
              <div>
                <p class="settings-eyebrow">계정 보안</p>
                <h3>비밀번호 변경</h3>
              </div>
              <button
                type="button"
                class="settings-modal__close"
                aria-label="닫기"
                :disabled="isChangingPassword"
                @click="closePasswordModal"
              >
                x
              </button>
            </header>

            <div class="settings-modal__body">
              <div class="settings-password">
                <div>
                  <strong>내 비밀번호 변경</strong>
                  <p>성공 시 현재 refresh token을 삭제하고 다시 로그인이 필요합니다.</p>
                </div>
                <div class="settings-form-grid">
                  <label class="settings-field">
                    <span>현재 비밀번호</span>
                    <input
                      v-model="passwordForm.currentPassword"
                      type="password"
                      autocomplete="current-password"
                      autofocus
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
                </div>
                <ul class="password-policy">
                  <li
                    v-for="item in passwordPolicyItems"
                    :key="item.key"
                    :class="{ 'is-valid': item.valid }"
                  >
                    <span class="password-policy__dot" />
                    {{ item.label }}
                  </li>
                </ul>
              </div>
            </div>

            <footer class="settings-modal__actions">
              <p v-if="feedback.securityError" class="settings-error">
                {{ feedback.securityError }}
              </p>
              <button
                type="button"
                class="settings-button settings-button--ghost"
                :disabled="isChangingPassword"
                @click="closePasswordModal"
              >
                취소
              </button>
              <button
                type="submit"
                class="settings-button settings-button--primary"
                :disabled="isChangingPassword || !isPasswordFormReady"
              >
                {{ isChangingPassword ? '변경 중' : '비밀번호 변경' }}
              </button>
            </footer>
          </form>
        </div>
      </Transition>
    </Teleport>

    <Teleport to="body">
      <Transition name="settings-modal">
        <div
          v-if="isProfileImageModalOpen"
          class="settings-modal"
          role="presentation"
          @click.self="closeProfileImageModal"
        >
          <section
            class="settings-modal__panel profile-image-modal"
            aria-label="프로필 이미지 변경"
          >
            <header class="settings-modal__header">
              <div>
                <p class="settings-eyebrow">프로필 이미지</p>
                <h3>프로필 이미지 변경</h3>
              </div>
              <button
                type="button"
                class="settings-modal__close"
                aria-label="닫기"
                @click="closeProfileImageModal"
              >
                x
              </button>
            </header>

            <div class="settings-modal__body profile-image-modal__body">
              <section class="profile-image-manager">
                <div class="profile-preview">
                  <div class="profile-preview__image profile-preview__image--large">
                    <img
                      v-if="profileForm.imageDataUrl"
                      :src="profileForm.imageDataUrl"
                      alt=""
                      crossorigin="anonymous"
                      referrerpolicy="strict-origin-when-cross-origin"
                    />
                    <span v-else>{{ userSettingsStore.profileInitials }}</span>
                  </div>
                  <div>
                    <strong>{{ profileForm.name || 'Callog User' }}</strong>
                    <p>{{ profileForm.company }} · {{ profileForm.department }}</p>
                    <small
                      v-if="hasPendingProfileImageChange"
                      class="profile-image-manager__pending"
                    >
                      저장 전 변경사항이 있습니다.
                    </small>
                  </div>
                </div>

                <div class="profile-actions profile-actions--modal">
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
                      accept="image/png,image/jpeg,image/webp"
                      class="settings-file"
                      @change="handleProfileImageUpload"
                    />
                  </label>
                  <button
                    type="button"
                    class="settings-button settings-button--ghost"
                    :disabled="!profileForm.imageDataUrl && !pendingProfileImageFile"
                    @click="clearProfileImage"
                  >
                    이미지 제거
                  </button>
                </div>
              </section>

              <section class="profile-history">
                <div class="profile-history__section">
                  <div class="profile-history__head">
                    <strong>최근 적용한 이미지</strong>
                    <span>{{
                      isLoadingImageHistories
                        ? '불러오는 중'
                        : `${profileImageHistories.appliedImages.length}/3`
                    }}</span>
                  </div>
                  <div
                    v-if="profileImageHistories.appliedImages.length"
                    class="profile-history__grid"
                  >
                    <article
                      v-for="image in profileImageHistories.appliedImages"
                      :key="`applied-${image.id}`"
                      class="profile-history-card"
                      :class="{
                        'is-current':
                          image.objectKey && image.objectKey === profileForm.profileImageKey,
                      }"
                    >
                      <img :src="image.imageUrl" alt="" crossorigin="anonymous" />
                      <div>
                        <strong>{{ image.source === 'AI' ? 'AI 이미지' : '업로드 이미지' }}</strong>
                        <small>{{ image.prompt || '직접 업로드' }}</small>
                      </div>
                      <button
                        type="button"
                        class="settings-button settings-button--ghost"
                        :disabled="
                          selectingHistoryId === image.id ||
                          (image.objectKey && image.objectKey === profileForm.profileImageKey)
                        "
                        @click="applyProfileImageHistory(image)"
                      >
                        {{
                          image.objectKey && image.objectKey === profileForm.profileImageKey
                            ? '사용 중'
                            : '적용'
                        }}
                      </button>
                    </article>
                  </div>
                  <p v-else class="profile-history__empty">아직 적용 기록이 없습니다.</p>
                </div>

                <div class="profile-history__section">
                  <div class="profile-history__head">
                    <strong>최근 생성한 이미지</strong>
                    <span>{{
                      isLoadingImageHistories
                        ? '불러오는 중'
                        : `${profileImageHistories.generatedImages.length}/3`
                    }}</span>
                  </div>
                  <div
                    v-if="profileImageHistories.generatedImages.length"
                    class="profile-history__grid"
                  >
                    <article
                      v-for="image in profileImageHistories.generatedImages"
                      :key="`generated-${image.id}`"
                      class="profile-history-card"
                      :class="{
                        'is-current':
                          image.objectKey && image.objectKey === profileForm.profileImageKey,
                      }"
                    >
                      <img :src="image.imageUrl" alt="" crossorigin="anonymous" />
                      <div>
                        <strong>생성 이미지</strong>
                        <small>{{ image.prompt || '프롬프트 없음' }}</small>
                      </div>
                      <button
                        type="button"
                        class="settings-button settings-button--ghost"
                        :disabled="
                          selectingHistoryId === image.id ||
                          (image.objectKey && image.objectKey === profileForm.profileImageKey)
                        "
                        @click="applyProfileImageHistory(image)"
                      >
                        {{
                          image.objectKey && image.objectKey === profileForm.profileImageKey
                            ? '사용 중'
                            : '적용'
                        }}
                      </button>
                    </article>
                  </div>
                  <p v-else class="profile-history__empty">생성한 이미지가 여기에 표시됩니다.</p>
                </div>
              </section>
            </div>

            <footer class="settings-modal__actions">
              <button
                type="button"
                class="settings-button settings-button--ghost"
                @click="closeProfileImageModal"
              >
                닫기
              </button>
              <button
                type="button"
                class="settings-button settings-button--primary"
                :disabled="isSavingProfile || !hasPendingProfileImageChange"
                @click="saveProfileImageChanges"
              >
                {{ isSavingProfile ? '저장 중' : '변경 저장' }}
              </button>
            </footer>
          </section>
        </div>
      </Transition>
    </Teleport>

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
                <p class="settings-eyebrow">1024 x 1024</p>
                <h3>프로필 이미지 생성</h3>
              </div>
              <button
                type="button"
                class="settings-modal__close"
                aria-label="닫기"
                @click="closeImageGenerationModal"
              >
                x
              </button>
            </header>

            <div class="settings-modal__body">
              <div class="settings-modal__preview">
                <span class="material-symbols-outlined">auto_awesome</span>
                <strong>1024</strong>
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
                {{
                  isGeneratingImage
                    ? '생성 중입니다. 보통 10~40초, 최대 2분까지 기다립니다.'
                    : '생성 이미지는 바로 적용되지 않고 생성 기록에 저장됩니다. 미리보기에서 원하는 이미지를 적용해 주세요.'
                }}
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
                :disabled="!imageGenerationPrompt.trim() || isGeneratingImage"
              >
                {{ isGeneratingImage ? '생성 중' : '이미지 생성' }}
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
  padding: var(--density-page-padding, 24px);
  scrollbar-gutter: stable;
}

.settings-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  min-height: 74px;
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

.settings-panel__head .settings-eyebrow,
.settings-panel__head h3 {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.settings-panel__head .settings-eyebrow {
  line-height: 16px;
}

.settings-panel__head h3 {
  margin-top: 4px;
  color: var(--text-heading);
  font-size: 18px;
  font-weight: 800;
  line-height: 25px;
}

.settings-pane {
  display: grid;
  gap: var(--density-pane-gap, 18px);
  padding: var(--density-card-padding, 20px);
}

.settings-profile,
.settings-logo-panel,
.settings-block--split,
.settings-row,
.settings-security-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.settings-profile {
  padding-bottom: 18px;
  border-bottom: 1px solid var(--line-soft);
}

.profile-preview,
.company-logo-preview {
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
.company-logo-preview p,
.settings-row p,
.settings-block p,
.settings-password p,
.settings-preview p {
  margin-top: 4px;
  color: var(--text-muted);
  font-size: 13px;
}

.settings-logo-panel,
.settings-block,
.settings-preview,
.settings-password {
  padding: var(--density-card-padding, 16px);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card-muted);
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

.company-logo-preview strong,
.settings-block strong,
.settings-row strong,
.settings-password strong,
.settings-preview strong {
  display: block;
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 800;
}

.profile-actions,
.settings-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.profile-history {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.profile-history__section {
  display: grid;
  gap: 10px;
  min-width: 0;
  padding: var(--density-card-padding, 14px);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card-muted);
}

.profile-history__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.profile-history__head strong {
  color: var(--text-heading);
  font-size: 14px;
  font-weight: 800;
}

.profile-history__head span,
.profile-history__empty,
.profile-history-card small {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.profile-history__grid {
  display: grid;
  gap: 10px;
}

.profile-history-card {
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 78px;
  padding: 10px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card);
}

.profile-history-card.is-current {
  border-color: var(--accent-color);
  box-shadow: 0 0 0 1px color-mix(in srgb, var(--accent-color) 35%, transparent);
}

.profile-history-card img {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  background: var(--surface-control);
}

.profile-history-card div {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.profile-history-card strong,
.profile-history-card small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.profile-history-card strong {
  color: var(--text-heading);
  font-size: 13px;
  font-weight: 800;
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
  min-height: var(--density-control-height, 40px);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-control);
  padding: 0 12px;
  color: var(--text-heading);
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast);
}

.settings-field input[readonly] {
  color: var(--text-muted);
}

.settings-field input:focus {
  border-color: var(--accent-color);
  background: var(--control-focus-color);
  outline: none;
}

.settings-message,
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
  min-height: var(--density-control-height, 40px);
}

.settings-actions p {
  margin-right: auto;
}

.settings-button {
  display: inline-flex;
  min-height: var(--density-control-height, 38px);
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

.settings-button--danger {
  border: 1px solid var(--danger-color);
  background: var(--danger-surface);
  color: var(--danger-text-strong);
}

.settings-button--ghost:hover {
  border-color: var(--line-strong);
  background: var(--surface-control-hover);
}

.settings-button:disabled {
  cursor: not-allowed;
  opacity: 0.62;
}

.settings-list,
.settings-account {
  display: grid;
  overflow: hidden;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
}

.settings-row {
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
  min-height: var(--density-control-height, 38px);
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

.settings-segmented button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.notification-methods {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.notification-method {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-height: 96px;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card);
  color: var(--text-body);
  text-align: left;
  cursor: pointer;
  transition:
    background var(--transition-fast),
    border-color var(--transition-fast),
    box-shadow var(--transition-fast);
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

.notification-method strong {
  display: block;
  color: var(--text-heading);
  font-size: 13px;
  font-weight: 800;
}

.notification-method small {
  display: block;
  margin-top: 5px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
  line-height: 1.45;
}

.notification-summary__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.notification-summary__chips span {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  border: 1px solid var(--line-soft);
  border-radius: 999px;
  background: var(--surface-control);
  padding: 0 10px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 800;
}

.notification-summary__chips span.is-active {
  border-color: color-mix(in srgb, var(--accent-color) 40%, var(--line-soft));
  background: color-mix(in srgb, var(--accent-color) 12%, var(--surface-control));
  color: var(--text-heading);
}

.settings-preview {
  display: grid;
  gap: 14px;
}

.settings-preview__sample {
  display: grid;
  gap: 10px;
  padding: 14px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card);
}

.settings-preview__line,
.settings-preview__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.settings-preview__line span {
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.settings-preview__actions {
  justify-content: flex-start;
}

.settings-preview__actions button {
  min-height: 30px;
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-sm);
  background: var(--surface-control);
  padding: 0 10px;
  color: var(--text-heading);
  font-size: 12px;
  font-weight: 800;
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

.settings-security-actions {
  justify-content: flex-start;
}

.settings-password {
  display: grid;
  gap: 16px;
}

.password-policy {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.password-policy li {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--text-muted);
  font-size: 12px;
  font-weight: 700;
}

.password-policy__dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--line-strong);
}

.password-policy li.is-valid {
  color: var(--success-color);
}

.password-policy li.is-valid .password-policy__dot {
  background: var(--success-color);
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

.profile-image-modal {
  display: grid;
  width: min(920px, 100%);
  max-height: min(820px, calc(100vh - 48px));
  grid-template-rows: auto minmax(0, 1fr) auto;
}

.password-modal {
  width: min(560px, 100%);
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
  font-size: 16px;
  line-height: 1;
}

.settings-modal__body {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.profile-image-modal__body {
  overflow: auto;
}

.profile-image-manager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: var(--density-card-padding, 14px);
  border: 1px solid var(--line-soft);
  border-radius: var(--radius-md);
  background: var(--surface-card-muted);
}

.profile-preview__image--large {
  width: 96px;
  height: 96px;
}

.profile-image-manager__pending {
  display: inline-flex;
  margin-top: 6px;
  color: var(--accent-color);
  font-size: 12px;
  font-weight: 800;
}

.profile-actions--modal {
  flex-wrap: wrap;
  justify-content: flex-end;
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

.settings-modal__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 20px 18px;
  border-top: 1px solid var(--line-soft);
}

.settings-modal__actions .settings-error {
  margin-right: auto;
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

  .profile-history {
    grid-template-columns: 1fr;
  }

  .settings-tabs {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .notification-methods {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .settings-page {
    padding: 16px;
  }

  .settings-header,
  .settings-profile,
  .profile-image-manager,
  .settings-logo-panel,
  .settings-block--split,
  .settings-row,
  .settings-account__row,
  .settings-security-actions {
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
  .settings-form-grid,
  .notification-methods,
  .password-policy {
    grid-template-columns: 1fr;
  }

  .profile-history-card {
    grid-template-columns: 56px minmax(0, 1fr);
  }

  .profile-history-card .settings-button {
    grid-column: 1 / -1;
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
