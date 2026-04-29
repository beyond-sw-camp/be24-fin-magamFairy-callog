import { computed, reactive, ref } from 'vue'
import { defineStore } from 'pinia'

const STORAGE_PREFIX = 'callog-user-settings'

const fallbackProfile = {
  name: 'Callog User',
  company: 'CALLOG',
  department: 'Marketing Operations',
  role: 'Member',
  phone: '010-0000-0000',
  email: 'user@callog.com',
  imageDataUrl: '',
  companyLogoDataUrl: '',
}

const defaultNotifications = {
  task: true,
  qa: true,
  ai: true,
  critical: true,
}

const defaultThemeUi = {
  theme: 'light',
  density: 'comfortable',
  reduceMotion: false,
  highContrast: false,
}

const defaultSecurity = {
  accountType: '일반 사용자',
  sessionStatus: '활성',
  passwordChangeRoute: '',
}

function getStorage() {
  return typeof window === 'undefined' ? null : window.localStorage
}

function sanitizeUserKey(value) {
  const normalizedValue = String(value || 'guest').trim()

  return normalizedValue.replace(/[^\w.@-]/g, '_') || 'guest'
}

function buildStorageKey(userKey) {
  return `${STORAGE_PREFIX}:${sanitizeUserKey(userKey)}`
}

function safeParse(value) {
  try {
    return value ? JSON.parse(value) : null
  } catch {
    return null
  }
}

function readFirstString(source, keys) {
  if (!source || typeof source !== 'object') {
    return ''
  }

  const value = keys
    .map((key) => source[key])
    .find((item) => typeof item === 'string' && item.trim())

  return value?.trim?.() ?? ''
}

function createInitials(name) {
  const normalizedName = String(name || '').trim()

  if (!normalizedName) {
    return 'CU'
  }

  const words = normalizedName.split(/\s+/).filter(Boolean)
  const value =
    words.length > 1
      ? words
          .slice(0, 2)
          .map((word) => word[0])
          .join('')
      : normalizedName.slice(0, 2)

  return value.toUpperCase()
}

function drawRoundedRect(context, x, y, width, height, radius) {
  const nextRadius = Math.min(radius, width / 2, height / 2)

  context.beginPath()
  context.moveTo(x + nextRadius, y)
  context.lineTo(x + width - nextRadius, y)
  context.quadraticCurveTo(x + width, y, x + width, y + nextRadius)
  context.lineTo(x + width, y + height - nextRadius)
  context.quadraticCurveTo(x + width, y + height, x + width - nextRadius, y + height)
  context.lineTo(x + nextRadius, y + height)
  context.quadraticCurveTo(x, y + height, x, y + height - nextRadius)
  context.lineTo(x, y + nextRadius)
  context.quadraticCurveTo(x, y, x + nextRadius, y)
  context.closePath()
}

function drawContainedImage(context, image, x, y, width, height) {
  const imageRatio = image.width / image.height
  const boxRatio = width / height
  const nextWidth = imageRatio > boxRatio ? width : height * imageRatio
  const nextHeight = imageRatio > boxRatio ? width / imageRatio : height
  const nextX = x + (width - nextWidth) / 2
  const nextY = y + (height - nextHeight) / 2

  context.drawImage(image, nextX, nextY, nextWidth, nextHeight)
}

function createDefaultAvatarDataUrl(initials) {
  if (typeof document === 'undefined') {
    return ''
  }

  const canvas = document.createElement('canvas')
  const context = canvas.getContext('2d')

  if (!context) {
    return ''
  }

  canvas.width = 240
  canvas.height = 240

  const gradient = context.createLinearGradient(32, 20, 220, 220)
  gradient.addColorStop(0, '#7c3aed')
  gradient.addColorStop(1, '#2563eb')

  context.fillStyle = gradient
  context.fillRect(0, 0, canvas.width, canvas.height)
  context.fillStyle = 'rgba(255,255,255,0.18)'
  context.beginPath()
  context.arc(195, 45, 72, 0, Math.PI * 2)
  context.fill()
  context.fillStyle = 'rgba(255,255,255,0.12)'
  context.beginPath()
  context.arc(32, 210, 96, 0, Math.PI * 2)
  context.fill()
  context.fillStyle = '#ffffff'
  context.font = '700 76px Arial, sans-serif'
  context.textAlign = 'center'
  context.textBaseline = 'middle'
  context.fillText(initials, 120, 122)

  return canvas.toDataURL('image/png')
}

function normalizeProfile(source, rawUser = null) {
  const fromUser = {
    name: readFirstString(rawUser, [
      'name',
      'username',
      'userName',
      'nickname',
      'loginId',
      'id',
      'sub',
    ]),
    company: readFirstString(rawUser, [
      'company',
      'companyName',
      'organization',
      'organizationName',
      'orgName',
    ]),
    department: readFirstString(rawUser, [
      'department',
      'departmentName',
      'team',
      'teamName',
      'division',
    ]),
    role: readFirstString(rawUser, ['roleName', 'role', 'authority']),
    phone: readFirstString(rawUser, ['phone', 'phoneNumber', 'contact', 'mobile']),
    email: readFirstString(rawUser, ['email', 'mail']),
    imageDataUrl: readFirstString(rawUser, [
      'imageDataUrl',
      'profileImage',
      'profileImageUrl',
      'avatar',
    ]),
    companyLogoDataUrl: readFirstString(rawUser, [
      'companyLogoDataUrl',
      'companyLogo',
      'companyLogoUrl',
      'logo',
      'logoUrl',
    ]),
  }

  const mergedProfile = {
    ...fallbackProfile,
    ...Object.fromEntries(Object.entries(fromUser).filter(([, value]) => Boolean(value))),
    ...(source ?? {}),
  }

  const initials = createInitials(mergedProfile.name)

  return {
    name: String(mergedProfile.name || fallbackProfile.name).trim(),
    company: String(mergedProfile.company || fallbackProfile.company).trim(),
    department: String(mergedProfile.department || fallbackProfile.department).trim(),
    role: String(mergedProfile.role || fallbackProfile.role).trim(),
    phone: String(mergedProfile.phone || fallbackProfile.phone).trim(),
    email: String(mergedProfile.email || fallbackProfile.email).trim(),
    imageDataUrl: String(mergedProfile.imageDataUrl || createDefaultAvatarDataUrl(initials)).trim(),
    companyLogoDataUrl: String(mergedProfile.companyLogoDataUrl || '').trim(),
  }
}

function assignState(target, source) {
  Object.keys(target).forEach((key) => {
    if (Object.prototype.hasOwnProperty.call(source, key)) {
      target[key] = source[key]
    }
  })
}

function loadImage(source) {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = reject
    image.src = source
  })
}

function triggerDownload(dataUrl, filename) {
  const anchor = document.createElement('a')
  anchor.href = dataUrl
  anchor.download = filename
  document.body.append(anchor)
  anchor.click()
  anchor.remove()
}

export const useUserSettingsStore = defineStore('userSettings', () => {
  const activeUserKey = ref('guest')
  const profile = reactive({ ...fallbackProfile })
  const notifications = reactive({ ...defaultNotifications })
  const themeUi = reactive({ ...defaultThemeUi })
  const security = reactive({ ...defaultSecurity })
  const generatorPrompt = ref('')
  const generatorStatus = ref('ready')
  const generatorMessage = ref('OpenAI 이미지 생성 API 연동 전 준비 상태입니다.')

  const storageKey = computed(() => buildStorageKey(activeUserKey.value))
  const profileInitials = computed(() => createInitials(profile.name))
  const profileCardData = computed(() => ({
    ...profile,
    initials: profileInitials.value,
    teamLine: `${profile.company} · ${profile.department}`,
  }))

  function persist() {
    const storage = getStorage()

    if (!storage) {
      return
    }

    storage.setItem(
      storageKey.value,
      JSON.stringify({
        profile: { ...profile },
        notifications: { ...notifications },
        themeUi: { ...themeUi },
        security: { ...security },
        generatorPrompt: generatorPrompt.value,
      }),
    )
  }

  function loadUserSettings(userKey = 'guest', rawUser = null) {
    activeUserKey.value = sanitizeUserKey(userKey)

    const storage = getStorage()
    const savedSettings = safeParse(storage?.getItem(storageKey.value)) ?? {}
    const nextProfile = normalizeProfile(savedSettings.profile, rawUser)

    assignState(profile, nextProfile)
    assignState(notifications, {
      ...defaultNotifications,
      ...(savedSettings.notifications ?? {}),
    })
    assignState(themeUi, {
      ...defaultThemeUi,
      ...(savedSettings.themeUi ?? {}),
    })
    assignState(security, {
      ...defaultSecurity,
      ...(savedSettings.security ?? {}),
    })

    generatorPrompt.value = String(savedSettings.generatorPrompt ?? '')
    generatorStatus.value = 'ready'
    generatorMessage.value = 'OpenAI 이미지 생성 API 연동 전 준비 상태입니다.'
  }

  function updateProfile(patch) {
    const nextProfile = normalizeProfile(
      {
        ...profile,
        ...(patch ?? {}),
      },
      null,
    )

    assignState(profile, nextProfile)
    persist()
  }

  function updateNotification(key, value) {
    if (!Object.prototype.hasOwnProperty.call(notifications, key)) {
      return
    }

    notifications[key] = Boolean(value)
    persist()
  }

  function updateThemeUi(patch) {
    assignState(themeUi, {
      ...themeUi,
      ...(patch ?? {}),
    })
    persist()
  }

  function updateSecurity(patch) {
    assignState(security, {
      ...security,
      ...(patch ?? {}),
    })
    persist()
  }

  function setGeneratorPrompt(value) {
    generatorPrompt.value = String(value ?? '')
    persist()
  }

  function markImageGenerationReady() {
    generatorStatus.value = 'pending'
    generatorMessage.value = '이미지 자동 생성은 OpenAI API 연결 후 활성화됩니다.'
  }

  async function downloadProfileCard() {
    if (typeof document === 'undefined') {
      return false
    }

    const canvas = document.createElement('canvas')
    const context = canvas.getContext('2d')

    if (!context) {
      return false
    }

    canvas.width = 720
    canvas.height = 432

    const background = context.createLinearGradient(0, 0, canvas.width, canvas.height)
    background.addColorStop(0, '#101827')
    background.addColorStop(0.58, '#182235')
    background.addColorStop(1, '#273451')

    context.fillStyle = background
    context.fillRect(0, 0, canvas.width, canvas.height)

    context.fillStyle = 'rgba(124, 58, 237, 0.28)'
    context.beginPath()
    context.arc(650, 50, 140, 0, Math.PI * 2)
    context.fill()

    context.fillStyle = 'rgba(90, 165, 255, 0.18)'
    context.beginPath()
    context.arc(52, 395, 150, 0, Math.PI * 2)
    context.fill()

    drawRoundedRect(context, 34, 32, 652, 368, 26)
    context.fillStyle = 'rgba(255, 255, 255, 0.08)'
    context.fill()
    context.strokeStyle = 'rgba(255, 255, 255, 0.16)'
    context.lineWidth = 2
    context.stroke()

    let companyLogo = null

    if (profile.companyLogoDataUrl?.startsWith('data:')) {
      try {
        companyLogo = await loadImage(profile.companyLogoDataUrl)
      } catch {
        companyLogo = null
      }
    }

    if (companyLogo) {
      context.save()
      drawRoundedRect(context, 34, 32, 652, 368, 26)
      context.clip()
      context.globalAlpha = 0.1
      drawContainedImage(context, companyLogo, 424, 58, 212, 96)
      context.globalAlpha = 0.055
      drawContainedImage(context, companyLogo, 432, 246, 190, 86)
      context.restore()
    }

    context.save()
    context.beginPath()
    context.arc(142, 152, 74, 0, Math.PI * 2)
    context.clip()

    try {
      if (profile.imageDataUrl?.startsWith('data:')) {
        const image = await loadImage(profile.imageDataUrl)
        context.drawImage(image, 68, 78, 148, 148)
      } else {
        throw new Error('empty profile image')
      }
    } catch {
      context.fillStyle = '#7c3aed'
      context.fillRect(68, 78, 148, 148)
      context.fillStyle = '#ffffff'
      context.font = '700 50px Arial, sans-serif'
      context.textAlign = 'center'
      context.textBaseline = 'middle'
      context.fillText(profileInitials.value, 142, 153)
    }

    context.restore()

    context.strokeStyle = 'rgba(255, 255, 255, 0.34)'
    context.lineWidth = 3
    context.beginPath()
    context.arc(142, 152, 76, 0, Math.PI * 2)
    context.stroke()

    context.textAlign = 'left'
    context.textBaseline = 'alphabetic'
    context.fillStyle = '#ffffff'
    context.font = '700 42px Arial, sans-serif'
    context.fillText(profile.name, 252, 132)

    context.fillStyle = '#c4b5fd'
    context.font = '600 22px Arial, sans-serif'
    context.fillText(profile.role, 254, 170)

    context.fillStyle = 'rgba(255, 255, 255, 0.78)'
    context.font = '500 20px Arial, sans-serif'
    context.fillText(profile.company, 254, 224)
    context.fillText(profile.department, 254, 258)
    context.fillText(profile.phone, 254, 308)
    context.fillText(profile.email, 254, 342)

    if (companyLogo) {
      drawRoundedRect(context, 66, 322, 144, 52, 14)
      context.fillStyle = 'rgba(255, 255, 255, 0.13)'
      context.fill()
      context.strokeStyle = 'rgba(255, 255, 255, 0.22)'
      context.lineWidth = 1
      context.stroke()

      context.save()
      drawRoundedRect(context, 80, 334, 116, 28, 7)
      context.clip()
      context.globalAlpha = 0.92
      drawContainedImage(context, companyLogo, 80, 334, 116, 28)
      context.restore()
    } else {
      context.fillStyle = 'rgba(255, 255, 255, 0.58)'
      context.font = '700 18px Arial, sans-serif'
      context.fillText(profile.company || 'CALLOG', 70, 350)
    }

    const filename = `callog-profile-card-${sanitizeUserKey(profile.name)}.png`
    triggerDownload(canvas.toDataURL('image/png'), filename)

    return true
  }

  return {
    activeUserKey,
    downloadProfileCard,
    generatorMessage,
    generatorPrompt,
    generatorStatus,
    loadUserSettings,
    markImageGenerationReady,
    notifications,
    profile,
    profileCardData,
    profileInitials,
    security,
    setGeneratorPrompt,
    themeUi,
    updateNotification,
    updateProfile,
    updateSecurity,
    updateThemeUi,
  }
})
